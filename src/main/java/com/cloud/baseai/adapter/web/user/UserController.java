package com.cloud.baseai.adapter.web.user;

import com.cloud.baseai.application.user.command.*;
import com.cloud.baseai.application.user.dto.*;
import com.cloud.baseai.application.user.service.UserAppService;
import com.cloud.baseai.infrastructure.exception.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h1>用户管理REST控制器</h1>
 *
 * <p>这个控制器是整个用户管理系统的门户，它负责处理用户注册、租户管理、权限分配等各种人员管理事务。
 * 在多租户SaaS系统中，用户管理不仅仅是简单的增删改查，还涉及复杂的权限模型和组织架构管理。</p>
 *
 * <p><b>多租户架构的精髓：</b></p>
 * <p>想象一下，我们的系统就像一座大型的商务大厦，每个租户就是其中的一个公司。
 * 每个公司都有自己的员工、部门和权限体系，但它们都共享着同一套基础设施。
 * 用户可能在多个公司中担任不同的角色，这就需要灵活的多对多关系管理。</p>
 *
 * <p><b>核心功能体系：</b></p>
 * <ul>
 * <li><b>用户生命周期管理：</b>从注册、激活到最终的账户管理</li>
 * <li><b>租户组织管理：</b>创建、配置和管理企业组织</li>
 * <li><b>权限角色系统：</b>灵活的RBAC权限控制机制</li>
 * <li><b>成员邀请协作：</b>团队建设和协作管理</li>
 * <li><b>安全审计追踪：</b>完整的操作日志和安全监控</li>
 * </ul>
 *
 * <p><b>安全性考虑：</b></p>
 * <p>用户管理是整个系统安全的基石。我们采用了分层的安全策略：
 * 接口级的权限控制、方法级的访问验证、数据级的租户隔离等。
 * 每一个操作都会被仔细审计，确保系统的安全性和合规性。</p>
 */
@RestController
@RequestMapping("/api/v1/users")
@Validated
@Tag(name = "用户管理", description = "User Management APIs - 提供完整的多租户用户管理解决方案")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserAppService userAppService;

    public UserController(UserAppService userAppService) {
        this.userAppService = userAppService;
    }

    // =================== 用户注册与认证 ===================

    /**
     * 用户注册
     *
     * <p>用户注册是整个用户旅程的起点。这个过程不仅仅是创建一个账户，
     * 更是建立用户与系统之间信任关系的开始。我们会进行各种验证：
     * 邮箱格式、密码强度、用户名唯一性等，确保每个新用户都能安全地加入我们的平台。</p>
     */
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "用户注册",
            description = "创建新用户账户。支持邮箱验证、密码强度检查和用户名唯一性验证。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "用户注册成功",
                    content = @Content(schema = @Schema(implementation = RegisterUserResult.class))),
            @ApiResponse(responseCode = "400", description = "注册信息无效"),
            @ApiResponse(responseCode = "409", description = "用户名或邮箱已存在")
    })
    public ResponseEntity<ApiResult<RegisterUserResult>> registerUser(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户注册信息",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "用户注册示例",
                                    value = """
                                            {
                                              "username": "john.doe",
                                              "email": "john.doe@example.com",
                                              "password": "SecurePass123!",
                                              "confirmPassword": "SecurePass123!",
                                              "inviteCode": "INV-123456"
                                            }
                                            """
                            )
                    )
            ) RegisterUserCommand cmd) {

        log.info("用户注册请求: username={}, email={}", cmd.username(), cmd.email());

        RegisterUserResult result = userAppService.registerUser(cmd);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, "用户注册成功，请检查邮箱进行账户激活"));
    }

    /**
     * 激活用户账户
     */
    @PostMapping("/activate")
    @Operation(summary = "激活用户账户", description = "通过邮箱验证码激活新注册的用户账户。")
    public ResponseEntity<ApiResult<UserProfileDTO>> activateUser(
            @Valid @RequestBody ActivateUserCommand cmd) {

        log.info("用户激活请求: email={}", cmd.email());

        UserProfileDTO result = userAppService.activateUser(cmd);
        return ResponseEntity.ok(ApiResult.success(result, "账户激活成功"));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    @Operation(summary = "获取用户资料", description = "获取当前登录用户的详细资料信息。")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserProfileDTO>> getUserProfile(
            @RequestParam Long userId) {

        log.debug("获取用户资料: userId={}", userId);

        UserProfileDTO result = userAppService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 更新用户资料
     */
    @PutMapping("/profile")
    @Operation(summary = "更新用户资料", description = "更新当前用户的基本信息，如邮箱、头像等。")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserProfileDTO>> updateUserProfile(
            @Valid @RequestBody UpdateUserProfileCommand cmd) {

        log.info("更新用户资料: userId={}", cmd.userId());

        UserProfileDTO result = userAppService.updateUserProfile(cmd);
        return ResponseEntity.ok(ApiResult.success(result, "用户资料更新成功"));
    }

    /**
     * 修改密码
     */
    @PostMapping("/password/change")
    @Operation(summary = "修改密码", description = "用户主动修改账户密码，需要验证原密码。")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<Void>> changePassword(
            @Valid @RequestBody ChangePasswordCommand cmd) {

        log.info("用户修改密码: userId={}", cmd.userId());

        userAppService.changePassword(cmd);
        return ResponseEntity.ok(ApiResult.success(null, "密码修改成功"));
    }

    // =================== 租户管理 ===================

    /**
     * 创建租户
     *
     * <p>创建租户是一个重要的业务操作，它代表着一个新的组织加入我们的平台。
     * 这个过程涉及初始化组织的基础数据、分配默认的管理员权限、
     * 设置基础配置等多个步骤，就像为新公司设立完整的办公环境。</p>
     */
    @PostMapping(value = "/tenants", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "创建租户",
            description = "创建新的租户组织。创建者自动成为该租户的管理员。"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<TenantDTO>> createTenant(
            @Valid @RequestBody CreateTenantCommand cmd) {

        log.info("创建租户请求: orgName={}, creatorId={}", cmd.orgName(), cmd.creatorId());

        TenantDTO result = userAppService.createTenant(cmd);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, "租户创建成功"));
    }

    /**
     * 获取用户的租户列表
     *
     * <p>一个用户可能同时属于多个组织，就像一个人可能同时在多个公司担任顾问。
     * 这个接口帮助用户查看自己所属的所有组织，以及在每个组织中的角色。</p>
     */
    @GetMapping("/tenants")
    @Operation(summary = "获取用户租户列表", description = "获取当前用户所属的所有租户及其角色信息。")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<List<UserTenantDTO>>> getUserTenants(
            @Parameter(description = "用户ID", required = true)
            @RequestParam Long userId) {

        log.debug("获取用户租户列表: userId={}", userId);

        List<UserTenantDTO> result = userAppService.getUserTenants(userId);
        return ResponseEntity.ok(ApiResult.success(result,
                String.format("找到 %d 个租户", result.size())));
    }

    /**
     * 获取租户详情
     */
    @GetMapping("/tenants/{tenantId}")
    @Operation(summary = "获取租户详情", description = "获取指定租户的详细信息，包括成员统计等。")
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<TenantDetailDTO>> getTenantDetail(
            @PathVariable Long tenantId) {

        log.debug("获取租户详情: tenantId={}", tenantId);

        TenantDetailDTO result = userAppService.getTenantDetail(tenantId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 更新租户信息
     */
    @PutMapping("/tenants/{tenantId}")
    @Operation(summary = "更新租户信息", description = "更新租户的基本信息，如组织名称、套餐等。")
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'ADMIN')")
    public ResponseEntity<ApiResult<TenantDTO>> updateTenant(
            @PathVariable Long tenantId,
            @Valid @RequestBody UpdateTenantCommand cmd) {

        log.info("更新租户信息: tenantId={}", tenantId);

        TenantDTO result = userAppService.updateTenant(cmd);
        return ResponseEntity.ok(ApiResult.success(result, "租户信息更新成功"));
    }

    // =================== 成员管理 ===================

    /**
     * 邀请成员
     *
     * <p>邀请新成员是团队建设的重要环节。我们的邀请机制既要保证安全性，
     * 又要提供良好的用户体验。被邀请人会收到包含邀请链接的邮件，
     * 可以选择接受或拒绝邀请。</p>
     */
    @PostMapping("/tenants/{tenantId}/members/invite")
    @Operation(
            summary = "邀请成员",
            description = "向指定邮箱发送租户邀请。被邀请人可通过邮件链接加入组织。"
    )
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'MANAGE_MEMBERS')")
    public ResponseEntity<ApiResult<MemberInvitationDTO>> inviteMember(
            @PathVariable Long tenantId,
            @Valid @RequestBody InviteMemberCommand cmd) {

        log.info("邀请成员: tenantId={}, email={}, role={}",
                tenantId, cmd.email(), cmd.roleId());

        MemberInvitationDTO result = userAppService.inviteMember(tenantId, cmd);

        return ResponseEntity.ok(ApiResult.success(result, "邀请已发送"));
    }

    /**
     * 获取租户成员列表
     */
    @GetMapping("/tenants/{tenantId}/members")
    @Operation(summary = "获取租户成员", description = "分页获取租户下的所有成员列表，包含角色和状态信息。")
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<PageResultDTO<TenantMemberDTO>>> getTenantMembers(
            @PathVariable Long tenantId,

            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "页码不能小于0")
            Integer page,

            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页大小不能小于1")
            @Max(value = 100, message = "每页大小不能超过100")
            Integer size,

            @Parameter(description = "成员状态过滤", example = "ACTIVE")
            @RequestParam(required = false) String status,

            @Parameter(description = "角色过滤")
            @RequestParam(required = false) Long roleId) {

        log.debug("获取租户成员: tenantId={}, page={}, size={}", tenantId, page, size);

        PageResultDTO<TenantMemberDTO> result = userAppService.getTenantMembers(
                tenantId, page, size, status, roleId);

        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 更新成员角色
     */
    @PutMapping("/tenants/{tenantId}/members/{userId}/role")
    @Operation(summary = "更新成员角色", description = "修改租户内某个成员的角色权限。")
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'MANAGE_MEMBERS')")
    public ResponseEntity<ApiResult<TenantMemberDTO>> updateMemberRole(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleCommand cmd) {

        log.info("更新成员角色: tenantId={}, userId={}, newRoleId={}",
                tenantId, userId, cmd.roleId());

        TenantMemberDTO result = userAppService.updateMemberRole(tenantId, userId, cmd);
        return ResponseEntity.ok(ApiResult.success(result, "成员角色更新成功"));
    }

    /**
     * 移除租户成员
     */
    @DeleteMapping("/tenants/{tenantId}/members/{userId}")
    @Operation(summary = "移除租户成员", description = "将指定用户从租户中移除。被移除的用户将失去对该租户的所有访问权限。")
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'MANAGE_MEMBERS')")
    public ResponseEntity<ApiResult<Void>> removeTenantMember(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            @RequestParam Long operatorId) {

        log.info("移除租户成员: tenantId={}, userId={}, operatorId={}",
                tenantId, userId, operatorId);

        userAppService.removeTenantMember(tenantId, userId, operatorId);
        return ResponseEntity.ok(ApiResult.success(null, "成员已移除"));
    }

    // =================== 角色权限管理 ===================

    /**
     * 获取系统角色列表
     *
     * <p>角色是权限管理的核心概念。就像军队中的军衔制度一样，
     * 不同的角色拥有不同的权限和责任。我们的角色系统支持灵活的权限组合。</p>
     */
    @GetMapping("/roles")
    @Operation(summary = "获取角色列表", description = "获取系统中所有可用的角色及其权限描述。")
    public ResponseEntity<ApiResult<List<RoleDTO>>> getRoles() {

        List<RoleDTO> result = userAppService.getAllRoles();
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 获取用户的全局角色
     */
    @GetMapping("/{userId}/roles")
    @Operation(summary = "获取用户全局角色", description = "获取用户在系统级别的角色权限。")
    @PreAuthorize("hasPermission(#userId, 'USER', 'READ') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResult<List<RoleDTO>>> getUserGlobalRoles(
            @PathVariable Long userId) {

        List<RoleDTO> result = userAppService.getUserGlobalRoles(userId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 分配全局角色
     */
    @PostMapping("/{userId}/roles")
    @Operation(summary = "分配全局角色", description = "为用户分配系统级别的角色权限。需要超级管理员权限。")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResult<Void>> assignGlobalRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AssignGlobalRolesCommand cmd) {

        log.info("分配全局角色: userId={}, roles={}", userId, cmd.roleIds());

        userAppService.assignGlobalRoles(userId, cmd);
        return ResponseEntity.ok(ApiResult.success(null, "角色分配成功"));
    }

    // =================== 邀请管理 ===================

    /**
     * 处理邀请响应
     *
     * <p>当用户收到邀请邮件并点击链接时，会来到这个接口。
     * 用户可以选择接受邀请加入组织，或者拒绝邀请。</p>
     */
    @PostMapping("/invitations/{invitationToken}/respond")
    @Operation(summary = "响应邀请", description = "接受或拒绝租户邀请。")
    public ResponseEntity<ApiResult<InvitationResponseDTO>> respondToInvitation(
            @PathVariable String invitationToken,
            @Valid @RequestBody RespondToInvitationCommand cmd) {

        log.info("响应邀请: token={}, action={}", invitationToken, cmd.action());

        InvitationResponseDTO result = userAppService.respondToInvitation(invitationToken, cmd);
        return ResponseEntity.ok(ApiResult.success(result,
                "ACCEPT".equals(cmd.action()) ? "邀请已接受" : "邀请已拒绝"));
    }

    /**
     * 获取待处理邀请
     */
    @GetMapping("/invitations/pending")
    @Operation(summary = "获取待处理邀请", description = "获取当前用户的所有待处理邀请。")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<List<PendingInvitationDTO>>> getPendingInvitations(
            @RequestParam String email) {

        List<PendingInvitationDTO> result = userAppService.getPendingInvitations(email);
        return ResponseEntity.ok(ApiResult.success(result,
                String.format("找到 %d 个待处理邀请", result.size())));
    }

    // =================== 用户搜索和管理 ===================

    /**
     * 搜索用户
     *
     * <p>在大型组织中，快速找到目标用户是一个重要功能。
     * 我们的搜索支持用户名、邮箱等多字段模糊匹配。</p>
     */
    @GetMapping("/search")
    @Operation(summary = "搜索用户", description = "根据关键词搜索用户，支持用户名、邮箱等字段模糊匹配。")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<PageResultDTO<UserSearchResultDTO>>> searchUsers(
            @Parameter(description = "搜索关键词", example = "john")
            @RequestParam String keyword,

            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,

            @Parameter(description = "租户ID过滤")
            @RequestParam(required = false) Long tenantId) {

        log.debug("搜索用户: keyword={}, page={}, size={}", keyword, page, size);

        PageResultDTO<UserSearchResultDTO> result = userAppService.searchUsers(
                keyword, page, size, tenantId);

        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取用户统计", description = "获取用户相关的统计信息，包括注册趋势、活跃度等。")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<UserStatisticsDTO>> getUserStatistics(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String timeRange) {

        UserStatisticsDTO result = userAppService.getUserStatistics(tenantId, timeRange);
        return ResponseEntity.ok(ApiResult.success(result));
    }
}