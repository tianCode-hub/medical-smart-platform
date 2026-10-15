package com.medical.utils.interceptor;

import com.medical.utils.annotation.Idempotent;
import com.medical.utils.core.RedisUtils;
import com.medical.utils.thread.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 接口幂等拦截器：
 * 1. 仅对标注了 @Idempotent 的 Controller 方法生效。
 * 2. 基于「登录用户 id + 请求 URI」生成唯一 Key，用 Redis SETNX 原子写入。
 *    短时间内同一用户对同一接口的重复请求会被拒绝（返回重复提交提示），达到防重提交的目的。
 * 3. Key 带 TTL 自动过期，不阻塞合法请求；请求终结后不主动删除，由过期时间兜底。
 */
@Slf4j
public class IdempotentInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENT_PREFIX = "idempotent:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 仅对标注了 @Idempotent 的方法做防重拦截
        Idempotent idempotent = AnnotatedElementUtils.findMergedAnnotation(method, Idempotent.class);
        if (idempotent == null) {
            return true;
        }

        // 从 ThreadLocal 取当前登录用户 id（LoginInterceptor 已放入）
        String userId = "anonymous";
        Object ctx = ThreadLocalUtil.get();
        if (ctx instanceof Map) {
            Object id = ((Map<?, ?>) ctx).get("id");
            if (id != null) {
                userId = id.toString();
            }
        }

        String key = IDEMPOTENT_PREFIX + userId + ":" + request.getMethod() + ":" + request.getRequestURI();
        boolean success = RedisUtils.setIfAbsent(key, "1", idempotent.timeout(), TimeUnit.SECONDS);
        if (!success) {
            log.warn("检测到重复提交，已拦截：userId={}, {} {}", userId, request.getMethod(), request.getRequestURI());
            response.setStatus(409);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":1,\"message\":\"请勿重复提交，请稍后重试\"}");
            return false;
        }
        return true;
    }
}