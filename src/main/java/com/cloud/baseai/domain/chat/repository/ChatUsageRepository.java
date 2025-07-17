package com.cloud.baseai.domain.chat.repository;

import com.cloud.baseai.application.chat.dto.ModelUsageDTO;
import com.cloud.baseai.domain.chat.model.ChatUsageDaily;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>对话使用量仓储接口</h2>
 */
public interface ChatUsageRepository {

    /**
     * 保存使用量记录
     */
    ChatUsageDaily save(ChatUsageDaily usage);

    /**
     * 查找指定日期的使用记录
     */
    Optional<ChatUsageDaily> findByStatDateAndTenantIdAndModelCode(LocalDate statDate, Long tenantId, String modelCode);

    /**
     * 查找时间范围内的使用记录
     */
    List<ChatUsageDaily> findByTenantIdAndStatDateBetween(Long tenantId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取热门模型
     */
    List<ModelUsageDTO> getTopUsedModels(Long tenantId, OffsetDateTime since, int limit);
}