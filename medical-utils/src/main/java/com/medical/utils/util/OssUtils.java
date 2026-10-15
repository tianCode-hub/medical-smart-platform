package com.medical.utils.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import com.medical.utils.config.AliyunProperties;
import com.medical.utils.core.SpringBeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OssUtils {
    private static Logger log = LoggerFactory.getLogger(OssUtils.class);

    //文件存储目录
    private static final String fileDir = "avarta/";

    private static OSS ossClient;

    /**
     * OSS 上传线程池：批量上传多个文件时并行执行网络 I/O，提升吞吐。
     * 拒绝策略使用调用线程执行，避免上传任务丢失。
     */
    private static final ExecutorService OSS_FILE_POOL = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64),
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 懒加载获取 OSS 配置（AccessKey 等从 aliyun.oss.* 读取，不硬编码）
     */
    private static AliyunProperties.Oss ossProps() {
        return SpringBeanUtils.getBean(AliyunProperties.class).getOss();
    }

    /**
     * 懒加载获取 OSS 客户端（单例）
     */
    private static OSS getOSSClient() {
        if (ossClient == null) {
            synchronized (OssUtils.class) {
                if (ossClient == null) {
                    AliyunProperties.Oss props = ossProps();
                    ossClient = new OSSClientBuilder().build(
                            props.getEndpoint(), props.getAccessKeyId(), props.getAccessKeySecret());
                }
            }
        }
        return ossClient;
    }

    /**
     * 上传到OSS服务器  如果同名文件会覆盖服务器上的
     *
     * @param inputStream 文件流
     * @param fileName    文件名称 包括后缀名
     * @return 返回路径
     */
    public static String uploadFile2OSS(InputStream inputStream, String fileName) {
        String path = "";
        try {
            AliyunProperties.Oss props = ossProps();
            //创建上传Object的Metadata
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(inputStream.available());
            objectMetadata.setCacheControl("no-cache");
            objectMetadata.setHeader("Pragma", "no-cache");
            objectMetadata.setContentType(getContentType(fileName.substring(fileName.lastIndexOf("."))));
            objectMetadata.setContentDisposition("inline;filename=" + fileName);
            //上传文件
            PutObjectResult putResult = OssUtils.getOSSClient().putObject(props.getBucketName(), fileDir + fileName, inputStream, objectMetadata);
            path = String.format("http://%s.%s/%s%s", props.getBucketName(), props.getEndpoint(), fileDir, fileName);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path;
    }

    /**
     * Description: 判断OSS服务文件上传时文件的contentType
     *
     * @param FilenameExtension 文件后缀
     * @return String
     */
    private static String getContentType(String FilenameExtension) {
        if (FilenameExtension.equalsIgnoreCase(".bmp")) {
            return "image/bmp";
        }
        if (FilenameExtension.equalsIgnoreCase(".gif")) {
        return "image/gif";
    }
        if (FilenameExtension.equalsIgnoreCase(".jpeg") ||
        FilenameExtension.equalsIgnoreCase(".jpg") ||
        FilenameExtension.equalsIgnoreCase(".png")) {
        return "image/jpeg";
    }
        if (FilenameExtension.equalsIgnoreCase(".html")) {
        return "text/html";
    }
        if (FilenameExtension.equalsIgnoreCase(".txt")) {
        return "text/plain";
    }
        if (FilenameExtension.equalsIgnoreCase(".vsd")) {
        return "application/vnd.visio";
    }
        if (FilenameExtension.equalsIgnoreCase(".pptx") ||
        FilenameExtension.equalsIgnoreCase(".ppt")) {
        return "application/vnd.ms-powerpoint";
    }
        if (FilenameExtension.equalsIgnoreCase(".docx") ||
        FilenameExtension.equalsIgnoreCase(".doc")) {
        return "application/msword";
    }
        if (FilenameExtension.equalsIgnoreCase(".xml")) {
        return "text/xml";
    }
        return "image/jpeg";
    }

    /**
     * 批量上传：多个文件（key=文件名, value=文件流）在 OSS 线程池中并行上传，
     * 返回每个文件上传后的访问路径。单个文件仍可直接使用 {@link #uploadFile2OSS}。
     *
     * @param files 文件名 -> 文件流
     * @return 与入参顺序一致的上传路径列表
     */
    public static List<String> batchUploadFile2OSS(Map<String, InputStream> files) {
        List<String> paths = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return paths;
        }
        List<Future<String>> futures = new ArrayList<>(files.size());
        for (Map.Entry<String, InputStream> entry : files.entrySet()) {
            String fileName = entry.getKey();
            InputStream inputStream = entry.getValue();
            futures.add(OSS_FILE_POOL.submit(() -> uploadFile2OSS(inputStream, fileName)));
        }
        for (Future<String> f : futures) {
            try {
                paths.add(f.get());
            } catch (Exception e) {
                log.error("batch oss upload error.", e);
                paths.add("");
            }
        }
        return paths;
    }

}