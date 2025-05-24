package com.cms.framework.web.service;

import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.core.domain.model.LoginUser;
import com.cms.common.enums.UserStatus;
import com.cms.common.exception.ServiceException;
import com.cms.common.utils.MessageUtils;
import com.cms.common.utils.StringUtils;
import com.cms.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 实现用户认证的核心逻辑服务类：
 * 用于验证用户名密码
 * 实现UserDetailsService自定义登录逻辑，用于认证用户
 * @author quoteZZZ
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService// UserDetailsService接口
{
    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    // 依赖注入：用户服务接口，用于查询用户信息
    @Autowired
    private ISysUserService userService;

    // 依赖注入：密码服务接口，用于验证用户密码
    @Autowired
    private SysPasswordService passwordService;

    // 依赖注入：权限服务接口，用于获取用户的菜单权限
    @Autowired
    private SysPermissionService permissionService;

    /**
     * 根据用户名加载用户详细信息
     * @param username 用户名
     * @return UserDetails 用户详细信息
     * @throws UsernameNotFoundException 如果用户不存在
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        // 根据用户名查询用户信息
        SysUser user = userService.selectUserByUserName(username);
        // 检查用户是否存在
        if (StringUtils.isNull(user))
        {
            // 用户不存在，记录日志并抛出异常提示
            log.info("登录用户：{} 不存在.", username);
            // 抛出用户不存在异常（i18N文件）
            throw new ServiceException(MessageUtils.message("user.not.exists")); // 抛出用户不存在异常
        }
        // 检查用户是否已被删除（逻辑删除）
        else if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            // 用户已被删除，记录日志并抛出异常提示
            log.info("登录用户：{} 已被删除.", username);
            throw new ServiceException(MessageUtils.message("user.password.delete")); // 抛出用户已删除异常
        }
        // 检查用户是否已被停用
        else if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            // 用户已被停用，记录日志并抛出异常提示
            log.info("登录用户：{} 已被停用.", username);
            throw new ServiceException(MessageUtils.message("user.blocked")); // 抛出用户已停用异常
        }
        // 验证用户密码是否正确
        passwordService.validate(user);
        // 创建并返回包含用户权限的 LoginUser 登录用户对象
        return createLoginUser(user);
    }

    /**
     * 创建登录用户对象
     * @param user 用户实体
     * @return UserDetails 登录用户对象
     */
    public UserDetails createLoginUser(SysUser user)
    {
        // 根据提供的SysUser对象创建LoginUser对象（包含用户ID,部门ID，用户信息，和用户权限信息）
        return new LoginUser(user.getUserId(), user.getDeptId(), user, permissionService.getMenuPermission(user));
    }
}
