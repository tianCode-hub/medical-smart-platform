package com.medical.model.doctor;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;


@Data
@TableName("doctoremployment")
public class DoctorEmployment {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer doctorId;
    private String doctorName;
    private String identity;
    private String employmentStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date hireDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date resignationDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}