package com.medical.model.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息：用于向用户推送医生请假、号源调整等通知
 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 收件人用户ID */
    private Integer userId;

    /** 来源医生ID（如：XXX医生请假） */
    private Integer doctorId;

    private String title;

    private String content;

    /** 0-未读 1-已读 */
    private Integer isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}