package com.medical.feign.appointment;

import com.medical.model.appointment.Appointment;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 预约服务内部 Feign 客户端：供医生请假等跨服务流程调用
 */
@FeignClient(value = "medical-appointment", fallbackFactory = IAppointmentClientFallbackFactory.class)
public interface IAppointmentClient {

    /** 查询某医生所有未取消的预约 */
    @GetMapping("/appointments/client/byDoctor/{doctorId}")
    List<Appointment> findByDoctor(@PathVariable("doctorId") Integer doctorId);

    /** 查询某医生已取消的预约（请假通知补偿定时任务用） */
    @GetMapping("/appointments/client/cancelledByDoctor/{doctorId}")
    List<Appointment> findCancelledByDoctor(@PathVariable("doctorId") Integer doctorId);

    /** 医生请假：取消该医生所有未取消预约并释放号源，返回受影响数量（封装 Result） */
    @PostMapping("/appointments/client/cancelByDoctor/{doctorId}")
    Integer cancelByDoctor(@PathVariable("doctorId") Integer doctorId);

    /** 获取同科室仍有余号的可改约医生（请假补救推荐） */
    @PostMapping("/appointments/client/available")
    List<Map<String, Object>> available(@RequestParam("departmentId") Integer departmentId,
                                        @RequestParam("excludeDoctorId") Integer excludeDoctorId);
}