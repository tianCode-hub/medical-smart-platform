package com.medical.doctor.service;


import com.medical.model.common.dtos.PageBean;
import com.medical.model.doctor.DoctorEmployment;


public interface DoctorEmploymentService {
    /**
     * 分页查询雇佣记录，支持按医生姓名/身份证号模糊搜索
     */
    PageBean<DoctorEmployment> findlist(Integer pageNum, Integer pageSize, String keyword);

    void update(DoctorEmployment doctorEmployment);

    void delete(Integer id);
}
