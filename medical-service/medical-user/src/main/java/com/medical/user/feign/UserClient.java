package com.medical.user.feign;

import com.medical.feign.user.IUserClient;
import com.medical.model.user.pojos.User;
import com.medical.user.service.UserService;
import com.medical.utils.annotation.NoLoginCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserClient implements IUserClient {
    @Autowired
    private UserService userService;
    // 内部 Feign 调用不携带登录态，需免登录放行
    @Override
    @NoLoginCheck
    @GetMapping("/user/client/{id}")
    public User findUserById(@PathVariable("id") Integer id) {
        return userService.getById(id);
    }
}
