package com.medical.message.service;

import com.medical.model.common.dtos.Result;

/**
 * 站内消息服务：查询、未读数、标记已读
 */
public interface MessageService {
    /** 查询当前登录用户的消息（倒序） */
    Result getMessages();
    /** 查询当前登录用户未读消息数 */
    Result unreadCount();
    /** 将指定消息标记为已读（仅限当前登录用户自己的消息） */
    Result markRead(Integer id);
}