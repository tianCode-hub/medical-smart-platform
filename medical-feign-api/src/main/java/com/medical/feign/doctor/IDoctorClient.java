package com.medical.feign.doctor;

import com.medical.model.doctor.Doctor;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;


@FeignClient(value = "medical-doctor", fallbackFactory = IDoctorClientFallbackFactory.class)
public interface IDoctorClient {
    @GetMapping("/doctor/client/{doctorId}")
    Doctor findDoctorById(@PathVariable("doctorId") Integer doctorId);

    @GetMapping("/doctor/client/userId/{userId}")
    Doctor findUserId(@PathVariable("userId") Integer userId);

    @GetMapping("/doctor/client/department/{departmentId}")
    List<Doctor> findByDepartment(@PathVariable("departmentId") Integer departmentId);
}