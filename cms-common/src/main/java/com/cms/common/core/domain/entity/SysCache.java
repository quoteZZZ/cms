package com.cms.common.core.domain.entity;

import com.cms.common.utils.StringUtils;

/**
 * 缓存信息实体类：
 * 用于缓存管理页面的缓存信息展示
 * @author quoteZZZ
 */
public class SysCache
{
    /** 缓存名称 */
    private String cacheName = "";

    /** 缓存键名 */
    private String cacheKey = "";

    /** 缓存内容 */
    private String cacheValue = "";

    /** 备注 */
    private String remark = "";

    // 无参构造方法
    public SysCache()
    {

    }

    // 有参构造方法
    public SysCache(String cacheName, String remark)
    {
        this.cacheName = cacheName;
        this.remark = remark;
    }

    // 有参构造方法
    public SysCache(String cacheName, String cacheKey, String cacheValue)
    {
        this.cacheName = StringUtils.replace(cacheName, ":", "");
        this.cacheKey = StringUtils.replace(cacheKey, cacheName, "");
        this.cacheValue = cacheValue;
    }

    // getter和setter方法
    public String getCacheName()
    {
        return cacheName;
    }

    public void setCacheName(String cacheName)
    {
        this.cacheName = cacheName;
    }

    public String getCacheKey()
    {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey)
    {
        this.cacheKey = cacheKey;
    }

    public String getCacheValue()
    {
        return cacheValue;
    }

    public void setCacheValue(String cacheValue)
    {
        this.cacheValue = cacheValue;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
