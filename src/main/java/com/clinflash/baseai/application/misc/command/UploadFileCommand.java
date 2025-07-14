package com.clinflash.baseai.application.misc.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>文件上传命令</h2>
 */
public record UploadFileCommand(
        @JsonProperty("bucket")
        @NotBlank(message = "存储桶名称不能为空")
        @Size(max = 64, message = "存储桶名称长度不能超过64个字符")
        String bucket,

        @JsonProperty("originalName")
        @NotBlank(message = "原始文件名不能为空")
        @Size(max = 256, message = "文件名长度不能超过256个字符")
        String originalName,

        @JsonProperty("contentType")
        String contentType,

        @JsonProperty("sizeBytes")
        @NotNull(message = "文件大小不能为空")
        Long sizeBytes,

        @JsonProperty("sha256")
        @NotBlank(message = "文件哈希值不能为空")
        @Size(min = 64, max = 64, message = "SHA256哈希值必须是64位十六进制字符")
        String sha256
) {
    /**
     * 验证文件大小是否在允许范围内
     */
    public boolean isValidSize() {
        return this.sizeBytes > 0 && this.sizeBytes <= 100L * 1024 * 1024 * 1024; // 100GB限制
    }

    /**
     * 检查是否为图片文件
     */
    public boolean isImageFile() {
        return this.contentType != null && this.contentType.startsWith("image/");
    }

    /**
     * 生成对象键
     *
     * <p>对象键的生成策略很重要，需要保证唯一性且便于管理。
     * 这里使用时间戳+哈希的方式，既能避免冲突又能追溯上传时间。</p>
     */
    public String generateObjectKey() {
        long timestamp = System.currentTimeMillis();
        String extension = extractFileExtension(this.originalName);
        return String.format("%d/%s%s", timestamp, this.sha256.substring(0, 16), extension);
    }

    private String extractFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
}