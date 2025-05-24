package com.cms.framework.aspectj;

import com.cms.common.annotation.DataScope;
import com.cms.common.constant.UserConstants;
import com.cms.common.core.domain.BaseEntity;
import com.cms.common.core.domain.entity.SysRole;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.core.domain.model.LoginUser;
import com.cms.common.utils.SecurityUtils;
import com.cms.common.utils.StringUtils;
import com.cms.common.utils.text.Convert;
import com.cms.framework.security.context.PermissionContextHolder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限过滤服务切面类：（自定义注解DataScope）
 * 通过AOP切面技术，在方法执行前根据用户的角色和权限动态生成SQL查询条件，
 * 实现数据范围的过滤，确保用户只能访问其有权限的数据。（数据权限过滤）
 * @author quoteZZZ
 */
@Aspect
@Component
public class DataScopeAspect
{
    /**
     * 全部数据权限
     */
    public static final String DATA_SCOPE_ALL = "1";

    /**
     * 自定数据权限
     */
    public static final String DATA_SCOPE_CUSTOM = "2";

    /**
     * 部门数据权限
     */
    public static final String DATA_SCOPE_DEPT = "3";

    /**
     * 部门及以下数据权限
     */
    public static final String DATA_SCOPE_DEPT_AND_CHILD = "4";

    /**
     * 仅本人数据权限
     */
    public static final String DATA_SCOPE_SELF = "5";

    /**
     * 数据权限过滤关键字
     */
    public static final String DATA_SCOPE = "dataScope";

    //前置通知：在方法执行前执行
    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) throws Throwable
    {
        //清理数据范围（权限）过滤条件（param.dataScope)防止sql注入
        clearDataScope(point);
        //设置数据范围（权限）过滤条件
        handleDataScope(point, controllerDataScope);
    }

    //处理数据权限
    protected void handleDataScope(final JoinPoint joinPoint, DataScope controllerDataScope)
    {
        // 获取当前的用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        // 如果登录用户不为空
        if (StringUtils.isNotNull(loginUser))
        {
            SysUser currentUser = loginUser.getUser();//获取当前用户信息
            // 如果是超级管理员，则不过滤数据
            if (StringUtils.isNotNull(currentUser) && !currentUser.isAdmin())
            {
                // 获取目标方法的权限字符串，列如，用户列表的权限字符串为：sys_user:list
                String permission = StringUtils.defaultIfEmpty(controllerDataScope.permission(), PermissionContextHolder.getContext());
                //设置数据范围（权限）过滤条件，根据当前用户，部门别名、用户别名、权限标识符对切点对象进行过滤
                dataScopeFilter(joinPoint, currentUser, controllerDataScope.deptAlias(), controllerDataScope.userAlias(), permission);
            }
        }
    }

    /**
     * 数据范围过滤
     * @param joinPoint 切点
     * @param user 用户
     * @param deptAlias 部门别名
     * @param userAlias 用户别名
     * @param permission 权限字符
     */
    public static void dataScopeFilter(JoinPoint joinPoint, SysUser user, String deptAlias, String userAlias, String permission)
    {
        //构建SQL字符串，用于拼接数据范围过滤条件
        StringBuilder sqlString = new StringBuilder();
        //用于存储已经添加过的数据范围类型（1~5），避免重复添加
        List<String> conditions = new ArrayList<String>();
        List<String> scopeCustomIds = new ArrayList<String>();
        //遍历用户角色集合，获取用户具有自定义权限的角色，并添加到scopeCustomIds集合中
        user.getRoles().forEach(role -> {
            if (DATA_SCOPE_CUSTOM.equals(role.getDataScope()) && StringUtils.equals(role.getStatus(), UserConstants.ROLE_NORMAL) && StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission)))
            {
                scopeCustomIds.add(Convert.toStr(role.getRoleId()));
            }
        });
        //遍历用户角色集合，获取用户具有权限的数据范围类型，并添加到conditions集合中
        for (SysRole role : user.getRoles())
        {
            //获取当前角色的数据范围类型1~5
            String dataScope = role.getDataScope();
            //如果数据范围类型已经存在，则跳过循环
            if (conditions.contains(dataScope) || StringUtils.equals(role.getStatus(), UserConstants.ROLE_DISABLE))
            {
                continue;
            }
            //如果当前角色没有权限，则跳过循环
            if (!StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission)))
            {
                continue;
            }
            //如果数据范围类型为全部，则清空SQL字符串并添加数据范围类型，跳过整个循环
            if (DATA_SCOPE_ALL.equals(dataScope))
            {
                sqlString = new StringBuilder();
                conditions.add(dataScope);
                break;
            }
            //如果数据范围类型为自定义，则拼接自定义数据权限的SQL字符串
            else if (DATA_SCOPE_CUSTOM.equals(dataScope))
            {
                if (scopeCustomIds.size() > 1)
                {
                    // 多个自定数据权限使用in查询，避免多次拼接。
                    sqlString.append(StringUtils.format(" OR {}.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id in ({}) ) ", deptAlias, String.join(",", scopeCustomIds)));
                }
                else
                {   // 单个自定数据权限使用=查询，避免多次拼接。
                    sqlString.append(StringUtils.format(" OR {}.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ", deptAlias, role.getRoleId()));
                }
            }
            //如果数据范围类型为部门及以下，则拼接部门及以下数据权限的SQL字符串
            else if (DATA_SCOPE_DEPT.equals(dataScope))
            {
                sqlString.append(StringUtils.format(" OR {}.dept_id = {} ", deptAlias, user.getDeptId()));
            }
            //如果数据范围类型为部门，则拼接部门数据权限的SQL字符串
            else if (DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope))
            {
                sqlString.append(StringUtils.format(" OR {}.dept_id IN ( SELECT dept_id FROM sys_dept WHERE dept_id = {} or find_in_set( {} , ancestors ) )", deptAlias, user.getDeptId(), user.getDeptId()));
            }
            //如果数据范围类型为仅本人，则拼接仅本人数据权限的SQL字符串
            else if (DATA_SCOPE_SELF.equals(dataScope))
            {
                // 如果userAlias不为空，则拼接userAlias别名
                if (StringUtils.isNotBlank(userAlias))
                {
                    sqlString.append(StringUtils.format(" OR {}.user_id = {} ", userAlias, user.getUserId()));
                }
                else// userAlias为空，则拼接deptAlias别名
                {
                    // 数据权限为仅本人且没有userAlias别名不查询任何数据
                    sqlString.append(StringUtils.format(" OR {}.dept_id = 0 ", deptAlias));
                }
            }
            conditions.add(dataScope);// 添加数据范围类型到conditions集合中
        }
        // 角色都不包含传递过来的权限字符，这个时候sqlString也会为空，所以要限制一下,不查询任何数据
        if (StringUtils.isEmpty(conditions))
        {
            sqlString.append(StringUtils.format(" OR {}.dept_id = 0 ", deptAlias));
        }
        // 将拼接好的数据权限SQL字符串添加到params参数中
        if (StringUtils.isNotBlank(sqlString.toString()))
        {
            Object params = joinPoint.getArgs()[0];
            if (StringUtils.isNotNull(params) && params instanceof BaseEntity)
            {
                BaseEntity baseEntity = (BaseEntity) params;
                baseEntity.getParams().put(DATA_SCOPE, " AND (" + sqlString.substring(4) + ")");
            }
        }
    }

    /**
     * 拼接权限sql前先清空params.dataScope参数防止注入
     */
    private void clearDataScope(final JoinPoint joinPoint)
    {
        //获取切点方法的第一个参数（就是查询方法的第一个参数，例如用户）
        Object params = joinPoint.getArgs()[0];
        //检查参数不为空且参数类型为BaseEntity
        if (StringUtils.isNotNull(params) && params instanceof BaseEntity)
        {
            //将参数转为BaseEntity类型
            BaseEntity baseEntity = (BaseEntity) params;
            //将数据权限过滤条件设置为空（dataScope）
            baseEntity.getParams().put(DATA_SCOPE, "");
        }
    }
}
