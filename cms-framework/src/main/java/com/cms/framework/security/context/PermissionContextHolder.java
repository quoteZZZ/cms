package com.cms.framework.security.context;

import com.cms.common.utils.text.Convert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 简化权限信息存储和获取的工具类：
 * 它通过 Spring 的请求上下文机制，为每个请求单独维护权限信息，适用于动态权限管理场景。
 * 用于在 当前请求上下文 中存储和获取用户的权限信息。其功能依赖 Spring 提供的 RequestContextHolder，
 * 通过管理请求范围内的权限数据，确保在处理请求时能够动态获取或设置权限信息。
 * @author quoteZZZ
 */
public class PermissionContextHolder
{
    //定义权限上下文，常量通常用于存储与权限上下文相关的属性名称
    private static final String PERMISSION_CONTEXT_ATTRIBUTES = "PERMISSION_CONTEXT";

    //设置权限上下文
    public static void setContext(String permission)
    {
        RequestContextHolder.currentRequestAttributes().setAttribute(PERMISSION_CONTEXT_ATTRIBUTES, permission,
                RequestAttributes.SCOPE_REQUEST);
    }

    //获取权限上下文
    public static String getContext()
    {
        return Convert.toStr(RequestContextHolder.currentRequestAttributes().getAttribute(PERMISSION_CONTEXT_ATTRIBUTES,
                RequestAttributes.SCOPE_REQUEST));
    }
}
