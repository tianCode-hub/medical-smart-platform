package com.medical.appointment.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.medical.appointment.service.AppointmentService;
import com.medical.model.appointment.Appointment;
import com.medical.model.common.dtos.Result;
import com.medical.utils.annotation.Idempotent;
import com.medical.utils.annotation.NoLoginCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RefreshScope
@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @PostMapping
    @Idempotent
    @SentinelResource(value = "createAppointment", blockHandler = "createAppointmentBlockHandler")
    @PreAuthorize("hasRole('patient')")
    public Result createAppointment(@RequestBody Appointment appointment,@RequestHeader("id") Integer userId) {

        return appointmentService.createAppointment(appointment,userId);
    }

    /**
     * Sentinel 限流/熔断兜底：资源被流控或熔断时进入该方法，快速失败返回降级提示。
     */
    public Result createAppointmentBlockHandler(Appointment appointment, Integer userId, BlockException e) {
        return Result.error("系统繁忙，请稍后再试");
    }


    @GetMapping("/detail")
    @PreAuthorize("hasAnyRole('patient','doctor','admin')")
    public Result getAppointmentsByUserId(@RequestHeader(value = "id", required = false) Integer headerUserId,
                                          @RequestParam(required = false) Integer userId) {
        // 优先使用登录态用户ID，避免传入他人ID越权查询；旧前端未传 header 时兼容 query 参数
        Integer uid = headerUserId != null ? headerUserId : userId;
        return appointmentService.getAppointmentsByUserId(uid);
    }

    /**
     * 医生端预约列表：根据当前登录医生账号（请求头 id）返回其未取消预约，医生端独立页面用
     */
    @GetMapping("/doctor/list")
    @PreAuthorize("hasRole('doctor')")
    public Result getAppointmentsByDoctor(@RequestHeader("id") Integer userId) {
        return appointmentService.getAppointmentsByDoctorUser(userId);
    }

    /**
     * 医生端今日号源统计：当前登录医生（请求头 id）的每日上限与今日已约数
     */
    @GetMapping("/doctor/stats")
    @PreAuthorize("hasRole('doctor')")
    public Result getDoctorTodayStat(@RequestHeader("id") Integer userId) {
        return appointmentService.getDoctorTodayStat(userId);
    }

    @GetMapping("/client/detail")
    public Result getApUserId(@RequestParam Integer patientId) {

        return appointmentService.getApUserId(patientId);
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('patient','admin')")
    public Result deleteAppointment(@RequestParam Integer id, @RequestHeader("id") Integer userId) {
        return appointmentService.deleteAppointment(id, userId);
    }

    /**
     * 批量删除预约：请求体传入待删除的预约ID数组
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('admin')")
    public Result batchDeleteAppointment(@RequestBody List<Integer> ids) {
        return appointmentService.deleteBatch(ids);
    }

    /**
     * 推荐同科室仍有号源的医生（医生请假补救，全科室可见，免登录）
     */
    @NoLoginCheck
    @GetMapping("/available")
    public Result available(@RequestParam Integer departmentId,
                            @RequestParam(required = false) Integer excludeDoctorId) {
        return appointmentService.available(departmentId, excludeDoctorId);
    }

}