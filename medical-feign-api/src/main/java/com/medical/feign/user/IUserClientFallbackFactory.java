package com.medical.feign.user;

import com.medical.model.user.pojos.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * IUserClient 兜底降级：用户服务不可用 / 超时 / 异常时触发，返回 null。
 */
@Slf4j
@Component
public class IUserClientFallbackFactory implements FallbackFactory<IUserClient> {

    @Override
    public IUserClient create(Throwable cause) {
        return new IUserClient() {
            @Override
            public User findUserById(Integer id) {
                log.warn("调用用户服务失败，降级返回 null：id={}, reason={}",
                        id, cause == null ? "未知" : cause.getMessage());
                return null;
            }
        };
    }
}