package com.medical.department.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.department.Department;
import com.medical.model.department.Offices;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * 查询所有科室分类
     */
    @Select("SELECT * FROM offices")
    List<Offices> getOffices();
}