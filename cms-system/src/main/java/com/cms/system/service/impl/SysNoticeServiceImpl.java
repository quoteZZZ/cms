package com.cms.system.service.impl;

import com.cms.common.annotation.DataScope;
import com.cms.common.core.domain.entity.SysNotice;
import com.cms.common.utils.SecurityUtils;
import com.cms.system.mapper.SysNoticeMapper;
import com.cms.system.service.ISysNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公告业务层实现类：
 * 用于管理公告信息
 * @author quoteZZZ
 */
@Service
public class SysNoticeServiceImpl implements ISysNoticeService
{
    @Autowired
    private SysNoticeMapper noticeMapper;

    /**
     * 查询公告信息
     * 
     * @param noticeId 公告ID
     * @return 公告信息
     */
    @Override
    public SysNotice selectNoticeById(Integer noticeId)
    {
        return noticeMapper.selectNoticeById(noticeId);
    }

    /**
     * 查询公告列表
     * 
     * @param notice 公告信息
     * @return 公告集合
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysNotice> selectNoticeList(SysNotice notice)
    {
        return noticeMapper.selectNoticeList(notice);
    }

    /**
     * 新增公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int insertNotice(SysNotice notice)
    {
        // 如果没有设置用户ID和部门ID，则自动设置为当前登录用户的ID和部门ID
        if (notice.getUserId() == null) {
            notice.setUserId(SecurityUtils.getUserId());
        }
        if (notice.getDeptId() == null) {
            notice.setDeptId(SecurityUtils.getDeptId());
        }
        return noticeMapper.insertNotice(notice);
    }

    /**
     * 修改公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int updateNotice(SysNotice notice)
    {
        return noticeMapper.updateNotice(notice);
    }

    /**
     * 删除公告对象
     * 
     * @param noticeId 公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeById(Integer noticeId)
    {
        return noticeMapper.deleteNoticeById(noticeId);
    }

    /**
     * 批量删除公告信息
     * 
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeByIds(Integer[] noticeIds)
    {
        return noticeMapper.deleteNoticeByIds(noticeIds);
    }
}
