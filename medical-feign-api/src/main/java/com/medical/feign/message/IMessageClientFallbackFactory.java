package com.medical.feign.message;

import com.medical.model.message.LeaveNotifyCheckRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * IMessageClient 兜底降级：消息服务不可用 / 超时 / 异常时触发。
 * 返回空列表，视为「无待补发用户」，避免阻断请假通知主流程。
 */
@Slf4j
@Component
public class IMessageClientFallbackFactory implements FallbackFactory<IMessageClient> {

    @Override
    public IMessageClient create(Throwable cause) {
        return new IMessageClient() {
            @Override
            public List<Integer> filterNotNotified(LeaveNotifyCheckRequest request) {
                log.warn("调用消息服务失败，降级返回空列表：reason={}",
                        cause == null ? "未知" : cause.getMessage());
                return new ArrayList<>();
            }
        };
    }
}