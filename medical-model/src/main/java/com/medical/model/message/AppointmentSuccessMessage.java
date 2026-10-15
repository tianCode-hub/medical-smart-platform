package com.medical.model.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 预约成功通知消息：预约创建成功后由 appointment 服务发布，
 * message 服务消费并写入患者所属账号的站内消息。
 * 必须实现 Serializable：RabbitTemplate 默认使用 JDK 序列化
 */
@Data
public class AppointmentSuccessMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 接收通知的患者所属账号ID */
    private Integer userId;

    /** 医生主键ID（用于医生端展示/定位） */
    private Integer doctorId;

    /** 医生所属账号ID（用于给医生写入站内通知） */
    private Integer doctorUserId;

    /** 是否同时通知医生（无医生账号时为 false） */
    private Boolean notifyDoctor;

    /** 医生姓名 */
    private String doctorName;

    /** 科室名称 */
    private String departmentName;

    /** 就诊日期 yyyy-MM-dd */
    private String appointmentDate;

    /** 预约码 */
    private String code;
}