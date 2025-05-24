package com.cms.system.service;

import java.util.List;
import com.cms.common.core.domain.entity.SysComp;
import com.cms.common.core.domain.entity.SysUserComp;
import org.apache.ibatis.annotations.Param;

/**
 * 竞赛信息Service接口
 * 
 * @author quoteZZZ
 * @date 2025-03-09
 */
public interface ISysCompService {
    /**
     * 查询竞赛信息
     * 
     * @param compId 竞赛信息主键，不能为空
     * @return 竞赛信息
     */
    public SysComp selectSysCompByCompId(Long compId);

    /**
     * 查询竞赛信息列表
     * 
     * @param sysComp 竞赛信息查询条件，可选
     * @param order   排序字段，需符合SQL规范（如 "comp_id DESC" 或 "access_frequency ASC"）
     * @return 竞赛信息集合
     */
    public List<SysComp> selectSysCompList(
        @Param("sysComp") SysComp sysComp, 
        @Param("order") String order
    );


    /**
     * 新增竞赛信息
     * 
     * @param sysComp 竞赛信息
     * @return 结果
     */
    public int insertSysComp(SysComp sysComp);

    /**
     * 修改竞赛信息
     * 
     * @param sysComp 竞赛信息
     * @return 结果
     */
    public int updateSysComp(SysComp sysComp);

    /**
     * 批量删除竞赛信息
     * 
     * @param compIds 需要删除的竞赛信息主键集合
     * @return 结果
     */
    public int deleteSysCompByCompIds(Long[] compIds) ;


    /**
     * 删除竞赛信息
     *
     * @param compId 竞赛信息主键
     * @return 结果
     */
    public int deleteSysCompByCompId(Long compId);

    /**
     * 取消授权评委竞赛
     *
     * @param userComp 用户和竞赛关联信息
     * @return 结果
     */
    public int deleteAuthUser(SysUserComp userComp);

    /**
     * 批量取消授权评委竞赛
     *
     * @param compId 竞赛ID
     * @param userIds 需要取消授权的用户数据ID集合
     * @return 结果
     * @throws IllegalArgumentException 如果 userIds 为空
     */
    public int deleteAuthUsers(Long compId, Long[] userIds);

    /**
     * 批量选择授权评委竞赛
     *
     * @param compId 竞赛ID
     * @param userIds 需要删除的用户数据ID集合
     * @return 结果
     */
    public int insertAuthUsers(Long compId, Long[] userIds);

    /**
     * 查询推荐竞赛
     *
     * @param type 推荐类型（random、category、access、latest）
     * @param category 竞赛类别（仅在 type=category 时有效）
     * @param count 推荐数量
     * @return 推荐竞赛列表
     */
    public List<SysComp> recommendCompetitions(String type, Character category, int count);

}