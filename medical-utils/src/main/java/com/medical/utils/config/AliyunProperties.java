package com.medical.utils.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云配置：短信 / OSS。
 * 从配置文件（application.yml / Nacos 配置中心）读取 aliyun.* 前缀的配置，
 * 避免在代码中硬编码 AccessKey 等敏感信息。
 */
@Component
@ConfigurationProperties(prefix = "aliyun")
public class AliyunProperties {

    /** 短信配置 */
    private Sms sms = new Sms();
    /** OSS 配置 */
    private Oss oss = new Oss();

    public Sms getSms() {
        return sms;
    }

    public void setSms(Sms sms) {
        this.sms = sms;
    }

    public Oss getOss() {
        return oss;
    }

    public void setOss(Oss oss) {
        this.oss = oss;
    }

    public static class Sms {
        /** 阿里云 AccessKeyId */
        private String accessKeyId;
        /** 阿里云 AccessKeySecret */
        private String accessKeySecret;

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }
    }

    public static class Oss {
        /** OSS 公网 Endpoint，如 oss-cn-beijing.aliyuncs.com */
        private String endpoint;
        /** Bucket 名称 */
        private String bucketName;
        /** 阿里云 AccessKeyId */
        private String accessKeyId;
        /** 阿里云 AccessKeySecret */
        private String accessKeySecret;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }
    }
}