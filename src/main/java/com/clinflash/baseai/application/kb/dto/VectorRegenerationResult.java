package com.clinflash.baseai.application.kb.dto;

import java.util.List;

/**
 * <h2>向量重新生成处理结果</h2>
 *
 * <p>详细的结果记录对于评估操作效果和排查问题至关重要。
 * 通过成功率可以评估重新生成操作的整体效果，错误信息则帮助定位和解决具体问题。</p>
 *
 * @param documentId   处理的文档ID
 * @param totalChunks  文档包含的知识块总数
 * @param successCount 成功重新生成向量的知识块数量
 * @param failureCount 重新生成失败的知识块数量
 * @param errors       详细的错误信息列表，便于问题诊断和修复
 */
public record VectorRegenerationResult(
        Long documentId,
        int totalChunks,
        int successCount,
        int failureCount,
        List<String> errors
) {
    /**
     * 创建空的重新生成结果
     *
     * @param documentId 文档ID
     * @return 空的结果对象，用于处理无知识块的情况
     */
    public static VectorRegenerationResult empty(Long documentId) {
        return new VectorRegenerationResult(documentId, 0, 0, 0, List.of());
    }

    /**
     * 检查是否完全成功
     *
     * @return true 如果所有知识块都成功重新生成了向量
     */
    public boolean isFullySuccessful() {
        return failureCount == 0 && totalChunks > 0;
    }

    /**
     * 计算成功率
     *
     * @return 成功率，范围0.0-1.0
     */
    public double getSuccessRate() {
        return totalChunks > 0 ? (double) successCount / totalChunks : 0.0;
    }
}