package com.medical.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.user.pojos.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);

    /**
     * 根据身份证号查询用户
     */
    @Select("SELECT * FROM user WHERE identity = #{identity}")
    User findByIdentity(@Param("identity") String identity);
}