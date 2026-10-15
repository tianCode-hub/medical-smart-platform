package com.medical.utils.config;

import com.github.pagehelper.PageInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * PageHelper 分页插件配置
 * <p>
 * 使用 pagehelper 核心库手动注册 PageInterceptor：
 * pagehelper-spring-boot-starter 的 PageHelperAutoConfiguration 会引用
 * org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration，
 * 而 Spring Boot 3 + MyBatis-Plus 场景下该类已不存在，会导致启动失败。
 * MyBatis-Plus 自动配置会自动收集容器中的 Interceptor Bean 加入插件链，因此注册 Bean 即可生效。
 */
@Configuration
public class PageHelperConfig {

    @Bean
    public PageInterceptor pageInterceptor() {
        PageInterceptor interceptor = new PageInterceptor();
        Properties properties = new Properties();
        // 数据库方言
        properties.setProperty("helperDialect", "mysql");
        // 页码合理化：页码越界时自动归一到首页/末页
        properties.setProperty("reasonable", "true");
        interceptor.setProperties(properties);
        return interceptor;
    }
}
