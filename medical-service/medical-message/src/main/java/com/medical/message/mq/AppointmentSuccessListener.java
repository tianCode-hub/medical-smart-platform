package com.medical.message.mq;

import com.medical.message.config.RabbitMQConfig;
import com.medical.message.mapper.MessageMapper;
import com.medical.model.message.AppointmentSuccessMessage;
import com.medical.model.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 预约成功通知消费者：从 RabbitMQ 异步接收（listener 并发线程池消费），
 * 为该患者账号写入一条「预约成功通知」站内消息。
 */
@Slf4j
@Component
public class AppointmentSuccessListener {

    @Autowired
    private MessageMapper messageMapper;

    @RabbitListener(queues = RabbitMQConfig.APPOINTMENT_SUCCESS_QUEUE)
    public void onAppointmentSuccess(AppointmentSuccessMessage message) {
        if (message == null || message.getUserId() == null) {
            return;
        }
        Message m = new Message();
        m.setUserId(message.getUserId());
        m.setDoctorId(null);
        m.setTitle("预约成功通知");
        m.setContent("您已成功预约【" + orEmpty(message.getDepartmentName())
                + (orEmpty(message.getDoctorName()).isEmpty() ? "" : "·" + orEmpty(message.getDoctorName())) + "】，"
                + "就诊日期：" + orEmpty(message.getAppointmentDate())
                + (orEmpty(message.getCode()).isEmpty() ? "" : "，预约码：" + orEmpty(message.getCode())) + "。"
                + "祝您早日康复！");
        m.setIsRead(0);
        m.setCreateTime(LocalDateTime.now());
        messageMapper.insert(m);
        log.info("RabbitMQ 消费预约成功通知：userId={}, doctor={}", message.getUserId(), message.getDoctorName());

        // 医生通知：患者预约成功后，同一条消息里若携带医生账号，则为医生也写入一条「新预约」站内消息
        if (Boolean.TRUE.equals(message.getNotifyDoctor()) && message.getDoctorUserId() != null) {
            Message dm = new Message();
            dm.setUserId(message.getDoctorUserId());
            dm.setDoctorId(message.getDoctorId());
            dm.setTitle("您有新的预约");
            dm.setContent("有患者预约了您于 " + orEmpty(message.getAppointmentDate()) + " 的就诊"
                    + (orEmpty(message.getCode()).isEmpty() ? "。" : "，预约码：" + orEmpty(message.getCode()) + "。")
                    + "请登录医生端查看详情并按时接诊。");
            dm.setIsRead(0);
            dm.setCreateTime(LocalDateTime.now());
            messageMapper.insert(dm);
            log.info("RabbitMQ 消费预约成功通知：已通知医生 userId={}", message.getDoctorUserId());
        }
    }

    private String orEmpty(String s) {
        return s == null ? "" : s;
    }
}