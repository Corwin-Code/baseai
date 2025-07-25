package com.cloud.baseai.infrastructure.repository.audit;

import com.cloud.baseai.domain.audit.model.SysAuditLog;
import com.cloud.baseai.domain.audit.repository.SysAuditLogRepository;
import com.cloud.baseai.infrastructure.exception.AuditException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.i18n.MessageManager;
import com.cloud.baseai.infrastructure.persistence.audit.entity.SysAuditLogEntity;
import com.cloud.baseai.infrastructure.persistence.audit.mapper.AuditMapper;
import com.cloud.baseai.infrastructure.repository.audit.spring.SpringSysAuditLogRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <h2>系统审计日志仓储实现类</h2>
 *
 * <p>仓储模式是领域驱动设计（DDD）中的一个重要概念。它的主要作用是将业务逻辑
 * 从数据访问细节中解耦出来。这就像是在我们的业务代码和数据库之间建立了一个
 * 抽象层，让我们的业务代码不需要关心数据是存储在MySQL、PostgreSQL还是其他
 * 任何数据库中。</p>
 *
 * <p><b>实现特点：</b></p>
 * <ul>
 * <li><b>事务安全：</b>所有的数据操作都在事务保护下进行，确保数据一致性</li>
 * <li><b>异常转换：</b>将底层的数据访问异常转换为业务异常，提高错误处理的一致性</li>
 * <li><b>性能优化：</b>通过批量操作、合理的索引使用等方式优化查询性能</li>
 * <li><b>参数验证：</b>在数据库操作前进行参数验证，避免无效的数据库调用</li>
 * </ul>
 */
@Repository
@Transactional(readOnly = true) // 默认为只读事务，写操作时会单独标注
public class SysAuditLogJpaRepository implements SysAuditLogRepository {

    private static final Logger logger = LoggerFactory.getLogger(SysAuditLogJpaRepository.class);

    /**
     * Spring Data JPA 仓储
     * <p>这是我们与数据库交互的最底层组件，提供了基础的CRUD操作和自定义查询方法。</p>
     */
    private final SpringSysAuditLogRepo springRepo;

    /**
     * 实体转换器
     * <p>负责在领域对象和JPA实体之间进行转换，这是保持领域模型纯净性的重要工具。</p>
     */
    private final AuditMapper mapper;

    /**
     * 构造函数 - 依赖注入
     *
     * <p>确保所有必需的依赖都被正确初始化，避免空指针异常。</p>
     *
     * @param springRepo Spring Data JPA 仓储
     * @param mapper     实体转换器
     */
    public SysAuditLogJpaRepository(SpringSysAuditLogRepo springRepo, AuditMapper mapper) {
        Assert.notNull(springRepo, "SpringSysAuditLogRepo不能为null");
        Assert.notNull(mapper, "AuditMapper不能为null");

        this.springRepo = springRepo;
        this.mapper = mapper;

        logger.info("审计日志仓储初始化完成");
    }

    /**
     * 保存审计日志
     *
     * <p>这个方法处理审计日志的保存操作。
     * 它会检查每一份文档，确保所有信息都完整正确，然后将其安全地存储起来。</p>
     *
     * <p><b>处理逻辑：</b></p>
     * <ol>
     * <li>验证输入参数的有效性</li>
     * <li>区分新增和更新操作</li>
     * <li>进行领域对象到JPA实体的转换</li>
     * <li>执行数据库操作</li>
     * <li>将结果转换回领域对象</li>
     * <li>处理可能出现的异常</li>
     * </ol>
     *
     * @param log 要保存的审计日志领域对象
     * @return 保存后的审计日志（包含生成的ID）
     * @throws AuditException 当保存操作失败时抛出
     */
    @Override
    @Transactional // 写操作需要可写事务
    public SysAuditLog save(SysAuditLog log) {
        // 第一步：验证输入参数
        Assert.notNull(log, "审计日志不能为null");

        try {
            // 验证领域对象的完整性
            log.validate();

            SysAuditLogEntity entity;

            if (log.id() == null) {
                // 这是一个新的审计日志，直接转换为实体
                entity = mapper.toEntity(log);
                logger.debug("准备保存新的审计日志: action={}, userId={}", log.action(), log.userId());
            } else {
                // 这是一个已存在的审计日志的更新操作
                // 首先尝试从数据库中查找现有记录
                entity = springRepo.findById(log.id())
                        .orElse(mapper.toEntity(log));

                // 使用领域对象的数据更新实体
                entity.updateFromDomain(log);
                logger.debug("准备更新审计日志: id={}, action={}", log.id(), log.action());
            }

            // 执行数据库保存操作
            SysAuditLogEntity savedEntity = springRepo.save(entity);

            // 将保存后的实体转换回领域对象
            SysAuditLog savedLog = mapper.toDomain(savedEntity);

            logger.info("审计日志保存成功: id={}, action={}, userId={}",
                    savedLog.id(), savedLog.action(), savedLog.userId());

            return savedLog;

        } catch (DataAccessException e) {
            // 数据访问异常通常是技术性问题，如数据库连接失败、约束违反等
            String errorMessage = String.format("保存审计日志失败: action=%s, userId=%s",
                    log.action(), log.userId());
            logger.error(errorMessage, e);
            throw AuditException.technicalError(ErrorCode.BIZ_AUDIT_017, e, log.action(), log.userId());

        } catch (IllegalArgumentException e) {
            // 参数验证异常通常是业务逻辑问题
            String errorMessage = "审计日志数据验证失败: " + e.getMessage();
            logger.warn(errorMessage, e);
            throw AuditException.businessError(ErrorCode.BIZ_AUDIT_018, e);

        } catch (Exception e) {
            // 其他未预期的异常
            String errorMessage = "保存审计日志时发生未知错误";
            logger.error(errorMessage, e);
            throw AuditException.systemError(ErrorCode.BIZ_AUDIT_019, e);
        }
    }

    /**
     * 根据ID查找审计日志
     *
     * <p>这个方法就像图书馆的索引系统，你提供一个编号，它就能快速找到对应的记录。
     * 这是最基础但也是最重要的查询方法之一。</p>
     *
     * @param id 审计日志ID
     * @return 找到的审计日志，如果不存在则返回空Optional
     * @throws AuditException 当查询操作失败时抛出
     */
    @Override
    public Optional<SysAuditLog> findById(Long id) {
        Assert.notNull(id, "审计日志ID不能为null");

        try {
            logger.debug("查询审计日志: id={}", id);

            Optional<SysAuditLogEntity> entityOpt = springRepo.findById(id);

            if (entityOpt.isPresent()) {
                SysAuditLog domainLog = mapper.toDomain(entityOpt.get());
                logger.debug("成功找到审计日志: id={}, action={}", id, domainLog.action());
                return Optional.of(domainLog);
            } else {
                logger.debug("未找到审计日志: id={}", id);
                return Optional.empty();
            }

        } catch (DataAccessException e) {
            String errorMessage = String.format("查询审计日志失败: id=%d", id);
            logger.error(errorMessage, e);
            throw AuditException.technicalError(ErrorCode.BIZ_AUDIT_020, e, id);
        }
    }

    /**
     * 查询用户操作历史
     *
     * <p>这个方法是审计系统中最复杂也是最有价值的查询之一。它就像一个智能的搜索引擎，
     * 能够根据多个条件快速找到相关的审计记录。这对于安全分析、用户行为跟踪、
     * 合规检查都非常重要。</p>
     *
     * <p><b>查询优化策略：</b></p>
     * <ul>
     * <li>使用复合索引来优化多条件查询</li>
     * <li>合理使用分页来避免一次性加载过多数据</li>
     * <li>在查询条件较少时采用更高效的查询路径</li>
     * </ul>
     *
     * @param userId    用户ID，null表示查询所有用户
     * @param startTime 开始时间，null表示不限制开始时间
     * @param endTime   结束时间，null表示不限制结束时间
     * @param actions   操作类型列表，null或空表示所有操作类型
     * @param pageable  分页参数
     * @return 查询结果的分页对象
     * @throws AuditException 当查询操作失败时抛出
     */
    @Override
    public Page<SysAuditLog> findUserActions(Long userId, OffsetDateTime startTime, OffsetDateTime endTime,
                                             List<String> actions, Pageable pageable) {
        // 参数验证
        Assert.notNull(pageable, "分页参数不能为null");

        try {
            logger.debug("查询用户操作历史: userId={}, startTime={}, endTime={}, actions={}, page={}, size={}",
                    userId, startTime, endTime, actions, pageable.getPageNumber(), pageable.getPageSize());

            // 验证分页参数的合理性
            validatePageable(pageable);

            // 验证时间范围的合理性
            validateTimeRange(startTime, endTime);

            // 执行查询
            Page<SysAuditLogEntity> entityPage = springRepo.findUserActions(
                    userId, startTime, endTime, actions, pageable);

            // 转换为领域对象
            Page<SysAuditLog> domainPage = entityPage.map(mapper::toDomain);

            logger.info("用户操作历史查询完成: userId={}, 总记录数={}, 当前页记录数={}",
                    userId, domainPage.getTotalElements(), domainPage.getNumberOfElements());

            return domainPage;

        } catch (DataAccessException e) {
            String errorMessage = String.format("查询用户操作历史失败: userId=%s", userId);
            logger.error(errorMessage, e);
            throw AuditException.technicalError(ErrorCode.BIZ_AUDIT_021, e, userId);
        }
    }

    /**
     * 根据租户ID查询审计日志
     *
     * <p>在多租户系统中，每个租户只能看到自己的审计日志。这个方法确保了
     * 数据的隔离性和安全性，就像公司里每个部门只能查看自己部门的文件一样。</p>
     *
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 查询结果的分页对象
     * @throws AuditException 当查询操作失败时抛出
     */
    @Override
    public Page<SysAuditLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable) {
        Assert.notNull(tenantId, "租户ID不能为null");
        Assert.notNull(pageable, "分页参数不能为null");

        try {
            logger.debug("查询租户审计日志: tenantId={}, page={}, size={}",
                    tenantId, pageable.getPageNumber(), pageable.getPageSize());

            validatePageable(pageable);

            Page<SysAuditLogEntity> entityPage = springRepo
                    .findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);

            Page<SysAuditLog> domainPage = entityPage.map(mapper::toDomain);

            logger.debug("租户审计日志查询完成: tenantId={}, 总记录数={}",
                    tenantId, domainPage.getTotalElements());

            return domainPage;

        } catch (DataAccessException e) {
            String errorMessage = String.format("查询租户审计日志失败: tenantId=%d", tenantId);
            logger.error(errorMessage, e);
            throw AuditException.technicalError(ErrorCode.BIZ_AUDIT_022, e, tenantId);
        }
    }

    /**
     * 根据时间范围查询审计日志
     *
     * <p>这个方法专门用于按时间范围查询审计日志，这在生成定期报告、
     * 进行安全分析时非常有用。</p>
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 查询结果的分页对象
     * @throws AuditException 当查询操作失败时抛出
     */
    @Override
    public Page<SysAuditLog> findByTimeRange(OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable) {
        Assert.notNull(startTime, "开始时间不能为null");
        Assert.notNull(endTime, "结束时间不能为null");
        Assert.notNull(pageable, "分页参数不能为null");

        try {
            validateTimeRange(startTime, endTime);
            validatePageable(pageable);

            logger.debug("按时间范围查询审计日志: startTime={}, endTime={}, page={}, size={}",
                    startTime, endTime, pageable.getPageNumber(), pageable.getPageSize());

            Page<SysAuditLogEntity> entityPage = springRepo
                    .findByTimeRange(startTime, endTime, pageable);

            Page<SysAuditLog> domainPage = entityPage.map(mapper::toDomain);

            logger.debug("时间范围查询完成: 总记录数={}", domainPage.getTotalElements());

            return domainPage;

        } catch (DataAccessException e) {
            String errorMessage = "按时间范围查询审计日志失败";
            logger.error(errorMessage, e);
            throw AuditException.technicalError(ErrorCode.BIZ_AUDIT_023, e);
        }
    }

    /**
     * 统计指定时间范围内的审计日志数量
     *
     * <p>这个方法用于快速获取统计信息，而不需要加载具体的数据。
     * 这就像是先了解图书馆某个分类下有多少本书，然后再决定是否需要详细查看。</p>
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计数量
     * @throws AuditException 当统计操作失败时抛出
     */
    @Override
    public long countByTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            logger.warn("时间范围参数为null，返回0");
            return 0;
        }

        try {
            validateTimeRange(startTime, endTime);

            logger.debug("统计时间范围内的审计日志数量: startTime={}, endTime={}", startTime, endTime);

            long count = springRepo.countByTimeRange(startTime, endTime);

            logger.debug("统计完成: 时间范围内共有{}条审计日志", count);

            return count;

        } catch (DataAccessException e) {
            String errorMessage = "统计审计日志数量失败";
            logger.error(errorMessage, e);
            throw AuditException.technicalError(ErrorCode.BIZ_AUDIT_024, e);
        }
    }

    /**
     * 按操作类型统计
     *
     * <p>这个方法提供按操作类型的统计信息，帮助了解系统中各种操作的频率分布。
     * 这对于系统优化和安全分析都很有价值。</p>
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计结果列表，每个元素包含操作类型和数量
     * @throws AuditException 当统计操作失败时抛出
     */
    @Override
    public List<Object[]> countByActionAndTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            logger.warn("时间范围参数为null，返回空列表");
            return new ArrayList<>();
        }

        try {
            validateTimeRange(startTime, endTime);

            logger.debug("按操作类型统计审计日志: startTime={}, endTime={}", startTime, endTime);

            List<Object[]> results = springRepo.countByActionAndTimeRange(startTime, endTime);

            logger.debug("操作类型统计完成: 共{}种操作类型", results.size());

            return results;

        } catch (DataAccessException e) {
            String errorMessage = "按操作类型统计审计日志失败";
            logger.error(errorMessage, e);
            throw AuditException.technicalError(ErrorCode.BIZ_AUDIT_025, e);
        }
    }

    /**
     * 按用户统计
     *
     * <p>这个方法提供按用户的统计信息，帮助了解各个用户的活跃程度。
     * 这对于用户行为分析和异常检测很有帮助。</p>
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计结果列表，每个元素包含用户ID和操作数量
     * @throws AuditException 当统计操作失败时抛出
     */
    @Override
    public List<Object[]> countByUserAndTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            logger.warn("时间范围参数为null，返回空列表");
            return new ArrayList<>();
        }

        try {
            validateTimeRange(startTime, endTime);

            logger.debug("按用户统计审计日志: startTime={}, endTime={}", startTime, endTime);

            List<Object[]> results = springRepo.countByUserAndTimeRange(startTime, endTime);

            logger.debug("用户统计完成: 共{}个用户有操作记录", results.size());

            return results;

        } catch (DataAccessException e) {
            String errorMessage = "按用户统计审计日志失败";
            logger.error(errorMessage, e);
            throw AuditException.technicalError(ErrorCode.BIZ_AUDIT_026, e);
        }
    }

    /**
     * 删除指定时间之前的审计日志
     *
     * <p>这个方法用于数据清理，删除过期的审计日志。这就像定期清理老旧的文件，
     * 既能节省存储空间，又能提高查询性能。但是要注意，在执行删除操作前，
     * 应该确保符合法规要求和公司政策。</p>
     *
     * @param cutoffTime 截止时间，在此时间之前的日志将被删除
     * @throws AuditException 当删除操作失败时抛出
     */
    @Override
    @Transactional // 删除操作需要可写事务
    public void deleteOldAuditLogs(OffsetDateTime cutoffTime) {
        Assert.notNull(cutoffTime, "截止时间不能为null");

        try {
            logger.info("开始删除旧的审计日志: cutoffTime={}", cutoffTime);

            // 在删除前先统计要删除的记录数量
            long countToDelete = springRepo.countByCreatedAtBefore(cutoffTime);

            if (countToDelete == 0) {
                logger.info("没有需要删除的审计日志");
                return;
            }

            logger.warn("准备删除{}条审计日志（{}之前的记录）", countToDelete, cutoffTime);

            // 执行删除操作
            springRepo.deleteOldAuditLogs(cutoffTime);

            logger.info("成功删除{}条旧的审计日志", countToDelete);

        } catch (DataAccessException e) {
            String errorMessage = "删除旧审计日志失败";
            logger.error(errorMessage, e);
            throw AuditException.technicalError(ErrorCode.BIZ_AUDIT_027, e);
        }
    }

    /**
     * 根据目标类型和目标ID查询审计日志
     *
     * <p>这个方法用于查询针对特定对象的所有操作记录。
     * 就像为每个重要文件建立操作档案，记录谁在什么时候对它做了什么操作。</p>
     */
    public Page<SysAuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            String targetType, Long targetId, Pageable pageable) {

        Assert.hasText(targetType, "目标类型不能为空");
        Assert.notNull(targetId, "目标ID不能为null");
        Assert.notNull(pageable, "分页参数不能为null");

        try {
            logger.debug("查询对象操作历史: targetType={}, targetId={}, page={}, size={}",
                    targetType, targetId, pageable.getPageNumber(), pageable.getPageSize());

            validatePageable(pageable);

            Page<SysAuditLogEntity> entityPage = springRepo
                    .findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId, pageable);

            Page<SysAuditLog> domainPage = entityPage.map(mapper::toDomain);

            logger.debug("对象操作历史查询完成: targetType={}, targetId={}, 总记录数={}",
                    targetType, targetId, domainPage.getTotalElements());

            return domainPage;

        } catch (DataAccessException e) {
            String errorMessage = String.format("查询对象操作历史失败: targetType=%s, targetId=%d",
                    targetType, targetId);
            logger.error(errorMessage, e);
            throw AuditException.technicalError(ErrorCode.BIZ_AUDIT_028, e, targetType, targetId);
        }
    }

    /**
     * 批量保存审计日志
     *
     * <p>这个方法提供批量保存功能，能够显著提高大量数据写入的性能。
     * 这就像是批量处理文件，比一个一个处理要高效得多。</p>
     *
     * @param logs 要保存的审计日志列表
     * @return 保存后的审计日志列表
     * @throws AuditException 当批量保存操作失败时抛出
     */
    @Transactional
    public List<SysAuditLog> saveAll(List<SysAuditLog> logs) {
        Assert.notNull(logs, "审计日志列表不能为null");

        if (logs.isEmpty()) {
            logger.debug("审计日志列表为空，直接返回");
            return new ArrayList<>();
        }

        try {
            logger.debug("开始批量保存审计日志: 数量={}", logs.size());

            // 验证所有日志的有效性
            for (SysAuditLog auditLog : logs) {
                auditLog.validate();
            }

            // 转换为实体列表
            List<SysAuditLogEntity> entities = logs.stream()
                    .map(mapper::toEntity)
                    .toList();

            // 批量保存
            List<SysAuditLogEntity> savedEntities = springRepo.saveAll(entities);

            // 转换回领域对象
            List<SysAuditLog> savedLogs = savedEntities.stream()
                    .map(mapper::toDomain)
                    .toList();

            logger.info("批量保存审计日志完成: 成功保存{}条记录", savedLogs.size());

            return savedLogs;

        } catch (DataAccessException e) {
            String errorMessage = String.format("批量保存审计日志失败: 数量=%d", logs.size());
            logger.error(errorMessage, e);
            throw AuditException.technicalError(ErrorCode.BIZ_AUDIT_029, e, logs.size());
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 验证分页参数的合理性
     *
     * <p>这个方法确保分页参数在合理的范围内，避免可能的性能问题。</p>
     *
     * @param pageable 分页参数
     * @throws IllegalArgumentException 当分页参数不合理时抛出
     */
    private void validatePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException(MessageManager.getMessage(ErrorCode.PARAM_019));
        }

        if (pageable.getPageSize() <= 0) {
            throw new IllegalArgumentException(MessageManager.getMessage(ErrorCode.PARAM_023));
        }

        if (pageable.getPageSize() > 1000) {
            throw new IllegalArgumentException(MessageManager.getMessage(ErrorCode.PARAM_024, pageable.getPageSize()));
        }
    }

    /**
     * 验证时间范围的合理性
     *
     * <p>这个方法确保时间范围参数是合理的，避免无意义的查询。</p>
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @throws IllegalArgumentException 当时间范围不合理时抛出
     */
    private void validateTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime != null && endTime != null) {
            if (startTime.isAfter(endTime)) {
                throw new IllegalArgumentException(MessageManager.getMessage(ErrorCode.PARAM_025, startTime, endTime));
            }

            // 检查时间范围是否过大（比如超过1年）
            if (startTime.plusYears(1).isBefore(endTime)) {
                logger.warn("查询时间范围过大，可能影响性能: startTime={}, endTime={}", startTime, endTime);
            }
        }
    }

    /**
     * 创建默认的排序参数
     *
     * <p>当没有指定排序参数时，使用默认的排序方式（按创建时间倒序）。</p>
     *
     * @param pageable 原始分页参数
     * @return 带有默认排序的分页参数
     */
    private Pageable createDefaultSortPageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }
        return pageable;
    }
}