package com.cms.system.mapper;

import java.util.List;
import com.cms.common.core.domain.entity.SysResult;
import org.apache.ibatis.annotations.Param;

/**
 * 成绩结果Mapper接口
 * 
 * @author quoteZZZ
 * @date 2025-03-09
 */
public interface SysResultMapper 
{
    /**
     * 查询成绩结果
     * 
     * @param resultId 成绩结果主键
     * @return 成绩结果
     */
    public SysResult selectSysResultByResultId(Long resultId);

    /**
     * 查询成绩结果列表
     * 
     * @param sysResult 成绩结果
     * @return 成绩结果集合
     */
    public List<SysResult> selectSysResultList(SysResult sysResult);

    /**
     * 新增成绩结果
     * 
     * @param sysResult 成绩结果
     * @return 结果
     */
    public int insertSysResult(SysResult sysResult);

    /**
     * 修改成绩结果
     * 
     * @param sysResult 成绩结果
     * @return 结果
     */
    public int updateSysResult(SysResult sysResult);

    /**
     * 删除成绩结果
     * 
     * @param resultId 成绩结果主键
     * @return 结果
     */
    public int deleteSysResultByResultId(Long resultId);

    /**
     * 批量删除成绩结果
     * 
     * @param resultIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysResultByResultIds(Long[] resultIds);

    // 根据报名编号查询成绩结果
    SysResult selectSysResultByRegistrId(Long registr);

    // 根据报名编号列表查询成绩结果列表
    List<SysResult> selectSysResultsByRegistrIds(List<Long> registrIds);

    /**
     * 根据用户ID和竞赛ID查询成绩结果
     * 
     * @param userId 用户ID
     * @param compId 竞赛ID
     * @return 成绩结果
     */
    public SysResult selectSysResultByUserIdAndCompId(@Param("userId") Long userId, @Param("compId") Long compId);

}