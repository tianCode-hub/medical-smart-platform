package com.medical.doctor.controller;


import com.medical.doctor.service.DoctorEmploymentService;
import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.doctor.DoctorEmployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/doctorEmployment")
public class DoctorEmploymentController {

    @Autowired
    private DoctorEmploymentService doctorEmploymentService;

    /**
     * 分页查询雇佣记录，支持按医生姓名/身份证号模糊搜索
     */
    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public Result<PageBean<DoctorEmployment>> getAllEmployments(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                                @RequestParam(required = false) String keyword){
        PageBean<DoctorEmployment> de=doctorEmploymentService.findlist(pageNum,pageSize,keyword);
        return Result.success(de);
    }

    @PutMapping
    @PreAuthorize("hasRole('admin')")
    public Result update(@RequestBody @Validated DoctorEmployment doctorEmployment){
        doctorEmploymentService.update(doctorEmployment);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result deleteEmployment(@PathVariable Integer id){
        doctorEmploymentService.delete(id);
        return Result.success();
    }
}
