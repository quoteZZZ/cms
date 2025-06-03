package com.cms.system.mapper;

import com.cms.common.core.domain.entity.SysUserComp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户与竞赛关联表 数据层：
 * @author quoteZZZ
 */
public interface SysUserCompMapper
{
    /**
     * 通过用户ID删除用户和竞赛关联
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserCompByUserId(Long userId);

    /**
     * 批量删除用户和竞赛关联
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteUserComp(Long[] ids);

    /**
     * 通过角色ID查询竞赛使用数量
     * @param compId 竞赛ID
     * @return 结果
     */
    public int countUserCompByCompId(Long compId);

    /**
     * 批量新增用户竞赛信息
     * @param userCompList 用户竞赛列表
     * @return 结果
     */
    public int batchUserComp(List<SysUserComp> userCompList);

    /**
     * 删除用户和竞赛关联信息
     * @param userComp 用户和竞赛关联信息
     * @return 结果
     */
    public int deleteUserCompInfo(SysUserComp userComp);

    /**
     * 批量取消授权用户竞赛
     * @param compId 竞赛ID
     * @param userIds 需要删除的用户数据ID
     * @return 结果
     */
    public int deleteUserCompInfos(@Param("compId") Long compId, @Param("userIds") Long[] userIds);

    /**
     * 检查用户是否已授权特定竞赛
     * @param userId 用户ID
     * @param compId 竞赛ID
     * @return 结果数量
     */
    public int checkUserCompExists(@Param("userId") Long userId, @Param("compId") Long compId);

    /**
     * 查询已被授权特定竞赛的所有用户ID列表
     * @param compId 竞赛ID
     * @return 用户ID列表
     */
    public List<Long> selectUserIdsByCompId(Long compId);
}
