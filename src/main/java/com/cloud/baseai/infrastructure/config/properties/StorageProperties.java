package com.cloud.baseai.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * <h2>文件存储配置属性类</h2>
 *
 * <p>该类管理文件存储的配置，支持本地存储和云存储两种模式，
 * 提供灵活的文件管理和安全控制功能。</p>
 */
@Data
@ConfigurationProperties(prefix = "baseai.storage")
public class StorageProperties {

    /**
     * 本地存储配置
     */
    private LocalProperties local = new LocalProperties();

    /**
     * 云存储配置（可选）
     */
    private CloudProperties cloud = new CloudProperties();

    /**
     * 本地存储配置内部类
     */
    @Data
    public static class LocalProperties {
        /**
         * 是否启用本地存储
         */
        private Boolean enabled = true;

        /**
         * 存储根路径
         */
        private String basePath = "./storage";

        /**
         * 单个文件最大大小
         */
        private String maxFileSize = "10MB";

        /**
         * 允许的文件类型
         */
        private List<String> allowedTypes = List.of(
                "pdf", "txt", "md", "docx", "doc", "jpg", "jpeg", "png", "gif"
        );

        /**
         * 是否启用文件校验
         */
        private Boolean enableChecksum = true;

        /**
         * 校验算法：MD5、SHA1、SHA256
         */
        private String checksumAlgorithm = "SHA256";
    }

    /**
     * 云存储配置内部类
     */
    @Data
    public static class CloudProperties {
        /**
         * 是否启用云存储
         */
        private Boolean enabled = false;

        /**
         * 云存储提供商：oss、s3、cos等
         */
        private String provider = "oss";

        /**
         * 服务端点
         */
        private String endpoint = "";

        /**
         * 访问密钥
         */
        private String accessKey = "";

        /**
         * 密钥
         */
        private String secretKey = "";

        /**
         * 存储桶名称
         */
        private String bucket = "baseai-storage";

        /**
         * 是否启用CDN加速
         */
        private Boolean enableCdn = false;

        /**
         * CDN域名
         */
        private String cdnDomain = "";
    }
}