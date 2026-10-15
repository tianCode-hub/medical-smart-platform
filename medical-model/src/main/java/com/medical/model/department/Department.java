package com.medical.model.department;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("department")
public class Department {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer officeId;
    private String name;
    private String description;
    private String campus;
    private String address;
}