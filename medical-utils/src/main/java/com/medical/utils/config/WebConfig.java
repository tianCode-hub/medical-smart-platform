package com.medical.utils.config;

import com.medical.utils.interceptor.IdempotentInterceptor;
import com.medical.utils.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 公共 Web 配置：为各业务服务注册全局登录拦截器与接口幂等拦截器。
 * 服务只需在启动类扫描 com.medical.utils 即可生效。
 * 方法上标注 @NoLoginCheck 的接口无需登录即可访问；
 * 方法上标注 @Idempotent 的接口会做防重复提交拦截。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/error");
        // 幂等拦截器：在登录校验之后执行，保证 ThreadLocal 中已有当前登录用户
        registry.addInterceptor(new IdempotentInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/error");
    }
}
