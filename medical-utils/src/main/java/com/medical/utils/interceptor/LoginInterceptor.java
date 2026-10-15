package com.medical.utils.interceptor;

import com.medical.utils.annotation.NoLoginCheck;
import com.medical.utils.common.JwtUtil;
import com.medical.utils.core.RedisUtils;
import com.medical.utils.thread.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录拦截器：
 * 1. 方法上标注了 @NoLoginCheck 则直接放行（无需登录）。
 * 2. 否则校验请求头中的 Token（同时校验 Redis 中的有效性），通过后把用户信息放入 ThreadLocal，
 *    并将角色构建为 Spring Security Authentication 注入 SecurityContext，供 @PreAuthorize 方法级授权使用。
 */
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只对 Controller 方法做拦截，静态资源直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 判断方法上是否有 @NoLoginCheck 注解，有则放行。
        // 使用 AnnotatedElementUtils 可同时识别：CGLIB 代理方法、接口方法上声明的注解
        boolean noLogin = AnnotatedElementUtils.hasAnnotation(method, NoLoginCheck.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), NoLoginCheck.class);
        if (noLogin) {
            return true;
        }

        // 获取 token
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasLength(token)) {
            response.setStatus(401);
            response.getWriter().write("请先登录");
            return false;
        }

        // 校验 Redis 中的 token 是否存在（是否有效）
        Object redisToken = RedisUtils.getValue(token);
        if (redisToken == null) {
            response.setStatus(401);
            response.getWriter().write("登录已过期，请重新登录");
            return false;
        }

        try {
            // 解析 token，获取用户信息
            Map<String, Object> claims = new HashMap<>(JwtUtil.parseToken(token));
            // 将原始 token 一并放入，供 Feign 透传使用
            claims.put("token", token);
            // 将用户信息放入 ThreadLocal，供后续业务使用
            ThreadLocalUtil.set(claims);
            // 构建 Spring Security Authentication，使 @PreAuthorize("hasRole('xxx')") 方法级授权生效
            buildSecurityContext(claims);
            return true;
        } catch (Exception e) {
            log.error("Token 校验失败", e);
            response.setStatus(401);
            response.getWriter().write("无效的登录凭证");
            return false;
        }
    }

    /**
     * 将登录用户信息构建为 Spring Security Authentication 注入 SecurityContextHolder。
     * 角色取自 JWT claims 中的 role（doctor / patient / admin / default），映射为 ROLE_xxx 权限，
     * 供接口上的 @PreAuthorize("hasRole('xxx')") / hasAnyRole 做 RBAC 授权。
     */
    private void buildSecurityContext(Map<String, Object> claims) {
        Object roleObj = claims.get("role");
        String role = roleObj == null ? "default" : String.valueOf(roleObj);
        List<GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(claims.get("id"), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理 ThreadLocal 与 SecurityContext，防止内存泄漏与跨请求串号
        ThreadLocalUtil.remove();
        SecurityContextHolder.clearContext();
    }
}