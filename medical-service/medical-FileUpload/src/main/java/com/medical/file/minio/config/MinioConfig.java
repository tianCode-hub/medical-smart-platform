package com.medical.file.minio.config;

import io.minio.MinioClient;
import jakarta.annotation.Resource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


/**
 * minio配置类
 */
@Component
// 使@ConfigurationProperties注解修饰的类生效，
// 或者不使用该注解，但是在@ConfigurationProperties注解修饰的类上使用@Component
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {
    @Resource
    private MinioProperties minioProperties;

    /**
     * 创建 MinIO 客户端
     */
    @Bean
    public MinioClient getMinioClient() {
        MinioClient minioClient = MinioClient.builder().endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        return minioClient;
    }
}
