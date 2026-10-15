package com.medical.department;

import com.medical.feign.department.IDepartmentClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication(scanBasePackages = {"com.medical.department", "com.medical.utils"})
@MapperScan("com.medical.department.**.mapper")
@EnableFeignClients(clients = {IDepartmentClient.class})
@ComponentScan("com.medical")
public class DepartmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(DepartmentApplication.class, args);
    }
}