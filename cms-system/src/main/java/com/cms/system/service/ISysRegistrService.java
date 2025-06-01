package com.cms.system.service;

import java.util.List;
import com.cms.common.core.domain.entity.SysRegistr;

/**
 * 报名信息Service接口
 * 
 * @author quoteZZZ
 * @date 2025-05-26
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
    public int deleteSysRegistrByRegistrIds(List<Long> registrIds);

    /**
     * 删除报名信息信息
     * 
     * @param registrId 报名信息主键
     * @return 结果
     */
    public int deleteSysRegistrByRegistrId(Long registrId);

    /**
     * 根据用户ID和竞赛ID查询参赛者信息
     * 
     * @param userId 用户ID
     * @param compId 竞赛ID
     * @return 参赛者信息
     */
    SysRegistr selectSysRegistrByUserIdAndCompId(Long userId, Long compId);

    // 新增方法：根据竞赛ID查询参赛者列表
    List<SysRegistr> selectSysRegistrListByCompId(Long compId);
}
