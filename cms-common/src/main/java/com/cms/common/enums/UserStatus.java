package com.cms.common.enums;

/**
 * 用户状态
 * 
 * @author quoteZZZ
 */
public enum UserStatus
{
    OK("0", "正常"), DISABLE("1", "停用"), DELETED("2", "删除");

    private final String code;// 状态码
    private final String info;//状态信息

    // 构造方法
    UserStatus(String code, String info)
    {
        this.code = code;
        this.info = info;
    }

    // 获取状态码
    public String getCode()
    {
        return code;
    }

    // 获取状态信息
    public String getInfo()
    {
        return info;
    }
}
