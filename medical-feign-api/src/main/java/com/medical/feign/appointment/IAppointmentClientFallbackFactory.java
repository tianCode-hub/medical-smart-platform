package com.medical.feign.appointment;

import com.medical.model.appointment.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * IAppointmentClient 兜底降级：预约服务不可用 / 超时 / 异常时触发。
 * 取消类方法返回 0（视为未处理），查询类方法返回空集合，避免阻断医生请假流程。
 */
@Slf4j
@Component
public class IAppointmentClientFallbackFactory implements FallbackFactory<IAppointmentClient> {

    @Override
    public IAppointmentClient create(Throwable cause) {
        return new IAppointmentClient() {
            @Override
            public List<Appointment> findByDoctor(Integer doctorId) {
                log.warn("调用预约服务失败，降级返回空列表：doctorId={}, reason={}",
                        doctorId, cause == null ? "未知" : cause.getMessage());
                return new ArrayList<>();
            }

            @Override
            public List<Appointment> findCancelledByDoctor(Integer doctorId) {
                log.warn("调用预约服务失败，降级返回空列表：doctorId={}, reason={}",
                        doctorId, cause == null ? "未知" : cause.getMessage());
                return new ArrayList<>();
            }

            @Override
            public Integer cancelByDoctor(Integer doctorId) {
                log.warn("调用预约服务取消失败，降级返回 0：doctorId={}, reason={}",
                        doctorId, cause == null ? "未知" : cause.getMessage());
                return 0;
            }

            @Override
            public List<Map<String, Object>> available(Integer departmentId, Integer excludeDoctorId) {
                log.warn("调用预约服务查询可改约医生失败，降级返回空列表：departmentId={}, reason={}",
                        departmentId, cause == null ? "未知" : cause.getMessage());
                return new ArrayList<>();
            }
        };
    }
}