package com.medical.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.appointment.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    /**
     * 联表查询患者预约列表（带患者姓名、医生姓名、科室信息），一次查询避免循环 Feign 调用的 N+1 问题
     */
    @Select("SELECT COALESCE(pu.username, p.username) AS patient_name, " +
            "du.username AS doctor_name, " +
            "dep.name AS department_name, " +
            "dep.campus AS campus, " +
            "dep.address AS address, " +
            "a.id, a.patient_id, a.doctor_id, a.department_id, a.status, a.money, a.code, " +
            "a.appointment_date, a.create_time, a.update_time " +
            "FROM appointment a " +
            "LEFT JOIN patient p ON a.patient_id = p.patient_id " +
            "LEFT JOIN user pu ON p.user_id = pu.id " +
            "LEFT JOIN doctor d ON a.doctor_id = d.doctor_id " +
            "LEFT JOIN user du ON d.user_id = du.id " +
            "LEFT JOIN department dep ON a.department_id = dep.id " +
            "WHERE a.patient_id = #{patientId} " +
            "ORDER BY a.id DESC")
    List<Appointment> findDetailByPatientId(@Param("patientId") Integer patientId);

    /**
     * 联表查询某医生所有未取消的预约（带患者姓名、科室信息），医生端预约列表用
     */
    @Select("SELECT COALESCE(pu.username, p.username) AS patient_name, " +
            "du.username AS doctor_name, " +
            "dep.name AS department_name, " +
            "dep.campus AS campus, " +
            "dep.address AS address, " +
            "a.id, a.patient_id, a.doctor_id, a.department_id, a.status, a.money, a.code, " +
            "a.appointment_date, a.create_time, a.update_time " +
            "FROM appointment a " +
            "LEFT JOIN patient p ON a.patient_id = p.patient_id " +
            "LEFT JOIN user pu ON p.user_id = pu.id " +
            "LEFT JOIN doctor d ON a.doctor_id = d.doctor_id " +
            "LEFT JOIN user du ON d.user_id = du.id " +
            "LEFT JOIN department dep ON a.department_id = dep.id " +
            "WHERE a.doctor_id = #{doctorId} AND (a.status IS NULL OR a.status != '已取消') " +
            "ORDER BY a.id DESC")
    List<Appointment> findDetailByDoctorId(@Param("doctorId") Integer doctorId);

    /**
     * 查询某医生所有未取消的预约
     */
    @Select("SELECT * FROM appointment WHERE doctor_id = #{doctorId} AND (status IS NULL OR status != '已取消')")
    List<Appointment> findByDoctorActive(@Param("doctorId") Integer doctorId);

    /**
     * 统计某医生未取消的预约数（作为 Redis 缺失时的兜底计数）
     */
    @Select("SELECT COUNT(*) FROM appointment WHERE doctor_id = #{doctorId} AND (status IS NULL OR status != '已取消')")
    long countActiveByDoctor(@Param("doctorId") Integer doctorId);

    /**
     * 查询某医生已取消的预约（医生请假通知补偿用）
     */
    @Select("SELECT * FROM appointment WHERE doctor_id = #{doctorId} AND status = '已取消'")
    List<Appointment> findByDoctorCancelled(@Param("doctorId") Integer doctorId);
}
