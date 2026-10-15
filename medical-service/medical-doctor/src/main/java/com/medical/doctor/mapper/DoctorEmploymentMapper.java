package com.medical.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.doctor.DoctorEmployment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface DoctorEmploymentMapper extends BaseMapper<DoctorEmployment> {

    /**
     * 联表查询雇佣记录 + 用户名 + 身份证号，支持按医生姓名/身份证号模糊搜索
     */
    @Select("<script>" +
            "SELECT d.*, u.username AS doctor_name, u.identity AS identity " +
            "FROM doctoremployment d " +
            "LEFT JOIN doctor doc ON d.doctor_id = doc.doctor_id " +
            "LEFT JOIN user u ON doc.user_id = u.id " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (u.username LIKE CONCAT('%', #{keyword}, '%') " +
            "         OR u.identity LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            "ORDER BY d.id " +
            "</script>")
    List<DoctorEmployment> getlistWithUserInfo(@Param("keyword") String keyword);
}
