package com.medical.utils.core;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 容器工具类：用于在非 Spring 管理的普通类中获取 Bean
 */
@Component
public class SpringBeanUtils implements ApplicationContextAware {

    /**
     * Spring 应用上下文对象
     */
    private static ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringBeanUtils.applicationContext = applicationContext;
    }

    /**
     * 根据名称获取 Bean 对象
     */
    public static <T> T getBean(String name) {
        return (T) applicationContext.getBean(name);
    }

    /**
     * 根据类型获取 Bean 对象
     */
    public static <T> T getBean(Class<T> clz) {
        return applicationContext.getBean(clz);
    }

}