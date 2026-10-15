package com.medical.file.minio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 文件上传/下载专用线程池：将 OSS/MinIO 的网络 I/O 从请求线程分离，
 * 支持批量文件并行上传/下载，提升吞吐效率。拒绝策略：由调用方线程执行，避免任务丢失。
 */
@Configuration
public class FileThreadPoolConfig {

    @Bean("fileThreadPool")
    public ThreadPoolTaskExecutor fileThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("file-io-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}