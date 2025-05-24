package com.cms.system.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cms.common.annotation.DataScope;
import com.cms.common.utils.DateUtils;
import com.cms.common.utils.uuid.IdGenerator;
import com.cms.common.core.domain.entity.SysRegistr;
import com.cms.system.mapper.SysRegistrMapper;
import com.cms.system.service.ISysRegistrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cms.system.mapper.SysScoreMapper;
import com.cms.common.core.domain.entity.SysScore;
import com.cms.system.service.ISysScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.system.service.ISysUserService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评分信息Service业务层处理
 * 
 * @author quoteZZZ
 * @date 2025-03-09
 */
@Service
public class SysScoreServiceImpl implements ISysScoreService 
{
    @Autowired
    private SysScoreMapper sysScoreMapper;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysRegistrService sysRegistrService;
    @Autowired
    private SysRegistrMapper sysRegistrMapper;
    // 定义日志记录器
    Logger logger = LoggerFactory.getLogger(SysScoreServiceImpl.class);

    /**
     * 查询评分信息
     * 
     * @param scoreId 评分信息主键
     * @return 评分信息
     */
    @Override
    public SysScore selectSysScoreByScoreId(Long scoreId)
    {
        logger.info("查询评分信息, scoreId: {}", scoreId);
        return sysScoreMapper.selectSysScoreByScoreId(scoreId);
    }

    /**
     * 查询评分信息列表
     * 
     * @param sysScore 评分信息
     * @return 评分信息
     */
    @Override
    @DataScope(userAlias = "u")
    public List<SysScore> selectSysScoreList(SysScore sysScore)
    {
        logger.info("查询评分信息列表, sysScore: {}", sysScore);
        return sysScoreMapper.selectSysScoreList(sysScore);
    }

    /**
     * 新增评分信息
     *
     * @param sysScore 评分信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertSysScore(SysScore sysScore) {
        logger.info("新增评分信息, sysScore: {}", sysScore);
        try {
            // 获得唯一id,设置到对象中
            sysScore.setScoreId(IdGenerator.generateId(0));

            // 根据参数的报名编码，获得报名信息
            SysRegistr sysRegistr = validateAndGetSysRegistr(sysScore.getRegistrId());
            // 根据参赛ID获取竞赛ID和用户ID
            Map<String, Long> compAndUserId = getCompAndUserIdByRegistrId(sysScore.getRegistrId());
            Long compId = compAndUserId.get("compId");
            Long userId = compAndUserId.get("userId");

            // 设置竞赛ID和用户ID
            sysScore.setCompId(compId);
            sysScore.setUserId(userId);

            // 根据参数的用户编码，获得评委名称和用户编码
            SysUser sysUser = validateAndGetSysUser(sysScore.getJudgeId());
            sysScore.setCreateBy(sysUser.getUserName());
            sysScore.setJudgeName(sysUser.getUserName());

            sysScore.setCreateTime(DateUtils.getNowDate());

            int result = sysScoreMapper.insertSysScore(sysScore);
            logger.info("新增评分信息结果: {}", result);

            // 如果评分成功，用户的评分次数+1
            if (result > 0) {
                updateRegistrScoreCount(sysRegistr);
            }

            return result;
        } catch (Exception e) {
            logger.error("新增评分信息失败, sysScore: {}", sysScore, e);
            throw new RuntimeException("新增评分信息失败", e);
        }
    }

    private SysRegistr validateAndGetSysRegistr(Long registId) {
        logger.info("验证并获取报名信息, registId: {}", registId);
        SysRegistr sysRegistr = sysRegistrService.selectSysRegistrByRegistrId(registId);
        if (sysRegistr == null) {
            throw new IllegalArgumentException("报名编码无效");
        }
        return sysRegistr;
    }

    private SysUser validateAndGetSysUser(Long userId) {
        logger.info("验证并获取用户信息, userId: {}", userId);
        SysUser sysUser = sysUserService.selectUserById(userId);
        if (sysUser == null) {
            throw new IllegalArgumentException("用户编码无效");
        }
        return sysUser;
    }

    private void updateRegistrScoreCount(SysRegistr sysRegistr) {
        logger.info("更新报名评分次数, sysRegistr: {}", sysRegistr);
        sysRegistr.setScoreCount(sysRegistr.getScoreCount() + 1);
        sysRegistrService.updateSysRegistr(sysRegistr);
    }

    /**
     * 修改评分信息
     * 
     * @param sysScore 评分信息
     * @return 结果
     */
    @Override
    public int updateSysScore(SysScore sysScore)
    {
        logger.info("修改评分信息, sysScore: {}", sysScore);
        try {
            sysScore.setUpdateTime(DateUtils.getNowDate());
            int result = sysScoreMapper.updateSysScore(sysScore);
            logger.info("修改评分信息结果: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("修改评分信息失败, sysScore: {}", sysScore, e);
            throw new RuntimeException("修改评分信息失败", e);
        }
    }

    /**
     * 批量删除评分信息
     * 
     * @param scoreIds 需要删除的评分信息主键
     * @return 结果
     */
    @Override
    public int deleteSysScoreByScoreIds(Long[] scoreIds)
    {
        logger.info("批量删除评分信息, scoreIds: {}", scoreIds);
        try {
            int result = sysScoreMapper.deleteSysScoreByScoreIds(scoreIds);
            logger.info("批量删除评分信息结果: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("批量删除评分信息失败, scoreIds: {}", scoreIds, e);
            throw new RuntimeException("批量删除评分信息失败", e);
        }
    }

    /**
     * 删除评分信息信息
     * 
     * @param scoreId 评分信息主键
     * @return 结果
     */
    @Override
    public int deleteSysScoreByScoreId(Long scoreId)
    {
        logger.info("删除评分信息信息, scoreId: {}", scoreId);
        try {
            int result = sysScoreMapper.deleteSysScoreByScoreId(scoreId);
            logger.info("删除评分信息信息结果: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("删除评分信息信息失败, scoreId: {}", scoreId, e);
            throw new RuntimeException("删除评分信息信息失败", e);
        }
    }

    /**
     * 通过参赛ID获取竞赛ID和用户ID
     *
     * @param registrId 参赛ID
     * @return 包含竞赛ID和用户ID的Map
     */
    private Map<String, Long> getCompAndUserIdByRegistrId(Long registrId) {
        logger.info("通过参赛ID获取竞赛ID和用户ID, registrId: {}", registrId);
        SysRegistr sysRegistr = sysRegistrMapper.selectSysRegistrByRegistrId(registrId);
        if (sysRegistr == null) {
            throw new IllegalArgumentException("参赛ID无效");
        }
        Map<String, Long> result = new HashMap<>();
        result.put("compId", sysRegistr.getCompId());
        result.put("userId", sysRegistr.getUserId());
        return result;
    }

    /**
     * 根据参赛ID计算平均分
     *
     * @param registerId 参赛ID
     * @return 平均分，若无评分数据则返回0.0
     */
    public double calculateAverageScoreByParticipantId(Long registerId) {
        logger.info("根据参加ID计算平均分, registerId: {}", registerId);
        // 根据参加ID查询评分数据
        SysScore score = new SysScore();
        score.setRegistrId(registerId); // 设置参赛ID
        List<SysScore> scores = sysScoreMapper.selectSysScoreList(score);

        // 检查评分数据是否存在
        if (scores == null || scores.isEmpty()) {
            logger.warn("没有找到评分数据，返回平均分0.0");
            throw new IllegalArgumentException("报名ID " + registerId + " 没有评分数据");
        }

        double totalScore = 0.0;
        // 遍历评分数据，计算总分
        for (SysScore sysscore : scores) {
            if (sysscore != null && sysscore.getScore() != null) {
                totalScore += sysscore.getScore();
            }
        }

        // 计算并返回平均分
        double averageScore = totalScore / scores.size();
        logger.info("计算得到的平均分: {}", averageScore);
        return averageScore;
    }

}