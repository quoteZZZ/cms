package com.cms.system.service;

import com.cms.common.core.domain.entity.SysLogininfor;

import java.util.List;

/**
 * 系统访问（登录）日志情况信息服务业务层：
 * 用于获取系统访问日志情况信息，并实现相关操作
 * @author quoteZZZ
 */
public interface ISysLogininforService
{
    /**
     * 新增系统登录日志
     * 
     * @param logininfor 访问日志对象
     */
    public void insertLogininfor(SysLogininfor logininfor);

    /**
     * 查询系统登录日志集合
     * 
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    public List<SysLogininfor> selectLogininforList(SysLogininfor logininfor);

    /**
     * 批量删除系统登录日志
     * 
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    public int deleteLogininforByIds(Long[] infoIds);

    /**
     * 清空系统登录日志
     */
    public void cleanLogininfor();
}
