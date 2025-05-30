package com.cms.web.controller.system;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.cms.common.annotation.Log;
import com.cms.common.core.controller.BaseController;
import com.cms.common.core.domain.AjaxResult;
import com.cms.common.enums.BusinessType;
import com.cms.common.core.domain.entity.SysRegistr;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.core.domain.entity.SysComp;
import com.cms.system.service.ISysRegistrService;
import com.cms.system.service.ISysCompService;
import com.cms.common.utils.excel.ExcelUtil;
import com.cms.common.utils.file.FileUploadUtils;
import com.cms.common.config.CmsConfig;
import com.cms.framework.web.service.TokenService;
import com.cms.common.utils.ServletUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cms.common.core.domain.R;
import com.cms.common.exception.ServiceException;

/**
 * 报名信息Controller
 * 
 * @author quoteZZZ
 * @date 2025-03
 */
@Api(tags = "报名信息Controller")
@RestController
@RequestMapping("/system/registr")
public class SysRegistrController extends BaseController {
    
    @Autowired
    private ISysRegistrService sysRegistrService;
    
    @Autowired
    private ISysCompService sysCompService;
    
    @Autowired
    private TokenService tokenService;
    
    private static final Logger log = LoggerFactory.getLogger(SysRegistrController.class);
    
    /**
     * 查询报名信息列表
     */
    @ApiOperation("查询报名信息列表")
    @PreAuthorize("@ss.hasPermi('system:registr:list')")
    @GetMapping("/list")
    public R<List<SysRegistr>> list(
            @ApiParam(value = "报名信息查询条件") SysRegistr sysRegistr) {
        startPage();
        List<SysRegistr> list = sysRegistrService.selectSysRegistrList(sysRegistr);
        return R.ok(list);
    }
    
    /**
     * 导出报名信息列表
     */
    @ApiOperation("导出报名信息列表")
    @PreAuthorize("@ss.hasPermi('system:registr:export')")
    @Log(title = "报名信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(
            HttpServletResponse response,
            @ApiParam(value = "报名信息导出条件") SysRegistr sysRegistr) {
        List<SysRegistr> list = sysRegistrService.selectSysRegistrList(sysRegistr);
        ExcelUtil<SysRegistr> util = new ExcelUtil<>(SysRegistr.class);
        util.exportExcel(response, list, "报名信息数据");
    }
    
    /**
     * 获取报名信息详细信息
     */
    @ApiOperation("获取报名信息详细信息")
    @PreAuthorize("@ss.hasPermi('system:registr:query')")
    @GetMapping("/{registrId}")
    public R<SysRegistr> getInfo(
            @ApiParam(value = "报名信息主键", required = true)
            @PathVariable("registrId") Long registrId) {
        return R.ok(sysRegistrService.selectSysRegistrByRegistrId(registrId));
    }
    
    /**
     * 新增报名信息
     */
    @ApiOperation("新增报名信息")
    @PreAuthorize("@ss.hasPermi('system:registr:add')")
    @Log(title = "报名信息", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Integer> add(
            @ApiParam(value = "新增报名信息数据", required = true)
            @RequestBody SysRegistr sysRegistr) {
        // 设置创建人
        sysRegistr.setCreateBy(getUsername());
        return R.ok(sysRegistrService.insertSysRegistr(sysRegistr));
    }
    
    /**
     * 修改报名信息
     */
    @ApiOperation("修改报名信息")
    @PreAuthorize("@ss.hasPermi('system:registr:edit')")
    @Log(title = "报名信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Integer> edit(
            @ApiParam(value = "修改报名信息数据", required = true)
            @RequestBody SysRegistr sysRegistr) {
        return R.ok(sysRegistrService.updateSysRegistr(sysRegistr));
    }
    
    /**
     * 更新报名状态
     */
    @ApiOperation("更新报名状态")
    @PreAuthorize("@ss.hasPermi('system:registr:edit')")
    @Log(title = "报名信息", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public R<String> updateRegistrStatus(
            @ApiParam(value = "报名ID", required = true) @RequestParam Long registrId,
            @ApiParam(value = "新的报名状态", required = true, example = "0=待审核,1=已通过,2=已拒绝") @RequestParam String registrStatus) {
        if (registrId == null || registrStatus == null) {
            return R.fail("报名ID和状态不能为空");
        }
        SysRegistr sysRegistr = new SysRegistr();
        sysRegistr.setRegistrId(registrId);
        sysRegistr.setRegistrStatus(registrStatus);
        sysRegistr.setUpdateBy(getUsername());
        int rows = sysRegistrService.updateSysRegistr(sysRegistr);
        if (rows > 0) {
            return R.ok("更新报名状态成功");
        }
        return R.fail("更新报名状态失败");
    }

    /**
     * 删除报名信息
     */
    @ApiOperation("删除报名信息")
    @PreAuthorize("@ss.hasPermi('system:registr:remove')")
    @Log(title = "报名信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{registrId}")
    public R<Integer> remove(
            @ApiParam(value = "报名信息主键", required = true)
            @PathVariable("registrId") Long registrId) {
        return R.ok(sysRegistrService.deleteSysRegistrByRegistrId(registrId));
    }
    
    /**
     * 批量删除报名信息
     */
    @ApiOperation("批量删除报名信息")
    @PreAuthorize("@ss.hasPermi('system:registr:remove')")
    @Log(title = "报名信息", businessType = BusinessType.DELETE)
    @PostMapping("/del")
    public R<Integer> batchDelete(@RequestBody Long[] registrIds) {
        return R.ok(sysRegistrService.deleteSysRegistrByRegistrIds(registrIds));
    }
    

}