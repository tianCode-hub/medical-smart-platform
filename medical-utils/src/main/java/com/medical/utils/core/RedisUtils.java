package com.medical.utils.core;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类：对 RedisTemplate 的常用操作做静态封装，方便业务代码直接调用。
 */
public class RedisUtils {

    private static final RedisTemplate<String, Object> redisTemplate = SpringBeanUtils.getBean("redisTemplate");

    /**
     * 设置 String 值
     */
    public static void setValue(final String key, final Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置 String 值并指定过期时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public static void setValueTimeout(final String key, final Object value, final long timeout, final TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 设置 String 值并指定过期时间（默认秒）
     */
    public static void setValueTimeout(final String key, final Object value, final long timeoutSeconds) {
        redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 获取 String 值
     */
    public static Object getValue(final String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取 String 值并转换为 String
     */
    public static String getString(final String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : value.toString();
    }

    /**
     * 删除 key
     */
    public static boolean delKey(final String key) {
        Boolean ret = redisTemplate.delete(key);
        return ret != null && ret;
    }

    /**
     * 判断 key 是否存在
     */
    public static boolean exist(final String key) {
        Boolean ret = redisTemplate.hasKey(key);
        return ret != null && ret;
    }

    /**
     * 设置过期时间
     */
    public static boolean expire(final String key, final long timeout, final TimeUnit unit) {
        Boolean ret = redisTemplate.expire(key, timeout, unit);
        return ret != null && ret;
    }

    /**
     * 自增
     */
    public static Long incy(final String key, final long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 原子写入（SETNX 语义）：仅当 key 不存在时写入并返回 true，否则返回 false。
     * 用于接口幂等防重复提交。
     */
    public static boolean setIfAbsent(final String key, final Object value, final long timeout, final TimeUnit unit) {
        Boolean ret = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
        return ret != null && ret;
    }
}