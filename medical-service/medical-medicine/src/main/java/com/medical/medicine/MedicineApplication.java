package com.medical.medicine;

import com.medical.feign.medicine.IMedicineClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication(scanBasePackages = {"com.medical.medicine", "com.medical.utils"})
@MapperScan("com.medical.medicine.mapper")
@EnableFeignClients(clients = {IMedicineClient.class})
@ComponentScan("com.medical")
public class MedicineApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicineApplication.class, args);
    }

}