package com.medical.utils.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置（方法级 RBAC 权限控制）。
 *
 * 设计定位：
 * 1. 鉴权（登录态校验）仍由 LoginInterceptor 基于 JWT + Redis 完成，此处不接管 URL 级过滤，
 *    anyRequest().permitAll() 保证与现有拦截器/白名单（@NoLoginCheck）体系互不干扰；
 * 2. 仅启用方法级安全 @EnableMethodSecurity，使接口通过 @PreAuthorize("hasRole('xxx')") 做角色授权，
 *    角色来源为 JWT claims 中的 role（doctor / patient / admin / default），由 LoginInterceptor 注入 SecurityContext。
 *
 * 条件装配：
 * - 仅 Servlet Web 应用且 classpath 存在 Spring Security 时生效；
 * - 业务服务（medical-service 子服务）引入 starter-security 后自动生效；
 * - 网关（WebFlux，未引入 security）扫描到本类时条件不满足自动跳过，避免类加载冲突。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({HttpSecurity.class, SecurityFilterChain.class})
public class SecurityConfig {

    /**
     * 放行所有请求：URL 级拦截交给 LoginInterceptor（含 @NoLoginCheck 白名单），
     * Spring Security 只负责方法级 @PreAuthorize 授权。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * 提供空 UserDetailsService，阻止 Spring Security 生成默认随机用户（避免启动日志打印默认密码）。
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }
}
