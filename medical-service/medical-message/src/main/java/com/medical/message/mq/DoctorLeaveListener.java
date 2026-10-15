package com.medical.message.mq;

import com.medical.message.config.RabbitMQConfig;
import com.medical.message.mapper.MessageMapper;
import com.medical.model.message.DoctorLeaveMessage;
import com.medical.model.message.DoctorRecommendInfo;
import com.medical.model.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 医生请假通知消费者：从 RabbitMQ 异步接收（listener 并发线程池，见 application.yml 中
 * listener.simple.concurrency），将请假通知写入各患者所属账号的站内消息。
 */
@Slf4j
@Component
public class DoctorLeaveListener {

    @Autowired
    private MessageMapper messageMapper;

    @RabbitListener(queues = RabbitMQConfig.DOCTOR_LEAVE_QUEUE)
    public void onDoctorLeave(DoctorLeaveMessage message) {
        List<Integer> userIds = message.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        String title = "医生请假通知";
        StringBuilder content = new StringBuilder();
        content.append("您预约的医生【").append(message.getDoctorName()).append("】今日紧急请假，该预约已被取消。")
                .append("\n").append("建议您改约以下同科室仍有余号的医生：");

        // 将候选人列表随正文给出，真正落实"提供其他还有号源的医生信息"
        List<DoctorRecommendInfo> recs = message.getRecommendDoctors();
        if (recs != null && !recs.isEmpty()) {
            for (DoctorRecommendInfo d : recs) {
                content.append("\n- ").append(d.getDoctorName())
                        .append(d.getTitle() == null || d.getTitle().isEmpty() ? "" : "（" + d.getTitle() + "）");
                if (d.getRemaining() != null) {
                    content.append("，今日余号 ").append(d.getRemaining());
                }
            }
        } else {
            content.append("\n- 暂无余号信息，请前往「预约管理」查看同科室其他医生");
        }
        content.append("\n给您带来不便敬请谅解。");

        LocalDateTime now = LocalDateTime.now();
        for (Integer userId : userIds) {
            Message m = new Message();
            m.setUserId(userId);
            m.setDoctorId(message.getDoctorId());
            m.setTitle(title);
            m.setContent(content.toString());
            m.setIsRead(0);
            m.setCreateTime(now);
            messageMapper.insert(m);
        }
        log.info("RabbitMQ 消费医生请假通知：doctorId={}, 写入 {} 条站内消息",
                message.getDoctorId(), userIds.size());
    }
}