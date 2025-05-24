package com.cms.web.controller.system;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cms.common.annotation.Log;
import com.cms.common.core.controller.BaseController;
import com.cms.common.enums.BusinessType;
import com.cms.common.core.domain.entity.SysRegistr;
import com.cms.system.service.ISysRegistrService;
import com.cms.common.utils.excel.ExcelUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import com.cms.common.core.domain.R;

/**
 * 报名信息Controller
 *
 * @author quoteZZZ
 * @date 2025-03-09
 */
@Api(tags = "报名信息Controller")
@RestController
@RequestMapping("/system/registr")
public class SysRegistrController extends BaseController {

    @Autowired
    private ISysRegistrService sysRegistrService;

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
     * 删除报名信息
     */
    @ApiOperation("删除报名信息")
    @PreAuthorize("@ss.hasPermi('system:registr:remove')")
    @Log(title = "报名信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{registrIds}")
    public R<Integer> remove(
            @ApiParam(value = "报名信息主键集合", required = true)
            @PathVariable Long[] registrIds) {
        return R.ok(sysRegistrService.deleteSysRegistrByRegistrIds(registrIds));
    }
}


