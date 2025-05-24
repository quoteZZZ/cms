package com.cms.framework.web.service;

import com.cms.common.constant.Constants;
import com.cms.common.core.domain.entity.SysRole;
import com.cms.common.core.domain.model.LoginUser;
import com.cms.common.utils.SecurityUtils;
import com.cms.common.utils.StringUtils;
import com.cms.framework.security.context.PermissionContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Set;

/**
 * 自定义权限验证服务类：（是权限校验的“逻辑执行层”，利用已加载的权限数据完成权限校验。）（ss取自SpringSecurity首字母）
 * 提供了验证用户权限和角色的功能，包括检查单个或多个权限/角色的拥有情况，并支持与 Spring Security 集成。
 * @author quoteZZZ
 */
@Service("ss")
public class PermissionService {

    /**
     * 验证用户是否具备某权限
     * @param permission 权限字符串
     * @return 用户是否具备某权限
     */
    public boolean hasPermi(String permission) {
        // 如果权限字符串为空，则直接返回 false
        if (StringUtils.isEmpty(permission)) {
            return false;
        }
        // 获取当前登录用户信息（）
        LoginUser loginUser = SecurityUtils.getLoginUser();
        // 如果用户信息为空或权限集合为空，则返回false
        if (StringUtils.isNull(loginUser) || CollectionUtils.isEmpty(loginUser.getPermissions())) {
            return false;
        }
        // 将权限信息设置到当前权限上下文，用于后续操作
        PermissionContextHolder.setContext(permission);
        // 判断用户权限集合中是否包含指定的权限
        return hasPermissions(loginUser.getPermissions(), permission);
    }

    /**
     * 验证用户是否不具备某权限，与 hasPermi 逻辑相反
     * @param permission 权限字符串
     * @return 用户是否不具备某权限
     */
    public boolean lacksPermi(String permission) {
        // 如果 hasPermi 返回 true，则说明用户具备权限，此时返回 false
        return hasPermi(permission) != true;
    }

    /**
     * 验证用户是否具有以下任意一个权限
     * @param permissions 以 PERMISSION_DELIMETER 为分隔符的权限列表
     * @return 用户是否具有以下任意一个权限
     */
    public boolean hasAnyPermi(String permissions) {
        // 如果权限列表为空，则直接返回 false
        if (StringUtils.isEmpty(permissions)) {
            return false;
        }
        // 获取当前登录用户信息
        LoginUser loginUser = SecurityUtils.getLoginUser();
        // 如果用户信息为空或权限集合为空，则返回 false
        if (StringUtils.isNull(loginUser) || CollectionUtils.isEmpty(loginUser.getPermissions())) {
            return false;
        }
        // 设置当前权限上下文
        PermissionContextHolder.setContext(permissions);
        // 获取用户权限集合
        Set<String> authorities = loginUser.getPermissions();
        // 遍历权限列表，判断用户是否拥有其中任意一个权限
        for (String permission : permissions.split(Constants.PERMISSION_DELIMETER)) {
            if (permission != null && hasPermissions(authorities, permission)) {
                return true;
            }
        }
        return false; // 如果遍历完权限列表后，用户仍然没有拥有任意一个权限，则返回 false
    }

    /**
     * 判断用户是否拥有某个角色
     * @param role 角色字符串
     * @return 用户是否具备某角色
     */
    public boolean hasRole(String role) {
        // 如果角色字符串为空，则返回 false
        if (StringUtils.isEmpty(role)) {
            return false;
        }
        // 获取当前登录用户信息
        LoginUser loginUser = SecurityUtils.getLoginUser();
        // 如果用户信息为空或角色集合为空，则返回 false
        if (StringUtils.isNull(loginUser) || CollectionUtils.isEmpty(loginUser.getUser().getRoles())) {
            return false;
        }
        // 遍历用户角色集合，判断是否包含指定角色
        for (SysRole sysRole : loginUser.getUser().getRoles()) {
            String roleKey = sysRole.getRoleKey();
            // 如果是超级管理员角色或与指定角色匹配，则返回 true
            if (Constants.SUPER_ADMIN.equals(roleKey) || roleKey.equals(StringUtils.trim(role))) {
                return true;
            }
        }
        return false; // 如果遍历完用户角色集合后，仍然没有找到匹配的角色，则返回 false
    }

    /**
     * 验证用户是否不具备某角色，与 hasRole 逻辑相反
     * @param role 角色名称
     * @return 用户是否不具备某角色
     */
    public boolean lacksRole(String role) {
        // 如果 hasRole 返回 true，则说明用户具备角色，此时返回 false
        return hasRole(role) != true;
    }

    /**
     * 验证用户是否具有以下任意一个角色
     * @param roles 以 ROLE_NAMES_DELIMETER 为分隔符的角色列表
     * @return 用户是否具有以下任意一个角色
     */
    public boolean hasAnyRoles(String roles) {
        // 如果角色列表为空，则返回 false
        if (StringUtils.isEmpty(roles)) {
            return false;
        }
        // 获取当前登录用户信息
        LoginUser loginUser = SecurityUtils.getLoginUser();
        // 如果用户信息为空或角色集合为空，则返回 false
        if (StringUtils.isNull(loginUser) || CollectionUtils.isEmpty(loginUser.getUser().getRoles())) {
            return false;
        }
        // 遍历角色列表，判断用户是否具备其中任意一个角色
        for (String role : roles.split(Constants.ROLE_DELIMETER)) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否包含权限
     * @param permissions 权限列表
     * @param permission 权限字符串
     * @return 用户是否具备某权限
     */
    private boolean hasPermissions(Set<String> permissions, String permission) {
        // 判断权限集合中是否包含指定权限或拥有所有权限的标志
        return permissions.contains(Constants.ALL_PERMISSION) || permissions.contains(StringUtils.trim(permission));
    }
}

