package com.medical.utils.constance;

/**
 * Redis 常量枚举：集中管理 Redis 相关的缓存键、过期时间与提示信息。
 */
public enum RedisConstants {

    /** 验证码缓存键前缀：login:code:{phone} */
    LOGIN_CODE_KEY("login:code:"),
    /** 验证码有效期（分钟） */
    LOGIN_CODE_TTL(2L),
    /** token 过期时间（小时） */
    TOKEN_EXPIRATION(1L),
    /** 手机号格式错误提示 */
    ERROR_PHONE_FORMAT("手机号格式错误！"),
    /** 验证码错误提示 */
    ERROR_VERIFICATION_CODE("验证码错误"),
    /** 用户昵称前缀 */
    USER_NICK_NAME_PREFIX("用户_"),
    /** 短信发送频率限制 key：sms:limit:{phone} */
    SMS_LIMIT_KEY("sms:limit:"),
    /** 短信发送最小间隔（秒） */
    SMS_LIMIT_TTL(60L),
    /** 每日号源计数 key 前缀：appointment:slot:{doctorId} */
    APPOINTMENT_SLOT_KEY("appointment:slot:"),
    /** 每日号源计数 key 过期时间（秒），约到次日 */
    APPOINTMENT_SLOT_TTL(86400L),
    /** 今日号源统计缓存 key 前缀：appointment:today:{doctorId} */
    APPOINTMENT_TODAY_KEY("appointment:today:"),
    /** 今日号源统计缓存过期时间（秒），短 TTL 保证展示数据基本实时 */
    APPOINTMENT_TODAY_TTL(30L);

    private final Object value;

    RedisConstants(String value) {
        this.value = value;
    }

    RedisConstants(Long value) {
        this.value = value;
    }

    /**
     * 获取 String 类型的常量值（键、提示信息等）
     */
    public String getStringValue() {
        return (String) value;
    }

    /**
     * 获取 long 类型的常量值（过期时间等）
     */
    public long getLongValue() {
        return (Long) value;
    }
}