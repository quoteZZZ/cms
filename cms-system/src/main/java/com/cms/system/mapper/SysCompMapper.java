package com.cms.system.mapper;

import java.util.List;
import com.cms.common.core.domain.entity.SysComp;

import org.apache.ibatis.annotations.Param;

/**
 * 竞赛信息Mapper接口
 */
public interface SysCompMapper {

    /**
     * 根据竞赛ID查询竞赛信息
     *
     * @param compId 竞赛ID
     * @return 竞赛信息
     */
    SysComp selectSysCompByCompId(Long compId);

    /**
     * 查询竞赛信息列表（支持多种排序方式）
     *
     * @param sysComp 竞赛信息查询条件
     * @param order   排序字段（需符合SQL规范，如 "comp_id DESC" 或 "access_frequency ASC"）
     * @return 竞赛信息集合
     */
    List<SysComp> selectSysCompList(@Param("sysComp") SysComp sysComp, @Param("order") String order);

    /**
     * 统计符合条件的竞赛记录数
     *
     * @param sysComp 竞赛信息查询条件
     * @return 符合条件的记录数
     */
    int selectCountByCondition(SysComp sysComp);

    /**
     * 新增竞赛信息
     *
     * @param sysComp 竞赛信息
     * @return 结果
     */
    int insertSysComp(SysComp sysComp);

    /**
     * 修改竞赛信息
     *
     * @param sysComp 竞赛信息
     * @return 结果
     */
    int updateSysComp(SysComp sysComp);

    /**
     * 删除竞赛信息
     *
     * @param compId 竞赛信息主键
     * @return 结果
     */
    int deleteSysCompByCompId(Long compId);

    /**
     * 批量删除竞赛信息
     *
     * @param compIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteSysCompByCompIds(List<Long> compIds);

    /**
     * 查询热门竞赛ID列表
     *
     * @return 热门竞赛ID列表
     */
    List<Long> selectHotCompIds();

    /**
     * 查询已分配给指定用户的竞赛列表
     *
     * @param userId 用户ID
     * @return 竞赛信息集合
     */
    List<SysComp> selectMyAssignedCompetitions(Long userId);

    /**
     * 查询未分配给指定用户的竞赛列表
     *
     * @param userId 用户ID
     * @return 竞赛信息集合
     */
    List<SysComp> selectUnassignedCompetitions(Long userId);

    /**
     * 递增竞赛访问频率
     *
     * @param compId 竞赛ID
     * @return 更新结果
     */
    int incrementAccessFrequency(Long compId);

}
