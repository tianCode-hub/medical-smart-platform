package com.medical.feign.medicine;

import com.medical.model.medicine.Medicine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * IMedicineClient 兜底降级：药品服务不可用 / 超时 / 异常时触发，返回 null。
 */
@Slf4j
@Component
public class IMedicineClientFallbackFactory implements FallbackFactory<IMedicineClient> {

    @Override
    public IMedicineClient create(Throwable cause) {
        return new IMedicineClient() {
            @Override
            public Medicine findMedicineById(Integer id) {
                log.warn("调用药品服务失败，降级返回 null：id={}, reason={}",
                        id, cause == null ? "未知" : cause.getMessage());
                return null;
            }
        };
    }
}