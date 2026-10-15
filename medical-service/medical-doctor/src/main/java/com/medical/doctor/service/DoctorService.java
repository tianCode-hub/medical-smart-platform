package com.medical.doctor.service;



import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.doctor.Doctor;

import java.util.List;

public interface DoctorService  {
    /**
     * 分页 + 条件搜索查询医生
     *
     * @param pageNum      页码
     * @param pageSize     每页条数
     * @param keyword      姓名/专长/职称关键字（模糊匹配，可为空）
     * @param departmentId 科室ID（可为空）
     */
    PageBean<Doctor> findAll(Integer pageNum, Integer pageSize, String keyword, Integer departmentId);

    void deleteUser(Integer id);

    Result deleteBatch(List<Integer> ids);

    void update(Doctor doctor);

    Doctor getById(Integer doctorId);

    Doctor getUserId(Integer userId);

    /** 按科室查询医生列表（Feign 内部、同科室全部状态） */
    List<Doctor> getByDepartment(Integer departmentId);

    Result getdepartment(Integer departmentId);

    /** 医生紧急请假：置状态、取消其未就诊预约并通知已挂号患者 */
    Result leave(Integer doctorId);
}
