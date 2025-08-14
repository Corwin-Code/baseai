package com.cloud.baseai.application.user.service;

import com.cloud.baseai.application.auth.service.AuthAppService;
import com.cloud.baseai.application.user.command.*;
import com.cloud.baseai.application.user.dto.*;
import com.cloud.baseai.domain.audit.service.AuditService;
import com.cloud.baseai.domain.user.model.*;
import com.cloud.baseai.domain.user.repository.*;
import com.cloud.baseai.domain.user.service.UserDomainService;
import com.cloud.baseai.infrastructure.exception.BusinessException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.exception.UserException;
import com.cloud.baseai.infrastructure.external.email.EmailService;
import com.cloud.baseai.infrastructure.external.sms.SmsService;
import com.cloud.baseai.infrastructure.utils.UserUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * <h2>用户应用服务</h2>
 *
 * <p>这是用户管理系统的核心应用服务，负责编排复杂的用户管理业务流程。
 * 与认证系统协作，确保用户管理和认证功能的无缝集成。</p>
 *
 * <p><b>与认证系统的协作关系：</b></p>
 * <ul>
 * <li><b>用户注册：</b>创建用户账户，为后续认证做准备</li>
 * <li><b>用户激活：</b>激活账户后用户可以正常登录</li>
 * <li><b>密码管理：</b>确保密码修改后认证系统能正确验证</li>
 * <li><b>权限管理：</b>角色变更后需要刷新用户的认证状态</li>
 */
@Service
public class UserAppService {

    private static final Logger log = LoggerFactory.getLogger(UserAppService.class);

    // 领域仓储
    private final UserRepository userRepo;
    private final TenantRepository tenantRepo;
    private final RoleRepository roleRepo;
    private final UserTenantRepository userTenantRepo;
    private final UserRoleRepository userRoleRepo;

    // 领域服务
    private final UserDomainService userDomainService;

    // 基础设施服务
    private final PasswordEncoder passwordEncoder;

    // 可选的外部服务
    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private SmsService smsService;

    @Autowired(required = false)
    private AuditService auditService;

    // 认证应用服务 - 用于协作
    @Autowired(required = false)
    private AuthAppService authAppService;

    // 异步执行器
    private final ExecutorService asyncExecutor;

    public UserAppService(
            UserRepository userRepo,
            TenantRepository tenantRepo,
            RoleRepository roleRepo,
            UserTenantRepository userTenantRepo,
            UserRoleRepository userRoleRepo,
            UserDomainService userDomainService,
            PasswordEncoder passwordEncoder) {

        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
        this.roleRepo = roleRepo;
        this.userTenantRepo = userTenantRepo;
        this.userRoleRepo = userRoleRepo;
        this.userDomainService = userDomainService;
        this.passwordEncoder = passwordEncoder;

        // 创建异步执行器
        this.asyncExecutor = Executors.newFixedThreadPool(5);
    }

    // =================== 用户注册与认证 ===================

    /**
     * 用户注册
     *
     * <p>用户注册是一个复杂的业务流程，需要考虑多个方面：</p>
     * <p>1. 数据验证：确保用户输入的信息格式正确、完整</p>
     * <p>2. 唯一性检查：防止重复的用户名或邮箱</p>
     * <p>3. 密码安全：密码加密和强度验证</p>
     * <p>4. 邮箱验证：发送激活邮件确保邮箱真实性</p>
     * <p>5. 认证准备：确保注册后的用户能够正常进行认证</p>
     */
    @Transactional
    public RegisterUserResult registerUser(RegisterUserCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("开始用户注册流程: username={}, email={}", cmd.username(), cmd.email());

        try {
            // 第一步：全面的业务验证
            validateRegistrationRequest(cmd);

            // 第二步：检查用户名和邮箱的唯一性
            if (userRepo.existsByUsername(cmd.username())) {
                throw UserException.duplicateUsername(cmd.username());
            }

            if (userRepo.existsByEmail(cmd.email())) {
                throw new UserException(ErrorCode.BIZ_USER_015, cmd.email());
            }

            // 第三步：密码加密处理
            String hashedPassword = passwordEncoder.encode(cmd.password());

            // 第四步：创建用户领域对象
            User newUser = User.create(
                    cmd.username(),
                    hashedPassword,
                    cmd.email(),
                    cmd.avatarUrl()
            );

            // 第五步：持久化用户数据
            User savedUser = userRepo.save(newUser);

            // 第六步：处理邀请码（如果提供）
            if (cmd.inviteCode() != null && !cmd.inviteCode().trim().isEmpty()) {
                handleInviteCodeRegistration(savedUser, cmd.inviteCode());
            }

            // 第七步：发送激活邮件（异步处理）
            sendActivationEmailAsync(savedUser.email(), savedUser.username());

            // 第八步：记录审计日志
            recordAuditLog("USER_REGISTERED", savedUser.id(), "用户注册成功");

            log.info("用户注册成功: userId={}, 耗时={}ms",
                    savedUser.id(), System.currentTimeMillis() - startTime);

            return new RegisterUserResult(
                    savedUser.id(),
                    savedUser.username(),
                    savedUser.email(),
                    "注册成功，请检查邮箱进行账户激活"
            );

        } catch (Exception e) {
            log.error("用户注册失败: username={}, email={}", cmd.username(), cmd.email(), e);

            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_USER_011)
                    .cause(e)
                    .context("operation", "registerUser")
                    .context("userId", cmd.email())
                    .build();
        }
    }

    /**
     * 激活用户账户
     *
     * <p>用户激活是认证流程的重要环节。激活成功后，用户就可以正常登录了。</p>
     */
    @Transactional
    public UserProfileDTO activateUser(ActivateUserCommand cmd) {
        log.info("激活用户账户: email={}", cmd.email());

        try {
            // 验证激活码的有效性
            if (!userDomainService.isValidActivationCode(cmd.email(), cmd.activationCode())) {
                throw new UserException(ErrorCode.BIZ_USER_031);
            }

            // 查找用户
            User user = userRepo.findByEmail(cmd.email())
                    .orElseThrow(() -> UserException.userNotFound(cmd.email()));

            // 激活账户
            User activatedUser = user.activate();
            activatedUser = userRepo.save(activatedUser);

            // 记录审计日志
            recordAuditLog("USER_ACTIVATED", activatedUser.id(), "用户账户激活");

            // 通知认证系统用户已激活（如果需要）
            notifyUserActivated(activatedUser);

            return toUserProfileDTO(activatedUser);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_USER_032)
                    .cause(e)
                    .context("operation", "activateUser")
                    .context("userId", cmd.email())
                    .build();
        }
    }

    /**
     * 获取用户资料
     */
    public UserProfileDTO getUserProfile(Long userId) {
        log.debug("获取用户资料: userId={}", userId);

        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> UserException.userNotFound(String.valueOf(userId)));

            return toUserProfileDTO(user);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_USER_009)
                    .cause(e)
                    .context("operation", "getUserProfile")
                    .context("userId", userId)
                    .build();
        }
    }

    /**
     * 更新用户资料
     */
    @Transactional
    public UserProfileDTO updateUserProfile(UpdateUserProfileCommand cmd) {
        log.info("更新用户资料: userId={}", cmd.userId());

        try {
            User user = userRepo.findById(cmd.userId())
                    .orElseThrow(() -> UserException.userNotFound(cmd.email()));

            // 如果要更新邮箱，需要检查唯一性
            if (!user.email().equals(cmd.email()) && userRepo.existsByEmail(cmd.email())) {
                throw new UserException(ErrorCode.BIZ_USER_015, cmd.email());
            }

            // 更新用户信息
            User updatedUser = user.updateProfile(cmd.email(), cmd.avatarUrl());
            updatedUser = userRepo.save(updatedUser);

            // 记录审计日志
            recordAuditLog("USER_PROFILE_UPDATED", updatedUser.id(), "用户资料更新");

            return toUserProfileDTO(updatedUser);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_USER_010)
                    .cause(e)
                    .context("operation", "updateUserProfile")
                    .context("userId", cmd.userId())
                    .build();
        }
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(ChangePasswordCommand cmd) {
        log.info("用户修改密码: userId={}", cmd.userId());

        try {
            User user = userRepo.findById(cmd.userId())
                    .orElseThrow(() -> UserException.userNotFound(String.valueOf(cmd.userId())));

            // 验证原密码
            if (!passwordEncoder.matches(cmd.oldPassword(), user.passwordHash())) {
                throw new UserException(ErrorCode.BIZ_USER_016);
            }

            // 验证新密码强度
            if (!UserUtils.isStrongPassword(cmd.newPassword())) {
                throw new UserException(ErrorCode.BIZ_USER_018);
            }

            // 更新密码
            String newHashedPassword = passwordEncoder.encode(cmd.newPassword());
            User updatedUser = user.changePassword(newHashedPassword);
            userRepo.save(updatedUser);

            // 记录审计日志
            recordAuditLog("PASSWORD_CHANGED", user.id(), "用户修改密码");

            // 出于安全考虑，撤销用户的所有登录令牌
            invalidateAllUserTokens(user.id());

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_USER_020)
                    .cause(e)
                    .context("operation", "changePassword")
                    .context("userId", cmd.userId())
                    .build();
        }
    }

    // =================== 租户管理 ===================

    /**
     * 创建租户
     *
     * <p>创建租户是一个重要的业务流程，包含以下几个关键步骤：</p>
     * <p>1. 验证创建者的权限和资格</p>
     * <p>2. 检查组织名称的唯一性</p>
     * <p>3. 创建租户基础数据</p>
     * <p>4. 将创建者设置为租户管理员</p>
     * <p>5. 初始化默认的角色和权限</p>
     */
    @Transactional
    public TenantDTO createTenant(CreateTenantCommand cmd) {
        log.info("创建租户: orgName={}, creatorId={}", cmd.orgName(), cmd.creatorId());

        try {
            // 验证创建者存在且有权限
            User creator = userRepo.findById(cmd.creatorId())
                    .orElseThrow(() -> UserException.userNotFound(String.valueOf(cmd.creatorId())));

            // 检查组织名称唯一性
            if (tenantRepo.existsByOrgName(cmd.orgName())) {
                throw new UserException(ErrorCode.BIZ_TENANT_002, cmd.orgName());
            }

            // 创建租户
            Tenant newTenant = Tenant.create(
                    cmd.orgName(),
                    cmd.planCode(),
                    cmd.expireAt()
            );
            Tenant savedTenant = tenantRepo.save(newTenant);

            // 获取管理员角色
            Role adminRole = roleRepo.findByName("TENANT_ADMIN")
                    .orElseThrow(() -> new UserException(ErrorCode.BIZ_ROLE_002));

            // 将创建者设置为租户管理员
            UserTenant userTenant = UserTenant.create(
                    creator.id(),
                    savedTenant.id(),
                    adminRole.id(),
                    TenantMemberStatus.ACTIVE
            );
            userTenantRepo.save(userTenant);

            // 记录审计日志
            recordAuditLog("TENANT_CREATED", savedTenant.id(),
                    "租户创建成功，创建者：" + creator.username());

            return toTenantDTO(savedTenant, getCreatorInfo(creator));

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_TENANT_011)
                    .cause(e)
                    .context("operation", "createTenant")
                    .context("orgName", cmd.orgName())
                    .build();
        }
    }

    /**
     * 获取用户的租户列表
     */
    public List<UserTenantDTO> getUserTenants(Long userId) {
        log.debug("获取用户租户列表: userId={}", userId);

        try {
            // 验证用户存在
            if (!userRepo.existsById(userId)) {
                throw UserException.userNotFound(String.valueOf(userId));
            }

            // 查询用户的租户关联
            List<UserTenant> userTenants = userTenantRepo.findByUserId(userId);

            if (userTenants.isEmpty()) {
                return new ArrayList<>();
            }

            // 获取租户信息
            Set<Long> tenantIds = userTenants.stream()
                    .map(UserTenant::tenantId)
                    .collect(Collectors.toSet());

            List<Tenant> tenants = tenantRepo.findByIds(new ArrayList<>(tenantIds));
            Map<Long, Tenant> tenantMap = tenants.stream()
                    .collect(Collectors.toMap(Tenant::id, tenant -> tenant));

            // 获取角色信息
            Set<Long> roleIds = userTenants.stream()
                    .map(UserTenant::roleId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<Role> roles = roleRepo.findByIds(new ArrayList<>(roleIds));
            Map<Long, Role> roleMap = roles.stream()
                    .collect(Collectors.toMap(Role::id, role -> role));

            // 构建结果
            return userTenants.stream()
                    .map(userTenant -> {
                        Tenant tenant = tenantMap.get(userTenant.tenantId());
                        Role role = roleMap.get(userTenant.roleId());

                        return new UserTenantDTO(
                                tenant.id(),
                                tenant.orgName(),
                                tenant.planCode(),
                                role != null ? role.name() : null,
                                role != null ? role.label() : null,
                                userTenant.status().name(),
                                userTenant.joinedAt()
                        );
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_TENANT_014)
                    .cause(e)
                    .context("operation", "getUserTenants")
                    .context("userId", userId)
                    .build();
        }
    }

    /**
     * 获取租户详情
     */
    public TenantDetailDTO getTenantDetail(Long tenantId) {
        log.debug("获取租户详情: tenantId={}", tenantId);

        try {
            Tenant tenant = tenantRepo.findById(tenantId)
                    .orElseThrow(() -> UserException.tenantNotFound(String.valueOf(tenantId)));

            // 统计成员数量
            long memberCount = userTenantRepo.countByTenantId(tenantId);

            // 统计各状态成员数量
            Map<String, Long> membersByStatus = userTenantRepo.countByTenantIdGroupByStatus(tenantId);

            return new TenantDetailDTO(
                    tenant.id(),
                    tenant.orgName(),
                    tenant.planCode(),
                    tenant.expireAt(),
                    (int) memberCount,
                    membersByStatus,
                    tenant.createdAt(),
                    tenant.updatedAt()
            );

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_TENANT_013)
                    .cause(e)
                    .context("operation", "getTenantDetail")
                    .context("tenantId", tenantId)
                    .build();
        }
    }

    /**
     * 更新租户信息
     */
    @Transactional
    public TenantDTO updateTenant(UpdateTenantCommand cmd) {
        log.info("更新租户信息: tenantId={}", cmd.tenantId());

        try {
            Tenant tenant = tenantRepo.findById(cmd.tenantId())
                    .orElseThrow(() -> UserException.tenantNotFound(String.valueOf(cmd.tenantId())));

            // 如果要更新组织名称，检查唯一性
            if (!tenant.orgName().equals(cmd.orgName()) &&
                    tenantRepo.existsByOrgName(cmd.orgName())) {
                throw new UserException(ErrorCode.BIZ_TENANT_002, cmd.orgName());
            }

            // 更新租户信息
            Tenant updatedTenant = tenant.updateInfo(cmd.orgName(), cmd.planCode(), cmd.expireAt());
            updatedTenant = tenantRepo.save(updatedTenant);

            // 记录审计日志
            recordAuditLog("TENANT_UPDATED", updatedTenant.id(), "租户信息更新");

            return toTenantDTO(updatedTenant, null);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_TENANT_012)
                    .cause(e)
                    .context("operation", "updateTenant")
                    .context("tenantId", cmd.tenantId())
                    .build();
        }
    }

    // =================== 成员管理 ===================

    /**
     * 邀请成员
     */
    @Transactional
    public MemberInvitationDTO inviteMember(Long tenantId, InviteMemberCommand cmd) {
        log.info("邀请成员: tenantId={}, email={}", tenantId, cmd.email());

        try {
            // 验证租户存在
            Tenant tenant = tenantRepo.findById(tenantId)
                    .orElseThrow(() -> UserException.tenantNotFound(String.valueOf(tenantId)));

            // 验证角色存在
            Role role = roleRepo.findById(cmd.roleId())
                    .orElseThrow(() -> new UserException(ErrorCode.BIZ_ROLE_001));

            // 验证邀请者权限
            if (!userDomainService.canInviteMembers(cmd.inviterId(), tenantId)) {
                throw new UserException(ErrorCode.BIZ_TENANT_009);
            }

            // 检查是否已经是成员
            Optional<User> existingUser = userRepo.findByEmail(cmd.email());
            if (existingUser.isPresent()) {
                boolean isMember = userTenantRepo.existsByUserIdAndTenantId(
                        existingUser.get().id(), tenantId);
                if (isMember) {
                    throw new UserException(ErrorCode.BIZ_TENANT_007);
                }
            }

            // 生成邀请令牌
            String invitationToken = userDomainService.generateInvitationToken(
                    tenantId, cmd.email(), cmd.roleId(), cmd.inviterId());

            // 发送邀请邮件
            sendInvitationEmailAsync(cmd.email(), tenant.orgName(), invitationToken);

            // 记录审计日志
            recordAuditLog("MEMBER_INVITED", tenantId,
                    "邀请成员：" + cmd.email() + "，角色：" + role.name());

            return new MemberInvitationDTO(
                    invitationToken,
                    cmd.email(),
                    tenant.orgName(),
                    role.label(),
                    OffsetDateTime.now().plusDays(7), // 7天有效期
                    "PENDING"
            );

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_TENANT_010)
                    .cause(e)
                    .context("operation", "inviteMember")
                    .context("inviter", cmd.inviterId())
                    .build();
        }
    }

    /**
     * 获取租户成员列表
     */
    public PageResultDTO<TenantMemberDTO> getTenantMembers(Long tenantId, int page, int size,
                                                           String status, Long roleId) {
        log.debug("获取租户成员: tenantId={}, page={}, size={}", tenantId, page, size);

        try {
            // 验证租户存在
            if (!tenantRepo.existsById(tenantId)) {
                throw UserException.tenantNotFound(String.valueOf(tenantId));
            }

            // 查询成员列表
            List<UserTenant> userTenants = userTenantRepo.findByTenantIdWithFilters(
                    tenantId, status, roleId, page, size);

            long total = userTenantRepo.countByTenantIdWithFilters(tenantId, status, roleId);

            if (userTenants.isEmpty()) {
                return new PageResultDTO<>(new ArrayList<>(), total, page, size);
            }

            // 获取用户信息
            Set<Long> userIds = userTenants.stream()
                    .map(UserTenant::userId)
                    .collect(Collectors.toSet());

            List<User> users = userRepo.findByIds(new ArrayList<>(userIds));
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::id, user -> user));

            // 获取角色信息
            Set<Long> roleIds = userTenants.stream()
                    .map(UserTenant::roleId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<Role> roles = roleRepo.findByIds(new ArrayList<>(roleIds));
            Map<Long, Role> roleMap = roles.stream()
                    .collect(Collectors.toMap(Role::id, role -> role));

            // 构建结果
            List<TenantMemberDTO> members = userTenants.stream()
                    .map(userTenant -> {
                        User user = userMap.get(userTenant.userId());
                        Role role = roleMap.get(userTenant.roleId());

                        return new TenantMemberDTO(
                                user.id(),
                                user.username(),
                                user.email(),
                                user.avatarUrl(),
                                role != null ? role.name() : null,
                                role != null ? role.label() : null,
                                userTenant.status().name(),
                                userTenant.joinedAt(),
                                user.lastLoginAt()
                        );
                    })
                    .collect(Collectors.toList());

            return new PageResultDTO<>(members, total, page, size);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_TENANT_005)
                    .cause(e)
                    .context("operation", "getTenantMembers")
                    .context("tenantId", tenantId)
                    .build();
        }
    }

    /**
     * 更新成员角色
     */
    @Transactional
    public TenantMemberDTO updateMemberRole(Long tenantId, Long userId, UpdateMemberRoleCommand cmd) {
        log.info("更新成员角色: tenantId={}, userId={}, newRoleId={}",
                tenantId, userId, cmd.roleId());

        try {
            // 验证成员关系存在
            UserTenant userTenant = userTenantRepo.findByUserIdAndTenantId(userId, tenantId)
                    .orElseThrow(() -> new UserException(ErrorCode.BIZ_TENANT_006));

            // 验证新角色存在
            Role newRole = roleRepo.findById(cmd.roleId())
                    .orElseThrow(() -> new UserException(ErrorCode.BIZ_ROLE_001));

            // 验证操作者权限
            if (!userDomainService.canManageMembers(cmd.operatorId(), tenantId)) {
                throw new UserException(ErrorCode.BIZ_TENANT_009);
            }

            // 更新角色
            UserTenant updatedUserTenant = userTenant.updateRole(cmd.roleId());
            userTenantRepo.save(updatedUserTenant);

            // 获取用户信息构建返回对象
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new UserException(ErrorCode.BIZ_USER_008));

            // 记录审计日志
            recordAuditLog("MEMBER_ROLE_UPDATED", tenantId,
                    "更新成员角色：" + user.username() + " -> " + newRole.name());

            // 通知认证系统刷新用户权限
            notifyUserPermissionsChanged(userId);

            return new TenantMemberDTO(
                    user.id(),
                    user.username(),
                    user.email(),
                    user.avatarUrl(),
                    newRole.name(),
                    newRole.label(),
                    updatedUserTenant.status().name(),
                    updatedUserTenant.joinedAt(),
                    user.lastLoginAt()
            );

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_ROLE_006)
                    .cause(e)
                    .context("operation", "updateMemberRole")
                    .context("userId", userId)
                    .build();
        }
    }

    /**
     * 移除租户成员
     */
    @Transactional
    public void removeTenantMember(Long tenantId, Long userId, Long operatorId) {
        log.info("移除租户成员: tenantId={}, userId={}, operatorId={}",
                tenantId, userId, operatorId);

        try {
            // 验证成员关系存在
            UserTenant userTenant = userTenantRepo.findByUserIdAndTenantId(userId, tenantId)
                    .orElseThrow(() -> new UserException(ErrorCode.BIZ_TENANT_006));

            // 验证操作者权限
            if (!userDomainService.canManageMembers(operatorId, tenantId)) {
                throw new UserException(ErrorCode.BIZ_TENANT_009);
            }

            // 不能移除自己
            if (userId.equals(operatorId)) {
                throw new UserException(ErrorCode.BIZ_TENANT_016);
            }

            // 检查是否是最后一个管理员
            if (userDomainService.isLastAdmin(userId, tenantId)) {
                throw new UserException(ErrorCode.BIZ_TENANT_017);
            }

            // 移除成员
            userTenantRepo.delete(userTenant);

            // 获取用户信息用于日志
            User user = userRepo.findById(userId).orElse(null);
            String username = user != null ? user.username() : "未知用户";

            // 记录审计日志
            recordAuditLog("MEMBER_REMOVED", tenantId, "移除成员：" + username);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_TENANT_015)
                    .cause(e)
                    .context("operation", "removeTenantMember")
                    .context("userId", userId)
                    .build();
        }
    }

    // =================== 角色权限管理 ===================

    /**
     * 获取所有角色
     */
    public List<RoleDTO> getAllRoles() {
        try {
            List<Role> roles = roleRepo.findAll();
            return roles.stream()
                    .map(this::toRoleDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new UserException(ErrorCode.BIZ_ROLE_004);
        }
    }

    /**
     * 获取用户的全局角色
     */
    public List<RoleDTO> getUserGlobalRoles(Long userId) {
        try {
            List<UserRole> userRoles = userRoleRepo.findByUserId(userId);

            if (userRoles.isEmpty()) {
                return new ArrayList<>();
            }

            Set<Long> roleIds = userRoles.stream()
                    .map(UserRole::roleId)
                    .collect(Collectors.toSet());

            List<Role> roles = roleRepo.findByIds(new ArrayList<>(roleIds));

            return roles.stream()
                    .map(this::toRoleDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new UserException(ErrorCode.BIZ_ROLE_003);
        }
    }

    /**
     * 分配全局角色
     */
    @Transactional
    public void assignGlobalRoles(Long userId, AssignGlobalRolesCommand cmd) {
        log.info("分配全局角色: userId={}, roles={}", userId, cmd.roleIds());

        try {
            // 验证用户存在
            if (!userRepo.existsById(userId)) {
                throw UserException.userNotFound(String.valueOf(userId));
            }

            // 验证角色存在
            List<Role> roles = roleRepo.findByIds(new ArrayList<>(cmd.roleIds()));
            if (roles.size() != cmd.roleIds().size()) {
                throw new UserException(ErrorCode.BIZ_ROLE_001);
            }

            // 删除现有的全局角色
            userRoleRepo.deleteByUserId(userId);

            // 分配新角色
            List<UserRole> newUserRoles = cmd.roleIds().stream()
                    .map(roleId -> UserRole.create(userId, roleId))
                    .collect(Collectors.toList());

            userRoleRepo.saveAll(newUserRoles);

            // 记录审计日志
            String roleNames = roles.stream()
                    .map(Role::name)
                    .collect(Collectors.joining(", "));
            recordAuditLog("GLOBAL_ROLES_ASSIGNED", userId, "分配全局角色：" + roleNames);

            // 通知认证系统刷新用户权限
            notifyUserPermissionsChanged(userId);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_ROLE_005)
                    .cause(e)
                    .context("operation", "assignGlobalRoles")
                    .context("userId", userId)
                    .build();
        }
    }

    // =================== 邀请管理 ===================

    /**
     * 响应邀请
     */
    @Transactional
    public InvitationResponseDTO respondToInvitation(String invitationToken, RespondToInvitationCommand cmd) {
        log.info("响应邀请: token={}, action={}", invitationToken, cmd.action());

        try {
            // 验证邀请令牌
            UserDomainService.InvitationTokenInfo invitationInfo = userDomainService.validateInvitationToken(invitationToken);

            if (invitationInfo == null) {
                throw new UserException(ErrorCode.BIZ_USER_024);
            }

            if ("ACCEPT".equals(cmd.action())) {
                return acceptInvitation(invitationInfo, cmd.userId());
            } else if ("REJECT".equals(cmd.action())) {
                return rejectInvitation(invitationInfo);
            } else {
                throw new UserException(ErrorCode.BIZ_USER_026);
            }

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_USER_028)
                    .cause(e)
                    .context("operation", "respondToInvitation")
                    .context("userId", cmd.userId())
                    .build();
        }
    }

    /**
     * 获取待处理邀请
     */
    public List<PendingInvitationDTO> getPendingInvitations(String email) {
        try {
            return userDomainService.getPendingInvitations(email)
                    .stream()
                    .map(this::toPendingInvitationDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_USER_027)
                    .cause(e)
                    .context("operation", "getPendingInvitations")
                    .context("email", email)
                    .build();
        }
    }

    // =================== 用户搜索和管理 ===================

    /**
     * 搜索用户
     */
    public PageResultDTO<UserSearchResultDTO> searchUsers(String keyword, int page, int size, Long tenantId) {
        log.debug("搜索用户: keyword={}, page={}, size={}, tenantId={}", keyword, page, size, tenantId);

        try {
            List<User> users = userRepo.searchByKeyword(keyword, page, size, tenantId);
            long total = userRepo.countByKeyword(keyword, tenantId);

            List<UserSearchResultDTO> results = users.stream()
                    .map(this::toUserSearchResultDTO)
                    .collect(Collectors.toList());

            return new PageResultDTO<>(results, total, page, size);

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_USER_012)
                    .cause(e)
                    .context("operation", "searchUsers")
                    .context("tenantId", tenantId)
                    .build();
        }
    }

    /**
     * 获取用户统计
     */
    public UserStatisticsDTO getUserStatistics(Long tenantId, String timeRange) {
        try {
            return userDomainService.calculateUserStatistics(tenantId, timeRange);

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_USER_013)
                    .cause(e)
                    .context("operation", "getUserStatistics")
                    .context("tenantId", tenantId)
                    .build();
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 验证注册请求
     */
    private void validateRegistrationRequest(RegisterUserCommand cmd) {
        // 验证用户名格式
        if (!UserUtils.isValidUsername(cmd.username())) {
            throw new UserException(ErrorCode.BIZ_USER_017);
        }

        // 验证邮箱格式
        if (!UserUtils.isValidEmail(cmd.email())) {
            throw new UserException(ErrorCode.BIZ_USER_014);
        }

        // 验证密码强度
        if (!UserUtils.isStrongPassword(cmd.password())) {
            throw new UserException(ErrorCode.BIZ_USER_018);
        }

        // 验证密码确认
        if (!cmd.password().equals(cmd.confirmPassword())) {
            throw new UserException(ErrorCode.BIZ_USER_019);
        }
    }

    /**
     * 处理邀请码注册
     */
    private void handleInviteCodeRegistration(User user, String inviteCode) {
        // 这里应该验证邀请码并自动加入对应的租户
        // 简化实现，实际应该查询邀请码对应的租户和角色
        log.info("处理邀请码注册: userId={}, inviteCode={}", user.id(), inviteCode);
    }

    /**
     * 异步发送激活邮件
     */
    private void sendActivationEmailAsync(String email, String username) {
        if (emailService != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    String activationCode = userDomainService.generateActivationCode(email);
                    emailService.sendActivationEmail(email, username, activationCode);
                    log.info("激活邮件发送成功: email={}", email);
                } catch (Exception e) {
                    log.error("激活邮件发送失败: email={}", email, e);
                }
            }, asyncExecutor);
        }
    }

    /**
     * 异步发送邀请邮件
     */
    private void sendInvitationEmailAsync(String email, String orgName, String invitationToken) {
        if (emailService != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendInvitationEmail(email, orgName, invitationToken);
                    log.info("邀请邮件发送成功: email={}, orgName={}", email, orgName);
                } catch (Exception e) {
                    log.error("邀请邮件发送失败: email={}, orgName={}", email, orgName, e);
                }
            }, asyncExecutor);
        }
    }

    /**
     * 接受邀请
     */
    private InvitationResponseDTO acceptInvitation(UserDomainService.InvitationTokenInfo invitationInfo, Long userId) {
        // 验证用户存在
        User user = userRepo.findById(userId)
                .orElseThrow(() -> UserException.userNotFound(String.valueOf(userId)));

        // 检查是否已经是成员
        boolean isMember = userTenantRepo.existsByUserIdAndTenantId(userId, invitationInfo.tenantId());
        if (isMember) {
            throw new UserException(ErrorCode.BIZ_TENANT_008);
        }

        // 创建成员关系
        UserTenant userTenant = UserTenant.create(
                userId,
                invitationInfo.tenantId(),
                invitationInfo.roleId(),
                TenantMemberStatus.ACTIVE
        );
        userTenantRepo.save(userTenant);

        // 标记邀请为已使用
        userDomainService.markInvitationAsUsed(invitationInfo.token());

        // 获取租户和角色信息
        Tenant tenant = tenantRepo.findById(invitationInfo.tenantId()).orElse(null);
        Role role = roleRepo.findById(invitationInfo.roleId()).orElse(null);

        return new InvitationResponseDTO(
                "ACCEPTED",
                tenant != null ? tenant.orgName() : "未知组织",
                role != null ? role.label() : "未知角色",
                "欢迎加入组织！"
        );
    }

    /**
     * 拒绝邀请
     */
    private InvitationResponseDTO rejectInvitation(UserDomainService.InvitationTokenInfo invitationInfo) {
        // 标记邀请为已拒绝
        userDomainService.markInvitationAsRejected(invitationInfo.token());

        // 获取租户信息
        Tenant tenant = tenantRepo.findById(invitationInfo.tenantId()).orElse(null);

        return new InvitationResponseDTO(
                "REJECTED",
                tenant != null ? tenant.orgName() : "未知组织",
                null,
                "您已拒绝了该邀请"
        );
    }

    /**
     * 记录审计日志
     */
    private void recordAuditLog(String action, Long targetId, String detail) {
        if (auditService != null) {
            try {
                auditService.recordUserAction(action, targetId, detail);
            } catch (Exception e) {
                log.warn("记录审计日志失败: action={}, targetId={}", action, targetId, e);
            }
        }
    }

    /**
     * 通知认证系统用户已激活
     *
     * <p>用户激活后，可能需要通知认证系统进行相应的处理。</p>
     */
    private void notifyUserActivated(User user) {
        try {
            // 这里可以添加与认证系统的集成逻辑
            // 例如：清理缓存、发送事件等
            log.debug("通知认证系统用户已激活: userId={}", user.id());

            // 如果需要，可以调用认证服务的相关方法
            // authAppService.onUserActivated(user.id());

        } catch (Exception e) {
            log.warn("通知认证系统用户激活失败: userId={}", user.id(), e);
        }
    }

    /**
     * 通知认证系统用户权限已变更
     *
     * <p>当用户的角色或权限发生变化时，需要通知认证系统刷新相关信息。</p>
     */
    private void notifyUserPermissionsChanged(Long userId) {
        try {
            log.debug("通知认证系统用户权限已变更: userId={}", userId);

            // 如果集成了缓存系统，可以清理用户权限缓存
            if (authAppService != null) {
                // authAppService.refreshUserPermissions(userId);
            }

            // 或者发布领域事件
            // eventPublisher.publishEvent(new UserPermissionsChangedEvent(userId));

        } catch (Exception e) {
            log.warn("通知认证系统权限变更失败: userId={}", userId, e);
        }
    }

    /**
     * 撤销用户的所有登录令牌
     *
     * <p>在安全敏感操作（如密码修改）后，撤销用户的所有登录令牌。</p>
     */
    private void invalidateAllUserTokens(Long userId) {
        try {
            // 如果集成了JWT服务，可以直接调用
            // jwtTokenService.revokeAllUserTokens(userId);

            log.debug("撤销用户所有登录令牌: userId={}", userId);

        } catch (Exception e) {
            log.warn("撤销用户令牌失败: userId={}", userId, e);
        }
    }

    // =================== DTO转换方法 ===================

    private UserProfileDTO toUserProfileDTO(User user) {
        return new UserProfileDTO(
                user.id(),
                user.username(),
                user.email(),
                user.avatarUrl(),
                user.lastLoginAt(),
                user.createdAt(),
                user.updatedAt()
        );
    }

    private TenantDTO toTenantDTO(Tenant tenant, String creatorName) {
        return new TenantDTO(
                tenant.id(),
                tenant.orgName(),
                tenant.planCode(),
                tenant.expireAt(),
                creatorName,
                tenant.createdAt(),
                tenant.updatedAt()
        );
    }

    private String getCreatorInfo(User creator) {
        return creator.username();
    }

    private RoleDTO toRoleDTO(Role role) {
        return new RoleDTO(
                role.id(),
                role.name(),
                role.label()
        );
    }

    private UserSearchResultDTO toUserSearchResultDTO(User user) {
        return new UserSearchResultDTO(
                user.id(),
                user.username(),
                user.email(),
                user.avatarUrl(),
                user.lastLoginAt(),
                user.createdAt()
        );
    }

    private PendingInvitationDTO toPendingInvitationDTO(UserDomainService.PendingInvitation invitation) {
        return new PendingInvitationDTO(
                invitation.token(),
                invitation.orgName(),
                invitation.roleName(),
                invitation.inviterName(),
                invitation.expiresAt()
        );
    }
}