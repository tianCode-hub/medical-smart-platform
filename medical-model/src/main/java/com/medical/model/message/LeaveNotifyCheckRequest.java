package com.medical.model.message;

import lombok.Data;

import java.util.List;

/**
 * 医生请假通知补发去重请求体：doctor 服务告知某请假医生及其候选患者用户，
 * 由消息服务返回其中尚未收到「医生请假通知」的用户列表（一次 Feign 调用批量判定，避免 N+1）。
 */
@Data
public class LeaveNotifyCheckRequest {

    /** 请假医生ID */
    private Integer doctorId;

    /** 待校验是否已通知的患者用户ID列表 */
    private List<Integer> userIds;
}