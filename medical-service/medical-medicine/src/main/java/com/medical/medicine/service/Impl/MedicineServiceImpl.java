package com.medical.medicine.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.medical.medicine.mapper.MedicineMapper;
import com.medical.medicine.service.MedicineService;
import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.medicine.Medicine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;


@Slf4j
@Service
public class MedicineServiceImpl implements MedicineService {

    @Autowired
    private MedicineMapper medicineMapper;

    @Override
    public PageBean<Medicine> findAll(Integer pageNum, Integer pageSize, String keyword) {
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        // 药品名称/功效模糊搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Medicine::getName, keyword)
                    .or()
                    .like(Medicine::getEfficacy, keyword));
        }

        PageHelper.startPage(pageNum, pageSize);
        List<Medicine> list = medicineMapper.selectList(wrapper);
        Page<Medicine> page = (Page<Medicine>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    @Override
    public Medicine getById(Integer id) {
        return medicineMapper.selectById(id);
    }

    @Override
    public Result insert(Medicine medicine) {
        medicineMapper.insert(medicine);
        log.info("新增药品：{}", medicine.getName());
        return Result.success("药品新增成功");
    }

    @Override
    public Result update(Medicine medicine) {
        medicineMapper.updateById(medicine);
        log.info("更新药品：id={}", medicine.getId());
        return Result.success("药品更新成功");
    }

    @Override
    public Result delete(Long id) {
        medicineMapper.deleteById(id);
        log.info("删除药品：id={}", id);
        return Result.success("药品删除成功");
    }

    @Override
    public Result deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("待删除ID列表不能为空");
        }
        int rows = medicineMapper.deleteBatchIds(ids);
        log.info("批量删除药品：{} 条", rows);
        return Result.success("批量删除成功，共 " + rows + " 条");
    }
}
