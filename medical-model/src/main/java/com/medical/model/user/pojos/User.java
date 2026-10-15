package com.medical.model.user.pojos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    @NotNull
    private Integer id;

    private String username;

    @JsonIgnore
    private String password;

    @Pattern(regexp = "^(男|女)?$")
    private String gender;

    private LocalDate birth;

    @Pattern(regexp = "^[0-9]{10,20}$")
    private String phone;

    private String address;
    private String identity;

    @NotEmpty
    @Email
    private String email;

    private String userPic;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @NotEmpty
    @Pattern(regexp = "^(doctor|patient|admin|default)$")
    private String role;
}