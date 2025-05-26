package com.cms.system.mapper;

import java.util.List;
import com.cms.common.core.domain.entity.SysRegistr;
import org.apache.ibatis.annotations.Param;

/**
 * 报名信息Mapper接口
 * 
 * @author quoteZZZ
 * @date 2025-03-09
 */
public interface SysRegistrMapper 
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
     * 根据用户ID和竞赛ID查询参赛者信息
     * 
     * @param userId 用户ID
     * @param compId 竞赛ID
     * @return 参赛者信息
     */
    public SysRegistr selectSysRegistrByUserIdAndCompId(@Param("userId") Long userId, @Param("compId") Long compId);

    /**
     * 新增报名信息
     * 
     * @param sysRegistr 报名信息
     * @return 结果
     */
    public int insertSysRegistr(SysRegistr sysRegistr);

    // 新增方法注释：根据竞赛ID查询参赛者列表
    /**
     * 根据竞赛ID查询参赛者列表
     * 
     * @param compId 竞赛ID
     * @return 参赛者列表
     */
    public List<SysRegistr> selectSysRegistrListByCompId(Long compId);

    /**
     * 修改报名信息
     * 
     * @param sysRegistr 报名信息
     * @return 结果
     */
    public int updateSysRegistr(SysRegistr sysRegistr);

    /**
     * 删除报名信息
     * 
     * @param registrId 报名信息主键
     * @return 结果
     */
    public int deleteSysRegistrByRegistrId(Long registrId);

    /**
     * 批量删除报名信息
     * 
     * @param registrIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysRegistrByRegistrIds(Long[] registrIds);
}
