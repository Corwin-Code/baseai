package com.clinflash.baseai.application.user.service;

import com.clinflash.baseai.application.user.command.*;
import com.clinflash.baseai.application.user.dto.*;
import com.clinflash.baseai.domain.user.model.*;
import com.clinflash.baseai.domain.user.repository.*;
import com.clinflash.baseai.domain.user.service.UserDomainService;
import com.clinflash.baseai.infrastructure.exception.UserBusinessException;
import com.clinflash.baseai.infrastructure.exception.UserTechnicalException;
import com.clinflash.baseai.domain.audit.service.AuditService;
import com.clinflash.baseai.infrastructure.external.email.EmailService;
import com.clinflash.baseai.infrastructure.external.sms.SmsService;
import com.clinflash.baseai.infrastructure.utils.UserConstants;
import com.clinflash.baseai.infrastructure.utils.UserUtils;
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
 * 就像一个经验丰富的人力资源总监，它需要协调用户注册、租户管理、权限分配、
 * 邀请流程等各个环节，确保每个操作都符合业务规则和安全要求。</p>
 *
 * <p><b>主要职责：</b></p>
 * <ul>
 * <li><b>流程编排：</b>协调复杂的多步骤业务流程</li>
 * <li><b>事务管理：</b>确保数据一致性和完整性</li>
 * <li><b>权限验证：</b>在操作前进行必要的权限检查</li>
 * <li><b>异常处理：</b>将技术异常转换为业务异常</li>
 * <li><b>外部集成：</b>协调邮件服务、短信服务等外部依赖</li>
 * </ul>
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
     * <p>5. 审计记录：记录注册行为用于安全分析</p>
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
                throw new UserBusinessException(
                        "DUPLICATE_USERNAME",
                        "用户名已存在：" + cmd.username()
                );
            }

            if (userRepo.existsByEmail(cmd.email())) {
                throw new UserBusinessException(
                        "DUPLICATE_EMAIL",
                        "邮箱已被注册：" + cmd.email()
                );
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

            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("USER_REGISTRATION_ERROR", "用户注册处理失败", e);
        }
    }

    /**
     * 激活用户账户
     */
    @Transactional
    public UserProfileDTO activateUser(ActivateUserCommand cmd) {
        log.info("激活用户账户: email={}", cmd.email());

        try {
            // 验证激活码的有效性
            if (!userDomainService.isValidActivationCode(cmd.email(), cmd.activationCode())) {
                throw new UserBusinessException(
                        "INVALID_ACTIVATION_CODE",
                        "激活码无效或已过期"
                );
            }

            // 查找用户
            User user = userRepo.findByEmail(cmd.email())
                    .orElseThrow(() -> new UserBusinessException(
                            "USER_NOT_FOUND",
                            "用户不存在：" + cmd.email()
                    ));

            // 激活账户
            User activatedUser = user.activate();
            activatedUser = userRepo.save(activatedUser);

            // 记录审计日志
            recordAuditLog("USER_ACTIVATED", activatedUser.id(), "用户账户激活");

            return toUserProfileDTO(activatedUser);

        } catch (Exception e) {
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("USER_ACTIVATION_ERROR", "用户激活失败", e);
        }
    }

    /**
     * 获取用户资料
     */
    public UserProfileDTO getUserProfile(Long userId) {
        log.debug("获取用户资料: userId={}", userId);

        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new UserBusinessException(
                            "USER_NOT_FOUND",
                            UserConstants.ErrorMessages.USER_NOT_FOUND
                    ));

            return toUserProfileDTO(user);

        } catch (Exception e) {
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("GET_USER_PROFILE_ERROR", "获取用户资料失败", e);
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
                    .orElseThrow(() -> new UserBusinessException(
                            "USER_NOT_FOUND",
                            UserConstants.ErrorMessages.USER_NOT_FOUND
                    ));

            // 如果要更新邮箱，需要检查唯一性
            if (!user.email().equals(cmd.email()) && userRepo.existsByEmail(cmd.email())) {
                throw new UserBusinessException(
                        "DUPLICATE_EMAIL",
                        "邮箱已被其他用户使用：" + cmd.email()
                );
            }

            // 更新用户信息
            User updatedUser = user.updateProfile(cmd.email(), cmd.avatarUrl());
            updatedUser = userRepo.save(updatedUser);

            // 记录审计日志
            recordAuditLog("USER_PROFILE_UPDATED", updatedUser.id(), "用户资料更新");

            return toUserProfileDTO(updatedUser);

        } catch (Exception e) {
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("UPDATE_USER_PROFILE_ERROR", "更新用户资料失败", e);
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
                    .orElseThrow(() -> new UserBusinessException(
                            "USER_NOT_FOUND",
                            UserConstants.ErrorMessages.USER_NOT_FOUND
                    ));

            // 验证原密码
            if (!passwordEncoder.matches(cmd.oldPassword(), user.passwordHash())) {
                throw new UserBusinessException(
                        "INVALID_OLD_PASSWORD",
                        "原密码错误"
                );
            }

            // 验证新密码强度
            if (!UserUtils.isStrongPassword(cmd.newPassword())) {
                throw new UserBusinessException(
                        "WEAK_PASSWORD",
                        "新密码强度不足，请使用至少8位包含大小写字母、数字和特殊字符的密码"
                );
            }

            // 更新密码
            String newHashedPassword = passwordEncoder.encode(cmd.newPassword());
            User updatedUser = user.changePassword(newHashedPassword);
            userRepo.save(updatedUser);

            // 记录审计日志
            recordAuditLog("PASSWORD_CHANGED", user.id(), "用户修改密码");

        } catch (Exception e) {
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("CHANGE_PASSWORD_ERROR", "修改密码失败", e);
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
                    .orElseThrow(() -> new UserBusinessException(
                            "USER_NOT_FOUND",
                            "创建者用户不存在"
                    ));

            // 检查组织名称唯一性
            if (tenantRepo.existsByOrgName(cmd.orgName())) {
                throw new UserBusinessException(
                        "DUPLICATE_TENANT_NAME",
                        "组织名称已存在：" + cmd.orgName()
                );
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
                    .orElseThrow(() -> new UserTechnicalException(
                            "ADMIN_ROLE_NOT_FOUND",
                            "系统管理员角色未找到"
                    ));

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
            if (e instanceof UserBusinessException || e instanceof UserTechnicalException) {
                throw e;
            }
            throw new UserTechnicalException("CREATE_TENANT_ERROR", "创建租户失败", e);
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
                throw new UserBusinessException(
                        "USER_NOT_FOUND",
                        UserConstants.ErrorMessages.USER_NOT_FOUND
                );
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
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("GET_USER_TENANTS_ERROR", "获取用户租户列表失败", e);
        }
    }

    /**
     * 获取租户详情
     */
    public TenantDetailDTO getTenantDetail(Long tenantId) {
        log.debug("获取租户详情: tenantId={}", tenantId);

        try {
            Tenant tenant = tenantRepo.findById(tenantId)
                    .orElseThrow(() -> new UserBusinessException(
                            "TENANT_NOT_FOUND",
                            "租户不存在"
                    ));

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
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("GET_TENANT_DETAIL_ERROR", "获取租户详情失败", e);
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
                    .orElseThrow(() -> new UserBusinessException(
                            "TENANT_NOT_FOUND",
                            "租户不存在"
                    ));

            // 如果要更新组织名称，检查唯一性
            if (!tenant.orgName().equals(cmd.orgName()) &&
                    tenantRepo.existsByOrgName(cmd.orgName())) {
                throw new UserBusinessException(
                        "DUPLICATE_TENANT_NAME",
                        "组织名称已存在：" + cmd.orgName()
                );
            }

            // 更新租户信息
            Tenant updatedTenant = tenant.updateInfo(cmd.orgName(), cmd.planCode(), cmd.expireAt());
            updatedTenant = tenantRepo.save(updatedTenant);

            // 记录审计日志
            recordAuditLog("TENANT_UPDATED", updatedTenant.id(), "租户信息更新");

            return toTenantDTO(updatedTenant, null);

        } catch (Exception e) {
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("UPDATE_TENANT_ERROR", "更新租户信息失败", e);
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
                    .orElseThrow(() -> new UserBusinessException(
                            "TENANT_NOT_FOUND",
                            "租户不存在"
                    ));

            // 验证角色存在
            Role role = roleRepo.findById(cmd.roleId())
                    .orElseThrow(() -> new UserBusinessException(
                            "ROLE_NOT_FOUND",
                            "角色不存在"
                    ));

            // 验证邀请者权限
            if (!userDomainService.canInviteMembers(cmd.inviterId(), tenantId)) {
                throw new UserBusinessException(
                        "INSUFFICIENT_PERMISSIONS",
                        "您没有邀请成员的权限"
                );
            }

            // 检查是否已经是成员
            Optional<User> existingUser = userRepo.findByEmail(cmd.email());
            if (existingUser.isPresent()) {
                boolean isMember = userTenantRepo.existsByUserIdAndTenantId(
                        existingUser.get().id(), tenantId);
                if (isMember) {
                    throw new UserBusinessException(
                            "ALREADY_MEMBER",
                            "用户已经是该租户的成员"
                    );
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
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("INVITE_MEMBER_ERROR", "邀请成员失败", e);
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
                throw new UserBusinessException(
                        "TENANT_NOT_FOUND",
                        "租户不存在"
                );
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
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("GET_TENANT_MEMBERS_ERROR", "获取租户成员失败", e);
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
                    .orElseThrow(() -> new UserBusinessException(
                            "MEMBER_NOT_FOUND",
                            "用户不是该租户的成员"
                    ));

            // 验证新角色存在
            Role newRole = roleRepo.findById(cmd.roleId())
                    .orElseThrow(() -> new UserBusinessException(
                            "ROLE_NOT_FOUND",
                            "角色不存在"
                    ));

            // 验证操作者权限
            if (!userDomainService.canManageMembers(cmd.operatorId(), tenantId)) {
                throw new UserBusinessException(
                        "INSUFFICIENT_PERMISSIONS",
                        "您没有管理成员的权限"
                );
            }

            // 更新角色
            UserTenant updatedUserTenant = userTenant.updateRole(cmd.roleId());
            userTenantRepo.save(updatedUserTenant);

            // 获取用户信息构建返回对象
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new UserTechnicalException(
                            "USER_NOT_FOUND",
                            "用户数据不一致"
                    ));

            // 记录审计日志
            recordAuditLog("MEMBER_ROLE_UPDATED", tenantId,
                    "更新成员角色：" + user.username() + " -> " + newRole.name());

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
            if (e instanceof UserBusinessException || e instanceof UserTechnicalException) {
                throw e;
            }
            throw new UserTechnicalException("UPDATE_MEMBER_ROLE_ERROR", "更新成员角色失败", e);
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
                    .orElseThrow(() -> new UserBusinessException(
                            "MEMBER_NOT_FOUND",
                            "用户不是该租户的成员"
                    ));

            // 验证操作者权限
            if (!userDomainService.canManageMembers(operatorId, tenantId)) {
                throw new UserBusinessException(
                        "INSUFFICIENT_PERMISSIONS",
                        "您没有管理成员的权限"
                );
            }

            // 不能移除自己
            if (userId.equals(operatorId)) {
                throw new UserBusinessException(
                        "CANNOT_REMOVE_SELF",
                        "不能移除自己"
                );
            }

            // 检查是否是最后一个管理员
            if (userDomainService.isLastAdmin(userId, tenantId)) {
                throw new UserBusinessException(
                        "CANNOT_REMOVE_LAST_ADMIN",
                        "不能移除最后一个管理员"
                );
            }

            // 移除成员
            userTenantRepo.delete(userTenant);

            // 获取用户信息用于日志
            User user = userRepo.findById(userId).orElse(null);
            String username = user != null ? user.username() : "未知用户";

            // 记录审计日志
            recordAuditLog("MEMBER_REMOVED", tenantId, "移除成员：" + username);

        } catch (Exception e) {
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("REMOVE_MEMBER_ERROR", "移除成员失败", e);
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
            throw new UserTechnicalException("GET_ROLES_ERROR", "获取角色列表失败", e);
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
            throw new UserTechnicalException("GET_USER_GLOBAL_ROLES_ERROR", "获取用户全局角色失败", e);
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
                throw new UserBusinessException(
                        "USER_NOT_FOUND",
                        UserConstants.ErrorMessages.USER_NOT_FOUND
                );
            }

            // 验证角色存在
            List<Role> roles = roleRepo.findByIds(new ArrayList<>(cmd.roleIds()));
            if (roles.size() != cmd.roleIds().size()) {
                throw new UserBusinessException(
                        "ROLE_NOT_FOUND",
                        "部分角色不存在"
                );
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

        } catch (Exception e) {
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("ASSIGN_GLOBAL_ROLES_ERROR", "分配全局角色失败", e);
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
                throw new UserBusinessException(
                        "INVALID_INVITATION_TOKEN",
                        "邀请链接无效或已过期"
                );
            }

            if ("ACCEPT".equals(cmd.action())) {
                return acceptInvitation(invitationInfo, cmd.userId());
            } else if ("REJECT".equals(cmd.action())) {
                return rejectInvitation(invitationInfo);
            } else {
                throw new UserBusinessException(
                        "INVALID_INVITATION_ACTION",
                        "无效的邀请操作"
                );
            }

        } catch (Exception e) {
            if (e instanceof UserBusinessException) {
                throw e;
            }
            throw new UserTechnicalException("RESPOND_INVITATION_ERROR", "处理邀请响应失败", e);
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
            throw new UserTechnicalException("GET_PENDING_INVITATIONS_ERROR", "获取待处理邀请失败", e);
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
            throw new UserTechnicalException("SEARCH_USERS_ERROR", "搜索用户失败", e);
        }
    }

    /**
     * 获取用户统计
     */
    public UserStatisticsDTO getUserStatistics(Long tenantId, String timeRange) {
        try {
            return userDomainService.calculateUserStatistics(tenantId, timeRange);

        } catch (Exception e) {
            throw new UserTechnicalException("GET_USER_STATISTICS_ERROR", "获取用户统计失败", e);
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 验证注册请求
     */
    private void validateRegistrationRequest(RegisterUserCommand cmd) {
        // 验证用户名格式
        if (!UserUtils.isValidUsername(cmd.username())) {
            throw new UserBusinessException(
                    "INVALID_USERNAME",
                    "用户名格式不正确，只能包含字母、数字、下划线和连字符，长度3-32位"
            );
        }

        // 验证邮箱格式
        if (!UserUtils.isValidEmail(cmd.email())) {
            throw new UserBusinessException(
                    "INVALID_EMAIL_FORMAT",
                    "邮箱格式不正确"
            );
        }

        // 验证密码强度
        if (!UserUtils.isStrongPassword(cmd.password())) {
            throw new UserBusinessException(
                    "WEAK_PASSWORD",
                    "密码强度不足，请使用至少8位包含大小写字母、数字和特殊字符的密码"
            );
        }

        // 验证密码确认
        if (!cmd.password().equals(cmd.confirmPassword())) {
            throw new UserBusinessException(
                    "PASSWORD_MISMATCH",
                    "两次输入的密码不一致"
            );
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
                .orElseThrow(() -> new UserBusinessException(
                        "USER_NOT_FOUND",
                        UserConstants.ErrorMessages.USER_NOT_FOUND
                ));

        // 检查是否已经是成员
        boolean isMember = userTenantRepo.existsByUserIdAndTenantId(userId, invitationInfo.tenantId());
        if (isMember) {
            throw new UserBusinessException(
                    "ALREADY_MEMBER",
                    "您已经是该组织的成员"
            );
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