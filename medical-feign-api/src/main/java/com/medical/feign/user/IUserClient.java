package com.medical.feign.user;

import com.medical.model.user.pojos.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(value = "medical-user", fallbackFactory = IUserClientFallbackFactory.class)
public interface IUserClient {
    @GetMapping("/user/client/{id}")
    User findUserById(@PathVariable("id") Integer id);
}
