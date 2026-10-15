package com.medical.medicine.controller;

import com.medical.medicine.service.MedicineService;
import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.medicine.Medicine;
import com.medical.utils.annotation.NoLoginCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RefreshScope
@RestController
@RequestMapping("/medicines")
public class MedicineController {

    @Autowired
    private MedicineService medicineService;

    /**
     * 分页 + 条件搜索查询药品
     *
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @param keyword  药品名称/功效关键字（模糊匹配，可为空）
     */
    @NoLoginCheck
    @GetMapping
    public Result<PageBean<Medicine>> getAllMedicines(@RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                                      @RequestParam(required = false) String keyword) {
        PageBean<Medicine> medicines = medicineService.findAll(pageNum, pageSize, keyword);
        return Result.success(medicines);
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public Result<String> addMedicine(@RequestBody Medicine medicine) {
        return medicineService.insert(medicine);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<String> updateMedicine(@PathVariable Long id, @RequestBody Medicine medicine) {
        medicine.setId(id);
        return medicineService.update(medicine);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<String> deleteMedicine(@PathVariable Long id) {
        return medicineService.delete(id);
    }

    /**
     * 批量删除药品：请求体传入待删除的药品ID数组
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('admin')")
    public Result<String> batchDeleteMedicine(@RequestBody List<Long> ids) {
        return medicineService.deleteBatch(ids);
    }
}
