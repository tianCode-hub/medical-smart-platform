package com.medical.model.department;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("offices")
public class Offices {

    @TableId(type = IdType.AUTO)
    private Integer officeId;

    private String officeName;
}