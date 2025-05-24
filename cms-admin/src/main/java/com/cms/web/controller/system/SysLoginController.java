package com.cms.web.controller.system;

import com.cms.common.constant.Constants;
import com.cms.common.core.domain.AjaxResult;
import com.cms.common.core.domain.entity.SysMenu;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.core.domain.model.LoginBody;
import com.cms.common.core.domain.model.LoginUser;
import com.cms.common.utils.SecurityUtils;
import com.cms.framework.web.service.SysLoginService;
import com.cms.framework.web.service.SysPermissionService;
import com.cms.framework.web.service.TokenService;
import com.cms.system.service.ISysMenuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 登录与验证表现层：
 * 用于处理用户登录、获取用户信息和路由信息
 * 功能描述：
 * 1. 负责用户认证：验证用户身份并返回访问令牌。
 * 2. 提供用户信息查询：返回用户基本信息、角色和权限。
 * 3. 提供菜单路由查询：根据用户权限生成动态菜单。
 *
 * RESTful接口说明：
 * - POST /login: 用户登录验证接口。
 * - GET /getInfo: 获取当前登录用户信息。
 * - GET /getRouters: 根据用户权限获取路由信息。
 *
 * 设计目标：
 * 确保系统用户能够通过登录认证并正确加载其权限和菜单信息。
 * @author quoteZZZ
 */
@Api(tags = "用户登录与权限管理")
@RestController
public class SysLoginController {

    //注入登录服务
    @Autowired
    private SysLoginService loginService;

    //注入菜单服务
    @Autowired
    private ISysMenuService menuService;

    //注入权限服务
    @Autowired
    private SysPermissionService permissionService;

    //注入令牌服务
    @Autowired
    private TokenService tokenService;

    /**
     * 用户登录接口
     *
     * @param loginBody 登录信息（包含用户名、密码、验证码和唯一标识）。
     * @return AjaxResult 包含生成的访问令牌。
     */
    @ApiOperation(value = "用户登录", notes = "用户输入用户名、密码和验证码登录系统")
    @PostMapping("/login")
    public AjaxResult login(
            @ApiParam(value = "登录信息", required = true) @RequestBody LoginBody loginBody
    ) {
        // 初始化一个成功的响应结果对象（AjaxResult对象）
        AjaxResult ajax = AjaxResult.success();
        // 调用登录服务serivce，完成用户身份验证并生成访问令牌
        String token = loginService.login(
                loginBody.getUsername(),  // 用户名
                loginBody.getPassword(), // 用户密码
                loginBody.getCode(),     // 输入的验证码
                loginBody.getUuid()      // 验证码的唯一标识（用于在Redis中查找）
        );
        // 将生成的令牌存入响应结果(AjaxResult对象)中
        ajax.put(Constants.TOKEN, token);
        // 返回包含令牌的响应结果(AjxsResult对象)
        return ajax;
    }

    /**
     * 获取用户信息接口
     * @return AjaxResult 包含用户基本信息、角色和权限集合。
     */
    @ApiOperation(value = "获取用户信息", notes = "返回当前登录用户的基本信息、角色和权限集合")
    @GetMapping("/getInfo")
    public AjaxResult getInfo() {
        // 从安全上下文中获取当前登录用户的详细信息
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser(); // 获取当前登录用户的基本信息
        // 查询用户的角色集合（例如：管理员、普通用户等），调用权限服务类获取角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 查询用户的权限集合（例如：某些具体操作权限）
        Set<String> permissions = permissionService.getMenuPermission(user);
        // 如果用户权限发生了变化，则更新权限并刷新令牌
        if (!loginUser.getPermissions().equals(permissions)) {
            loginUser.setPermissions(permissions); // 更新用户权限
            tokenService.refreshToken(loginUser); // 刷新用户的令牌信息
        }
        // 初始化一个成功的响应结果对象
        AjaxResult ajax = AjaxResult.success();
        // 将用户基本信息、角色和权限加入响应结果中
        ajax.put("user", user);// 用户基本信息
        ajax.put("roles", roles);// 用户角色集合
        ajax.put("permissions", permissions);// 用户权限集合
        // 返回包含用户信息的响应结果
        return ajax;
    }

    /**
     * 获取动态路由信息接口
     * @return AjaxResult 包含用户权限对应的路由信息。
     */
    @ApiOperation(value = "获取路由信息", notes = "返回当前用户根据权限生成的动态菜单路由信息")
    @GetMapping("/getRouters")
    public AjaxResult getRouters() {
        // 获取当前登录用户的ID
        Long userId = SecurityUtils.getUserId();
        // 根据用户ID查询用户的所有菜单树（该用户有权访问的菜单），以树状结构返回
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        // 调用菜单服务构建前端需要的路由信息格式
        return AjaxResult.success(menuService.buildMenus(menus));
    }
}
