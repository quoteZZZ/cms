package com.cms.system.mapper;

import com.cms.common.core.domain.entity.SysUserRole;
import com.cms.common.core.domain.entity.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户与角色关联表 数据层：
 * @author quoteZZZ
 */
public interface SysUserRoleMapper
{
    /**
     * 通过用户ID删除用户和角色关联
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserRoleByUserId(Long userId);

    /**
     * 批量删除用户和角色关联
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteUserRole(Long[] ids);

    /**
     * 通过角色ID查询角色使用数量
     * @param roleId 角色ID
     * @return 结果
     */
    public int countUserRoleByRoleId(Long roleId);

    /**
     * 批量新增用户角色信息
     * @param userRoleList 用户角色列表
     * @return 结果
     */
    public int batchUserRole(List<SysUserRole> userRoleList);

    /**
     * 删除用户和角色关联信息
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    public int deleteUserRoleInfo(SysUserRole userRole);

    /**
     * 批量取消授权用户角色
     * @param roleId 角色ID
     * @param userIds 需要删除的用户数据ID
     * @return 结果
     */
    public int deleteUserRoleInfos(@Param("roleId") Long roleId, @Param("userIds") Long[] userIds);

    /**
     * 检查用户是否拥有指定角色代码
     *
     * @param userId 用户ID
     * @param roleKey 角色代码
     * @return 记录数
     */
    int checkUserRoleByKey(@Param("userId") Long userId, @Param("roleKey") String roleKey);

    /**
     * 判断用户是否是评委角色
     *
     * @param userId 用户ID
     * @return 是评委角色返回1，否则返回0
     */
    int isJudge(Long userId);

    /**
     * 根据用户ID查询用户所属角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> selectRolesByUserId(Long userId);
}
