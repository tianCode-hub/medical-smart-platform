package com.medical.message;


import com.medical.feign.message.IMessageClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication(scanBasePackages = {"com.medical.message", "com.medical.utils"})
@EnableDiscoveryClient
@MapperScan("com.medical.message.mapper")
@EnableFeignClients(clients = {IMessageClient.class})
@ComponentScan("com.medical")
public class MessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageApplication.class, args);
    }
}