package com.cms.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.cms.common.utils.serializer.SensitiveJsonSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.cms.common.enums.DesensitizedType;

/**
 * 数据脱敏注解
 * （被com/cms/common/config/serializer/SensitiveJsonSerializer.java使用）
 * @author quoteZZZ
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveJsonSerializer.class)
public @interface Sensitive
{
    // 默认值
    DesensitizedType desensitizedType();
}
