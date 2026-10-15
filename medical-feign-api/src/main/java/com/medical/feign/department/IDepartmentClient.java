package com.medical.feign.department;

import com.medical.model.department.Department;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(value = "medical-department", fallbackFactory = IDepartmentClientFallbackFactory.class)
public interface IDepartmentClient {
    @GetMapping("/departments/client/{id}")
    Department findDepartmentById(@PathVariable("id") Integer id);
}