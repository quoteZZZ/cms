package com.cms.framework.web.service;

import com.cms.common.constant.UserConstants;
import com.cms.common.core.domain.entity.SysRole;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.utils.StringUtils;
import com.cms.system.service.ISysMenuService;
import com.cms.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户权限处理的核心业务层类：（是权限管理的“数据提供层”，负责计算和加载用户权限。）
 * 专门用于计算和获取用户的角色权限和菜单权限，为系统中的权限管理和访问控制提供支撑。通过调用角色服务和菜单服务接口，实现权限的动态加载和分配。
 * 主要用于处理用户的权限信息，提供以下两个核心功能：
 * 获取角色数据权限：根据用户信息判断其是否为管理员，返回相应的角色权限集合。
 * 获取菜单数据权限：根据用户信息判断其是否为管理员，返回相应的菜单权限集合。
 * 若用户不是管理员，则根据其角色列表获取具体的菜单权限。
 * @author quoteZZZ
 */
@Component
public class SysPermissionService
{
    @Autowired
    private ISysRoleService roleService;//角色服务接口

    @Autowired
    private ISysMenuService menuService;//菜单服务接口

    /**
     * 获取角色数据权限
     * @param user 用户信息
     * @return 角色权限信息（角色集合）
     */
    public Set<String> getRolePermission(SysUser user)
    {
        //初始化权限集合（set集合，一个用户可以有多个角色，可以存储重复的角色权限数据）
        Set<String> roles = new HashSet<String>();
        // 判断是否是软件管理员
        if (user.isAdmin())
        {
            roles.add("admin");//将admin角色添加到集合中
        }
        else
        {
            // 获取用户角色权限集合（所有），从用户信息中获取
            roles.addAll(roleService.selectRolePermissionByUserId(user.getUserId()));
        }
        // 返回角色数据权限集合
        return roles;
    }

    /**
     * 获取菜单数据权限
     * @param user 用户信息
     * @return 菜单权限信息
     */
    public Set<String> getMenuPermission(SysUser user)
    {
        //初始化权限集合（set集合，一个用户可以有多个角色，可以存储重复的角色权限数据）
        Set<String> perms = new HashSet<String>();
        // 判断用户是否为软件管理员，软件管理员拥有所有权限
        if (user.isAdmin())
        {
            perms.add("*:*:*");// 将所有权限添加到集合中
        }
        else //不是管理员，则获取用户权限
        {
            // 获取用户角色集合（所有），从用户信息中获取
            List<SysRole> roles = user.getRoles();
            //// 判断用户角色集合是否为空（如果角色列表不为空）
            if (!CollectionUtils.isEmpty(roles))
            {
                // 遍历角色列表，为每个角色设置权限，并添加到总的权限集合中
                for (SysRole role : roles)
                {
                    // 判断角色是否为正常状态
                    if (StringUtils.equals(role.getStatus(), UserConstants.ROLE_NORMAL))
                    {
                        // 根据角色Id，获取角色权限集合
                        Set<String> rolePerms = menuService.selectMenuPermsByRoleId(role.getRoleId());
                        // 将角色权限集合添加到角色对象中，以便于数据权限匹配使用
                        role.setPermissions(rolePerms);
                        // 将角色权限集合添加到总的权限集合中
                        perms.addAll(rolePerms);
                    }
                }
            }
            else
            {
                // 如果用户没有角色，则直接获取用户的权限（跟上面一样，只不过用户和权限没直接相连，会先查角色，再查权限），因为用户可能没用角色集合
                perms.addAll(menuService.selectMenuPermsByUserId(user.getUserId()));
            }
        }
        // 返回菜单权限信息集合
        return perms;
    }
}
