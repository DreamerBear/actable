package com.mhc.actable.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


/**
 * 开启Actable
 *
 * 可使用EntitySyncHandler同步表结构
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ActableAutoConfiguration.class})
public @interface EnableActable {
}
