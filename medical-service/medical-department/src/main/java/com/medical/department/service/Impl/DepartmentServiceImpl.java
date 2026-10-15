package com.medical.department.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.medical.department.mapper.DepartmentMapper;
import com.medical.department.service.DepartmentService;
import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.department.Department;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;


@Slf4j
@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public void addDepartment(Department department) {
        departmentMapper.insert(department);
        log.info("新增科室：{}", department.getName());
    }

    @Override
    public Result getDepartmentById(int id) {
        Department department = departmentMapper.selectById(id);
        if (department != null) {
            return Result.success(department);
        } else {
            return Result.error("科室不存在");
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        return departmentMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public void updateDepartment(Department department) {
        departmentMapper.updateById(department);
        log.info("更新科室：id={}", department.getId());
    }

    @Override
    public void deleteDepartment(int id) {
        departmentMapper.deleteById(id);
        log.info("删除科室：id={}", id);
    }

    @Override
    public Result deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("待删除ID列表不能为空");
        }
        int rows = departmentMapper.deleteBatchIds(ids);
        log.info("批量删除科室：{} 条", rows);
        return Result.success("批量删除成功，共 " + rows + " 条");
    }

    @Override
    public Department getById(Integer id) {
        return departmentMapper.selectById(id);
    }

    @Override
    public Result getOffices() {
        return Result.success(departmentMapper.getOffices());
    }

    @Override
    public Result getDepartmentOfficeId(Integer officeId) {
        // 单表查询，使用 LambdaQueryWrapper 替代手写 SQL
        List<Department> departments = departmentMapper.selectList(
                new LambdaQueryWrapper<Department>().eq(Department::getOfficeId, officeId));
        return Result.success(departments);
    }

    @Override
    public PageBean<Department> getDepartmentsPage(Integer pageNum, Integer pageSize, String keyword, Integer officeId) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        // 科室名称模糊搜索
       if (StringUtils.hasLength(keyword)) {
           wrapper.like(Department::getName,keyword);
       }
        // 按科室分类筛选
        if (officeId != null ) {
            wrapper.eq(Department::getOfficeId,officeId);
        }

        PageHelper.startPage(pageNum, pageSize);
        List<Department> list = departmentMapper.selectList(wrapper);
        Page<Department> page = (Page<Department>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }
}
