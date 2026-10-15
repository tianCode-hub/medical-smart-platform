package com.medical.department.controller;


import com.medical.department.service.DepartmentService;
import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.department.Department;
import com.medical.utils.annotation.NoLoginCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RefreshScope
@RestController
@RequestMapping("/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('admin')")
    public Result<String> addDepartment(@RequestBody Department department) {
        departmentService.addDepartment(department);
        return Result.success("科室新增成功");
    }

    @NoLoginCheck
    @GetMapping("/id")
    public Result<Department> getDepartmentById(@RequestParam("id") Integer id) {

        return departmentService.getDepartmentById(id);
    }

    @NoLoginCheck
    @GetMapping
    public Result<List<Department>> getAllDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        return Result.success(departments);
    }

    /**
     * 分页 + 条件搜索查询科室
     *
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @param keyword  科室名称关键字（模糊匹配，可为空）
     * @param officeId 科室分类ID（可为空）
     * @return 分页结果
     */
    @NoLoginCheck
    @GetMapping("/page")
    public Result<PageBean<Department>> getDepartmentsPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Integer officeId) {
        PageBean<Department> pageBean = departmentService.getDepartmentsPage(pageNum, pageSize, keyword, officeId);
        return Result.success(pageBean);
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('admin')")
    public Result<String> updateDepartment(@RequestBody Department department) {
        departmentService.updateDepartment(department);
        return Result.success("科室更新成功");
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<String> deleteDepartment(@PathVariable int id) {
        departmentService.deleteDepartment(id);
        return Result.success("科室删除成功");
    }

    /**
     * 批量删除科室：请求体传入待删除的科室ID数组
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('admin')")
    public Result<String> batchDeleteDepartment(@RequestBody List<Integer> ids) {
        return departmentService.deleteBatch(ids);
    }

    @NoLoginCheck
    @GetMapping("/offices")
    public Result getOffices(){
        return departmentService.getOffices();
    }

    @NoLoginCheck
    @GetMapping("/getoffices")
    public Result getDepartmentOfficesId(@RequestParam("officeId") Integer officeId){
        return departmentService.getDepartmentOfficeId(officeId);
    }
}
