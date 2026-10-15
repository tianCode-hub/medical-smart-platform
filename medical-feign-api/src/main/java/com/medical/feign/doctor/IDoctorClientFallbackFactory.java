package com.medical.feign.doctor;

import com.medical.model.doctor.Doctor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * IDoctorClient 兜底降级：医生服务不可用 / 超时 / 异常时触发。
 * 返回 null（单条）或空集合，由调用方按「无数据」处理，避免故障扩散。
 */
@Slf4j
@Component
public class IDoctorClientFallbackFactory implements FallbackFactory<IDoctorClient> {

    @Override
    public IDoctorClient create(Throwable cause) {
        return new IDoctorClient() {
            @Override
            public Doctor findDoctorById(Integer doctorId) {
                log.warn("调用医生服务失败，降级返回 null：doctorId={}, reason={}",
                        doctorId, cause == null ? "未知" : cause.getMessage());
                return null;
            }

            @Override
            public Doctor findUserId(Integer userId) {
                log.warn("调用医生服务失败，降级返回 null：userId={}, reason={}",
                        userId, cause == null ? "未知" : cause.getMessage());
                return null;
            }

            @Override
            public List<Doctor> findByDepartment(Integer departmentId) {
                log.warn("调用医生服务失败，降级返回空列表：departmentId={}, reason={}",
                        departmentId, cause == null ? "未知" : cause.getMessage());
                return new ArrayList<>();
            }
        };
    }
}