package com.cms.system.mapper;

import java.util.List;
import com.cms.common.core.domain.entity.SysScore;
import org.apache.ibatis.annotations.Param;

/**
 * 评分信息Mapper接口
 * 
 * @author quoteZZZ
 * @date 2025-03-09
 */
public interface SysScoreMapper 
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
     * 查询所有评分数据
     * 
     * @return 评分数据列表
     */
    public List<SysScore> selectAllScores();

    /**
     * 查询所有评分数据，包含报名编号
     * 
     * @return 评分数据列表
     */
    public List<SysScore> selectAllScoresWithRegistrId();

    /**
     * 根据竞赛ID查询所有评分
     *
     * @param compId 竞赛ID
     * @return 评分数据列表
     */
    public List<SysScore> selectScoresByCompId(Long compId);

    /**
     * 根据竞赛ID和用户ID查询评分
     *
     * @param compId 竞赛ID
     * @param userId 用户ID
     * @return 评分数据列表
     */
    public List<SysScore> selectScoresByCompIdAndUserId(@Param("compId") Long compId, @Param("userId") Long userId);

    /**
     * 根据��名ID查询评分
     *
     * @param registrId 报名ID
     * @return 评分数据列表
     */
    public List<SysScore> selectScoresByRegistrId(Long registrId);

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
     * 删除评分信息
     * 
     * @param scoreId 评分信息主键
     * @return 结果
     */
    public int deleteSysScoreByScoreId(Long scoreId);

    /**
     * 批量删除评分信息
     * 
     * @param scoreIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysScoreByScoreIds(List<Long> scoreIds);
}
