package com.medical.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.doctor.Doctor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {

    /**
     * 联表查询医生列表（带用户名、科室名），支持按姓名/专长/职称模糊搜索与科室筛选
     */
    @Select("<script>" +
            "SELECT d.*, u.username, dep.name AS department_name " +
            "FROM doctor d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (u.username LIKE CONCAT('%', #{keyword}, '%') " +
            "         OR d.specialty LIKE CONCAT('%', #{keyword}, '%') " +
            "         OR d.title LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "  <if test='departmentId != null'>" +
            "    AND d.department_id = #{departmentId}" +
            "  </if>" +
            "</where>" +
            "ORDER BY d.doctor_id " +
            "</script>")
    List<Doctor> selectDoctorList(@Param("keyword") String keyword, @Param("departmentId") Integer departmentId);

    /**
     * 按 doctor_id 关联查医生档案：联表带出 user.username（登录名），保证与登录名/预约界面一致
     */
    @Select("SELECT d.*, u.username, dep.name AS department_name " +
            "FROM doctor d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE d.doctor_id = #{doctorId}")
    Doctor selectByIdWithUser(@Param("doctorId") Integer doctorId);

    /**
     * 按 user_id 关联查医生档案：联表带出 user.username（登录名），确保与预约界面医生名一致
     */
    @Select("SELECT d.*, u.username, dep.name AS department_name " +
            "FROM doctor d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE d.user_id = #{userId}")
    Doctor selectByUserId(@Param("userId") Integer userId);

    /**
     * 查询所有处于请假状态的医生（医生请假通知补偿定时任务用）
     */
    @Select("SELECT * FROM doctor WHERE employment_status = '请假'")
    List<Doctor> selectOnLeave();
}
