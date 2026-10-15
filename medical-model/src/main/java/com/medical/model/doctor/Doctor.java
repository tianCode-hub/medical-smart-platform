package com.medical.model.doctor;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("doctor")
public class Doctor {

    @TableId(type = IdType.AUTO)
    private Integer doctorId;

    private Integer userId;
    private String username;
    private String specialty;
    private String title;
    private String education;
    private String licenseNumber;
    private Integer yearsOfPractice;
    private String hospital;
    private Integer departmentId;
    private String departmentName;
    private String workingHours;
    private String roomNumber;
    private String certifications;
    private String training;
    private String appointmentSlots;
    private Integer maxAppointmentsPerDay;
    private String employmentStatus;
}