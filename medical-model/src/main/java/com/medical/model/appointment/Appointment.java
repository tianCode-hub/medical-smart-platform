package com.medical.model.appointment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;


@Data
@TableName("appointment")
public class Appointment {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer patientId;
    private Integer doctorId;
    private Integer departmentId;
    private String status;
    private String money;
    private String code;
    private String campus;
    private String address;
    private String patientName;
    private String departmentPhone;
    private String doctorName;
    private String departmentName;

    @JsonFormat(pattern = "yyyy年MM月dd日")
    private Date appointmentDate;

    @JsonFormat(pattern = "yyyy年MM月dd日 HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy年MM月dd日 HH:mm:ss")
    private LocalDateTime updateTime;
}