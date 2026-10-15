package com.medical.file.minio.service.impl;


import com.medical.file.minio.config.MinioProperties;
import com.medical.file.minio.service.FileStorageService;
import com.medical.file.minio.util.MinioUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Service
@Slf4j
public class MinIOFileStorageService implements FileStorageService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    @Qualifier("fileThreadPool")
    private ThreadPoolTaskExecutor fileThreadPool;

    private final static String separator = "/";

    /**
     * @param dirPath  目录前缀
     * @param filename 文件名
     * @return yyyy/MM/dd/file.jpg 形式的对象路径
     */
    public String builderFilePath(String dirPath, String filename) {
        StringBuilder stringBuilder = new StringBuilder(50);
        if (!StringUtils.isEmpty(dirPath)) {
            stringBuilder.append(dirPath).append(separator);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String todayStr = sdf.format(new Date());
        stringBuilder.append(todayStr).append(separator);
        stringBuilder.append(filename);
        return stringBuilder.toString();
    }

    /**
     * 上传图片文件
     *
     * @param prefix      文件前缀
     * @param filename    文件名
     * @param inputStream 文件流
     * @return 文件全路径
     */
    @Override
    public String uploadImgFile(String prefix, String filename, InputStream inputStream) {
        String filePath = builderFilePath(prefix, filename);
        try {
            // 确保桶存在
            minioUtil.existBucket(minioProperties.getBucketName());
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .object(filePath)
                    .contentType("image/jpeg")
                    .bucket(minioProperties.getBucketName())
                    .stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(putObjectArgs);
            return minioUtil.buildAccessUrl(minioProperties.getBucketName(), filePath);
        } catch (Exception ex) {
            log.error("minio put file error.", ex);
            throw new RuntimeException("上传文件失败");
        }
    }

    /**
     * 上传 html 文件
     *
     * @param prefix      文件前缀
     * @param filename    文件名
     * @param inputStream 文件流
     * @return 文件全路径
     */
    @Override
    public String uploadHtmlFile(String prefix, String filename, InputStream inputStream) {
        String filePath = builderFilePath(prefix, filename);
        try {
            // 确保桶存在
            minioUtil.existBucket(minioProperties.getBucketName());
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .object(filePath)
                    .contentType("text/html")
                    .bucket(minioProperties.getBucketName())
                    .stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(putObjectArgs);
            return minioUtil.buildAccessUrl(minioProperties.getBucketName(), filePath);
        } catch (Exception ex) {
            log.error("minio put file error.", ex);
            throw new RuntimeException("上传文件失败");
        }
    }

    /**
     * 删除文件
     *
     * @param pathUrl 文件全路径
     */
    @Override
    public void delete(String pathUrl) {
        try {
            String[] bucketAndObject = minioUtil.parsePathUrl(pathUrl);
            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                    .bucket(bucketAndObject[0]).object(bucketAndObject[1]).build();
            minioClient.removeObject(removeObjectArgs);
        } catch (Exception e) {
            log.error("minio remove file error.  pathUrl:{}", pathUrl, e);
            throw new RuntimeException("删除文件失败");
        }
    }


    /**
     * 下载文件
     *
     * @param pathUrl 文件全路径
     * @return 文件字节流
     */
    @Override
    public byte[] downLoadFile(String pathUrl) {
        InputStream inputStream;
        try {
            String[] bucketAndObject = minioUtil.parsePathUrl(pathUrl);
            inputStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketAndObject[0]).object(bucketAndObject[1]).build());
        } catch (Exception e) {
            log.error("minio down file error.  pathUrl:{}", pathUrl, e);
            throw new RuntimeException("下载文件失败");
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100];
        int rc;
        try {
            if (inputStream != null) {
                while ((rc = inputStream.read(buff, 0, 100)) > 0) {
                    byteArrayOutputStream.write(buff, 0, rc);
                }
            }
        } catch (IOException e) {
            log.error("minio read file error.", e);
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * 批量下载：将多个文件下载提交到文件线程池并行处理，显著提升大批量文件下载吞吐。
     * 返回顺序与入参 pathUrls 一致。
     */
    @Override
    public List<byte[]> batchDownLoadFile(List<String> pathUrls) {
        if (pathUrls == null || pathUrls.isEmpty()) {
            return new ArrayList<>();
        }
        List<Future<byte[]>> futures = new ArrayList<>();
        try {
            for (String url : pathUrls) {
                futures.add(fileThreadPool.submit(() -> downLoadFile(url)));
            }
            List<byte[]> result = new ArrayList<>(pathUrls.size());
            for (Future<byte[]> f : futures) {
                result.add(f.get());
            }
            log.info("minio 批量下载完成，共 {} 个文件", pathUrls.size());
            return result;
        } catch (Exception e) {
            log.error("minio batch download error.", e);
            throw new RuntimeException("批量下载文件失败", e);
        }
    }
}