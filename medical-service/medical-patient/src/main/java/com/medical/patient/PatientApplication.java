package com.medical.patient;

import com.medical.feign.patient.IPatientClient;
import com.medical.patient.feign.PatientClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication(scanBasePackages = {"com.medical.patient", "com.medical.utils"})
@MapperScan("com.medical.patient.mapper")
@EnableFeignClients("com.medical.feign")
@ComponentScan("com.medical")
public class PatientApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientApplication.class, args);
    }

}