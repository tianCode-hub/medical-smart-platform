package com.medical.file.minio.controller;


import com.medical.file.minio.service.FileStorageService;
import com.medical.file.minio.util.MinioUtil;
import com.medical.model.common.dtos.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Future;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@RefreshScope
@RestController
@RequestMapping("/files")
public class FileUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    @Qualifier("fileThreadPool")
    private ThreadPoolTaskExecutor fileThreadPool;

    /**
     * 通用文件上传：自动校验、防重名、自动识别 ContentType、自动建桶
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            return Result.success(minioUtil.upload(file));
        } catch (Exception e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam(value = "prefix", required = false, defaultValue = "defaultPrefix") String prefix,
                                      @RequestParam("file") MultipartFile file) {
        try {
            minioUtil.validateFile(file);
            String fileName = minioUtil.buildFileName(file.getOriginalFilename());
            String filePath = fileStorageService.uploadImgFile(prefix, fileName, file.getInputStream());
            return Result.success(filePath);
        } catch (Exception e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/upload/html")
    public Result<String> uploadHtml(@RequestParam(value = "prefix", required = false, defaultValue = "defaultPrefix") String prefix,
                                     @RequestParam("file") MultipartFile file) {
        try {
            minioUtil.validateFile(file);
            String fileName = minioUtil.buildFileName(file.getOriginalFilename());
            String filePath = fileStorageService.uploadHtmlFile(prefix, fileName, file.getInputStream());
            return Result.success(filePath);
        } catch (Exception e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public Result<Void> deleteFile(@RequestParam("pathUrl") String pathUrl) {
        try {
            fileStorageService.delete(pathUrl);
            return Result.success();
        } catch (Exception e) {
            return Result.error("文件删除失败：" + e.getMessage());
        }
    }

    /**
     * 文件下载（流式响应，支持浏览器直接预览/下载）
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("pathUrl") String pathUrl) {
        try {
            byte[] fileData = fileStorageService.downLoadFile(pathUrl);

            // 从 URL 中提取文件名（最后一个 / 之后的内容，已 URL 解码）
            String fileName = pathUrl.substring(pathUrl.lastIndexOf("/") + 1);
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment;filename=\"" + fileName + "\"")
                    .body(fileData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 批量上传：多个文件通过文件线程池并行上传，返回各文件的访问 URL 列表
     */
    @PostMapping("/upload/batch")
    public Result<List<String>> uploadBatch(@RequestParam(value = "prefix", required = false, defaultValue = "img") String prefix,
                                            @RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.error("请至少选择一个文件");
        }
        try {
            List<Future<String>> futures = new ArrayList<>(files.length);
            for (MultipartFile file : files) {
                minioUtil.validateFile(file);
                String fileName = minioUtil.buildFileName(file.getOriginalFilename());
                futures.add(fileThreadPool.submit(() -> fileStorageService.uploadImgFile(prefix, fileName, file.getInputStream())));
            }
            List<String> urls = new ArrayList<>(files.length);
            for (Future<String> f : futures) {
                urls.add(f.get());
            }
            return Result.success(urls);
        } catch (Exception e) {
            return Result.error("批量上传失败：" + e.getMessage());
        }
    }

    /**
     * 批量下载：多个文件在文件线程池中并行下载，返回 base64 内容列表
     */
    @GetMapping("/download/batch")
    public Result<List<String>> downloadBatch(@RequestParam("pathUrls") List<String> pathUrls) {
        try {
            List<byte[]> bytes = fileStorageService.batchDownLoadFile(pathUrls);
            List<String> base64List = new ArrayList<>(bytes.size());
            for (byte[] b : bytes) {
                base64List.add(Base64.getEncoder().encodeToString(b));
            }
            return Result.success(base64List);
        } catch (Exception e) {
            return Result.error("批量下载失败：" + e.getMessage());
        }
    }
}