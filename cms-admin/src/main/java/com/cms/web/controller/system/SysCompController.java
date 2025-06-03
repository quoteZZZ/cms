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
import java.util.ArrayList;
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
public TableDataInfo list(
        @ApiParam(value = "竞赛信息查询条件") SysComp sysComp) {
    startPage();
    //打印查询条件
    logger.info("查询条件：{}", sysComp);
    List<SysComp> list = sysCompService.selectSysCompList(sysComp, "comp_id DESC");

    //再次筛选list中（竞赛状态和竞赛阶段存在的竞赛）根据前端的条件查询
    if (sysComp.getCompStatus() != null || sysComp.getStageStatus() != null) {
        list = list.stream()
                .filter(comp -> (sysComp.getCompStatus() == null || comp.getCompStatus().equals(sysComp.getCompStatus())) &&
                                (sysComp.getStageStatus() == null || comp.getStageStatus().equals(sysComp.getStageStatus())))
                .collect(Collectors.toList());
    }

    return getDataTable(list);
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
    public R<Integer> batchDelete(@RequestBody List<Long> compIds) {
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
    @ApiOperation("查询已分配用户竞赛列表")
    @PreAuthorize("@ss.hasPermi('system:comp:list')")
    @GetMapping("/AuthJudge/allocatedList")
    public TableDataInfo allocatedList(
            @ApiParam(value = "竞赛ID", required = true)
            @RequestParam Long compId) {
        startPage();
        logger.info("查询已分配用户列表, 竞赛ID: {}", compId);

        if (compId == null) {
            return getErrorDataTable("竞赛ID不能为空");
        }

        try {
            SysUser user = new SysUser();
            user.setCompId(compId); // 设置竞赛ID
            List<SysUser> list = userService.selectAllocatedJudgeList(user);
            logger.info("已分配用户列表查询成功, 竞赛ID: {}, 结果数量: {}", compId, list.size());
            return getDataTable(list);
        } catch (Exception e) {
            logger.error("查询已分配用户列表失败, 竞赛ID: {}, 错误: {}", compId, e.getMessage(), e);
            return getErrorDataTable("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询未分配用户竞赛列表
     */
    @ApiOperation("查询未分配用户竞赛列表")
    @PreAuthorize("@ss.hasPermi('system:comp:list')")
    @GetMapping("/AuthJudge/unallocatedList")
    public TableDataInfo unallocatedList(
            @ApiParam(value = "竞赛ID", required = true)
            @RequestParam Long compId) {
        startPage();
        logger.info("查询未分配用户列表, 竞赛ID: {}", compId);

        if (compId == null) {
            return getErrorDataTable("竞赛ID不能为空");
        }

        try {
            SysUser user = new SysUser();
            user.setCompId(compId); // 设置竞赛ID
            List<SysUser> list = userService.selectUnallocatedJudgeList(user);

            // 过滤非评委用户
            List<SysUser> filteredList = list.stream()
                    .filter(sysuser -> userService.isJudge(sysuser.getUserId()))
                    .collect(Collectors.toList());

            logger.info("未分配用户列表查询成功, 竞赛ID: {}, 原始结果数量: {}, 过滤后数量: {}",
                    compId, list.size(), filteredList.size());

            return getDataTable(filteredList);
        } catch (Exception e) {
            logger.error("查询未分配用户列表失败, 竞赛ID: {}, 错误: {}", compId, e.getMessage(), e);
            return getErrorDataTable("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取带有错误消息的空数据表
     */
    private TableDataInfo getErrorDataTable(String message) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(500);
        rspData.setMsg(message);
        rspData.setRows(new ArrayList<>());
        rspData.setTotal(0);
        return rspData;
    }

    /**
     * 取消授权用户
     */
    @ApiOperation("取消授权用户")
    @PreAuthorize("@ss.hasPermi('system:comp:edit')")
    @Log(title = "竞赛管理", businessType = BusinessType.GRANT)
    @PutMapping("/AuthJudge/cancel")
    public R<Integer> cancelAuthUser(@RequestBody SysUserComp userComp) {
        logger.info("取消授权用户, 竞赛ID: {}, 用户ID: {}", userComp.getCompId(), userComp.getUserId());

        // 参数校验
        if (userComp == null || userComp.getUserId() == null || userComp.getCompId() == null) {
            logger.warn("取消授权用户参数无效");
            return R.fail("竞赛ID和用户ID不能为空");
        }

        try {
            int result = sysCompService.deleteAuthUser(userComp);
            if (result > 0) {
                logger.info("取消授权用户成功, 竞赛ID: {}, 用户ID: {}", userComp.getCompId(), userComp.getUserId());
                return R.ok(result);
            } else {
                logger.warn("取消授权用户失败, 竞赛ID: {}, 用户ID: {}, 可能不存在此关联",
                        userComp.getCompId(), userComp.getUserId());
                return R.fail("取消授权用户失败，可能不存在此关联");
            }
        } catch (Exception e) {
            logger.error("取消授权用户异常, 竞赛ID: {}, 用户ID: {}, 错误: {}",
                    userComp.getCompId(), userComp.getUserId(), e.getMessage(), e);
            return R.fail("取消授权用户失败: " + e.getMessage());
        }
    }

    /**
     * 批量取消授权用户
     */
    @ApiOperation("批量取消授权用户")
    @PreAuthorize("@ss.hasPermi('system:comp:edit')")
    @Log(title = "竞赛管理", businessType = BusinessType.GRANT)
    @PostMapping("/AuthJudge/cancelAll")
    public R<Integer> cancelAuthUserAll(
            @ApiParam(value = "批量取消授权参数", required = true)
            @RequestBody CancelAuthRequest request) {

        Long compId = request.getCompId();
        Long[] userIds = request.getUserIds();

        logger.info("批量取消授权用户, 竞赛ID: {}, 用户数量: {}", compId, userIds != null ? userIds.length : 0);

        // 参数校验
        if (compId == null) {
            logger.warn("批量取消授权用户失败: 竞赛ID为空");
            return R.fail("竞赛ID不能为空");
        }

        if (userIds == null || userIds.length == 0) {
            logger.warn("批量取消授权用户失败: 用户ID列表为空");
            return R.fail("用户ID列表不能为空");
        }

        try {
            int result = sysCompService.deleteAuthUsers(compId, userIds);
            logger.info("批量取消授权用户完成, 竞赛ID: {}, 影响行数: {}", compId, result);

            if (result > 0) {
                return R.ok(result, "成功取消" + result + "个用户的授权");
            } else {
                return R.fail("没有用户授权被取消，可能指定的用户未分配到此竞赛");
            }
        } catch (Exception e) {
            logger.error("批量取消授权用户异常, 竞赛ID: {}, 错误: {}", compId, e.getMessage(), e);
            return R.fail("批量取消授权用户失败: " + e.getMessage());
        }
    }

    /**
     * 批量选择用户授权
     *
     * @param request 包含竞赛ID和用户ID列表的请求参数
     * @return 授权结果，包含成功授权的用户数量
     */
    @ApiOperation("批量选择用户授权")
    @PreAuthorize("@ss.hasPermi('system:comp:edit')")
    @Log(title = "竞赛管理", businessType = BusinessType.GRANT)
    @PostMapping("/AuthJudge/selectAll")
    public R<Integer> selectAuthUserAll(
            @ApiParam(value = "批量授权参数", required = true)
            @RequestBody AuthUserRequest request) {

        Long compId = request.getCompId();
        Long[] userIds = request.getUserIds();

        logger.info("批量选择用户授权, 竞赛ID: {}, 用户信息: {}", compId, request);

        // 参数校验
        if (compId == null) {
            logger.warn("批量选择用户授权失败: 竞赛ID为空");
            return R.fail("竞赛ID不能为空");
        }

        // 处理只有一个用户ID的情况
        if (userIds == null && request.getUserId() != null) {
            userIds = new Long[]{request.getUserId()};
            logger.info("转换单个用户ID为数组, 用户ID: {}", request.getUserId());
        }

        if (userIds == null || userIds.length == 0) {
            logger.warn("批量选择用户授权失败: 用户ID列表为空");
            return R.fail("用户ID列表不能为空");
        }

        try {
            int result = sysCompService.insertAuthUsers(compId, userIds);

            if (result > 0) {
                logger.info("批量选择用户授权成功, 竞赛ID: {}, 成功授权数量: {}", compId, result);
                return R.ok(result, "成功授权" + result + "个用户");
            } else {
                logger.warn("批量选择用户授权未生效, 竞赛ID: {}, 可能用户已被授权", compId);
                return R.fail("未成功授权任何用户，可能用户已被授权");
            }
        } catch (Exception e) {
            logger.error("批量选择用户授权异常, 竞赛ID: {}, 错误: {}", compId, e.getMessage(), e);
            return R.fail("批量授权用户失败: " + e.getMessage());
        }
    }

    /**
     * 用于取消授权的请求对象
     */
    public static class CancelAuthRequest {
        private Long compId;
        private Long[] userIds;

        public Long getCompId() {
            return compId;
        }

        public void setCompId(Long compId) {
            this.compId = compId;
        }

        public Long[] getUserIds() {
            return userIds;
        }

        public void setUserIds(Long[] userIds) {
            this.userIds = userIds;
        }
    }

    /**
     * 用于用户授权的请求对象
     */
    public static class AuthUserRequest {
        private Long compId;
        private Long[] userIds;
        private Long userId;

        public Long getCompId() {
            return compId;
        }

        public void setCompId(Long compId) {
            this.compId = compId;
        }

        public Long[] getUserIds() {
            return userIds;
        }

        public void setUserIds(Long[] userIds) {
            this.userIds = userIds;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
}
