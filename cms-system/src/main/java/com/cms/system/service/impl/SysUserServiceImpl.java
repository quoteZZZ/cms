package com.cms.system.service.impl;

import com.cms.common.annotation.DataScope;
import com.cms.common.constant.UserConstants;
import com.cms.common.core.domain.entity.SysRole;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.exception.ServiceException;
import com.cms.common.utils.SecurityUtils;
import com.cms.common.utils.StringUtils;
import com.cms.common.utils.bean.BeanValidators;
import com.cms.common.utils.spring.SpringUtils;
import com.cms.common.utils.uuid.IdGenerator;
import com.cms.common.core.domain.entity.SysPost;
import com.cms.common.core.domain.entity.SysUserPost;
import com.cms.common.core.domain.entity.SysUserRole;
import com.cms.system.mapper.*;
import com.cms.system.service.ISysConfigService;
import com.cms.system.service.ISysDeptService;
import com.cms.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理业务层实现类：
 * 用户模块：增删改查、导入导出、重置密码、状态修改、数据范围校验、根据用户名称查询用户、根据用户ID查询用户、根据用户ID查询用户所属角色组、根据用户ID查询用户所属岗位组、校验用户名称是否唯一、校验手机号码是否唯一、校验email是否唯一、根据用户ID查询用户详细信息、根据用户ID查询用户所属角色组、根据用户ID查询用户
 * @author quoteZZZ
 */
@Service
public class SysUserServiceImpl implements ISysUserService
{
    // 日志对象
    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    @Autowired
    private SysUserMapper userMapper;// 用户数据层

    @Autowired
    private SysRoleMapper roleMapper;// 角色数据层

    @Autowired
    private SysPostMapper postMapper;// 岗位数据层

    @Autowired
    private SysUserRoleMapper userRoleMapper;// 用户和角色关联数据层

    @Autowired
    private SysUserPostMapper userPostMapper;// 用户和岗位关联数据层

    @Autowired
    private ISysConfigService configService;// 系统配置服务

    @Autowired
    private ISysDeptService deptService;// 部门服务

    @Autowired
    protected Validator validator;// 校验器

    /**
     * 根据条件分页查询用户列表
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")// 数据权限注解（d表示限定部门查询，u表示限定用户查询)
    public List<SysUser> selectUserList(SysUser user)
    {
        return userMapper.selectUserList(user);
    }

    /**
     * 根据条件分页查询已分配用户角色列表
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")// 数据权限注解（d表示限定部门查询，u表示限定用户查询)
    public List<SysUser> selectAllocatedList(SysUser user)
    {
        return userMapper.selectAllocatedList(user);
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")// 数据权限注解（d表示限定部门查询，u表示限定用户查询)
    public List<SysUser> selectUnallocatedList(SysUser user)
    {
        return userMapper.selectUnallocatedList(user);
    }

    /**
     * 根据条件分页查询已分配评委竞赛列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")// 数据权限注解（d表示限定部门查询，u表示限定用户查询)
    public List<SysUser> selectAllocatedJudgeList(SysUser user)
    {
        return userMapper.selectAllocatedJudgeList(user);
    }

    /**
     * 根据条件分页查询未分配评委竞赛列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")// 数据权限注解（d表示限定部门查询，u表示限定用户查询)
    public List<SysUser> selectUnallocatedJudgeList(SysUser user)
    {
        return userMapper.selectUnallocatedJudgeList(user);
    }



    /**
     * 通过用户名查询用户
     */
    @Override
    public SysUser selectUserByUserName(String userName) {
        if (StringUtils.isEmpty(userName)) {
            log.warn("用户名为空，无法查询用户信息");
            return null;
        }
        SysUser user = userMapper.selectUserByUserName(userName);
        if (user == null) {
            log.warn("用户名={} 的用户信息不存在", userName);
        }
        return user;
    }

    /**
     * 通过用户ID查询用户
     */
    @Override
    public SysUser selectUserById(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("用户ID为空或无效，无法查询用户信息");
            return null;
        }
        SysUser user = userMapper.selectUserById(userId);
        if (user == null) {
            log.warn("用户ID={} 的用户信息不存在", userId);
        }
        return user;
    }

    /**
     * 查询用户所属角色组
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(String userName) {
        if (StringUtils.isEmpty(userName)) {
            log.warn("用户名为空，无法查询用户角色组");
            return StringUtils.EMPTY;
        }
        List<SysRole> list = roleMapper.selectRolesByUserName(userName);
        if (CollectionUtils.isEmpty(list)) {
            log.info("用户 {} 没有分配任何角色", userName);
            return StringUtils.EMPTY; // 返回空字符串而不是 null
        }
        return list.stream()
                .map(SysRole::getRoleName)
                .collect(Collectors.joining(","));
    }

    /**
     * 查询用户所属岗位组
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserPostGroup(String userName) {
        if (StringUtils.isEmpty(userName)) {
            log.warn("用户名为空，无法查询用户岗位组");
            return StringUtils.EMPTY;
        }
        List<SysPost> list = postMapper.selectPostsByUserName(userName);
        if (CollectionUtils.isEmpty(list)) {
            log.info("用户 {} 没有分配任何岗位", userName);
            return StringUtils.EMPTY; // 返回空字符串而不是 null
        }
        return list.stream()
                .map(SysPost::getPostName)
                .collect(Collectors.joining(","));
    }

    /**
     * 校验用户名称是否唯一
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkUserNameUnique(user.getUserName());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验手机号码是否唯一
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkPhoneUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkPhoneUnique(user.getPhonenumber());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验email是否唯一
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkEmailUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkEmailUnique(user.getEmail());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验用户是否允许操作
     * @param user 用户信息
     */
    @Override
    public void checkUserAllowed(SysUser user)
    {
        if (StringUtils.isNotNull(user.getUserId()) && user.isAdmin())
        {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验用户是否有数据权限
     */
    @Override
    public void checkUserDataScope(Long userId) {
        if (!SysUser.isAdmin(SecurityUtils.getUserId())) {
            SysUser user = new SysUser();
            user.setUserId(userId);
            List<SysUser> users = SpringUtils.getAopProxy(this).selectUserList(user);
            if (StringUtils.isEmpty(users)) {
                log.error("没有权限访问用户数据！");
                //throw new ServiceException("没有权限访问用户数据！");
            }
        }
    }

    /**
     * 新增保存用户信息
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertUser(SysUser user)
    {
        //获得唯一id,设置到对象中
        user.setUserId(IdGenerator.generateId(0));
        // 新增用户信息
        int rows = userMapper.insertUser(user);
        // 新增用户岗位关联
        insertUserPost(user);
        // 新增用户与角色管理
        insertUserRole(user);
        return rows;
    }

    /**
     * 注册用户信息，默认角色为参赛者:4L
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean registerUser(SysUser user)
    {
        //获得唯一id,设置到对象中
        user.setUserId(IdGenerator.generateId(0));
        Boolean total = userMapper.insertUser(user) > 0; //这个是插入到用户表中
        //插入到角色表中，然后注意一下这个new Long[]{4L}，4L代表的含义是2是xx角色的id，L是Long长整型，这个样子是写死的，所有注册的权限是参赛者角色权限。
        insertUserRole(userMapper.selectUserByUserName(user.getUserName()).getUserId(),new Long[]{4L});
        return total;
        //return userMapper.insertUser(user) > 0; 原本存在的，可以删掉可以注释掉
    }


    /**
     * 修改保存用户信息
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateUser(SysUser user)
    {
        Long userId = user.getUserId();
        // 删除用户与角色关联
        userRoleMapper.deleteUserRoleByUserId(userId);
        // 新增用户与角色管理
        insertUserRole(user);
        // 删除用户与岗位关联
        userPostMapper.deleteUserPostByUserId(userId);
        // 新增用户与岗位管理
        insertUserPost(user);
        return userMapper.updateUser(user);
    }

    /**
     * 用户授权角色
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional
    public void insertUserAuth(Long userId, Long[] roleIds)
    {
        userRoleMapper.deleteUserRoleByUserId(userId);
        insertUserRole(userId, roleIds);
    }

    /**
     * 修改用户状态
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserStatus(SysUser user)
    {
        return userMapper.updateUser(user);
    }

    /**
     * 修改用户基本信息
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserProfile(SysUser user)
    {
        return userMapper.updateUser(user);
    }

    /**
     * 修改用户头像
     * @param userName 用户名
     * @param avatar 头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(String userName, String avatar)
    {
        return userMapper.updateUserAvatar(userName, avatar) > 0;
    }

    /**
     * 重置用户密码
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int resetPwd(SysUser user)
    {
        return userMapper.updateUser(user);
    }

    /**
     * 重置用户密码
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(String userName, String password)
    {
        return userMapper.resetUserPwd(userName, password);
    }

    /**
     * 新增用户角色信息
     * @param user 用户对象
     */
    public void insertUserRole(SysUser user)
    {
        this.insertUserRole(user.getUserId(), user.getRoleIds());
    }

    /**
     * 新增用户岗位信息
     * @param user 用户对象
     */
    public void insertUserPost(SysUser user)
    {
        Long[] posts = user.getPostIds();
        if (StringUtils.isNotEmpty(posts))
        {
            // 新增用户与岗位管理
            List<SysUserPost> list = new ArrayList<SysUserPost>(posts.length);
            for (Long postId : posts)
            {
                SysUserPost up = new SysUserPost();
                up.setUserId(user.getUserId());
                up.setPostId(postId);
                list.add(up);
            }
            userPostMapper.batchUserPost(list);
        }
    }

    /**
     * 新增用户角色信息
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    public void insertUserRole(Long userId, Long[] roleIds)
    {
        if (StringUtils.isNotEmpty(roleIds))
        {
            // 新增用户与角色管理
            List<SysUserRole> list = new ArrayList<SysUserRole>(roleIds.length);
            for (Long roleId : roleIds)
            {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            userRoleMapper.batchUserRole(list);
        }
    }

    /**
     * 通过用户ID删除用户
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteUserById(Long userId)
    {
        // 删除用户与角色关联
        userRoleMapper.deleteUserRoleByUserId(userId);
        // 删除用户与岗位表
        userPostMapper.deleteUserPostByUserId(userId);
        return userMapper.deleteUserById(userId);
    }

    /**
     * 批量删除用户信息
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteUserByIds(Long[] userIds)
    {
        for (Long userId : userIds)
        {
            checkUserAllowed(new SysUser(userId));
            checkUserDataScope(userId);
        }
        // 删除用户与角色关联
        userRoleMapper.deleteUserRole(userIds);
        // 删除用户与岗位关联
        userPostMapper.deleteUserPost(userIds);
        return userMapper.deleteUserByIds(userIds);
    }

    /**
     * 导入用户数据
     * @param userList 用户数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isNull(userList) || userList.size() == 0)
        {
            throw new ServiceException("导入用户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (SysUser user : userList)
        {
            try
            {
                // 验证是否存在这个用户
                SysUser u = userMapper.selectUserByUserName(user.getUserName());
                if (StringUtils.isNull(u))
                {
                    BeanValidators.validateWithException(validator, user);
                    deptService.checkDeptDataScope(user.getDeptId());
                    String password = configService.selectConfigByKey("sys.user.initPassword");
                    user.setPassword(SecurityUtils.encryptPassword(password));
                    user.setCreateBy(operName);
                    userMapper.insertUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    BeanValidators.validateWithException(validator, user);
                    checkUserAllowed(u);
                    checkUserDataScope(u.getUserId());
                    deptService.checkDeptDataScope(user.getDeptId());
                    user.setUserId(u.getUserId());
                    user.setUpdateBy(operName);
                    userMapper.updateUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、账号 " + user.getUserName() + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    /**
     * 获取所有评委用户列表（角色ID为3的用户）
     * @return 评委用户集合
     */
    @Override
    public List<SysUser> selectJudges() {
        // 获取所有用户（根据业务需求可优化为分页查询）
        List<SysUser> allUsers = this.selectUserList(new SysUser());
        return allUsers.stream()
                .filter(user -> isJudge(user.getUserId())) // 调用 isJudge 方法过滤
                .collect(Collectors.toList());
    }

    /**
     * 判断用户是否为评委角色
     * @param userId 用户ID
     * @return true-是评委，false-不是评委
     * @description 通过角色ID=3判断（需确保数据库中评委角色ID为3）
     */
    public boolean isJudge(Long userId) {
        return userMapper.isJudge(userId) > 0; // 存在角色 ID = 3，则返回 true
    }


}
