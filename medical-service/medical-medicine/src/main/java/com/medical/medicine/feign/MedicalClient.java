package com.medical.medicine.feign;

import com.medical.feign.medicine.IMedicineClient;
import com.medical.medicine.service.MedicineService;
import com.medical.model.medicine.Medicine;
import com.medical.utils.annotation.NoLoginCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MedicalClient implements IMedicineClient {
    @Autowired
    private MedicineService medicineService;

    // 内部 Feign 调用不携带登录态，需免登录放行
    @Override
    @NoLoginCheck
    @GetMapping("/medicine/client/{id}")
    public Medicine findMedicineById(@PathVariable("id") Integer id) {
        return medicineService.getById(id);
    }
}
