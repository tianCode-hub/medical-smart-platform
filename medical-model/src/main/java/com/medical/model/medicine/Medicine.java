package com.medical.model.medicine;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@TableName("medicines")
public class Medicine {

    @TableId(type = IdType.AUTO)
    @NotNull
    private Long id;

    @NotEmpty
    private String name;

    private String efficacy;
    private String ingredients;
    private String specifications;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    // usage 是 MySQL 保留字，须用反引号转义，否则 MyBatis-Plus 生成的 SQL 报语法错误
    @TableField("`usage`")
    private String usage;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    @URL
    private String imageUrl;

    @NotNull
    @Min(0)
    private Integer stock;
}