package com.mhc.actable.core.manager.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All rights Reserved, Designed By www.maihaoche.com
 *
 * @Package com.example.xxc.demo.utils.spring
 * @author: xuchao（xuchao@maihaoche.com）
 * @date: 2019-06-12 16:42
 * @Copyright: 2017-2020 www.maihaoche.com Inc. All rights reserved.
 * 注意：本内容仅限于卖好车内部传阅，禁止外泄以及用于其他的商业目
 */
@Component("ActableSpringContextUtil")
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringContextUtil.applicationContext == null) {
            SpringContextUtil.applicationContext = applicationContext;
        }
    }

    public static <T> T getBean(String name) throws BeansException {
        return (T) applicationContext.getBean(name);
    }

    public static <T> T getBean(Class<T> clz) throws BeansException {
        return applicationContext.getBean(clz);
    }

    public static <T> List<T> getBeansOfType(Class<T> type) {
        return applicationContext.getBeansOfType(type).entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
    }

    public static List<Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(annotationType);
        return beansWithAnnotation.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
    }

    public static String getProperty(String key) {
        return applicationContext.getEnvironment().getProperty(key);
    }
}
