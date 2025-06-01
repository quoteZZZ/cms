package com.cms.system.service.impl;

import com.cms.common.core.domain.entity.SysComp;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.core.domain.entity.SysUserComp;
import com.cms.common.exception.ServiceException;
import com.cms.common.utils.DateUtils;
import com.cms.system.mapper.SysUserCompMapper;
import com.cms.system.service.ISysCompService;
import com.cms.system.service.ISysUserCompService;
import com.cms.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户与竞赛关联Service实现
 *
 * @author quoteZZZ
 * @date 2025-05-31
 */
@Service
public class SysUserCompServiceImpl implements ISysUserCompService {
    private static final Logger log = LoggerFactory.getLogger(SysUserCompServiceImpl.class);

    @Autowired
    private SysUserCompMapper userCompMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysCompService compService;

    /**
     * 用户报名参加竞赛
     *
     * @param userId 用户ID
     * @param compId 竞赛ID
     * @return 结果
     */
    @Override
    @Transactional
    public int joinCompetition(Long userId, Long compId) {
        // 验证用户和竞赛是否存在
        SysUser user = userService.selectUserById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        SysComp comp = compService.selectSysCompByCompId(compId);
        if (comp == null) {
            throw new ServiceException("竞赛不存在");
        }

        // 检查竞赛状态
        if (comp.getCompStatus() != '0' && comp.getCompStatus() != '1') {
            throw new ServiceException("竞赛已结束或不允许报名");
        }

        // 检查是否已报名
        List<Long> userCompIds = selectUserCompetitions(userId);
        if (userCompIds.contains(compId)) {
            throw new ServiceException("已报名该竞赛，请勿重复操作");
        }

        // 创建用户竞赛关联
        SysUserComp userComp = new SysUserComp();
        userComp.setUserId(userId);
        userComp.setCompId(compId);
        userComp.setCreateTime(DateUtils.getNowDate());

        // 更新用户部门为竞赛部门（如果存在）
        if (comp.getDeptId() != null) {
            // 保存用户原始部门ID，可考虑添加到SysUserComp实体类中
            Long originalDeptId = user.getDeptId();

            user.setDeptId(comp.getDeptId());
            userService.updateUser(user);

            log.info("用户[{}]报名参加竞赛[{}]，部门从[{}]更新为[{}]",
                    userId, compId, originalDeptId, comp.getDeptId());
        }

        // 插入关联记录
        List<SysUserComp> list = List.of(userComp);
        return userCompMapper.batchUserComp(list);
    }

    /**
     * 用户退出竞赛
     *
     * @param userId 用户ID
     * @param compId 竞赛ID
     * @return 结果
     */
    @Override
    @Transactional
    public int exitCompetition(Long userId, Long compId) {
        // 验证用户和竞赛是否存在
        SysUser user = userService.selectUserById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        SysComp comp = compService.selectSysCompByCompId(compId);
        if (comp == null) {
            throw new ServiceException("竞赛不存在");
        }

        // 检查是否已报名
        List<Long> userCompIds = selectUserCompetitions(userId);
        if (!userCompIds.contains(compId)) {
            throw new ServiceException("未报名该竞赛，无法退出");
        }

        // 检查竞赛状态是否允许退出
        if (comp.getCompStatus() == '2') {
            throw new ServiceException("竞赛已结束，无法退出");
        }

        // 更新用户部门（重置为默认部门或其他适当部门）
        if (user.getDeptId() != null && comp.getDeptId() != null &&
            user.getDeptId().equals(comp.getDeptId())) {
            // 重置为默认部门（此处设为100，实际应根据业务逻辑修改）
            user.setDeptId(100L);
            userService.updateUser(user);

            log.info("用户[{}]退出竞赛[{}]，部门重置为默认部门[{}]",
                    userId, compId, user.getDeptId());
        }

        // 删除关联记录
        SysUserComp userComp = new SysUserComp();
        userComp.setUserId(userId);
        userComp.setCompId(compId);
        return userCompMapper.deleteUserCompInfo(userComp);
    }

    /**
     * 查询用户参加的竞赛列表
     *
     * @param userId 用户ID
     * @return 竞赛ID列表
     */
    @Override
    public List<Long> selectUserCompetitions(Long userId) {
        // 查询用户所有竞赛关联
        SysUser user = new SysUser();
        user.setUserId(userId);

        // 获取已分配的竞赛
        List<SysComp> comps = compService.selectMyAssignedCompetitions(userId);

        return comps.stream()
                .map(SysComp::getCompId)
                .collect(Collectors.toList());
    }

    /**
     * 查询竞赛的参与用户
     *
     * @param compId 竞赛ID
     * @return 用户ID列表
     */
    @Override
    public List<Long> selectCompetitionUsers(Long compId) {
        // 设置查询条件
        SysUser user = new SysUser();
        user.setCompId(compId);

        // 查询分配的用户
        List<SysUser> users = userService.selectAllocatedJudgeList(user);

        return users.stream()
                .map(SysUser::getUserId)
                .collect(Collectors.toList());
    }

    /**
     * 批量删除竞赛关联关系
     *
     * @param compId 竞赛ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteCompetitionUsers(Long compId) {
        // 查询关联的用户
        List<Long> userIds = selectCompetitionUsers(compId);

        // 获取竞赛详情
        SysComp comp = compService.selectSysCompByCompId(compId);
        if (comp == null) {
            throw new ServiceException("竞赛不存在");
        }

        // 更新用户部门（如果当前部门是竞赛部门）
        if (comp.getDeptId() != null) {
            for (Long userId : userIds) {
                SysUser user = userService.selectUserById(userId);
                if (user != null && user.getDeptId() != null &&
                    user.getDeptId().equals(comp.getDeptId())) {
                    // 重置为默认部门
                    user.setDeptId(100L);
                    userService.updateUser(user);
                }
            }
        }

        // 删除竞赛的所有用户关联
        Long[] userIdArray = userIds.toArray(new Long[0]);
        if (userIdArray.length > 0) {
            return userCompMapper.deleteUserCompInfos(compId, userIdArray);
        }
        return 0;
    }

    /**
     * 判断用户是否已加入某竞赛
     *
     * @param userId 用户ID
     * @param compId 竞赛ID
     * @return 如果已加入则返回true，否则返回false
     */
    @Override
    public boolean isUserJoinedCompetition(Long userId, Long compId) {
        log.info("检查用户[{}]是否已加入竞赛[{}]", userId, compId);
        if (userId == null || compId == null) {
            throw new ServiceException("用户ID和竞赛ID不能为空");
        }

        List<Long> userCompIds = selectUserCompetitions(userId);
        return userCompIds.contains(compId);
    }
}
