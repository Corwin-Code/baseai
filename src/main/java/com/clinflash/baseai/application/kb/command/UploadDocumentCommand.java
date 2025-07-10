package com.clinflash.baseai.application.kb.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>上传文档命令</h2>
 *
 * <p>用于文档上传和处理的命令对象。包含文档的基本信息和内容。</p>
 *
 * @param tenantId   租户ID
 * @param title      文档标题
 * @param content    文档内容
 * @param sourceType 来源类型
 * @param sourceUri  源地址（可选）
 * @param mimeType   MIME类型（可选）
 * @param langCode   语言代码（可选，默认auto）
 * @param operatorId 操作人ID
 */
public record UploadDocumentCommand(
        @NotNull(message = "租户ID不能为空")
        Long tenantId,

        @NotBlank(message = "文档标题不能为空")
        @Size(max = 256, message = "文档标题长度不能超过256字符")
        String title,

        @NotBlank(message = "文档内容不能为空")
        String content,

        @NotBlank(message = "来源类型不能为空")
        String sourceType,

        String sourceUri,
        String mimeType,
        String langCode,

        @NotNull(message = "操作人ID不能为空")
        Long operatorId
) {

    /**
     * 构造函数，设置默认值
     */
    public UploadDocumentCommand {
        if (langCode == null || langCode.trim().isEmpty()) {
            langCode = "auto";
        }
    }

    /**
     * 验证文档内容大小
     *
     * @param maxSizeInBytes 最大字节数
     * @return true如果大小合法
     */
    public boolean isContentSizeValid(int maxSizeInBytes) {
        return content != null && content.getBytes().length <= maxSizeInBytes;
    }

    /**
     * 获取估算的Token数量
     *
     * @return 估算的Token数量
     */
    public int getEstimatedTokenCount() {
        if (content == null) {
            return 0;
        }
        // 简化估算：中文约1.5字符/token，英文约4字符/token
        return langCode != null && langCode.startsWith("zh") ?
                (int) (content.length() / 1.5) : content.length() / 4;
    }
}