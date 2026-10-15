package com.medical.medicine.service;

import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.medicine.Medicine;

import java.util.List;




public interface MedicineService {
    /**
     * 分页 + 条件搜索查询药品
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  药品名称/功效关键字（模糊匹配，可为空）
     */
    PageBean<Medicine> findAll(Integer pageNum, Integer pageSize, String keyword);

    Medicine getById(Integer id);

    Result insert(Medicine medicine);

    Result update(Medicine medicine);

    Result delete(Long id);

    Result deleteBatch(List<Long> ids);
}
