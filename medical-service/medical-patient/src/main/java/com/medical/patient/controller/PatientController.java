package com.medical.patient.controller;

import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.patient.Patient;
import com.medical.patient.service.PatientService;
import com.medical.utils.annotation.NoLoginCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RefreshScope
@RestController
@RequestMapping("/patient")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @NoLoginCheck
    @GetMapping("/{id}")
    public Result<Patient> getPatientById(@PathVariable Integer id) {
        Patient patient = patientService.getById(id);
        return patient != null ? Result.success(patient) : Result.error("患者不存在");
    }

    /**
     * 分页 + 条件搜索查询患者
     *
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @param keyword  患者姓名/电话关键字（模糊匹配，可为空）
     */
    @NoLoginCheck
    @GetMapping
    public Result<PageBean<Patient>> getAllPatients(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(required = false) String keyword) {
        return Result.success(patientService.findAll(pageNum, pageSize, keyword));
    }


    @PostMapping
    @PreAuthorize("hasAnyRole('patient','admin')")
    public Result<String> createPatient(@RequestBody Patient patient) {
        return patientService.insert(patient);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('patient','admin')")
    public Result<String> updatePatient(@PathVariable int id, @RequestBody Patient patient) {
        return patientService.update(patient, id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<String> deletePatient(@PathVariable int id) {

        return patientService.delete(id);
    }

    /**
     * 批量删除患者：请求体传入待删除的患者ID数组
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('admin')")
    public Result<String> batchDeletePatient(@RequestBody List<Integer> ids) {
        return patientService.deleteBatch(ids);
    }
}
