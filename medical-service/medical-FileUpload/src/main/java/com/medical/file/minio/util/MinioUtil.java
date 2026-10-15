package com.medical.file.minio.util;

import cn.hutool.core.util.IdUtil;
import com.medical.file.minio.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * MinIO 工具类
 */
@Component
public class MinioUtil {

    @Resource
    private MinioProperties minioProperties;

    @Autowired
    private MinioClient minioClient;

    /** 图片存储目录前缀 */
    private static final String DIR_IMG = "img/";

    /** 允许上传的文件扩展名白名单 */
    private static final String[] ALLOWED_EXTENSIONS = {
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
            ".txt", ".html", ".pdf", ".xml",
            ".doc", ".docx", ".ppt", ".pptx"
    };

    /** 单文件大小上限：10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 判断Bucket是否存在，不存在则创建
     */
    public void existBucket(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("创建Bucket失败: " + bucketName, e);
        }
    }

    /**
     * 校验上传文件：非空、大小上限、扩展名白名单
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小不能超过 10MB");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            throw new RuntimeException("文件名不合法");
        }
        String ext = name.substring(name.lastIndexOf(".")).toLowerCase();
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(ext)) {
                return;
            }
        }
        throw new RuntimeException("不支持的文件类型: " + ext);
    }

    /**
     * 生成防重名文件名（时间戳 + UUID + 原后缀）
     *
     * @param originalFilename 原始文件名
     * @return 新文件名
     */
    public String buildFileName(String originalFilename) {
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return System.currentTimeMillis() + "_" + IdUtil.fastSimpleUUID() + suffix;
    }

    /**
     * 拼接文件访问 URL：优先使用对外访问前缀，否则使用 endpoint
     */
    public String buildAccessUrl(String bucket, String object) {
        String base = minioProperties.getAccessPath();
        if (!StringUtils.hasLength(base)) {
            base = minioProperties.getEndpoint();
        }
        return base + "/" + bucket + "/" + object;
    }

    /**
     * 安全解析访问 URL，返回 [bucket, object]
     *
     * @param pathUrl 文件访问 URL
     */
    public String[] parsePathUrl(String pathUrl) {
        if (!StringUtils.hasLength(pathUrl)) {
            throw new RuntimeException("文件路径不能为空");
        }
        String key = pathUrl;
        String base = minioProperties.getAccessPath();
        if (StringUtils.hasLength(base) && key.startsWith(base + "/")) {
            key = key.substring(base.length());
        } else if (key.startsWith(minioProperties.getEndpoint() + "/")) {
            key = key.substring(minioProperties.getEndpoint().length());
        } else {
            throw new RuntimeException("非法的文件路径");
        }
        // 去掉开头的斜杠
        key = key.startsWith("/") ? key.substring(1) : key;
        String[] parts = key.split("/", 2);
        if (parts.length != 2 || !StringUtils.hasLength(parts[0]) || !StringUtils.hasLength(parts[1])) {
            throw new RuntimeException("非法的文件路径");
        }
        return parts; // [bucket, object]
    }

    /**
     * 文件上传（对外接口，接收 MultipartFile）
     *
     * @param file 前端传递的文件
     * @return 文件访问 URL
     */
    public String upload(MultipartFile file) {
        validateFile(file);
        String fileName = buildFileName(file.getOriginalFilename());
        return upload(file, fileName);
    }

    /**
     * 文件上传（内部调用）
     *
     * @param file     文件
     * @param fileName 存储在 MinIO 中的对象名
     * @return 文件访问 URL
     */
    public String upload(MultipartFile file, String fileName) {
        try (InputStream inputStream = file.getInputStream()) {
            // 确保 Bucket 存在
            existBucket(minioProperties.getBucketName());

            // 获取 ContentType
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = getContentType(fileName);
            }

            // 上传到 MinIO（img/ 目录下）
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(DIR_IMG + fileName)
                    .stream(inputStream, -1, 10485760) // 未知大小传-1，partSize 建议 10MB
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("文件上传MinIO失败", e);
        }

        // 返回文件访问路径（与对象存储路径保持一致，含 img/ 前缀）
        return buildAccessUrl(minioProperties.getBucketName(), DIR_IMG + fileName);
    }

    /**
     * 根据文件后缀获取 ContentType
     */
    private String getContentType(String fileName) {
        String fileSuffix = fileName.toLowerCase();
        if (fileSuffix.endsWith(".bmp")) {
            return "image/bmp";
        } else if (fileSuffix.endsWith(".gif")) {
            return "image/gif";
        } else if (fileSuffix.endsWith(".jpg") || fileSuffix.endsWith(".jpeg") || fileSuffix.endsWith(".png")) {
            return "image/jpeg";
        } else if (fileSuffix.endsWith(".html")) {
            return "text/html";
        } else if (fileSuffix.endsWith(".txt")) {
            return "text/plain";
        } else if (fileSuffix.endsWith(".vsd")) {
            return "application/vnd.visio";
        } else if (fileSuffix.endsWith(".ppt") || fileSuffix.endsWith(".pptx")) {
            return "application/vnd.ms-powerpoint";
        } else if (fileSuffix.endsWith(".doc") || fileSuffix.endsWith(".docx")) {
            return "application/msword";
        } else if (fileSuffix.endsWith(".xml")) {
            return "text/xml";
        } else {
            return "application/octet-stream";
        }
    }
}