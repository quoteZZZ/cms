package com.cms.web.controller.system;

import com.cms.common.annotation.Log;
import com.cms.common.core.controller.BaseController;
import com.cms.common.core.domain.AjaxResult;
import com.cms.common.core.page.TableDataInfo;
import com.cms.common.enums.BusinessType;
import com.cms.common.core.domain.entity.SysNotice;
import com.cms.common.utils.SecurityUtils;
import com.cms.system.service.ISysNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 公告信息操作处理表现层：
 * 
 * @author quoteZZZ
 */
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController extends BaseController
{
    @Autowired
    private ISysNoticeService noticeService;

    /**
     * 获取通知公告列表
     */
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysNotice notice)
    {
        startPage();
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        return getDataTable(list);
    }

    /**
     * 根据通知公告编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:notice:query')")
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@PathVariable Integer noticeId)
    {
        return success(noticeService.selectNoticeById(noticeId));
    }
    
    /**
     * 获取竞赛相关公告列表（无需权限验证，供前台使用）
     */
    @GetMapping("/competition")
    public AjaxResult listCompetitionNotices(SysNotice notice)
    {
        // 只查询状态为正常的公告
        notice.setStatus('0'); // 0表示正常状态
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        return success(list);
    }

    /**
     * 新增通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotice notice)
    {
        notice.setCreateBy(getUsername());
        
        // 设置默认用户和部门ID
        if (notice.getUserId() == null) {
            notice.setUserId(SecurityUtils.getUserId());
        }
        if (notice.getDeptId() == null) {
            notice.setDeptId(SecurityUtils.getDeptId());
        }
        
        return toAjax(noticeService.insertNotice(notice));
    }

    /**
     * 修改通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotice notice)
    {
        notice.setUpdateBy(getUsername());

        
        return toAjax(noticeService.updateNotice(notice));
    }

    /**
     * 删除通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable Integer[] noticeIds)
    {
        return toAjax(noticeService.deleteNoticeByIds(noticeIds));
    }
}
