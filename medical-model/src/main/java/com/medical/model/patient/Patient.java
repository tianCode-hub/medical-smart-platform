package com.medical.model.patient;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("patient")
public class Patient {

    @TableId(type = IdType.AUTO)
    private Integer patientId;

    private Integer userId;
    private Integer currentMedications;
    private String username;
    private String phone;
    private String medicalHistory;
    private String currentCondition;
    private String allergies;
    private String currentMedicationName;
    private String familyMedicalHistory;
    private String bloodType;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
}