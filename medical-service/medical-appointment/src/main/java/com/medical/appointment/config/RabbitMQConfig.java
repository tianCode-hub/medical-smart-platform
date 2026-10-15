package com.medical.appointment.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 交换机声明（与 message 服务同名，幂等）：
 * appointment 为「预约成功通知」的生产端，只声明交换机，队列与绑定由 message 服务消费端声明。
 */
@Configuration
public class RabbitMQConfig {

    public static final String MEDICAL_EXCHANGE = "medical.message.direct";
    public static final String APPOINTMENT_SUCCESS_ROUTING = "appointment.success";

    @Bean
    public DirectExchange medicalMessageExchange() {
        return new DirectExchange(MEDICAL_EXCHANGE, true, false);
    }
}