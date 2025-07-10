package com.clinflash.baseai.infrastructure.persistence.user.mapper;

import com.clinflash.baseai.domain.user.model.*;
import com.clinflash.baseai.infrastructure.persistence.user.entity.*;
import com.clinflash.baseai.infrastructure.utils.KbUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>用户模块映射器</h2>
 *
 * <p>映射器负责在领域对象和JPA实体之间进行转换。这是一个关键的基础设施组件，
 * 它确保了领域层的纯净性，让领域对象不需要了解持久化的技术细节。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>映射器就像翻译官一样，在两个不同的"语言体系"之间进行转换。
 * 领域对象关注业务逻辑和规则，而JPA实体关注数据持久化和ORM映射。
 * 映射器让这两个层面可以独立演化，而不会相互影响。</p>
 *
 * <p><b>职责边界：</b></p>
 * <ul>
 * <li><b>纯粹转换：</b>只负责对象转换，不包含业务逻辑</li>
 * <li><b>双向映射：</b>支持领域对象到实体和实体到领域对象的转换</li>
 * <li><b>空安全：</b>处理null值和边界情况</li>
 * <li><b>批量转换：</b>提供高效的集合转换方法</li>
 * </ul>
 */
@Component
public class UserMapper {

    // =================== User 映射 ===================

    /**
     * 将用户领域对象转换为JPA实体
     *
     * <p>这个转换过程需要特别小心地处理ID字段。对于新创建的领域对象（ID为null），
     * 我们需要确保JPA实体也没有ID，这样数据库才会自动生成新的ID。</p>
     */
    public SysUserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }
        return SysUserEntity.fromDomain(domain);
    }

    /**
     * 将JPA实体转换为用户领域对象
     *
     * <p>从实体到领域对象的转换相对简单，因为实体包含了完整的数据。
     * 我们需要确保转换后的领域对象处于有效状态。</p>
     */
    public User toDomain(SysUserEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.toDomain();
    }

    /**
     * 批量转换用户领域对象列表
     *
     * <p>批量转换在处理大量数据时非常重要。我们使用Java Stream API
     * 来提供高效的函数式转换，同时保持代码的可读性。</p>
     */
    public List<SysUserEntity> toUserEntityList(List<User> domains) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换用户JPA实体列表
     */
    public List<User> toUserDomainList(List<SysUserEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== Tenant 映射 ===================

    /**
     * 将租户领域对象转换为JPA实体
     */
    public SysTenantEntity toEntity(Tenant domain) {
        if (domain == null) {
            return null;
        }
        return SysTenantEntity.fromDomain(domain);
    }

    /**
     * 将JPA实体转换为租户领域对象
     */
    public Tenant toDomain(SysTenantEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.toDomain();
    }

    /**
     * 批量转换租户领域对象列表
     */
    public List<SysTenantEntity> toTenantEntityList(List<Tenant> domains) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换租户JPA实体列表
     */
    public List<Tenant> toTenantDomainList(List<SysTenantEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== Role 映射 ===================

    /**
     * 将角色领域对象转换为JPA实体
     */
    public SysRoleEntity toEntity(Role domain) {
        if (domain == null) {
            return null;
        }
        return SysRoleEntity.fromDomain(domain);
    }

    /**
     * 将JPA实体转换为角色领域对象
     */
    public Role toDomain(SysRoleEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.toDomain();
    }

    /**
     * 批量转换角色领域对象列表
     */
    public List<SysRoleEntity> toRoleEntityList(List<Role> domains) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换角色JPA实体列表
     */
    public List<Role> toRoleDomainList(List<SysRoleEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== UserTenant 映射 ===================

    /**
     * 将用户-租户关联领域对象转换为JPA实体
     *
     * <p>用户-租户关联使用复合主键，映射时需要特别注意主键的处理。
     * 我们需要确保复合主键的所有字段都正确设置。</p>
     */
    public SysUserTenantEntity toEntity(UserTenant domain) {
        if (domain == null) {
            return null;
        }
        return SysUserTenantEntity.fromDomain(domain);
    }

    /**
     * 将JPA实体转换为用户-租户关联领域对象
     */
    public UserTenant toDomain(SysUserTenantEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.toDomain();
    }

    /**
     * 批量转换用户-租户关联领域对象列表
     */
    public List<SysUserTenantEntity> toUserTenantEntityList(List<UserTenant> domains) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换用户-租户关联JPA实体列表
     */
    public List<UserTenant> toUserTenantDomainList(List<SysUserTenantEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== UserRole 映射 ===================

    /**
     * 将用户-角色关联领域对象转换为JPA实体
     */
    public SysUserRoleEntity toEntity(UserRole domain) {
        if (domain == null) {
            return null;
        }
        return SysUserRoleEntity.fromDomain(domain);
    }

    /**
     * 将JPA实体转换为用户-角色关联领域对象
     */
    public UserRole toDomain(SysUserRoleEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.toDomain();
    }

    /**
     * 批量转换用户-角色关联领域对象列表
     */
    public List<SysUserRoleEntity> toUserRoleEntityList(List<UserRole> domains) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换用户-角色关联JPA实体列表
     */
    public List<UserRole> toUserRoleDomainList(List<SysUserRoleEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== 辅助方法 ===================

    /**
     * 安全地转换枚举值
     *
     * <p>枚举转换需要特别小心，因为数据库中可能存在无效的枚举值。
     * 我们提供一个安全的转换方法，当遇到无效值时返回默认值而不是抛出异常。</p>
     */
    public TenantMemberStatus safeConvertToMemberStatus(Integer statusCode) {
        if (statusCode == null) {
            return TenantMemberStatus.PENDING; // 默认状态
        }

        try {
            return TenantMemberStatus.fromCode(statusCode);
        } catch (IllegalArgumentException e) {
            // 记录警告并返回默认状态
            log.warn("无效的成员状态代码: {}, 使用默认状态", statusCode);
            return TenantMemberStatus.PENDING;
        }
    }

    /**
     * 安全地转换枚举到代码
     */
    public Integer safeConvertToStatusCode(TenantMemberStatus status) {
        return status != null ? status.getCode() : TenantMemberStatus.PENDING.getCode();
    }

    /**
     * 验证映射结果的完整性
     *
     * <p>这个方法用于在开发和测试阶段验证映射的正确性。
     * 它检查关键字段是否正确转换，帮助早期发现映射问题。</p>
     */
    public boolean validateUserMapping(User domain, SysUserEntity entity) {
        if (domain == null || entity == null) {
            return false;
        }

        return domain.username().equals(entity.getUsername()) &&
                domain.email().equals(entity.getEmail()) &&
                domain.passwordHash().equals(entity.getPasswordHash());
    }

    /**
     * 验证租户映射结果的完整性
     */
    public boolean validateTenantMapping(Tenant domain, SysTenantEntity entity) {
        if (domain == null || entity == null) {
            return false;
        }

        return domain.orgName().equals(entity.getOrgName()) &&
                compareNullableStrings(domain.planCode(), entity.getPlanCode());
    }

    /**
     * 比较可能为null的字符串
     */
    private boolean compareNullableStrings(String str1, String str2) {
        return KbUtils.safeEquals(str1, str2);
    }

    /**
     * 清理和规范化字符串
     *
     * <p>在映射过程中，我们可能需要对字符串进行清理和规范化处理。
     * 比如去除多余的空格、统一大小写等。</p>
     */
    public String cleanAndNormalizeString(String input) {
        if (input == null) {
            return null;
        }

        // 去除首尾空格
        String cleaned = input.trim();

        // 如果清理后为空字符串，返回null
        if (cleaned.isEmpty()) {
            return null;
        }

        return cleaned;
    }

    /**
     * 验证邮箱格式
     *
     * <p>在映射过程中进行基本的数据验证，确保映射后的对象仍然有效。</p>
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // 简单的邮箱格式验证
        return email.contains("@") && email.contains(".") && !email.startsWith("@") && !email.endsWith("@");
    }

    /**
     * 截断过长的字符串
     *
     * <p>确保字符串长度不超过数据库字段的限制。</p>
     */
    public String truncateString(String input, int maxLength) {
        if (input == null) {
            return null;
        }

        if (input.length() <= maxLength) {
            return input;
        }

        return input.substring(0, maxLength);
    }

    // 静态logger，用于记录映射过程中的警告和错误
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserMapper.class);
}