package com.medical.feign.message;

import com.medical.model.message.LeaveNotifyCheckRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 消息服务内部 Feign 客户端：供 doctor 等业务服务查询站内消息领域信息
 */
@FeignClient(value = "medical-message", fallbackFactory = IMessageClientFallbackFactory.class)
public interface IMessageClient {

    /**
     * 过滤出尚未收到指定医生请假通知的患者用户ID（请假通知补发去重用）
     */
    @PostMapping("/messages/client/filterNotNotified")
    List<Integer> filterNotNotified(@RequestBody LeaveNotifyCheckRequest request);
}