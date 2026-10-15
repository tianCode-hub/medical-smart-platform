package com.medical.user.controller;

import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.user.dtos.LoginRequest;
import com.medical.model.user.dtos.LoginResponse;
import com.medical.model.user.dtos.RegisterRequest;
import com.medical.model.user.pojos.User;
import com.medical.user.service.UserService;
import com.medical.utils.annotation.NoLoginCheck;
import com.medical.utils.common.Md5Util;
import com.medical.utils.thread.ThreadLocalUtil;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


@RefreshScope
@RestController
@Validated
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     * @param registerRequest 注册请求对象，包含手机号、密码、身份、用户名和角色
     * @return 注册结果
     */
    @NoLoginCheck
    @PostMapping("/register")
    public Result register(@RequestBody RegisterRequest registerRequest) {
        Result register = userService.register(registerRequest);
        return register;
    }

    /**
     * 发送手机验证码
     * @param phone 用户手机号
     * @return 发送验证码的结果
     */
    @NoLoginCheck
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone) {
        return userService.sendCode(phone);
    }

    /**
     * 手机验证码登录
     * @param loginRequest 登录请求对象，包含手机号和验证码
     * @return 登录结果，包含生成的Token等信息
     */
    @NoLoginCheck
    @PostMapping("/loginForPhone")
    public Result loginForPhone(@RequestBody LoginRequest loginRequest) {
        return userService.loginForPhone(loginRequest);
    }

    /**
     * 用户名和密码登录
     * @param loginRequest 登录请求对象，包含手机号和密码
     * @return 登录结果，包含生成的Token等信息
     */
    @NoLoginCheck
    @PostMapping("/login")
    public Result<LoginResponse> loginForPassword(@RequestBody LoginRequest loginRequest) {
        return userService.loginForPassword(loginRequest);
    }

    /**
     * 忘记密码
     * @param forgotPasswordRequest 忘记密码请求对象，包含手机号、验证码和新密码
     * @return 重置密码的结果
     */
    @NoLoginCheck
    @PostMapping("/forgotPassword")
    public Result forgotPassword(@RequestBody LoginRequest forgotPasswordRequest) {
        Result result = userService.forgotPassword(forgotPasswordRequest);
        return Result.success(result);
    }

    /**
     * 获取当前用户信息
     * @return 当前用户的信息
     */
    @GetMapping("/userInfo")
    public Result<User> userInfo(@RequestHeader("phone") String phone) {
        return userService.findByPhone(phone);
    }

    /**
     * 删除用户
     * @param id 用户ID
     * @return 删除操作的结果
     */
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result deleteUser(@PathVariable Integer id) {
        return userService.deleteUser(id);
    }

    /**
     * 批量删除用户：请求体传入待删除的用户ID数组
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('admin')")
    public Result batchDeleteUser(@RequestBody List<Integer> ids) {
        return userService.deleteBatch(ids);
    }

    /**
     * 更新用户信息
     * @param user 用户对象，包含更新后的信息
     * @return 更新操作的结果
     */
    @PutMapping("/update")
    @PreAuthorize("hasRole('admin')")
    public Result update(@RequestBody @Validated User user) {
        return userService.update(user);
    }

    /**
     * 更新用户头像
     * @param file
     * @return
     */
    @PatchMapping("/updateAvatar")
    public Result updateAvatar(MultipartFile file) {
        return userService.updateAvatar(file);
    }

    /**
     * 更新用户密码
     * @param params 包含旧密码(newPwd)和新密码(newPwd)的键值对
     * @return 更新密码操作的结果
     */
    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> params) {
        // 兼容不同字段命名
        String oldPwd = params.get("oldPwd") != null ? params.get("oldPwd") : params.get("oldPassword");
        String newPwd = params.get("newPwd") != null ? params.get("newPwd") : params.get("newPassword");

        if (!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd)) {
            return Result.error("旧密码和新密码不能为空");
        }

        // 从 ThreadLocal 中获取当前登录用户（由 LoginInterceptor 写入）
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || claims.get("id") == null) {
            return Result.error("登录状态异常，请重新登录");
        }
        Integer id = ((Number) claims.get("id")).intValue();
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 校验旧密码
        if (!Md5Util.getMD5String(oldPwd).equals(user.getPassword())) {
            return Result.error("旧密码错误");
        }
        userService.updatePwd(newPwd);
        return Result.success("密码修改成功");
    }

    /**
     * 获取所有用户，分页显示
     * @param pageNum 页码
     * @param pageSize 每页显示的用户数量
     * @return 分页的用户信息
     */
    @GetMapping("/allUsers")
    @PreAuthorize("hasRole('admin')")
    public Result<PageBean<User>> getAllUsers(@RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        PageBean<User> users = userService.getAllUsers(pageNum, pageSize);
        return Result.success(users);
    }
}
