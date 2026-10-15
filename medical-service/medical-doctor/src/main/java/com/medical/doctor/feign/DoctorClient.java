package com.medical.doctor.feign;

import com.medical.doctor.service.DoctorService;
import com.medical.feign.doctor.IDoctorClient;
import com.medical.model.doctor.Doctor;
import com.medical.utils.annotation.NoLoginCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class DoctorClient implements IDoctorClient {

    @Autowired
    private DoctorService doctorService;


    // 内部 Feign 调用不携带登录态，需免登录放行
    @Override
    @NoLoginCheck
    @GetMapping("/doctor/client/{doctorId}")
    public Doctor findDoctorById(@PathVariable("doctorId")Integer doctorId) {
        return doctorService.getById(doctorId);
    }

    @Override
    @NoLoginCheck
    @GetMapping("/doctor/client/userId/{userId}")
    public Doctor findUserId(@PathVariable("userId")Integer userId){
        return doctorService.getUserId(userId);
    }

    @Override
    @NoLoginCheck
    @GetMapping("/doctor/client/department/{departmentId}")
    public List<Doctor> findByDepartment(@PathVariable("departmentId") Integer departmentId) {
        return doctorService.getByDepartment(departmentId);
    }
}
