package com.medical.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.patient.Patient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface PatientMapper extends BaseMapper<Patient> {

    /**
     * 联表查询患者列表（带用户名、电话、当前用药名称），支持按姓名/电话模糊搜索
     */
    @Select("<script>" +
            "SELECT p.*, u.username, u.phone, m.name AS current_medication_name " +
            "FROM patient p " +
            "LEFT JOIN user u ON p.user_id = u.id " +
            "LEFT JOIN medicines m ON p.current_medications = m.id " +
            "<where>" +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (u.username LIKE CONCAT('%', #{keyword}, '%') " +
            "         OR u.phone LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "</where>" +
            "ORDER BY p.patient_id " +
            "</script>")
    List<Patient> selectPatientList(@Param("keyword") String keyword);
}
