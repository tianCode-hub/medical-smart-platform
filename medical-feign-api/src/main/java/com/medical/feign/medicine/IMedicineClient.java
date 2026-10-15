package com.medical.feign.medicine;

import com.medical.model.medicine.Medicine;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(value = "medical-medicine", fallbackFactory = IMedicineClientFallbackFactory.class)
public interface IMedicineClient {

    @GetMapping("/medicine/client/{id}")
    Medicine findMedicineById(@PathVariable("id") Integer id);

}
