package com.cms.system.service;

import java.util.List;
import com.cms.common.core.domain.entity.SysResult;

/**
 * 成绩结果Service接口
 * 
 * @author quoteZZZ
 * @date 2025-03-09
 */
public interface ISysResultService 
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
     * 批量删除成绩结果
     * 
     * @param resultIds 需要删除的成绩结果主键集合
     * @return 结果
     */
    public int deleteSysResultByResultIds(List<Long> resultIds);

    /**
     * 删除成绩结果信息
     * 
     * @param resultId 成绩结果主键
     * @return 结果
     */
    public int deleteSysResultByResultId(Long resultId);
}
