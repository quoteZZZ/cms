package com.cms.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel注解集注解：
 * （被com/cms/common/utils/poi/ExcelUtil.java类使用）
 * 用于多个Excel注解
 * @author quoteZZZ
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Excels
{
    public Excel[] value();
}
