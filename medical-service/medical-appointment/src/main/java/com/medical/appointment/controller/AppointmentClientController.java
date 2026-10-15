package com.medical.appointment.controller;

import com.medical.appointment.service.AppointmentService;
import com.medical.feign.appointment.IAppointmentClient;
import com.medical.model.appointment.Appointment;
import com.medical.model.common.dtos.Result;
import com.medical.utils.annotation.NoLoginCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 预约服务内部 Feign 实现：跨服务（医生请假）调用，全部免登录放行
 */
@RestController
public class AppointmentClientController implements IAppointmentClient {

    @Autowired
    private AppointmentService appointmentService;

    @Override
    @NoLoginCheck
    @GetMapping("/appointments/client/byDoctor/{doctorId}")
    public List<Appointment> findByDoctor(@PathVariable("doctorId") Integer doctorId) {
        return appointmentService.findByDoctor(doctorId);
    }

    @Override
    @NoLoginCheck
    @GetMapping("/appointments/client/cancelledByDoctor/{doctorId}")
    public List<Appointment> findCancelledByDoctor(@PathVariable("doctorId") Integer doctorId) {
        return appointmentService.findCancelledByDoctor(doctorId);
    }

    @Override
    @NoLoginCheck
    @PostMapping("/appointments/client/cancelByDoctor/{doctorId}")
    public Integer cancelByDoctor(@PathVariable("doctorId") Integer doctorId) {
        Result result = appointmentService.cancelByDoctor(doctorId);
        Object data = result.getData();
        return data instanceof Number ? ((Number) data).intValue() : 0;
    }

    @Override
    @NoLoginCheck
    @PostMapping("/appointments/client/available")
    public List<Map<String, Object>> available(@RequestParam Integer departmentId,
                                               @RequestParam Integer excludeDoctorId) {
        Result result = appointmentService.available(departmentId, excludeDoctorId);
        Object data = result.getData();
        return data instanceof List ? (List<Map<String, Object>>) data : new ArrayList<>();
    }
}