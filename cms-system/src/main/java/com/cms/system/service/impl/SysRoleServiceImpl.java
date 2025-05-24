package com.cms.system.service.impl;

import com.cms.common.annotation.DataScope;
import com.cms.common.constant.UserConstants;
import com.cms.common.core.domain.entity.SysRole;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.exception.ServiceException;
import com.cms.common.utils.SecurityUtils;
import com.cms.common.utils.StringUtils;
import com.cms.common.utils.spring.SpringUtils;
import com.cms.common.core.domain.entity.SysRoleDept;
import com.cms.common.core.domain.entity.SysRoleMenu;
import com.cms.common.core.domain.entity.SysUserRole;
import com.cms.system.mapper.SysRoleDeptMapper;
import com.cms.system.mapper.SysRoleMapper;
import com.cms.system.mapper.SysRoleMenuMapper;
import com.cms.system.mapper.SysUserRoleMapper;
import com.cms.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 角色管理业务层实现类：
 * 角色管理的服务接口，提供了角色的增删改查、权限校验以及角色与用户、菜单、部门之间的关联操作。
 * @author quoteZZZ
 */
@Service
public class SysRoleServiceImpl implements ISysRoleService
{
    @Autowired
    private SysRoleMapper roleMapper;// 角色数据层

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;// 角色和菜单关联数据层

    @Autowired
    private SysUserRoleMapper userRoleMapper;// 用户和角色关联数据层

    @Autowired
    private SysRoleDeptMapper roleDeptMapper;// 角色和部门关联数据层
//
    /**
     * 根据条件分页查询角色数据
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    @Override
    @DataScope(deptAlias = "d")// 数据权限注解（"d"表示筛选特定部门)
    public List<SysRole> selectRoleList(SysRole role)
    {
        // 调用Mapper层方法查询角色列表
        return roleMapper.selectRoleList(role);
    }

    /**
     * 根据用户ID查询角色
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRole> selectRolesByUserId(Long userId)
    {
        // 查询用户拥有的角色权限
        List<SysRole> userRoles = roleMapper.selectRolePermissionByUserId(userId);
        // 查询所有角色
        List<SysRole> roles = selectRoleAll();
        // 遍历所有角色，标记用户拥有的角色
        for (SysRole role : roles)
        {
            for (SysRole userRole : userRoles)
            {
                if (role.getRoleId().longValue() == userRole.getRoleId().longValue())
                {
                    role.setFlag(true);
                    break;
                }
            }
        }
        return roles;
    }

    /**
     * 根据用户ID查询权限
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectRolePermissionByUserId(Long userId)
    {
        // 查询用户拥有的角色权限
        List<SysRole> perms = roleMapper.selectRolePermissionByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        // 将权限添加到集合中
        for (SysRole perm : perms)
        {
            if (StringUtils.isNotNull(perm))
            {
                permsSet.addAll(Arrays.asList(perm.getRoleKey().trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 查询所有角色
     * @return 角色列表
     */
    @Override
    public List<SysRole> selectRoleAll()
    {
        // 通过AOP代理调用selectRoleList方法查询所有角色
        return SpringUtils.getAopProxy(this).selectRoleList(new SysRole());
    }

    /**
     * 根据用户ID获取角色选择框列表
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    @Override
    public List<Long> selectRoleListByUserId(Long userId)
    {
        // 查询用户拥有的角色ID列表
        return roleMapper.selectRoleListByUserId(userId);
    }

    /**
     * 通过角色ID查询角色
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    @Override
    public SysRole selectRoleById(Long roleId)
    {
        // 调用Mapper层方法查询角色信息
        return roleMapper.selectRoleById(roleId);
    }

    /**
     * 校验角色名称是否唯一
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleNameUnique(SysRole role)
    {
        // 获取角色ID，如果为空则设为-1
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        // 查询是否存在相同名称的角色
        SysRole info = roleMapper.checkRoleNameUnique(role.getRoleName());
        // 如果存在且不是当前角色，则返回不唯一
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验角色权限是否唯一
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleKeyUnique(SysRole role)
    {
        // 获取角色ID，如果为空则设为-1
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        // 查询是否存在相同权限的角色
        SysRole info = roleMapper.checkRoleKeyUnique(role.getRoleKey());
        // 如果存在且不是当前角色，则返回不唯一
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验角色是否允许操作
     * @param role 角色信息
     */
    @Override
    public void checkRoleAllowed(SysRole role)
    {
        // 如果角色ID不为空且是超级管理员，则抛出异常
        if (StringUtils.isNotNull(role.getRoleId()) && role.isAdmin())
        {
            throw new ServiceException("不允许操作超级管理员角色");
        }
    }

    /**
     * 校验角色是否有数据权限
     * @param roleIds 角色id
     */
    @Override
    public void checkRoleDataScope(Long... roleIds)
    {
        // 如果当前用户不是超级管理员，则检查角色数据权限
        if (!SysUser.isAdmin(SecurityUtils.getUserId()))
        {
            for (Long roleId : roleIds)
            {
                SysRole role = new SysRole();
                role.setRoleId(roleId);
                // 查询角色列表
                List<SysRole> roles = SpringUtils.getAopProxy(this).selectRoleList(role);
                // 如果角色列表为空，则抛出异常
                if (StringUtils.isEmpty(roles))
                {
                    throw new ServiceException("没有权限访问角色数据！");
                }
            }
        }
    }

    /**
     * 通过角色ID查询角色使用数量
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    public int countUserRoleByRoleId(Long roleId)
    {
        // 调用Mapper层方法查询使用该角色的用户数量
        return userRoleMapper.countUserRoleByRoleId(roleId);
    }

    /**
     * 新增保存角色信息
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertRole(SysRole role)
    {
        // 新增角色信息
        roleMapper.insertRole(role);
        // 插入角色菜单信息
        return insertRoleMenu(role);
    }

    /**
     * 修改保存角色信息
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateRole(SysRole role)
    {
        // 修改角色信息
        roleMapper.updateRole(role);
        // 删除角色与菜单关联
        roleMenuMapper.deleteRoleMenuByRoleId(role.getRoleId());
        // 插入新的角色菜单信息
        return insertRoleMenu(role);
    }

    /**
     * 修改角色状态
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public int updateRoleStatus(SysRole role)
    {
        // 更新角色状态
        return roleMapper.updateRole(role);
    }

    /**
     * 修改数据权限信息
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional// 事务注解
    public int authDataScope(SysRole role)
    {
        // 修改角色信息
        roleMapper.updateRole(role);
        // 删除角色与部门关联
        roleDeptMapper.deleteRoleDeptByRoleId(role.getRoleId());
        // 新增角色和部门信息（数据权限）
        return insertRoleDept(role);
    }

    /**
     * 新增角色菜单信息
     * @param role 角色对象
     */
    public int insertRoleMenu(SysRole role)
    {
        int rows = 1;
        // 创建角色菜单列表
        List<SysRoleMenu> list = new ArrayList<SysRoleMenu>();
        for (Long menuId : role.getMenuIds())
        {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(role.getRoleId());
            rm.setMenuId(menuId);
            list.add(rm);
        }
        // 如果列表不为空，则批量插入角色菜单信息
        if (list.size() > 0)
        {
            rows = roleMenuMapper.batchRoleMenu(list);
        }
        return rows;
    }

    /**
     * 新增角色部门信息(数据权限)
     * @param role 角色对象
     */
    public int insertRoleDept(SysRole role)
    {
        int rows = 1;
        // 创建角色部门列表
        List<SysRoleDept> list = new ArrayList<SysRoleDept>();
        for (Long deptId : role.getDeptIds())
        {
            SysRoleDept rd = new SysRoleDept();
            rd.setRoleId(role.getRoleId());
            rd.setDeptId(deptId);
            list.add(rd);
        }
        // 如果列表不为空，则批量插入角色部门信息
        if (list.size() > 0)
        {
            rows = roleDeptMapper.batchRoleDept(list);
        }
        return rows;
    }

    /**
     * 通过角色ID删除角色
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    @Transactional// 事务注解
    public int deleteRoleById(Long roleId)
    {
        // 删除角色与菜单关联
        roleMenuMapper.deleteRoleMenuByRoleId(roleId);
        // 删除角色与部门关联
        roleDeptMapper.deleteRoleDeptByRoleId(roleId);
        // 删除角色信息
        return roleMapper.deleteRoleById(roleId);
    }

    /**
     * 批量删除角色信息
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    @Override
    @Transactional// 事务注解
    public int deleteRoleByIds(Long[] roleIds)
    {
        for (Long roleId : roleIds)
        {
            // 校验角色是否允许操作
            checkRoleAllowed(new SysRole(roleId));
            // 校验角色是否有数据权限
            checkRoleDataScope(roleId);
            // 查询角色信息
            SysRole role = selectRoleById(roleId);
            // 如果角色已分配给用户，则抛出异常
            if (countUserRoleByRoleId(roleId) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", role.getRoleName()));
            }
        }
        // 删除角色与菜单关联
        roleMenuMapper.deleteRoleMenu(roleIds);
        // 删除角色与部门关联
        roleDeptMapper.deleteRoleDept(roleIds);
        // 删除角色信息
        return roleMapper.deleteRoleByIds(roleIds);
    }

    /**
     * 取消授权用户角色
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    @Override
    public int deleteAuthUser(SysUserRole userRole)
    {
        // 删除用户角色关联信息
        return userRoleMapper.deleteUserRoleInfo(userRole);
    }

    /**
     * 批量取消授权用户角色
     * @param roleId 角色ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 结果
     */
    @Override
    public int deleteAuthUsers(Long roleId, Long[] userIds)
    {
        // 批量删除用户角色关联信息
        return userRoleMapper.deleteUserRoleInfos(roleId, userIds);
    }

    /**
     * 批量选择授权用户角色
     * @param roleId 角色ID
     * @param userIds 需要授权的用户数据ID
     * @return 结果
     */
    @Override
    public int insertAuthUsers(Long roleId, Long[] userIds)
    {
        // 创建用户角色列表
        List<SysUserRole> list = new ArrayList<SysUserRole>();
        for (Long userId : userIds)
        {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            list.add(ur);
        }
        // 批量插入用户角色关联信息
        return userRoleMapper.batchUserRole(list);
    }
}
