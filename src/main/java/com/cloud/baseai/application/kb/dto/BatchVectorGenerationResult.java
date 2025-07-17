package com.cloud.baseai.application.kb.dto;

/**
 * <h2>批量向量生成处理结果</h2>
 *
 * <p>提供了丰富的状态信息，从"已启动"到"处理中"再到"已完成"，
 * 用户可以清楚地了解当前的处理阶段。</p>
 *
 * @param taskId         任务唯一标识符，用于后续的状态查询和结果追踪
 * @param totalDocuments 待处理的文档总数
 * @param status         当前处理状态，如STARTED、PROCESSING、COMPLETED、FAILED等
 * @param message        状态描述信息，提供人性化的处理进度说明
 */
public record BatchVectorGenerationResult(
        String taskId,
        int totalDocuments,
        String status,
        String message
) {
}