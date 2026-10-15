package com.medical.doctor.job;

import com.medical.doctor.config.RabbitMQConfig;
import com.medical.doctor.mapper.DoctorMapper;
import com.medical.feign.appointment.IAppointmentClient;
import com.medical.feign.message.IMessageClient;
import com.medical.feign.patient.IPatientClient;
import com.medical.model.appointment.Appointment;
import com.medical.model.doctor.Doctor;
import com.medical.model.message.DoctorLeaveMessage;
import com.medical.model.message.DoctorRecommendInfo;
import com.medical.model.message.LeaveNotifyCheckRequest;
import com.medical.model.patient.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 医生请假通知补偿定时任务（RabbitMQ 消息兜底）：
 * 如果某医生请假时因队列积压、消费失败或重启等导致患者未及时收到通知，
 * 本任务定期扫描处于「请假」状态的医生，找出已取消预约中尚未收到请假通知的患者，
 * 重新发布医生请假消息，由 message 服务异步补写站内消息，确保通知不遗漏。
 */
@Slf4j
@Component
public class DoctorLeaveCompensationJob {

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private IAppointmentClient iAppointmentClient;

    @Autowired
    private IMessageClient iMessageClient;

    @Autowired
    private IPatientClient iPatientClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 每 5 分钟补偿一次。去重依据：message 服务中已存在该医生对该用户的「医生请假通知」则跳过。
     */
    @Scheduled(fixedDelay = 300000)
    public void compensate() {
        List<Doctor> onLeave = doctorMapper.selectOnLeave();
        if (onLeave == null || onLeave.isEmpty()) {
            return;
        }
        for (Doctor doctor : onLeave) {
            try {
                compensateDoctor(doctor);
            } catch (Exception e) {
                log.error("医生请假通知补偿失败：doctorId={}", doctor.getDoctorId(), e);
            }
        }
    }

    private void compensateDoctor(Doctor doctor) {
        Integer doctorId = doctor.getDoctorId();
        List<Appointment> cancelled = iAppointmentClient.findCancelledByDoctor(doctorId);
        if (cancelled == null || cancelled.isEmpty()) {
            return;
        }

        // 聚合该医生所有已取消预约患者对应的用户ID（去重）
        List<Integer> allUserIds = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        for (Appointment a : cancelled) {
            if (a.getPatientId() == null) {
                continue;
            }
            try {
                Patient p = iPatientClient.findPatientById(a.getPatientId());
                if (p != null && p.getUserId() != null && seen.add(p.getUserId())) {
                    allUserIds.add(p.getUserId());
                }
            } catch (Exception e) {
                log.warn("查询患者 userId 失败：patientId={}", a.getPatientId(), e);
            }
        }
        if (allUserIds.isEmpty()) {
            return;
        }

        // 一次 Feign 调用（批量），由 message 服务过滤出尚未收到请假通知的患者，避免 N+1
        LeaveNotifyCheckRequest req = new LeaveNotifyCheckRequest();
        req.setDoctorId(doctorId);
        req.setUserIds(allUserIds);
        List<Integer> pendingUserIds = iMessageClient.filterNotNotified(req);
        if (pendingUserIds == null || pendingUserIds.isEmpty()) {
            return;
        }

        DoctorLeaveMessage msg = new DoctorLeaveMessage();
        msg.setDoctorId(doctorId);
        msg.setDoctorName(doctor.getUsername());
        msg.setUserIds(pendingUserIds);
        // 补偿通知同样附上同科室仍有余号的可改约医生
        msg.setRecommendDoctors(buildRecommendDoctors(doctor));
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MEDICAL_EXCHANGE,
                    RabbitMQConfig.DOCTOR_LEAVE_ROUTING,
                    msg);
            log.info("医生请假通知补偿发布：doctorId={}, 补发 {} 位患者", doctorId, pendingUserIds.size());
        } catch (Exception e) {
            log.error("补偿发布医生请假消息失败：doctorId={}", doctorId, e);
        }
    }

    /** 查询同科室仍有余号的可改约医生，供通知正文附推荐列表 */
    private List<DoctorRecommendInfo> buildRecommendDoctors(Doctor doctor) {
        List<DoctorRecommendInfo> result = new ArrayList<>();
        if (doctor == null || doctor.getDepartmentId() == null) {
            return result;
        }
        try {
            List<Map<String, Object>> recs = iAppointmentClient.available(doctor.getDepartmentId(), doctor.getDoctorId());
            if (recs != null) {
                for (Map<String, Object> r : recs) {
                    if (r == null || r.get("doctorId") == null) {
                        continue;
                    }
                    DoctorRecommendInfo info = new DoctorRecommendInfo();
                    info.setDoctorId(((Number) r.get("doctorId")).intValue());
                    info.setDoctorName((String) r.get("doctorName"));
                    info.setTitle((String) r.get("title"));
                    info.setRemaining(r.get("remaining") instanceof Number ? ((Number) r.get("remaining")).longValue() : null);
                    info.setMax(r.get("max") instanceof Number ? ((Number) r.get("max")).intValue() : null);
                    result.add(info);
                }
            }
        } catch (Exception e) {
            log.warn("补偿：查询同科室可改约医生失败：doctorId={}", doctor.getDoctorId(), e);
        }
        return result;
    }
}