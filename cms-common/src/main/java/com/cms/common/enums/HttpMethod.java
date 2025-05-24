package com.cms.common.enums;

import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * 请求方式
 *
 * @author quoteZZZ
 */
public enum HttpMethod
{
    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;

    // 缓存
    private static final Map<String, HttpMethod> mappings = new HashMap<>(16);

    // 初始化
    static
    {
        for (HttpMethod httpMethod : values())
        {
            mappings.put(httpMethod.name(), httpMethod);
        }
    }

    // 解析
    @Nullable
    public static HttpMethod resolve(@Nullable String method)
    {
        return (method != null ? mappings.get(method) : null);
    }

    // 匹配
    public boolean matches(String method)
    {
        return (this == resolve(method));
    }
}
