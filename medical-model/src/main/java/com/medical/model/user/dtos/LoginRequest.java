package com.medical.model.user.dtos;

import lombok.Data;


@Data
public class LoginRequest {
    private String phone;
    private String password;
    private String code;

}
