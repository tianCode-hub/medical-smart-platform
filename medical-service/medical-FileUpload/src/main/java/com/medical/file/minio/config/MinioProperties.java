package com.medical.file.minio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 属性类：绑定配置文件 minio.* 前缀
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * 端点
     */
    private String endpoint;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 密钥
     */
    private String secretKey;

    /**
     * 桶名称
     */
    private String bucketName;

    /**
     * 对外访问前缀（生产环境可配置为公网域名，如 https://cdn.example.com；
     * 不配置时默认使用 endpoint）
     */
    private String accessPath;
}