package com.medical.message.controller;

import com.medical.message.service.MessageService;
import com.medical.model.common.dtos.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 站内消息：需登录，按接口需登录访问（不加 @NoLoginCheck）
 */
@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /** 当前登录用户的消息列表 */
    @GetMapping
    @PreAuthorize("hasAnyRole('doctor','patient')")
    public Result getMessages() {
        return messageService.getMessages();
    }

    /** 未读消息数 */
    @GetMapping("/unreadCount")
    @PreAuthorize("hasAnyRole('doctor','patient')")
    public Result unreadCount() {
        return messageService.unreadCount();
    }

    /** 标记已读 */
    @PutMapping("/read")
    @PreAuthorize("hasAnyRole('doctor','patient')")
    public Result markRead(@RequestParam Integer id) {
        return messageService.markRead(id);
    }
}