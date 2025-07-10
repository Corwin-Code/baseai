package com.clinflash.baseai.application.kb.dto;

import java.util.List;

/**
 * <h2>批量上传处理结果</h2>
 *
 * <p>结果对象提供了完整的统计数据，包括成功率计算、
 * 错误分类等，便于用户评估导入质量和调整策略。</p>
 *
 * @param batchName        批次名称，用于标识和追踪特定的批量操作
 * @param successDocuments 成功处理的文档列表，包含完整的文档信息
 * @param failedDocuments  处理失败的文档列表，包含错误原因和位置信息
 * @param successCount     成功处理的文档数量
 * @param failureCount     处理失败的文档数量
 */
public record BatchUploadResult(
        String batchName,
        List<DocumentDTO> successDocuments,
        List<FailedDocument> failedDocuments,
        int successCount,
        int failureCount
) {
    /**
     * 失败文档信息记录
     *
     * @param index 在原批次中的索引位置，便于定位问题文档
     * @param title 文档标题，便于用户识别
     * @param error 详细的错误信息，帮助用户理解失败原因
     */
    public record FailedDocument(int index, String title, String error) {
    }

    /**
     * 检查批次是否有失败项
     *
     * @return true 如果存在处理失败的文档
     */
    public boolean hasFailures() {
        return failureCount > 0;
    }

    /**
     * 计算批量处理成功率
     *
     * @return 成功率，范围0.0-1.0
     */
    public double getSuccessRate() {
        int total = successCount + failureCount;
        return total > 0 ? (double) successCount / total : 0.0;
    }
}