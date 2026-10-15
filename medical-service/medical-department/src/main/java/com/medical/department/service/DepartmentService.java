package com.medical.department.service;




import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.department.Department;

import java.util.List;



public interface DepartmentService   {
    void addDepartment(Department department);
    Result getDepartmentById(int id);
    List<Department> getAllDepartments();
    void updateDepartment(Department department);
    void deleteDepartment(int id);

    Result deleteBatch(List<Integer> ids);

    Department getById(Integer id);

    Result getOffices();

    Result getDepartmentOfficeId(Integer officeId);

    /**
     * 分页 + 条件搜索查询科室
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  科室名称关键字（模糊匹配，可为空）
     * @param officeId 科室分类ID（可为空）
     * @return 分页结果
     */
    PageBean<Department> getDepartmentsPage(Integer pageNum, Integer pageSize, String keyword, Integer officeId);
}