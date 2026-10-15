package com.medical.message.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.medical.message.mapper.MessageMapper;
import com.medical.message.service.MessageService;
import com.medical.model.common.dtos.Result;
import com.medical.model.message.Message;
import com.medical.utils.thread.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    /** 从登录态获取当前用户ID */
    private Integer currentUserId() {
        java.util.Map<String, Object> map = ThreadLocalUtil.get();
        return map == null ? null : ((Number) map.get("id")).intValue();
    }

    @Override
    public Result getMessages() {
        Integer userId = currentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .orderByDesc(Message::getCreateTime);
        return Result.success(messageMapper.selectList(wrapper));
    }

    @Override
    public Result unreadCount() {
        Integer userId = currentUserId();
        if (userId == null) {
            return Result.success(0);
        }
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0);
        return Result.success(messageMapper.selectCount(wrapper));
    }

    @Override
    public Result markRead(Integer id) {
        Integer userId = currentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        // 仅允许标记属于自己的消息，防止越权
        LambdaUpdateWrapper<Message> wrapper = new LambdaUpdateWrapper<Message>()
                .eq(Message::getId, id)
                .eq(Message::getUserId, userId)
                .set(Message::getIsRead, 1);
        int rows = messageMapper.update(null, wrapper);
        log.info("标记消息已读：id={}, userId={}, 生效={}", id, userId, rows);
        return Result.success();
    }
}