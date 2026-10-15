package com.medical.patient.service;


import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.patient.Patient;

import java.util.List;

public interface PatientService  {

    /**
     * 分页 + 条件搜索查询患者
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  患者姓名/电话关键字（模糊匹配，可为空）
     */
    PageBean<Patient> findAll(Integer pageNum, Integer pageSize, String keyword);

    Result insert(Patient patient);

    Result update(Patient patient, Integer id);

    Result delete(int patientId);

    Result deleteBatch(List<Integer> ids);

    Patient getById(Integer patientId);

    Patient findUserId(Integer userId);
}
