package com.cms.system.service;

import java.util.List;
import com.cms.common.core.domain.entity.SysRegistr;

/**
 * 报名信息Service接口
 * 
 * @author quoteZZZ
 * @date 2025-03-09
 */
public interface ISysRegistrService 
{
    /**
     * 查询报名信息
     * 
     * @param registrId 报名信息主键
     * @return 报名信息
     */
    public SysRegistr selectSysRegistrByRegistrId(Long registrId);

    /**
     * 查询报名信息列表
     * 
     * @param sysRegistr 报名信息
     * @return 报名信息集合
     */
    public List<SysRegistr> selectSysRegistrList(SysRegistr sysRegistr);

    /**
     * 新增报名信息
     * 
     * @param sysRegistr 报名信息
     * @return 结果
     */
    public int insertSysRegistr(SysRegistr sysRegistr);

    /**
     * 修改报名信息
     * 
     * @param sysRegistr 报名信息
     * @return 结果
     */
    public int updateSysRegistr(SysRegistr sysRegistr);

    /**
     * 批量删除报名信息
     * 
     * @param registrIds 需要删除的报名信息主键集合
     * @return 结果
     */
    public int deleteSysRegistrByRegistrIds(Long[] registrIds);

    /**
     * 删除报名信息信息
     * 
     * @param registrId 报名信息主键
     * @return 结果
     */
    public int deleteSysRegistrByRegistrId(Long registrId);

    SysRegistr selectSysRegistrByUserIdAndCompId(Long userId, Long compId);
}
