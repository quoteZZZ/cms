package com.cms.common.core.domain;

import com.cms.common.constant.HttpStatus;

import java.io.Serializable;

/**
 * 响应结果类（R类）
 * 封装API响应结果，提供统一的成功和失败响应格式，支持带或不带数据及自定义消息的响应创建，并实现序列化接口以确保对象状态可被保存和传输。
 * @author quoteZZZ
 */
public class R<T> implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 成功 */
    public static final int SUCCESS = HttpStatus.SUCCESS;

    /** 失败 */
    public static final int FAIL = HttpStatus.ERROR;

    private int code;

    private String msg;

    private T data;

    public static <T> R<T> ok()
    {
        return restResult(null, SUCCESS, "操作成功");
    }

    public static <T> R<T> ok(T data)
    {
        return restResult(data, SUCCESS, "操作成功");
    }

    public static <T> R<T> ok(T data, String msg)
    {
        return restResult(data, SUCCESS, msg);
    }

    public static <T> R<T> fail()
    {
        return restResult(null, FAIL, "操作失败");
    }

    public static <T> R<T> fail(String msg)
    {
        return restResult(null, FAIL, msg);
    }

    public static <T> R<T> fail(T data)
    {
        return restResult(data, FAIL, "操作失败");
    }

    public static <T> R<T> fail(T data, String msg)
    {
        return restResult(data, FAIL, msg);
    }

    public static <T> R<T> fail(int code, String msg)
    {
        return restResult(null, code, msg);
    }

    // 封装返回结果
    private static <T> R<T> restResult(T data, int code, String msg)
    {
        R<T> apiResult = new R<>();// 创建R对象
        apiResult.setCode(code);// 设置返回结果状态码
        apiResult.setData(data);// 设置返回结果数据
        apiResult.setMsg(msg);// 设置返回结果消息
        return apiResult;
    }

    // 获取返回结果状态码
    public int getCode()
    {
        return code;
    }

    // 设置返回结果状态码
    public void setCode(int code)
    {
        this.code = code;
    }

    // 获取返回结果消息
    public String getMsg()
    {
        return msg;
    }

    // 设置返回结果消息
    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    // 获取返回结果数据
    public T getData()
    {
        return data;
    }

    // 设置返回结果数据
    public void setData(T data)
    {
        this.data = data;
    }

    // 判断返回结果是否为错误
    public static <T> Boolean isError(R<T> ret)
    {
        return !isSuccess(ret);
    }

    // 判断返回结果是否为成功
    public static <T> Boolean isSuccess(R<T> ret)
    {
        return R.SUCCESS == ret.getCode();
    }
}
