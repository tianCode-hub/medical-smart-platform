package com.medical.message.controller;

import com.medical.feign.message.IMessageClient;
import com.medical.message.mapper.MessageMapper;
import com.medical.model.message.LeaveNotifyCheckRequest;
import com.medical.utils.annotation.NoLoginCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息服务内部 Feign 实现：供 doctor 等业务服务调用，全部免登录放行
 */
@RestController
public class MessageClientController implements IMessageClient {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    @NoLoginCheck
    @PostMapping("/messages/client/filterNotNotified")
    public List<Integer> filterNotNotified(@RequestBody LeaveNotifyCheckRequest request) {
        List<Integer> result = new ArrayList<>();
        if (request == null || request.getDoctorId() == null || request.getUserIds() == null) {
            return result;
        }
        for (Integer userId : request.getUserIds()) {
            if (userId == null) {
                continue;
            }
            if (messageMapper.countLeaveNotify(request.getDoctorId(), userId) == 0) {
                result.add(userId);
            }
        }
        return result;
    }
}