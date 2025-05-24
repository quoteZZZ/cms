package com.cms.common.utils.serializer;

import java.io.IOException;
import java.util.Objects;

import com.cms.common.core.domain.model.LoginUser;
import com.cms.common.utils.SecurityUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.cms.common.annotation.Sensitive;
import com.cms.common.enums.DesensitizedType;


/**
 * 数据脱敏序列化过滤
 * 用于 用户信息脱敏
 * @author quoteZZZ
 */
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer
{
    // 默认脱敏类型
    private DesensitizedType desensitizedType;

    // 序列化
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        if (desensitization())
        {
            gen.writeString(desensitizedType.desensitizer().apply(value));
        }
        else
        {
            gen.writeString(value);
        }
    }

    // 创建上下文
    //根据字段上的 @Sensitive 注解动态设置脱敏类型并返回相应的序列化器，若注解不存在或字段类型不是字符串，则返回默认序列化器。
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException
    {
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (Objects.nonNull(annotation) && Objects.equals(String.class, property.getType().getRawClass()))
        {
            this.desensitizedType = annotation.desensitizedType();
            return this;
        }
        return prov.findValueSerializer(property.getType(), property);
    }

    /**
     * 是否需要脱敏处理
     */
    private boolean desensitization()
    {
        try
        {
            LoginUser securityUser = SecurityUtils.getLoginUser();
            // 管理员不脱敏
            return !securityUser.getUser().isAdmin();
        }
        catch (Exception e)
        {
            return true;
        }
    }
}
