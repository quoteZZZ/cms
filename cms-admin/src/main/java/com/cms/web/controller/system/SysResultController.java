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
import com.cms.common.core.domain.entity.SysResult;
import com.cms.system.service.ISysResultService;
import com.cms.common.utils.excel.ExcelUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import com.cms.common.core.domain.R;

/**
 * 成绩结果Controller
 *
 * @author quoteZZZ
 * @date 2025-03-09
 */
@Api(tags = "成绩结果Controller")
@RestController
@RequestMapping("/system/result")
public class SysResultController extends BaseController {

    @Autowired
    private ISysResultService sysResultService;

/**
 * 查询成绩结果列表
 */
@ApiOperation("查询成绩结果列表")
@PreAuthorize("@ss.hasPermi('system:result:list')")
@GetMapping("/list")
    public R<List<SysResult>> list(
            @ApiParam(value = "成绩结果查询条件") SysResult sysResult) {
        startPage();
        List<SysResult> list = sysResultService.selectSysResultList(sysResult);
        return R.ok(list);
    }

    /**
     * 导出成绩结果列表
     */
    @ApiOperation("导出成绩结果列表")
    @PreAuthorize("@ss.hasPermi('system:result:export')")
    @Log(title = "成绩结果", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(
            HttpServletResponse response,
            @ApiParam(value = "成绩结果导出条件") SysResult sysResult) {
        List<SysResult> list = sysResultService.selectSysResultList(sysResult);
        ExcelUtil<SysResult> util = new ExcelUtil<>(SysResult.class);
        util.exportExcel(response, list, "成绩结果数据");
    }

    /**
     * 获取成绩结果详细信息
     */
    @ApiOperation("获取成绩结果详细信息")
    @PreAuthorize("@ss.hasPermi('system:result:query')")
    @GetMapping("/{resultId}")
    public R<SysResult> getInfo(
            @ApiParam(value = "成绩结果主键", required = true)
            @PathVariable("resultId") Long resultId) {
        return R.ok(sysResultService.selectSysResultByResultId(resultId));
    }

    /**
     * 新增成绩结果
     */
    @ApiOperation("新增成绩结果")
    @PreAuthorize("@ss.hasPermi('system:result:add')")
    @Log(title = "成绩结果", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Integer> add(
            @ApiParam(value = "新增成绩结果数据", required = true)
            @RequestBody SysResult sysResult) {
        return R.ok(sysResultService.insertSysResult(sysResult));
    }

    /**
     * 修改成绩结果
     */
    @ApiOperation("修改成绩结果")
    @PreAuthorize("@ss.hasPermi('system:result:edit')")
    @Log(title = "成绩结果", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Integer> edit(
            @ApiParam(value = "修改成绩结果数据", required = true)
            @RequestBody SysResult sysResult) {
        return R.ok(sysResultService.updateSysResult(sysResult));
    }

    /**
     * 删除成绩结果
     */
    @ApiOperation("删除成绩结果")
    @PreAuthorize("@ss.hasPermi('system:result:remove')")
    @Log(title = "成绩结果", businessType = BusinessType.DELETE)
    @DeleteMapping("/{resultIds}")
    public R<Integer> remove(
            @ApiParam(value = "成绩结果主键集合", required = true)
            @PathVariable Long[] resultIds) {
        return R.ok(sysResultService.deleteSysResultByResultIds(resultIds));
    }
}


