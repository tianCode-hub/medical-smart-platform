package com.medical.doctor.service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.medical.doctor.mapper.DoctorEmploymentMapper;
import com.medical.doctor.service.DoctorEmploymentService;
import com.medical.model.common.dtos.PageBean;
import com.medical.model.doctor.DoctorEmployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class DoctorEmploymentServiceImpl implements DoctorEmploymentService {

    @Autowired
    private DoctorEmploymentMapper doctorEmploymentMapper;

    @Override
    public PageBean<DoctorEmployment> findlist(Integer pageNum, Integer pageSize, String keyword) {
        PageHelper.startPage(pageNum, pageSize);
        List<DoctorEmployment> as = doctorEmploymentMapper.getlistWithUserInfo(keyword);
        Page<DoctorEmployment> p = (Page<DoctorEmployment>) as;
        return new PageBean<>(p.getTotal(), p.getResult());
    }

    @Override
    public void update(DoctorEmployment doctorEmployment) {
        doctorEmployment.setUpdateTime(LocalDateTime.now());
        doctorEmploymentMapper.updateById(doctorEmployment);
    }

    @Override
    public void delete(Integer id) {
        doctorEmploymentMapper.deleteById(id);
    }
}