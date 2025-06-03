package com.cms.web.controller.system;

import java.util.List;
import java.util.Date;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.cms.common.annotation.Log;
import com.cms.common.core.controller.BaseController;
import com.cms.common.core.page.TableDataInfo;
import com.cms.common.enums.BusinessType;
import com.cms.common.core.domain.entity.SysScore;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.core.domain.entity.SysRegistr;
import com.cms.system.service.ISysScoreService;
import com.cms.system.service.ISysRegistrService;
import com.cms.common.utils.excel.ExcelUtil;
import com.cms.framework.web.service.TokenService;
import com.cms.common.utils.ServletUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cms.common.core.domain.R;

/**
 * 评分信息Controller
 *
 * @author quoteZZZ
 * @date 2025-03-09
 */
@Api(tags = "评分信息Controller")
@RestController
@RequestMapping("/system/score")
public class SysScoreController extends BaseController {

    @Autowired
    private ISysScoreService sysScoreService;

    @Autowired
    private ISysRegistrService sysRegistrService;

    @Autowired
    private TokenService tokenService;

    private static final Logger log = LoggerFactory.getLogger(SysScoreController.class);

    /**
     * 查询评分信息列表
     */
    @ApiOperation("查询评分信息列表")
    @PreAuthorize("@ss.hasPermi('system:score:list')")
    @GetMapping("/list")
    public TableDataInfo list(
            @ApiParam(value = "评分信息查询条件") SysScore sysScore) {
        startPage();
        List<SysScore> list = sysScoreService.selectSysScoreList(sysScore);
        return getDataTable(list);
    }

    /**
     * 导出评分信息列表
     */
    @ApiOperation("导出评分信息列表")
    @PreAuthorize("@ss.hasPermi('system:score:export')")
    @Log(title = "评分信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(
            HttpServletResponse response,
            @ApiParam(value = "评分信息导出条件") SysScore sysScore) {
        List<SysScore> list = sysScoreService.selectSysScoreList(sysScore);
        ExcelUtil<SysScore> util = new ExcelUtil<>(SysScore.class);
        util.exportExcel(response, list, "评分信息数据");
    }

    /**
     * 获取评分信息详细信息
     */
    @ApiOperation("获取评分信息详细信息")
    @PreAuthorize("@ss.hasPermi('system:score:query')")
    @GetMapping("/{scoreId}")
    public R<SysScore> getInfo(
            @ApiParam(value = "评分信息主键", required = true)
            @PathVariable("scoreId") Long scoreId) {
        return R.ok(sysScoreService.selectSysScoreByScoreId(scoreId));
    }

    /**
     * 新增评分信息
     */
    @ApiOperation("新增评分信息")
    @PreAuthorize("@ss.hasPermi('system:score:add')")
    @Log(title = "评分信息", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Integer> add(
            @ApiParam(value = "新增评分信息数据", required = true)
            @RequestBody SysScore sysScore) {
        return R.ok(sysScoreService.insertSysScore(sysScore));
    }

    /**
     * 修改评分信息
     */
    @ApiOperation("修改评分信息")
    @PreAuthorize("@ss.hasPermi('system:score:edit')")
    @Log(title = "评分信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Integer> edit(
            @ApiParam(value = "修改评分信息数据", required = true)
            @RequestBody SysScore sysScore) {
        return R.ok(sysScoreService.updateSysScore(sysScore));
    }

    /**
     * 删除评分信息
     */
    @ApiOperation("删除评分信息")
    @PreAuthorize("@ss.hasPermi('system:score:remove')")
    @Log(title = "评分信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{scoreIds}")
    public R<Integer> remove(
            @ApiParam(value = "评分信息主键集合", required = true)
            @PathVariable List<Long> scoreIds) {
        return R.ok(sysScoreService.deleteSysScoreByScoreIds(scoreIds));
    }



}

