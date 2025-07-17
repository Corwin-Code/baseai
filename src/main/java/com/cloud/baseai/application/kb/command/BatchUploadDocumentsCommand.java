package com.cloud.baseai.application.kb.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>批量文档处理命令</h2>
 *
 * <p>用于批量上传和处理多个文档。</p>
 *
 * @param tenantId   租户ID
 * @param documents  文档信息列表
 * @param operatorId 操作人ID
 * @param batchName  批次名称（可选）
 */
public record BatchUploadDocumentsCommand(
        @NotNull(message = "租户ID不能为空")
        Long tenantId,

        @NotNull(message = "文档列表不能为空")
        @Size(min = 1, max = 100, message = "批量上传文档数量必须在1-100之间")
        java.util.List<DocumentInfo> documents,

        @NotNull(message = "操作人ID不能为空")
        Long operatorId,

        String batchName
) {

    /**
     * 文档信息
     *
     * @param title      标题
     * @param content    内容
     * @param sourceType 来源类型
     * @param sourceUri  源地址
     * @param mimeType   MIME类型
     */
    public record DocumentInfo(
            @NotBlank String title,
            @NotBlank String content,
            @NotBlank String sourceType,
            String sourceUri,
            String mimeType
    ) {
    }

    /**
     * 计算总内容大小
     *
     * @return 总字节数
     */
    public long getTotalContentSize() {
        return documents.stream()
                .mapToLong(doc -> doc.content().getBytes().length)
                .sum();
    }

    /**
     * 验证批次大小
     *
     * @param maxSizeInBytes 最大字节数
     * @return true如果大小合法
     */
    public boolean isBatchSizeValid(long maxSizeInBytes) {
        return getTotalContentSize() <= maxSizeInBytes;
    }
}