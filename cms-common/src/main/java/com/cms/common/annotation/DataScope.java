package com.cms.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限过滤注解类：
 * （被 com.cms.framework.aspectj.DataScopeAspect切面类使用）
 * 是一个用于数据权限过滤的自定义注解类，提供部门表别名、用户表别名和权限字符的配置，
 * 默认值为空字符串，应用于方法级别并在运行时解析。
 * @author quoteZZZ
 */
@Target(ElementType.METHOD)//使用位置：方法
@Retention(RetentionPolicy.RUNTIME)//使用策略：运行时（运行时，可以反射机制读取）
@Documented// 用于生成文档
public @interface DataScope
{
    /**
     * 部门表的别名
     */
    public String deptAlias() default "";

    /*
    * 竞赛表的别名
    * */
//    public String contestAlias() default "";

    /**
     * 用户表的别名
     */
    public String userAlias() default "";

    /**
     * 权限字符（用于多个角色匹配符合要求的权限）默认根据权限注解@ss获取，多个权限用逗号分隔开来
     */
    public String permission() default "";
}
