package com.medical.doctor;

import com.medical.doctor.feign.DoctorClient;
import com.medical.feign.doctor.IDoctorClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(scanBasePackages = {"com.medical.doctor", "com.medical.utils"})
@MapperScan("com.medical.doctor.mapper")
@EnableFeignClients("com.medical.feign")
@EnableScheduling
@ComponentScan("com.medical")
public class DoctorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoctorApplication.class, args);
    }

}