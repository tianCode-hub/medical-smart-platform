package com.medical.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.model.message.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 站内消息：消息服务为 message 表唯一归属方
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 统计某用户是否已收到指定医生的请假通知（请假通知补发去重用）
     */
    @Select("SELECT COUNT(*) FROM message " +
            "WHERE doctor_id = #{doctorId} AND user_id = #{userId} AND title = '医生请假通知'")
    long countLeaveNotify(@Param("doctorId") Integer doctorId, @Param("userId") Integer userId);
}