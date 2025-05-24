package com.cms.system.service;

import java.util.List;
import com.cms.common.core.domain.entity.SysScore;

/**
 * 评分信息Service接口
 * 
 * @author quoteZZZ
 * @date 2025-03-09
 */
public interface ISysScoreService 
{
    /**
     * 查询评分信息
     * 
     * @param scoreId 评分信息主键
     * @return 评分信息
     */
    public SysScore selectSysScoreByScoreId(Long scoreId);

    /**
     * 查询评分信息列表
     * 
     * @param sysScore 评分信息
     * @return 评分信息集合
     */
    public List<SysScore> selectSysScoreList(SysScore sysScore);

    /**
     * 新增评分信息
     * 
     * @param sysScore 评分信息
     * @return 结果
     */
    public int insertSysScore(SysScore sysScore);

    /**
     * 修改评分信息
     * 
     * @param sysScore 评分信息
     * @return 结果
     */
    public int updateSysScore(SysScore sysScore);

    /**
     * 批量删除评分信息
     * 
     * @param scoreIds 需要删除的评分信息主键集合
     * @return 结果
     */
    public int deleteSysScoreByScoreIds(Long[] scoreIds);

    /**
     * 删除评分信息信息
     * 
     * @param scoreId 评分信息主键
     * @return 结果
     */
    public int deleteSysScoreByScoreId(Long scoreId);
}
