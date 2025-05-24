package com.cms.framework.security.context;

import org.springframework.security.core.Authentication;

/**
 * 身份验证信息的线程上下文管理器类：
 * AuthenticationContextHolder 是一个线程安全的上下文管理工具类，
 * 用于存储和管理当前线程中的用户身份验证信息 (Authentication)。
 * @author quoteZZZ
 */
public class AuthenticationContextHolder
{
    //用于存储每个线程独立的 Authentication 对象
    private static final ThreadLocal<Authentication> contextHolder = new ThreadLocal<>();

    // 获取身份验证信息
    public static Authentication getContext()
    {
        return contextHolder.get();
    }

    // 设置身份验证信息
    public static void setContext(Authentication context)
    {
        contextHolder.set(context);
    }

    // 清除身份验证信息
    public static void clearContext()
    {
        contextHolder.remove();
    }
}
