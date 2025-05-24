package com.cms.system.service.impl;

import com.cms.common.annotation.DataScope;
import com.cms.common.core.domain.entity.SysLogininfor;
import com.cms.system.mapper.SysLogininforMapper;
import com.cms.system.service.ISysLogininforService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统登录访问日志情况信息业务层实现类：
 * 实现了系统登录日志的增删查功能，用于管理系统的访问日志记录。
 * @author quoteZZZ
 */
@Service
public class SysLogininforServiceImpl implements ISysLogininforService
{

    @Autowired
    private SysLogininforMapper logininforMapper;// 登录日志的持久层接口

    /**
     * 新增系统登录日志
     * @param logininfor 访问日志对象
     */
    @Override
    public void insertLogininfor(SysLogininfor logininfor)
    {
        logininforMapper.insertLogininfor(logininfor);
    }

    /**
     * 查询系统登录日志集合
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    @DataScope(deptAlias = "d", userAlias = "u")// 数据权限注解（d表示限定部门查询，u表示限定用户查询）
    @Override
    public List<SysLogininfor> selectLogininforList(SysLogininfor logininfor)
    {
        return logininforMapper.selectLogininforList(logininfor);
    }

    /**
     * 批量删除系统登录日志
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    @Override
    public int deleteLogininforByIds(Long[] infoIds)
    {
        return logininforMapper.deleteLogininforByIds(infoIds);
    }

    /**
     * 清空系统登录日志
     */
    @Override
    public void cleanLogininfor()
    {
        logininforMapper.cleanLogininfor();
    }
}
