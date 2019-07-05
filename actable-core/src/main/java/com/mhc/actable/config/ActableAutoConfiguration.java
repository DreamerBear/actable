package com.mhc.actable.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * All rights Reserved, Designed By www.maihaoche.com
 *
 * @Package com.mhc.actable.config
 * @author: xuchao（xuchao@maihaoche.com）
 * @date: 2019-07-05 10:52
 * @Copyright: 2017-2020 www.maihaoche.com Inc. All rights reserved.
 * 注意：本内容仅限于卖好车内部传阅，禁止外泄以及用于其他的商业目
 */
@Configuration
@ComponentScan("com.mhc.actable.core.*")
@MapperScan("com.mhc.actable.core.dao.*")
public class ActableAutoConfiguration {
}
