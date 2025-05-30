package com.cms.web.controller.system;

import com.cms.common.annotation.Log;
import com.cms.common.core.controller.BaseController;
import com.cms.common.core.domain.AjaxResult;
import com.cms.common.core.domain.R;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.core.page.TableDataInfo;
import com.cms.common.enums.BusinessType;
import com.cms.common.utils.excel.ExcelUtil;
import com.cms.framework.web.service.SysPermissionService;
import com.cms.framework.web.service.TokenService;
import com.cms.common.core.domain.entity.SysComp;
import com.cms.common.core.domain.entity.SysUserComp;
import com.cms.system.mapper.SysUserMapper;
import com.cms.system.service.ISysCompService;
import com.cms.system.service.ISysDeptService;
import com.cms.system.service.ISysRoleService;
import com.cms.system.service.ISysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 竞赛信息Controller
 *
 * @author quoteZZZ
 * @date 2025-03-09
 */
@Api(tags = "竞赛信息Controller")
@RestController
@RequestMapping("/system/comp")
public class SysCompController extends BaseController {

    @Autowired
    private ISysCompService sysCompService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysDeptService deptService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


/**
 * 查询竞赛信息列表
 *
 * 功能描述：
 * 1. 缓存空值处理防止穿透
 * 2. 随机TTL防止雪崩
 * 3. 逻辑过期策略实现异步更新
 * 4. 旁路缓存模式处理缓存缺失
 *
 * @param sysComp 竞赛信息查询条件
 * @return 竞赛信息列表
 */
@ApiOperation("查询竞赛信息列表")
@PreAuthorize("@ss.hasPermi('system:comp:list')")
@GetMapping("/list")
public R<List<SysComp>> list(
        @ApiParam(value = "竞赛信息查询条件") SysComp sysComp) {
    startPage();
    //打印查询条件
    logger.info("查询条件：{}", sysComp);
    List<SysComp> list = sysCompService.selectSysCompList(sysComp, "comp_id DESC");
    return R.ok(list);
}

/**
 * 导出竞赛信息列表
 *
 * 功能描述：
 * 1. 缓存空值处理防止穿透
 * 2. 随机TTL防止雪崩
 * 3. 逻辑过期策略实现异步更新
 * 4. 旁路缓存模式处理缓存缺失
 *
 * @param response HTTP响应对象
 * @param sysComp  竞赛信息导出条件
 */
@ApiOperation("导出竞赛信息列表")
@PreAuthorize("@ss.hasPermi('system:comp:export')")
@Log(title = "竞赛信息", businessType = BusinessType.EXPORT)
@PostMapping("/export")
public void export(
        HttpServletResponse response,
        @ApiParam(value = "竞赛信息导出条件") SysComp sysComp) {
    List<SysComp> list = sysCompService.selectSysCompList(sysComp, "comp_id");
    ExcelUtil<SysComp> util = new ExcelUtil<>(SysComp.class);
    util.exportExcel(response, list, "竞赛信息数据");
}

/**
 * 获取竞赛信息详细信息
 *
 * 功能描述：
 * 1. 缓存空值处理防止穿透
 * 2. 随机TTL防止雪崩
 * 3. 逻辑过期策略实现异步更新
 * 4. 旁路缓存模式处理缓存缺失
 *
 * @param compId 竞赛ID，不能为空
 * @return 竞赛信息
 */
@ApiOperation("获取竞赛信息详细信息")
@PreAuthorize("@ss.hasPermi('system:comp:query')")
@GetMapping("/{compId}")
public R<SysComp> getInfo(
        @ApiParam(value = "竞赛信息主键", required = true)
        @PathVariable("compId") Long compId) {
    return R.ok(sysCompService.selectSysCompByCompId(compId));
}


    /**
     * 新增竞赛信息
     */
    @ApiOperation("新增竞赛信息")
    @PreAuthorize("@ss.hasPermi('system:comp:add')")
    @Log(title = "竞赛信息", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Integer> add(
            @ApiParam(value = "新增竞赛信息数据", required = true)
            @RequestBody SysComp sysComp) {
        return R.ok(sysCompService.insertSysComp(sysComp));
    }

    /**
     * 修改竞赛信息
     */
    @ApiOperation("修改竞赛信息")
    @PreAuthorize("@ss.hasPermi('system:comp:edit')")
    @Log(title = "竞赛信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Integer> edit(
            @ApiParam(value = "修改竞赛信息数据", required = true)
            @RequestBody SysComp sysComp) {
        return R.ok(sysCompService.updateSysComp(sysComp));
    }

    /**
     * 删除竞赛信息（单条）
     */
    @ApiOperation("删除竞赛信息（单条）")
    @PreAuthorize("@ss.hasPermi('system:comp:remove')")
    @Log(title = "竞赛信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{compId}")
    public R<Integer> delete(
            @ApiParam(value = "竞赛信息主键", required = true)
            @PathVariable("compId") Long compId) {
        return R.ok(sysCompService.deleteSysCompByCompId(compId));
    }

    /**
     * 删除竞赛信息（批量）
     */
    @ApiOperation("删除竞赛信息（批量）")
    @PreAuthorize("@ss.hasPermi('system:comp:remove')")
    @Log(title = "竞赛信息", businessType = BusinessType.DELETE)
    @PostMapping("/del")
    public R<Integer> batchDelete(@RequestBody Long[] compIds) {
        return R.ok(sysCompService.deleteSysCompByCompIds(compIds));
    }

    /**
     * 查询已分配给评委的竞赛列表
     */
    @PreAuthorize("@ss.hasPermi('system:comp:list')")
    @GetMapping("/myAssignedList")
    public TableDataInfo myAssignedList() {
        startPage();
        Long userId = getUserId(); // 获取当前登录用户ID
        List<SysComp> list = sysCompService.selectMyAssignedCompetitions(userId);
        return getDataTable(list);
    }

    /**
     * 查询未分配给评委的竞赛列表
     */
    @PreAuthorize("@ss.hasPermi('system:comp:list')")
    @GetMapping("/unassignedList")
    public TableDataInfo unassignedList() {
        startPage();
        Long userId = getUserId(); // 获取当前登录用户ID
        List<SysComp> list = sysCompService.selectUnassignedCompetitions(userId);
        return getDataTable(list);
    }

    /**
     * 查询推荐竞赛
     */
    @PreAuthorize("@ss.hasPermi('system:comp:list')")
    @GetMapping("/recommend")
    public R<List<SysComp>> recommendCompetitions(
            @ApiParam(value = "推荐类型(random/category/access/latest)", required = true)
            @RequestParam String type,
            @ApiParam(value = "竞赛类别（仅当type=category时有效）")
            @RequestParam(required = false) Character category,
            @ApiParam(value = "推荐数量", required = true)
            @RequestParam int count) {
        // 参数校验
        if (count <= 0) {
            return R.fail("推荐数量必须大于0");
        }
        if ("category".equalsIgnoreCase(type) && category == null) {
            return R.fail("当推荐类型为category时，必须提供竞赛类别参数");
        }

        logger.info("请求推荐竞赛, type={}, category={}, count={}", type, category, count);

        try {
            List<SysComp> result = sysCompService.recommendCompetitions(type, category, count);
            return R.ok(result);
        } catch (Exception e) {
            logger.error("推荐竞赛信息失败: {}", e.getMessage());
            return R.fail(e.getMessage());
        }
    }

    /**
     * 查询已分配用户竞赛列表
     */
    @PreAuthorize("@ss.hasPermi('system:comp:list')")
    @GetMapping("/AuthJudge/allocatedList")
    public TableDataInfo allocatedList(
            @ApiParam(value = "竞赛ID", required = true)
            @RequestParam Long compId) {
        startPage();
        SysUser user = new SysUser();
        user.setCompId(compId); // 设置竞赛ID
        List<SysUser> list = userService.selectAllocatedJudgeList(user);
        return getDataTable(list);
    }

    /**
     * 查询未分配用户竞赛列表
     */
    @PreAuthorize("@ss.hasPermi('system:comp:list')")
    @GetMapping("/AuthJudge/unallocatedList")
    public TableDataInfo unallocatedList(
            @ApiParam(value = "竞赛ID", required = true)
            @RequestParam Long compId) {
        startPage();
        SysUser user = new SysUser();
        user.setCompId(compId); // 设置竞赛ID
        List<SysUser> list = userService.selectUnallocatedJudgeList(user);
        // 过滤非评委用户
        list = list.stream()
                .filter(sysuser -> userService.isJudge(sysuser.getUserId())) // 调用服务判断是否为评委角色
                .collect(Collectors.toList());
        return getDataTable(list);
    }


    /**
     * 取消授权用户
     */
    @PreAuthorize("@ss.hasPermi('system:comp:edit')")
    @Log(title = "竞赛管理", businessType = BusinessType.GRANT)
    @PutMapping("/AuthJudge/cancel")
    public AjaxResult cancelAuthUser(@RequestBody SysUserComp userComp)
    {
        return toAjax(sysCompService.deleteAuthUser(userComp));
    }

    /**
     * 批量取消授权用户
     */
    @PreAuthorize("@ss.hasPermi('system:comp:edit')")
    @Log(title = "竞赛管理", businessType = BusinessType.GRANT)
    @PutMapping("/AuthJudge/cancelAll")
    public R<Integer> cancelAuthUserAll(
            @ApiParam(value = "竞赛ID", required = true) Long compId,
            @ApiParam(value = "用户ID集合", required = true) @RequestBody Long[] userIds) {
        return R.ok(sysCompService.deleteAuthUsers(compId, userIds));
    }

    /**
     * 批量选择用户授权
     */
    @PreAuthorize("@ss.hasPermi('system:comp:edit')")
    @Log(title = "竞赛管理", businessType = BusinessType.GRANT)
    @PutMapping("/AuthJudge/selectAll")
    public R<Integer> selectAuthUserAll(
            @ApiParam(value = "竞赛ID", required = true)
            @RequestParam Long compId,
            @ApiParam(value = "用户ID集合", required = true)
            @RequestParam Long[] userIds) {
        return R.ok(sysCompService.insertAuthUsers(compId, userIds));
    }

    /**
     * 修改竞赛阶段状态
     */
    @ApiOperation("修改竞赛阶段状态")
    @PreAuthorize("@ss.hasPermi('system:comp:edit')")
    @Log(title = "竞赛信息管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{compId}/stage")
    public R<Void> updateCompetitionStage(
            @ApiParam(value = "竞赛ID", required = true) @PathVariable("compId") Long compId,
            @ApiParam(value = "新的阶段状态", required = true, example = "0=报名,1=初赛,2=复赛,3=决赛,4=评审,5=公示") @RequestParam("newStageStatus") Character newStageStatus) {
        // Service层应包含校验逻辑：如竞赛是否存在、状态转换是否合法等
        // sysCompService.updateCompStageStatus(compId, newStageStatus, getUserId());
        // 此处仅为示例，实际应调用service层方法
        SysComp compToUpdate = new SysComp();
        compToUpdate.setCompId(compId);
        compToUpdate.setStageStatus(newStageStatus);
        compToUpdate.setUpdateBy(getUsername());
        int rows = sysCompService.updateSysComp(compToUpdate); // Assumes updateSysComp can handle partial updates based on non-null fields
        return rows > 0 ? R.ok() : R.fail("更新竞赛阶段状态失败");
    }

    /**
     * 修改竞赛整体状态
     */
    @ApiOperation("修改竞赛整体状态")
    @PreAuthorize("@ss.hasPermi('system:comp:edit')")
    @Log(title = "竞赛信息管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{compId}/compStatus")
    public R<Void> updateCompetitionStatus(
            @ApiParam(value = "竞赛ID", required = true) @PathVariable("compId") Long compId,
            @ApiParam(value = "新的竞赛状态", required = true, example = "0=未开始,1=进行中,2=已结束") @RequestParam("newCompStatus") Character newCompStatus) {
        // Service层应包含校验逻辑
        // sysCompService.updateCompStatus(compId, newCompStatus, getUserId());
        SysComp compToUpdate = new SysComp();
        compToUpdate.setCompId(compId);
        compToUpdate.setCompStatus(newCompStatus);
        compToUpdate.setUpdateBy(getUsername());
        int rows = sysCompService.updateSysComp(compToUpdate); // Assumes updateSysComp can handle partial updates
        return rows > 0 ? R.ok() : R.fail("更新竞赛状态失败");
    }

}
