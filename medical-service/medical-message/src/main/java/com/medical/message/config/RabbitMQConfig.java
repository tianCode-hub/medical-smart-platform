package com.medical.message.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 交换机/队列/绑定声明（与 doctor 服务同名声明，Broker 幂等）
 */
@Configuration
public class RabbitMQConfig {

    public static final String MEDICAL_EXCHANGE = "medical.message.direct";
    public static final String DOCTOR_LEAVE_QUEUE = "doctor.leave.queue";
    public static final String DOCTOR_LEAVE_ROUTING = "doctor.leave";
    public static final String APPOINTMENT_SUCCESS_QUEUE = "appointment.success.queue";
    public static final String APPOINTMENT_SUCCESS_ROUTING = "appointment.success";

    @Bean
    public DirectExchange medicalMessageExchange() {
        return new DirectExchange(MEDICAL_EXCHANGE, true, false);
    }

    @Bean
    public Queue doctorLeaveQueue() {
        return new Queue(DOCTOR_LEAVE_QUEUE, true);
    }

    @Bean
    public Binding doctorLeaveBinding(DirectExchange medicalMessageExchange, Queue doctorLeaveQueue) {
        return BindingBuilder.bind(doctorLeaveQueue).to(medicalMessageExchange).with(DOCTOR_LEAVE_ROUTING);
    }

    @Bean
    public Queue appointmentSuccessQueue() {
        return new Queue(APPOINTMENT_SUCCESS_QUEUE, true);
    }

    @Bean
    public Binding appointmentSuccessBinding(DirectExchange medicalMessageExchange, Queue appointmentSuccessQueue) {
        return BindingBuilder.bind(appointmentSuccessQueue).to(medicalMessageExchange).with(APPOINTMENT_SUCCESS_ROUTING);
    }
}