package com.medical.medicine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.medicine.Medicine;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface MedicineMapper extends BaseMapper<Medicine> {

}