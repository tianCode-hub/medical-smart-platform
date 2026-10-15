package com.medical.feign.department;

import com.medical.model.department.Department;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * IDepartmentClient 兜底降级：科室服务不可用 / 超时 / 异常时触发，返回 null。
 */
@Slf4j
@Component
public class IDepartmentClientFallbackFactory implements FallbackFactory<IDepartmentClient> {

    @Override
    public IDepartmentClient create(Throwable cause) {
        return new IDepartmentClient() {
            @Override
            public Department findDepartmentById(Integer id) {
                log.warn("调用科室服务失败，降级返回 null：id={}, reason={}",
                        id, cause == null ? "未知" : cause.getMessage());
                return null;
            }
        };
    }
}