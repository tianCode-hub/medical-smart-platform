package com.medical.user.service;



import com.medical.model.common.dtos.PageBean;
import com.medical.model.common.dtos.Result;
import com.medical.model.user.dtos.LoginRequest;
import com.medical.model.user.dtos.LoginResponse;
import com.medical.model.user.dtos.RegisterRequest;
import com.medical.model.user.pojos.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;






public interface UserService  {
  Result findByPhone(String phone);

    Result register(RegisterRequest registerRequest);

   Result update(User user);

    Result updateAvatar(MultipartFile file);

    void updatePwd(String newPwd);

    PageBean<User> getAllUsers(Integer pageNum, Integer pageSize);


    Result deleteUser(Integer id);

    Result deleteBatch(List<Integer> ids);

    User findByIdentity(String identity);

    Result loginForPhone(LoginRequest loginRequest);

    Result sendCode(String phone);

    Result<LoginResponse> loginForPassword(LoginRequest loginRequest);

    Result forgotPassword(LoginRequest forgotPasswordRequest);

 User getById(Integer id);
}