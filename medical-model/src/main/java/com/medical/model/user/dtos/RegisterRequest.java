package com.medical.model.user.dtos;


import jakarta.validation.constraints.Pattern;
import lombok.Data;



@Data
public class RegisterRequest {
    private  String token;
    @Pattern(regexp = "^\\S{5,16}$")
    private String phone;
    private String username;
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)?\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])\\d{3}(\\d|X|x)$")
    private String identity;
    @Pattern(regexp = "^\\S{5,16}$")
    private String password;
    @Pattern(regexp = "^(doctor|patient|admin)$")
    private String role;

}
