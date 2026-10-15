package com.medical.user;


import com.medical.feign.user.IUserClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication(scanBasePackages = {"com.medical.user", "com.medical.utils"})
@EnableDiscoveryClient
@MapperScan("com.medical.user.mapper")
@EnableFeignClients(clients = {IUserClient.class})
@ComponentScan("com.medical")
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class,args);
    }
}
