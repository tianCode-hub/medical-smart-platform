package com.medical.message.config;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息反序列化白名单：
 * Spring AMQP 3.x 默认禁止反序列化非白名单的自定义类（JDK 序列化）。
 * 此处放行 medical-model 中的跨服务消息 DTO，否则 @RabbitListener 消费会抛
 * SecurityException: Attempt to deserialize unauthorized class ...*
 */
@Configuration
public class MessageConverterConfig {

    @Bean
    public MessageConverter messageConverter() {
        // 基于 SimpleMessageConverter，额外放行两类：
        // 1) 本项目跨服务消息 DTO（medical-model 的 message 包）
        // 2) JDK 常见集合类型（ArrayList/HashSet 等）。Spring AMQP 3.x 安全白名单默认不信任 java.util.* 集合，
        //    而 DoctorLeaveMessage.userIds/recommendDoctors 反序列化需要它们。
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.addAllowedListPatterns(
                "com.medical.model.message.*",
                "java.util.*",
                "java.lang.*",
                "java.time.*",
                "java.io.Serializable");
        return converter;
    }
}