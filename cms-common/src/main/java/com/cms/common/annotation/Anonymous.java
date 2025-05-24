package com.cms.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 匿名访问不鉴权注解:
 * （被 com.cms.framework.config.properties 包中的 PermitAllUrlProperties 类使用，用于标识方法或类可以匿名访问，无需鉴权。）
 * 用于标识方法或类可以匿名访问，无需鉴权。
 * @author quoteZZZ
 */
@Target({ ElementType.METHOD, ElementType.TYPE })// 该注解定义了（注解的使用位置）可以应用于方法和方法参数的修饰范围。
@Retention(RetentionPolicy.RUNTIME)// （注解的作用范围）表示该注解会在运行时保留，可以通过反射机制读取。
@Documented// 表示该注解会被JavaDoc工具记录，在生成文档时会被保存。
public @interface Anonymous
{
}
