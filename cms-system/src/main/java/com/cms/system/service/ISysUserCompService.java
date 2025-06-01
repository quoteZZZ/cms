package com.cms.system.service;

import com.cms.common.core.domain.entity.SysUserComp;
import java.util.List;

/**
 * 用户与竞赛关联Service接口
 *
 * @author quoteZZZ
 * @date 2025-05-30
 */
public interface ISysUserCompService {
    /**
     * 用户报名参加竞赛
     *
     * @param userId 用户ID
     * @param compId 竞赛ID
     * @return 结果
     */
    public int joinCompetition(Long userId, Long compId);

    /**
     * 用户退出竞赛
     *
     * @param userId 用户ID
     * @param compId 竞赛ID
     * @return 结果
     */
    public int exitCompetition(Long userId, Long compId);

    /**
     * 查询用户参加的竞赛列表
     *
     * @param userId 用户ID
     * @return 竞赛ID列表
     */
    public List<Long> selectUserCompetitions(Long userId);

    /**
     * 查询竞赛的参与用户
     *
     * @param compId 竞赛ID
     * @return 用户ID列表
     */
    public List<Long> selectCompetitionUsers(Long compId);

    /**
     * 检查用户是否已报名参加某竞赛
     * 该方法通过直接查询关联表进行判断，避免使用contains进行全表查询，提高性能
     *
     * @param userId 用户ID
     * @param compId 竞赛ID
     * @return true-已报名，false-未报名
     */
    public boolean isUserJoinedCompetition(Long userId, Long compId);

    /**
     * 批量删除竞赛关联关系
     *
     * @param compId 竞赛ID
     * @return 结果
     */
    public int deleteCompetitionUsers(Long compId);
}
