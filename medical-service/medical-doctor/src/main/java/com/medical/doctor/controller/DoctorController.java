package com.medical.doctor.controller;


import com.medical.doctor.service.DoctorService;
import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.doctor.Doctor;
import com.medical.utils.annotation.NoLoginCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RefreshScope
@RestController
@RequestMapping("/doctor")
public class DoctorController {
    @Autowired
    private DoctorService doctorService;

    /**
     * 分页 + 条件搜索查询医生
     *
     * @param pageNum      页码，默认 1
     * @param pageSize     每页条数，默认 10
     * @param keyword      姓名/专长/职称关键字（模糊匹配，可为空）
     * @param departmentId 科室ID（可为空）
     */
    @NoLoginCheck
    @GetMapping
    public Result<PageBean<Doctor>> getAllDoctors(@RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Integer departmentId) {
        PageBean<Doctor> doctors = doctorService.findAll(pageNum, pageSize, keyword, departmentId);

        return Result.success(doctors);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result deleteDoctor(@PathVariable Integer id) {
        doctorService.deleteUser(id);
        return Result.success();
    }

    /**
     * 批量删除医生：请求体传入待删除的医生ID数组
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('admin')")
    public Result batchDeleteDoctor(@RequestBody List<Integer> ids) {
        return doctorService.deleteBatch(ids);
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('admin')")
    public Result update(@RequestBody @Validated Doctor doctor) {
        doctorService.update(doctor);
        return Result.success();
    }

    /**
     * 医生紧急请假：自动取消其未就诊预约，并向已挂号患者发送站内消息
     */
    @PutMapping("/leave/{id}")
    @PreAuthorize("hasRole('doctor')")
    public Result leave(@PathVariable Integer id) {
        return doctorService.leave(id);
    }

    @NoLoginCheck
    @GetMapping("/getdepartment")
    public Result getdepartment(@RequestParam("departmentId") Integer departmentId){
        return doctorService.getdepartment(departmentId);
    }
}
