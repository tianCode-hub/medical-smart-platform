package com.medical.feign.patient;

import com.medical.model.patient.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * IPatientClient 兜底降级：患者服务不可用 / 超时 / 异常时触发，返回 null。
 */
@Slf4j
@Component
public class IPatientClientFallbackFactory implements FallbackFactory<IPatientClient> {

    @Override
    public IPatientClient create(Throwable cause) {
        return new IPatientClient() {
            @Override
            public Patient findPatientById(Integer patientId) {
                log.warn("调用患者服务失败，降级返回 null：patientId={}, reason={}",
                        patientId, cause == null ? "未知" : cause.getMessage());
                return null;
            }

            @Override
            public Patient findUserId(Integer userId) {
                log.warn("调用患者服务失败，降级返回 null：userId={}, reason={}",
                        userId, cause == null ? "未知" : cause.getMessage());
                return null;
            }
        };
    }
}