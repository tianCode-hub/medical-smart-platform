package com.medical.model.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 医生请假补救推荐的可改约医生信息：同科室仍有余号的在职医生。
 * 作为 DoctorLeaveMessage 的成员，必须实现 Serializable（JDK 序列化）
 */
@Data
public class DoctorRecommendInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 医生ID */
    private Integer doctorId;

    /** 医生姓名 */
    private String doctorName;

    /** 职称 */
    private String title;

    /** 今日剩余号源 */
    private Long remaining;

    /** 每日最大号源 */
    private Integer max;
}