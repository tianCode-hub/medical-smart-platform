package com.medical.user.interceptor;



import com.medical.utils.core.RedisUtils;
import com.medical.utils.thread.ThreadLocalUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FeignAuthRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 获取当前请求的 Token
        String token = getTokenFromHeader();

        if (token != null) {
            // 从 Redis 中验证 Token 是否存在（即是否有效）
            Object redisToken = RedisUtils.getValue(token);

            if (redisToken != null) {
                // 如果 Token 有效，则将其附加到 Feign 请求的 Authorization 头中
                template.header("Authorization", token);
            }
        }
    }

    /**
     * 获取当前请求中的 Token，这里假设 Token 保存在 ThreadLocal 中。
     * 你可以根据实际情况调整获取 Token 的方法。
     */
    private String getTokenFromHeader() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims != null) {
            return (String) claims.get("token");  // 假设 Token 保存在 claims 中的 "token" 字段
        }
        return null;
    }
}