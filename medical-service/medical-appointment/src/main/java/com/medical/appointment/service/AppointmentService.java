package com.medical.appointment.service;

import com.medical.model.appointment.Appointment;
import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;

import java.time.LocalDate;
import java.util.List;



public interface AppointmentService {
    Result createAppointment(Appointment appointment,Integer userId);
    Result getAppointmentsByUserId(Integer userId);

    void updateAppointment(Appointment appointment);
    Result deleteAppointment(Integer id, Integer userId);





    Result getApUserId(Integer patientId);

    Result deleteBatch(List<Integer> ids);

    /** 查询某医生所有未取消的预约（Feign 内部、请假处理用） */
    List<Appointment> findByDoctor(Integer doctorId);

    /** 按当前登录医生账号（userId）查询其预约列表（医生端展示，带患者/科室名） */
    Result getAppointmentsByDoctorUser(Integer userId);

    /** 按当前登录医生账号（userId）查询今日号源统计：max（每日上限）与今日已约数 */
    Result getDoctorTodayStat(Integer userId);

    /** 查询某医生已取消的预约（Feign 内部、请假通知补偿用） */
    List<Appointment> findCancelledByDoctor(Integer doctorId);

    /** 医生请假：取消该医生所有未取消预约并释放号源，返回受影响数量 */
    Result cancelByDoctor(Integer doctorId);

    /** 推荐同科室仍有号源的医生（请假补救） */
    Result available(Integer departmentId, Integer excludeDoctorId);
}