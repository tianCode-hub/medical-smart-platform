package com.medical.user.service.Impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.user.dtos.LoginRequest;
import com.medical.model.user.dtos.LoginResponse;
import com.medical.model.user.dtos.RegisterRequest;
import com.medical.model.user.pojos.User;
import com.medical.user.mapper.UserMapper;
import com.medical.user.service.UserService;
import com.medical.utils.common.AliSmsUtils;
import com.medical.utils.common.JwtUtil;
import com.medical.utils.common.Md5Util;
import com.medical.utils.common.NumberUtil;
import com.medical.utils.constance.RedisConstants;
import com.medical.utils.constance.SmsTemplateEnum;
import com.medical.utils.core.RedisUtils;
import com.medical.utils.thread.ThreadLocalUtil;
import com.medical.utils.util.OssUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public Result findByPhone(String phone) {
        User user = userMapper.findByPhone(phone);
        return Result.success(user);
    }

    @Override
    public Result register(RegisterRequest registerRequest) {
        String phone = registerRequest.getPhone();
        String identity = registerRequest.getIdentity();

        User u = userMapper.findByPhone(phone);
        User i = userMapper.findByIdentity(identity);
        if (u == null && i == null) {
//            添加用户注册信息到数据库
            User user = new User();
            user.setPhone(phone);
            user.setPassword(Md5Util.getMD5String(registerRequest.getPassword()));
            user.setRole(registerRequest.getRole());
            user.setUsername(registerRequest.getUsername());
            user.setIdentity(identity);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);
            return Result.success();
        } else {
            return Result.error("身份证或者手机号已被占用");
        }
    }

    @Override
    public Result deleteUser(Integer id) {
        userMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("待删除ID列表不能为空");
        }
        int rows = userMapper.deleteBatchIds(ids);
        log.info("批量删除用户：{} 条", rows);
        return Result.success("批量删除成功，共 " + rows + " 条");
    }

    @Override
    public User findByIdentity(String identity) {
        return userMapper.findByIdentity(identity);
    }

    @Override
    public Result sendCode(String phone) {
        if (!NumberUtil.isPhone(phone)) {
            return Result.error(RedisConstants.ERROR_PHONE_FORMAT.getStringValue());
        }

        // 防刷：同一手机号在间隔时间内只允许发送一次
        String limitKey = RedisConstants.SMS_LIMIT_KEY.getStringValue() + phone;
        if (RedisUtils.exist(limitKey)) {
            return Result.error("发送过于频繁，请稍后再试");
        }

        String code = RandomUtil.randomNumbers(6);

        //          通过线程池发送
//        lambda表达式  : 函数接口中方法的简化
        threadPoolExecutor.execute(() -> {
            //        发送验证码
          AliSmsUtils.sendSms(SmsTemplateEnum.QF_TEMPLATE.getName(),
                    SmsTemplateEnum.QF_TEMPLATE.getCode(),
                    phone,
                    code);
//            redis存储验证码
            RedisUtils.setValueTimeout(RedisConstants.LOGIN_CODE_KEY.getStringValue() + phone, code, RedisConstants.LOGIN_CODE_TTL.getLongValue(), TimeUnit.MINUTES);
            // 记录发送间隔，间隔时间内禁止重复发送
            RedisUtils.setValueTimeout(limitKey, "1", RedisConstants.SMS_LIMIT_TTL.getLongValue(), TimeUnit.SECONDS);
            log.debug("发送验证码成功，验证码：{}", code);

        });

        return Result.success();
    }

    @Override
    public Result<LoginResponse> loginForPassword(LoginRequest loginRequest) {
        String phone = loginRequest.getPhone();
        String password = loginRequest.getPassword();
        User u = userMapper.findByPhone(phone);
        if (u == null) {
            return Result.error("用户名错误");
        }
        if (Md5Util.getMD5String(password).equals(u.getPassword())) {
            HashMap<String, Object> claims = new HashMap<>();
            claims.put("id", u.getId());
            claims.put("phone", u.getPhone());
            claims.put("role", u.getRole());
            String token = JwtUtil.genToken(claims);
            RedisUtils.setValueTimeout(token, token, RedisConstants.TOKEN_EXPIRATION.getLongValue(), TimeUnit.HOURS);
            LoginResponse response = new LoginResponse(token, u.getId(), u.getUsername(), u.getRole(), u.getPhone());
            return Result.success(response);
        }
        return Result.error("密码错误");
    }

    @Override
    public Result<LoginResponse> loginForPhone(LoginRequest loginRequest) {
        String phone = loginRequest.getPhone();
        String code = loginRequest.getCode();
        if (!NumberUtil.isPhone(phone)) {
            return Result.error(RedisConstants.ERROR_PHONE_FORMAT.getStringValue());
        }
        String cacheCode = RedisUtils.getString(RedisConstants.LOGIN_CODE_KEY.getStringValue() + phone);
       if (cacheCode == null || !cacheCode.equals(code)) {
           return Result.error(RedisConstants.ERROR_VERIFICATION_CODE.getStringValue());
       }
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            user = createUserWithPhone(phone);
        }
        String token = generateToken(user);
        storeTokenInRedis(token, user);
        // 登录成功，删除已使用的验证码，防止重复使用
        RedisUtils.delKey(RedisConstants.LOGIN_CODE_KEY.getStringValue() + phone);
        LoginResponse response = new LoginResponse(token, user.getId(), user.getUsername(), user.getRole(), user.getPhone());
        return Result.success(response);
    }

    private String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("phone", user.getPhone());
        claims.put("role", user.getRole());
        claims.put("username", user.getUsername());
        return JwtUtil.genToken(claims);
    }

    private void storeTokenInRedis(String token, User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("phone", user.getPhone());
        userMap.put("role", user.getRole());
        userMap.put("username", user.getUsername());
        RedisUtils.setValueTimeout(token, JwtUtil.genToken(userMap), RedisConstants.TOKEN_EXPIRATION.getLongValue(), TimeUnit.HOURS);
    }

    public User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setUsername(RedisConstants.USER_NICK_NAME_PREFIX.getStringValue() + RandomUtil.randomString(10));
        user.setPassword(RandomUtil.randomString(8));
        user.setIdentity("defaultIdentity");
        user.setRole("define");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    @Override
    public Result forgotPassword(LoginRequest forgotPasswordRequest) {
        String phone = forgotPasswordRequest.getPhone();
        String code = forgotPasswordRequest.getCode();
        String newPassword = forgotPasswordRequest.getPassword();
        if (!NumberUtil.isPhone(phone)) {
            return Result.error(RedisConstants.ERROR_PHONE_FORMAT.getStringValue());
        }
        String cacheCode = RedisUtils.getString(RedisConstants.LOGIN_CODE_KEY.getStringValue() + phone);
        if (cacheCode == null || !cacheCode.equals(code)) {
            return Result.error(RedisConstants.ERROR_VERIFICATION_CODE.getStringValue());
        }
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            return Result.error("用户不存在！");
        }
        // LambdaUpdateWrapper 局部更新密码
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, user.getId())
                .set(User::getPassword, Md5Util.getMD5String(newPassword))
                .set(User::getUpdateTime, LocalDateTime.now());
        userMapper.update(null, wrapper);
        return Result.success("密码重置成功！");
    }

    @Override
    public User getById(Integer id) {
        return userMapper.selectById(id);
    }

    @Override
    public Result update(User user) {
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success();
    }

    @Override
    public Result updateAvatar(MultipartFile file) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer id = ((Number) map.get("id")).intValue();
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            return Result.error("文件名不合法");
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
//        获取新的文件名
        filename = uuid + filename.substring(filename.lastIndexOf("."));

        try {
//            获取头像路径
            String path = OssUtils.uploadFile2OSS(file.getInputStream(), filename);

            LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(User::getId, id)
                    .set(User::getUserPic, path)
                    .set(User::getUpdateTime, LocalDateTime.now());
            userMapper.update(null, wrapper);
            return Result.success();
        } catch (IOException e) {
            log.error("头像上传失败", e);
            return Result.error("头像上传失败，请稍后重试");
        }
    }

    @Override
    public void updatePwd(String newPwd) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer id = ((Number) map.get("id")).intValue();
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, id)
                .set(User::getPassword, Md5Util.getMD5String(newPwd))
                .set(User::getUpdateTime, LocalDateTime.now());
        userMapper.update(null, wrapper);
    }

    @Override
    public PageBean<User> getAllUsers(Integer pageNum, Integer pageSize) {
        PageBean<User> us = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<User> as = userMapper.selectList(new LambdaQueryWrapper<>());
        Page<User> p = (Page<User>) as;
        us.setTotal(p.getTotal());
        us.setItems(p.getResult());
        return us;
    }
}