package com.cms.common.utils;

/**
 * 处理并记录日志文件
 * 
 * @author quoteZZZ
 */
public class LogUtils
{
    // 日志文件名称
    public static String getBlock(Object msg)
    {
        if (msg == null)
        {
            msg = "";
        }
        return "[" + msg.toString() + "]";
    }
}
