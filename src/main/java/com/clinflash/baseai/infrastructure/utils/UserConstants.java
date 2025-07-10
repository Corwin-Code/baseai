package com.clinflash.baseai.infrastructure.utils;

/**
 * <h2>用户常量定义</h2>
 *
 * <p>常量类集中管理系统中的各种常量值，避免魔法数字和重复定义。
 * 这样的设计提高了代码的可维护性和可读性。</p>
 */
public final class UserConstants {

    /**
     * 字段长度限制
     */
    public static final class FieldLengths {
        public static final int MAX_USERNAME_LENGTH = 32;
        public static final int MIN_USERNAME_LENGTH = 3;
        public static final int MAX_EMAIL_LENGTH = 128;
        public static final int MAX_PASSWORD_LENGTH = 64;
        public static final int MIN_PASSWORD_LENGTH = 8;
        public static final int MAX_ORG_NAME_LENGTH = 128;
        public static final int MIN_ORG_NAME_LENGTH = 2;
        public static final int MAX_ROLE_NAME_LENGTH = 64;
        public static final int MAX_AVATAR_URL_LENGTH = 256;
        public static final int MAX_INVITE_CODE_LENGTH = 64;
    }

    /**
     * 业务限制
     */
    public static final class BusinessLimits {
        public static final int MAX_TENANTS_PER_USER = 10;          // 用户最多可加入的租户数
        public static final int MAX_ROLES_PER_USER = 5;            // 用户最多可拥有的全局角色数
        public static final int MAX_MEMBERS_PER_TENANT = 1000;     // 租户最多成员数
        public static final int INVITATION_EXPIRE_DAYS = 7;        // 邀请有效期（天）
        public static final int ACTIVATION_EXPIRE_HOURS = 24;      // 激活码有效期（小时）
        public static final int PASSWORD_HISTORY_COUNT = 5;        // 密码历史记录数量
        public static final int MAX_LOGIN_ATTEMPTS = 5;            // 最大登录尝试次数
        public static final int ACCOUNT_LOCK_MINUTES = 30;         // 账户锁定时间（分钟）
    }

    /**
     * 分页参数
     */
    public static final class Pagination {
        public static final int DEFAULT_PAGE_SIZE = 20;
        public static final int MAX_PAGE_SIZE = 100;
        public static final int MAX_SEARCH_RESULTS = 1000;
    }

    /**
     * 缓存配置
     */
    public static final class Cache {
        public static final String USER_CACHE = "users";
        public static final String TENANT_CACHE = "tenants";
        public static final String ROLE_CACHE = "roles";
        public static final String PERMISSION_CACHE = "permissions";
        public static final int DEFAULT_CACHE_TTL_MINUTES = 60;
        public static final int PERMISSION_CACHE_TTL_MINUTES = 30;
    }

    /**
     * 角色名称常量
     */
    public static final class RoleNames {
        public static final String SUPER_ADMIN = "SUPER_ADMIN";
        public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
        public static final String TENANT_OWNER = "TENANT_OWNER";
        public static final String TENANT_ADMIN = "TENANT_ADMIN";
        public static final String TENANT_MANAGER = "TENANT_MANAGER";
        public static final String TENANT_MEMBER = "TENANT_MEMBER";
        public static final String GUEST = "GUEST";
    }

    /**
     * 错误消息常量
     */
    public static final class ErrorMessages {
        public static final String USER_NOT_FOUND = "用户不存在";
        public static final String TENANT_NOT_FOUND = "租户不存在";
        public static final String ROLE_NOT_FOUND = "角色不存在";
        public static final String DUPLICATE_USERNAME = "用户名已存在";
        public static final String DUPLICATE_EMAIL = "邮箱已被注册";
        public static final String INVALID_PASSWORD = "密码不正确";
        public static final String WEAK_PASSWORD = "密码强度不足";
        public static final String ACCOUNT_LOCKED = "账户已被锁定";
        public static final String ACCOUNT_DISABLED = "账户已被禁用";
        public static final String INSUFFICIENT_PERMISSIONS = "权限不足";
        public static final String INVITATION_EXPIRED = "邀请已过期";
        public static final String INVITATION_INVALID = "邀请无效";
        public static final String ACTIVATION_CODE_EXPIRED = "激活码已过期";
        public static final String ACTIVATION_CODE_INVALID = "激活码无效";
    }

    /**
     * 审计操作类型
     */
    public static final class AuditActions {
        public static final String USER_REGISTERED = "USER_REGISTERED";
        public static final String USER_ACTIVATED = "USER_ACTIVATED";
        public static final String USER_LOGIN = "USER_LOGIN";
        public static final String USER_LOGOUT = "USER_LOGOUT";
        public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
        public static final String PASSWORD_RESET = "PASSWORD_RESET";
        public static final String PROFILE_UPDATED = "PROFILE_UPDATED";
        public static final String TENANT_CREATED = "TENANT_CREATED";
        public static final String TENANT_UPDATED = "TENANT_UPDATED";
        public static final String MEMBER_INVITED = "MEMBER_INVITED";
        public static final String MEMBER_JOINED = "MEMBER_JOINED";
        public static final String MEMBER_REMOVED = "MEMBER_REMOVED";
        public static final String ROLE_ASSIGNED = "ROLE_ASSIGNED";
        public static final String ROLE_REMOVED = "ROLE_REMOVED";
        public static final String PERMISSION_GRANTED = "PERMISSION_GRANTED";
        public static final String PERMISSION_REVOKED = "PERMISSION_REVOKED";
    }

    /**
     * 默认配置值
     */
    public static final class Defaults {
        public static final String DEFAULT_PLAN_CODE = "BASIC";
        public static final String DEFAULT_LANGUAGE = "zh-CN";
        public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
        public static final String DEFAULT_AVATAR_SERVICE = "https://api.dicebear.com/7.x/avataaars/svg";
        public static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 480; // 8小时
        public static final int DEFAULT_REMEMBER_ME_DAYS = 30;
    }

    /**
     * 正则表达式模式
     */
    public static final class Patterns {
        public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_-]{3,32}$";
        public static final String EMAIL_PATTERN = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        public static final String STRONG_PASSWORD_PATTERN =
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        public static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";
        public static final String INVITE_CODE_PATTERN = "^[A-Za-z0-9-_]{6,32}$";
    }

    /**
     * HTTP状态相关
     */
    public static final class HttpStatus {
        public static final int SUCCESS = 200;
        public static final int CREATED = 201;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int CONFLICT = 409;
        public static final int TOO_MANY_REQUESTS = 429;
        public static final int INTERNAL_SERVER_ERROR = 500;
    }

    // 私有构造函数，防止实例化
    private UserConstants() {
        throw new UnsupportedOperationException("常量类不能被实例化");
    }
}