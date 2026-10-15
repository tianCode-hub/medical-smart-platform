package com.medical.model.message;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 医生请假通知消息体：doctor 服务发布、message 服务消费，跨 RabbitMQ 传输。
 * 必须实现 Serializable：RabbitTemplate 默认使用 JDK 序列化
 */
@Data
public class DoctorLeaveMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 请假医生ID */
    private Integer doctorId;

    /** 请假医生姓名 */
    private String doctorName;

    /** 需要通知的用户ID集合（该医生未取消预约对应的患者所属账号） */
    private List<Integer> userIds;

    /** 同科室仍有余号的可改约医生列表（提供给患者换约） */
    private List<DoctorRecommendInfo> recommendDoctors;
}