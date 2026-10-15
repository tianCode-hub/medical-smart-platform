package com.medical.utils.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等注解：标注在 Controller 的写方法上，用于拦截短时间内的重复提交。
 * 执行拦截器会基于「用户标识 + 方法 + 请求体中的幂等键」生成唯一 Key，
 * 通过 Redis SETNX 原子写入，短时间内重复请求会被拒绝。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等 Key 的过期时间（秒），默认 10 秒，防止误拦截长时间内的合法连续提交。
     */
    long timeout() default 10L;

    /**
     * 请求体中用于生成幂等键的字段名（取其一即可）。
     * 为空时用「请求体序列化 + 用户 id」组合生成，避免依赖前端传字段。
     */
    String keyField() default "";
}