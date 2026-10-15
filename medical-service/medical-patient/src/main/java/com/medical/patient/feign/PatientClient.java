package com.medical.patient.feign;

import com.medical.feign.patient.IPatientClient;

import com.medical.model.patient.Patient;

import com.medical.patient.service.PatientService;
import com.medical.utils.annotation.NoLoginCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class PatientClient implements IPatientClient {
    @Autowired
    private PatientService patientService;

    // 内部 Feign 调用不携带登录态，需免登录放行
    @Override
    @NoLoginCheck
    @GetMapping("/patients/client/{patientId}")
    public Patient findPatientById(@PathVariable("patientId") Integer patientId) {
        return patientService.getById(patientId);
    }

    @Override
    @NoLoginCheck
    @GetMapping("/patients/client/userId/{userId}")
    public Patient findUserId(@PathVariable("userId") Integer userId){
        return patientService.findUserId(userId);
    }

}
