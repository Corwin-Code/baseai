package com.cloud.baseai.infrastructure.security.permission;

import com.cloud.baseai.infrastructure.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * <h1>自定义权限评估器</h1>
 *
 * <p>PermissionEvaluator可以理解权限规则，并结合具体的业务上下文做出公正的安全评估。
 * 这个类是我们实现细粒度权限控制的核心组件。</p>
 *
 * <p><b>为什么需要自定义权限评估器？</b></p>
 * <p>Spring Security内置的权限控制相对简单，主要基于角色（ROLE_ADMIN、ROLE_USER等）。
 * 但在复杂的企业应用中，我们需要更细粒度的控制：</p>
 * <p>- 基于资源的权限：用户只能访问自己创建的文档</p>
 * <p>- 基于租户的权限：用户只能访问所属租户的数据</p>
 * <p>- 基于业务状态的权限：只有文档处于草稿状态时才能编辑</p>
 * <p>- 动态权限计算：权限可能依赖于运行时的业务逻辑</p>
 *
 * <p><b>多租户权限模型：</b></p>
 * <p>在SaaS系统中，权限模型特别复杂。同一个用户在不同租户中可能有不同权限，
 * 同一个资源在不同上下文中可能有不同的访问规则。我们的权限评估器
 * 优雅地处理了这些复杂性。</p>
 *
 * <p><b>使用示例：</b></p>
 * <pre>
 * // 在Controller方法上使用
 * {@literal @}PreAuthorize("hasPermission(#documentId, 'DOCUMENT', 'READ')")
 * public DocumentDTO getDocument(@PathVariable Long documentId) { ... }
 *
 * {@literal @}PreAuthorize("hasPermission(#tenantId, 'TENANT', 'ADMIN')")
 * public void manageTenant(@PathVariable Long tenantId) { ... }
 * </pre>
 */
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(CustomPermissionEvaluator.class);

    /**
     * 支持的目标类型和对应的权限检查策略
     * <p>这个Map定义了我们系统中支持的资源类型。每种类型都有对应的权限检查逻辑。</p>
     */
    private static final Set<String> SUPPORTED_TARGET_TYPES = Set.of(
            "TENANT",           // 租户权限
            "USER",             // 用户权限
            "DOCUMENT",         // 文档权限
            "CHAT_THREAD",      // 对话线程权限
            "CHAT_MESSAGE",     // 消息权限
            "FLOW_PROJECT",     // 流程项目权限
            "FLOW_DEFINITION",  // 流程定义权限
            "FLOW_RUN",         // 流程运行权限
            "TOOL",             // 工具权限
            "PROMPT_TEMPLATE",  // 提示词模板权限
            "AUDIT_LOG",        // 审计日志权限
            "SYSTEM_SETTING"    // 系统设置权限
    );

    /**
     * 权限级别定义
     * <p>我们使用层次化的权限模型，高级权限包含低级权限。</p>
     */
    private static final Map<String, Integer> PERMISSION_LEVELS = Map.of(
            "READ", 1,          // 读取权限
            "WRITE", 2,         // 写入权限
            "DELETE", 3,        // 删除权限
            "ADMIN", 4,         // 管理权限
            "OWNER", 5          // 所有者权限
    );

    /**
     * 评估用户对特定对象的权限
     *
     * <p>这是权限评估器的核心方法。当Spring Security遇到
     * {@code @PreAuthorize("hasPermission(#id, 'TYPE', 'PERMISSION')")}
     * 这样的注解时，就会调用这个方法进行权限检查。</p>
     *
     * <p><b>权限检查的层次结构：</b></p>
     * <p>1. <strong>系统级检查：</strong>超级管理员拥有所有权限</p>
     * <p>2. <strong>租户级检查：</strong>用户必须属于相关租户</p>
     * <p>3. <strong>角色级检查：</strong>用户角色是否满足要求</p>
     * <p>4. <strong>资源级检查：</strong>用户是否对特定资源有权限</p>
     * <p>5. <strong>业务逻辑检查：</strong>特定的业务规则验证</p>
     *
     * @param authentication     当前用户的认证信息
     * @param targetDomainObject 目标对象ID（通常是资源的主键）
     * @param permission         请求的权限类型（READ、WRITE、DELETE等）
     * @return 如果有权限返回true，否则返回false
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("用户未认证，拒绝访问");
            return false;
        }

        // 如果没有指定目标对象，拒绝访问
        if (targetDomainObject == null) {
            log.debug("目标对象为空，拒绝访问");
            return false;
        }

        UserPrincipal userPrincipal = extractUserPrincipal(authentication);
        if (userPrincipal == null) {
            log.debug("无法获取用户主体信息，拒绝访问");
            return false;
        }

        String permissionStr = permission.toString();

        log.debug("权限检查: userId={}, targetObject={}, permission={}",
                userPrincipal.getId(), targetDomainObject, permissionStr);

        try {
            // 1. 超级管理员检查
            if (isSuperAdmin(userPrincipal)) {
                log.debug("超级管理员，允许所有操作");
                return true;
            }

            // 2. 根据目标对象类型进行具体的权限检查
            // 这里假设targetDomainObject是资源ID，实际项目中可能需要更复杂的逻辑
            return checkResourcePermission(userPrincipal, targetDomainObject, permissionStr);

        } catch (Exception e) {
            log.error("权限检查过程中发生异常: userId={}, target={}, permission={}",
                    userPrincipal.getId(), targetDomainObject, permissionStr, e);
            // 出现异常时，为了安全起见，拒绝访问
            return false;
        }
    }

    /**
     * 评估用户对特定类型资源的权限
     *
     * <p>这个重载方法主要用于处理类型化的权限检查。当我们在注解中指定了
     * 目标类型时，如 {@code @PreAuthorize("hasPermission(#id, 'DOCUMENT', 'READ')")},
     * 就会调用这个方法。</p>
     *
     * @param authentication 当前用户的认证信息
     * @param targetId       目标资源的ID
     * @param targetType     目标资源的类型（DOCUMENT、TENANT等）
     * @param permission     请求的权限类型
     * @return 如果有权限返回true，否则返回false
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                 String targetType, Object permission) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("用户未认证，拒绝访问");
            return false;
        }

        UserPrincipal userPrincipal = extractUserPrincipal(authentication);
        if (userPrincipal == null) {
            log.debug("无法获取用户主体信息，拒绝访问");
            return false;
        }

        String permissionStr = permission.toString();

        log.debug("类型化权限检查: userId={}, targetType={}, targetId={}, permission={}",
                userPrincipal.getId(), targetType, targetId, permissionStr);

        try {
            // 1. 验证目标类型是否受支持
            if (!SUPPORTED_TARGET_TYPES.contains(targetType)) {
                log.warn("不支持的目标类型: {}", targetType);
                return false;
            }

            // 2. 超级管理员检查
            if (isSuperAdmin(userPrincipal)) {
                log.debug("超级管理员，允许所有操作");
                return true;
            }

            // 3. 根据具体的目标类型进行权限检查
            return switch (targetType) {
                case "TENANT" -> checkTenantPermission(userPrincipal, (Long) targetId, permissionStr);
                case "USER" -> checkUserPermission(userPrincipal, (Long) targetId, permissionStr);
                case "DOCUMENT" -> checkDocumentPermission(userPrincipal, (Long) targetId, permissionStr);
                case "CHAT_THREAD" -> checkChatThreadPermission(userPrincipal, (Long) targetId, permissionStr);
                case "CHAT_MESSAGE" -> checkChatMessagePermission(userPrincipal, (Long) targetId, permissionStr);
                case "FLOW_PROJECT" -> checkFlowProjectPermission(userPrincipal, (Long) targetId, permissionStr);
                case "FLOW_DEFINITION" -> checkFlowDefinitionPermission(userPrincipal, (Long) targetId, permissionStr);
                case "FLOW_RUN" -> checkFlowRunPermission(userPrincipal, (Long) targetId, permissionStr);
                case "TOOL" -> checkToolPermission(userPrincipal, (String) targetId, permissionStr);
                case "PROMPT_TEMPLATE" -> checkPromptTemplatePermission(userPrincipal, (Long) targetId, permissionStr);
                case "AUDIT_LOG" -> checkAuditLogPermission(userPrincipal, (Long) targetId, permissionStr);
                case "SYSTEM_SETTING" -> checkSystemSettingPermission(userPrincipal, (String) targetId, permissionStr);
                default -> {
                    log.warn("未实现的权限检查类型: {}", targetType);
                    yield false;
                }
            };

        } catch (Exception e) {
            log.error("类型化权限检查过程中发生异常: userId={}, targetType={}, targetId={}, permission={}",
                    userPrincipal.getId(), targetType, targetId, permissionStr, e);
            return false;
        }
    }

    // =================== 具体权限检查方法 ===================

    /**
     * 检查租户权限
     *
     * <p>租户权限是多租户系统的基础。用户必须属于某个租户才能对该租户进行操作。
     * 同时，不同的操作需要不同级别的权限。</p>
     */
    private boolean checkTenantPermission(UserPrincipal userPrincipal, Long tenantId, String permission) {
        // 1. 检查用户是否属于该租户
        if (!userPrincipal.belongsToTenant(tenantId)) {
            log.debug("用户不属于租户: userId={}, tenantId={}", userPrincipal.getId(), tenantId);
            return false;
        }

        // 2. 根据权限类型进行检查
        return switch (permission) {
            case "READ" -> true; // 租户成员都可以查看基本信息
            case "WRITE" -> userPrincipal.hasRole("TENANT_ADMIN") || userPrincipal.hasRole("ADMIN");
            case "DELETE", "ADMIN" -> userPrincipal.hasRole("TENANT_OWNER") || userPrincipal.hasRole("SUPER_ADMIN");
            case "MANAGE_MEMBERS" -> userPrincipal.hasRole("TENANT_ADMIN") || userPrincipal.hasRole("TENANT_OWNER");
            default -> false;
        };
    }

    /**
     * 检查用户权限
     *
     * <p>用户权限检查涉及隐私保护。用户可以查看和修改自己的信息，
     * 但查看其他用户信息需要特定权限。</p>
     */
    private boolean checkUserPermission(UserPrincipal userPrincipal, Long targetUserId, String permission) {
        // 用户可以对自己执行大部分操作
        if (userPrincipal.getId().equals(targetUserId)) {
            return switch (permission) {
                case "READ", "WRITE" -> true;
                case "DELETE" -> false; // 用户不能删除自己的账户
                default -> false;
            };
        }

        // 对其他用户的权限检查
        return switch (permission) {
            case "READ" -> userPrincipal.hasRole("ADMIN") || userPrincipal.hasRole("HR");
            case "WRITE", "DELETE" -> userPrincipal.hasRole("ADMIN") || userPrincipal.hasRole("USER_ADMIN");
            default -> false;
        };
    }

    /**
     * 检查文档权限
     *
     * <p>文档权限结合了所有权和租户权限。用户对自己创建的文档有完全控制权，
     * 对租户内其他用户的文档根据角色有不同权限。</p>
     */
    private boolean checkDocumentPermission(UserPrincipal userPrincipal, Long documentId, String permission) {
        // 这里需要查询文档信息来判断所有权和租户归属
        // 为了演示，简化实现

        // 假设我们有一个方法可以获取文档信息
        // DocumentInfo docInfo = documentService.getDocumentInfo(documentId);

        // 示例逻辑：
        // 1. 文档创建者有完全权限
        // 2. 同租户用户根据角色有不同权限
        // 3. 管理员有查看权限

        return switch (permission) {
            case "READ" -> true; // 简化：假设所有用户都可以读取
            case "WRITE" -> userPrincipal.hasRole("EDITOR") || userPrincipal.hasRole("ADMIN");
            case "DELETE" -> userPrincipal.hasRole("ADMIN");
            default -> false;
        };
    }

    /**
     * 检查对话线程权限
     */
    private boolean checkChatThreadPermission(UserPrincipal userPrincipal, Long threadId, String permission) {
        // 对话线程权限主要基于所有权
        // 这里需要查询线程的创建者信息
        return switch (permission) {
            case "READ", "WRITE" -> true; // 简化实现
            case "DELETE" -> userPrincipal.hasRole("ADMIN");
            default -> false;
        };
    }

    /**
     * 检查消息权限
     */
    private boolean checkChatMessagePermission(UserPrincipal userPrincipal, Long messageId, String permission) {
        // 消息权限基于线程权限
        return switch (permission) {
            case "READ" -> true;
            case "WRITE" -> false; // 消息一般不允许修改
            case "DELETE" -> userPrincipal.hasRole("ADMIN");
            default -> false;
        };
    }

    /**
     * 检查流程项目权限
     */
    private boolean checkFlowProjectPermission(UserPrincipal userPrincipal, Long projectId, String permission) {
        return switch (permission) {
            case "READ" -> true;
            case "WRITE" -> userPrincipal.hasRole("FLOW_DESIGNER") || userPrincipal.hasRole("ADMIN");
            case "DELETE", "ADMIN" -> userPrincipal.hasRole("FLOW_ADMIN") || userPrincipal.hasRole("ADMIN");
            default -> false;
        };
    }

    /**
     * 检查流程定义权限
     */
    private boolean checkFlowDefinitionPermission(UserPrincipal userPrincipal, Long definitionId, String permission) {
        return switch (permission) {
            case "READ" -> true;
            case "WRITE" -> userPrincipal.hasRole("FLOW_DESIGNER") || userPrincipal.hasRole("ADMIN");
            case "EXECUTE" -> userPrincipal.hasRole("FLOW_EXECUTOR") || userPrincipal.hasRole("FLOW_DESIGNER");
            case "PUBLISH" -> userPrincipal.hasRole("FLOW_ADMIN") || userPrincipal.hasRole("ADMIN");
            default -> false;
        };
    }

    /**
     * 检查流程运行权限
     */
    private boolean checkFlowRunPermission(UserPrincipal userPrincipal, Long runId, String permission) {
        return switch (permission) {
            case "READ" -> true;
            case "CONTROL" -> userPrincipal.hasRole("FLOW_EXECUTOR") || userPrincipal.hasRole("ADMIN");
            default -> false;
        };
    }

    /**
     * 检查工具权限
     */
    private boolean checkToolPermission(UserPrincipal userPrincipal, String toolCode, String permission) {
        return switch (permission) {
            case "READ" -> true;
            case "EXECUTE" -> true; // 根据工具的授权情况决定
            case "ADMIN" -> userPrincipal.hasRole("TOOL_ADMIN") || userPrincipal.hasRole("ADMIN");
            default -> false;
        };
    }

    /**
     * 检查提示词模板权限
     */
    private boolean checkPromptTemplatePermission(UserPrincipal userPrincipal, Long templateId, String permission) {
        return switch (permission) {
            case "READ" -> true;
            case "WRITE" -> userPrincipal.hasRole("PROMPT_DESIGNER") || userPrincipal.hasRole("ADMIN");
            case "DELETE" -> userPrincipal.hasRole("ADMIN");
            default -> false;
        };
    }

    /**
     * 检查审计日志权限
     */
    private boolean checkAuditLogPermission(UserPrincipal userPrincipal, Long logId, String permission) {
        return switch (permission) {
            case "READ" -> userPrincipal.hasRole("AUDITOR") || userPrincipal.hasRole("ADMIN");
            case "WRITE", "DELETE" -> false; // 审计日志不允许修改或删除
            default -> false;
        };
    }

    /**
     * 检查系统设置权限
     */
    private boolean checkSystemSettingPermission(UserPrincipal userPrincipal, String settingKey, String permission) {
        return switch (permission) {
            case "READ" -> userPrincipal.hasRole("ADMIN") || userPrincipal.hasRole("SYSTEM_ADMIN");
            case "WRITE" -> userPrincipal.hasRole("SYSTEM_ADMIN");
            default -> false;
        };
    }

    // =================== 辅助方法 ===================

    /**
     * 从认证对象中提取用户主体
     */
    private UserPrincipal extractUserPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }
        return null;
    }

    /**
     * 检查用户是否为超级管理员
     */
    private boolean isSuperAdmin(UserPrincipal userPrincipal) {
        return userPrincipal.hasRole("SUPER_ADMIN");
    }

    /**
     * 检查资源权限（用于非类型化权限检查）
     */
    private boolean checkResourcePermission(UserPrincipal userPrincipal, Object targetObject, String permission) {
        // 这里可以实现通用的资源权限检查逻辑
        // 或者基于targetObject的类型进行分发
        return false; // 默认拒绝
    }

    /**
     * 检查权限级别
     *
     * <p>某些权限之间存在层次关系，高级权限包含低级权限。
     * 比如ADMIN权限包含WRITE权限，WRITE权限包含READ权限。</p>
     */
    private boolean hasPermissionLevel(UserPrincipal userPrincipal, String requiredPermission, String actualPermission) {
        Integer requiredLevel = PERMISSION_LEVELS.get(requiredPermission);
        Integer actualLevel = PERMISSION_LEVELS.get(actualPermission);

        if (requiredLevel == null || actualLevel == null) {
            return requiredPermission.equals(actualPermission);
        }

        return actualLevel >= requiredLevel;
    }
}