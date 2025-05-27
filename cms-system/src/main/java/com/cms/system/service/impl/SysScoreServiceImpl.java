package com.cms.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.cms.common.annotation.DataScope;
import com.cms.common.utils.DateUtils;
import com.cms.common.utils.uuid.IdGenerator;
import com.cms.common.core.domain.entity.SysRegistr;
import com.cms.common.core.domain.entity.SysResult;
import com.cms.system.mapper.SysRegistrMapper;
import com.cms.system.mapper.SysResultMapper;
import com.cms.system.service.ISysRegistrService;
import com.cms.system.service.ISysResultService;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

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

    @Autowired
    private SysResultMapper sysResultMapper;

    @Autowired
    private ISysResultService sysResultService;

    @Autowired
    private RedissonClient redissonClient;

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
     * @return 评分信息集合
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
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
            // 参数校验
            if (sysScore == null) {
                logger.error("评分信息不能为空");
                throw new IllegalArgumentException("评分信息不能为空");
            }
            if (sysScore.getRegistrId() == null) {
                logger.error("报名编码不能为空");
                throw new IllegalArgumentException("报名编码不能为空");
            }
            if (sysScore.getJudgeId() == null) {
                logger.error("评委编码不能为空");
                throw new IllegalArgumentException("评委编码不能为空");
            }
            if (sysScore.getScore() == null) {
                logger.error("评分不能为空");
                throw new IllegalArgumentException("评分不能为空");
            }

            // 检查评分范围
            if (sysScore.getScore() < 0 || sysScore.getScore() > 100) {
                logger.error("评分必须在0-100之间, score: {}", sysScore.getScore());
                throw new IllegalArgumentException("评分必须在0-100之间");
            }

            // 获得唯一id,设置到对象中
            sysScore.setScoreId(IdGenerator.generateId(0));

            // 根据参数的报名编码，获得报名信息
            SysRegistr sysRegistr = validateAndGetSysRegistr(sysScore.getRegistrId());
            // 根据参赛ID获取竞赛ID和用户ID (参赛者的用户ID)
            Map<String, Long> compAndUserId = getCompAndUserIdByRegistrId(sysScore.getRegistrId());
            Long compId = compAndUserId.get("compId");
            Long participantUserId = compAndUserId.get("userId"); // 参赛者的用户ID

            // 检查评委是否已经评过分
            SysScore existingScore = getExistingScore(sysScore.getJudgeId(), sysScore.getRegistrId());
            if (existingScore != null) {
                logger.warn("评委已经对该参赛者评过分, judgeId: {}, registrId: {}",
                        sysScore.getJudgeId(), sysScore.getRegistrId());
                throw new IllegalArgumentException("您已对该参赛者评分，请勿重复评分");
            }

            // 获取执行操作的评委用户
            SysUser judgeUser = validateAndGetSysUser(sysScore.getJudgeId());

            // 设置评分记录的参赛者用户ID
            sysScore.setUserId(participantUserId);
            // 设置评分记录的部门ID为当前评委的部门ID
            // (评委的部门ID应在分配至竞赛时已更新为竞赛对应部门ID)
            sysScore.setDeptId(judgeUser.getDeptId());

            // 设置评委名称和创建者为当前评委的用户名
            sysScore.setCreateBy(judgeUser.getUserName());
            sysScore.setJudgeName(judgeUser.getUserName());

            // 获取参赛者用户名称
            SysUser participantUser = sysUserService.selectUserById(participantUserId);
            if (participantUser != null) {
                sysScore.setUserName(participantUser.getUserName());
            }

            // 设置创建时间
            sysScore.setCreateTime(DateUtils.getNowDate());

            // 使用分布式锁保证评分操作的原子性
            String lockKey = "score:lock:registr:" + sysScore.getRegistrId() + ":judge:" + sysScore.getJudgeId();
            RLock lock = redissonClient.getLock(lockKey);
            try {
                if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                    try {
                        // 再次检查是否存在重复评分（双重检查）
                        SysScore checkScore = getExistingScore(sysScore.getJudgeId(), sysScore.getRegistrId());
                        if (checkScore != null) {
                            logger.warn("双重检查：评委已对该参赛者评分, judgeId: {}, registrId: {}",
                                    sysScore.getJudgeId(), sysScore.getRegistrId());
                            throw new IllegalArgumentException("您已对该参赛者评分，请勿重复评分");
                        }

                        // 执行插入操作
                        int rows = sysScoreMapper.insertSysScore(sysScore);
                        if (rows > 0) {
                            // 更新报名记录的评分计数
                            updateRegistrScoreCount(sysScore.getRegistrId());

                            // 触发成绩计算（这里可以调用结果服务的方法或者发送消息）
                            calculateAndUpdateResult(sysScore.getRegistrId());
                        }
                        return rows;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    logger.warn("获取评分锁超时, judgeId: {}, registrId: {}", sysScore.getJudgeId(), sysScore.getRegistrId());
                    throw new IllegalStateException("系统繁忙，请稍后重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("获取评分锁被中断", e);
                throw new IllegalStateException("评分操作被中断，请稍后重试");
            }
        } catch (IllegalArgumentException e) {
            // 接收业务验证异常并直接抛出
            throw e;
        } catch (Exception e) {
            logger.error("插入评分信息异常", e);
            throw new RuntimeException("插入评分信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证并获取报名信息
     *
     * @param registrId 报名ID
     * @return 报名信息
     * @throws IllegalArgumentException 如果报名信息不存在或无效
     */
    private SysRegistr validateAndGetSysRegistr(Long registrId) {
        logger.info("验证并获取报名信息, registrId: {}", registrId);
        SysRegistr sysRegistr = sysRegistrMapper.selectSysRegistrByRegistrId(registrId);
        if (sysRegistr == null) {
            logger.error("报名信息不存在, registrId: {}", registrId);
            throw new IllegalArgumentException("报名信息不存在或已失效");
        }
        return sysRegistr;
    }

    /**
     * 根据报名ID获取竞赛ID和用户ID
     *
     * @param registrId 报名ID
     * @return 包含竞赛ID和用户ID的Map
     * @throws IllegalArgumentException 如果报名信息不存在
     */
    private Map<String, Long> getCompAndUserIdByRegistrId(Long registrId) {
        logger.info("根据报名ID获取竞赛ID和用户ID, registrId: {}", registrId);
        SysRegistr sysRegistr = sysRegistrMapper.selectSysRegistrByRegistrId(registrId);
        if (sysRegistr == null) {
            logger.error("报名信息不存在, registrId: {}", registrId);
            throw new IllegalArgumentException("报名信息不存在或已失效");
        }

        Map<String, Long> result = new HashMap<>(2);
        result.put("compId", sysRegistr.getCompId());
        result.put("userId", sysRegistr.getUserId());

        return result;
    }

    /**
     * 检查评委是否已对特定参赛者评分
     *
     * @param judgeId 评委ID
     * @param registrId 报名ID
     * @return 如果已评分则返回评分记录，否则返回null
     */
    private SysScore getExistingScore(Long judgeId, Long registrId) {
        logger.info("检查评委是否已评分, judgeId: {}, registrId: {}", judgeId, registrId);
        SysScore query = new SysScore();
        query.setJudgeId(judgeId);
        query.setRegistrId(registrId);

        List<SysScore> scores = sysScoreMapper.selectSysScoreList(query);
        return scores != null && !scores.isEmpty() ? scores.get(0) : null;
    }

    /**
     * 验证并获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     * @throws IllegalArgumentException 如果用户不存在
     */
    private SysUser validateAndGetSysUser(Long userId) {
        logger.info("验证并获取用户信息, userId: {}", userId);
        SysUser sysUser = sysUserService.selectUserById(userId);
        if (sysUser == null) {
            logger.error("用户不存在, userId: {}", userId);
            throw new IllegalArgumentException("用户不存在或已失效");
        }
        return sysUser;
    }

    /**
     * 更新报名记录的评分计数
     *
     * @param registrId 报名ID
     */
    private void updateRegistrScoreCount(Long registrId) {
        logger.info("更新报名评分计数, registrId: {}", registrId);
        try {
            SysRegistr sysRegistr = sysRegistrMapper.selectSysRegistrByRegistrId(registrId);
            if (sysRegistr != null) {
                // 设置评分计数加1
                Integer currentCount = sysRegistr.getScoreCount();
                sysRegistr.setScoreCount(currentCount == null ? 1 : currentCount + 1);
                sysRegistrMapper.updateSysRegistr(sysRegistr);
                logger.info("报名评分计数更新成功, registrId: {}, 新计数: {}", registrId, sysRegistr.getScoreCount());
            }
        } catch (Exception e) {
            logger.error("更新报名评分计数失败", e);
            // 这里不抛出异常，避免影响主流程
        }
    }

    /**
     * 计算并更新成绩结果
     *
     * @param registrId 报名ID
     */
    private void calculateAndUpdateResult(Long registrId) {
        logger.info("触发成绩计算, registrId: {}", registrId);
        try {
            // 获取所有该参赛者的评分记录
            List<SysScore> scoreList = sysScoreMapper.selectScoresByRegistrId(registrId);

            if (scoreList == null || scoreList.isEmpty()) {
                logger.warn("没有找到评分记录，无法计算成绩, registrId: {}", registrId);
                return;
            }

            // 计算平均分
            BigDecimal totalScore = BigDecimal.ZERO;
            int validScoreCount = 0;

            for (SysScore score : scoreList) {
                if (score.getScore() != null) {
                    totalScore = totalScore.add(BigDecimal.valueOf(score.getScore()));
                    validScoreCount++;
                }
            }

            if (validScoreCount == 0) {
                logger.warn("没有有效的评分记录, registrId: {}", registrId);
                return;
            }

            BigDecimal finalScore = totalScore.divide(BigDecimal.valueOf(validScoreCount), 2, RoundingMode.HALF_UP);
            logger.info("计算得到的平均分: {}, registrId: {}", finalScore, registrId);

            // 获取报名信息
            SysRegistr sysRegistr = sysRegistrMapper.selectSysRegistrByRegistrId(registrId);
            if (sysRegistr == null) {
                logger.error("报名信息不存在, registrId: {}", registrId);
                return;
            }

            // 构建成绩结果对象
            SysResult sysResult = new SysResult();
            sysResult.setCompId(sysRegistr.getCompId());
            sysResult.setUserId(sysRegistr.getUserId());
            sysResult.setRegistrId(registrId);
            sysResult.setFinalScore(finalScore);

            // 调用结果服务保存成绩
            sysResultService.insertSysResult(sysResult);
            logger.info("成绩更新完成, registrId: {}", registrId);
        } catch (Exception e) {
            logger.error("计算更新成绩失败", e);
            // 这里不抛出异常，避免影响主流程
        }
    }

    /**
     * 修改评分信息
     *
     * @param sysScore 评分信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateSysScore(SysScore sysScore) {
        logger.info("修改评分信息, sysScore: {}", sysScore);
        try {
            // 参数校验
            if (sysScore == null) {
                logger.error("评分信息不能为空");
                throw new IllegalArgumentException("评分信息不能为空");
            }

            if (sysScore.getScoreId() == null) {
                logger.error("评分ID不能为空");
                throw new IllegalArgumentException("评分ID不能为空");
            }

            // 检查评分是否存在
            SysScore existingScore = sysScoreMapper.selectSysScoreByScoreId(sysScore.getScoreId());
            if (existingScore == null) {
                logger.error("评分记录不存在, scoreId: {}", sysScore.getScoreId());
                throw new IllegalArgumentException("评分记录不存在或已被删除");
            }

            // 检查评分范围
            if (sysScore.getScore() != null && (sysScore.getScore() < 0 || sysScore.getScore() > 100)) {
                logger.error("评分必须在0-100之间, score: {}", sysScore.getScore());
                throw new IllegalArgumentException("评分必须在0-100之间");
            }

            // 设置更新时间和更新人
            sysScore.setUpdateTime(DateUtils.getNowDate());

            // 使用分布式锁保证评分更新操作的原子性
            String lockKey = "score:update:lock:" + sysScore.getScoreId();
            RLock lock = redissonClient.getLock(lockKey);
            try {
                if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                    try {
                        // 执行更新操作
                        int rows = sysScoreMapper.updateSysScore(sysScore);
                        if (rows > 0 && existingScore.getRegistrId() != null) {
                            // 如果评分值发生变化并且更新成功，需要重新计算和更新成绩
                            if (sysScore.getScore() != null &&
                                !sysScore.getScore().equals(existingScore.getScore())) {
                                calculateAndUpdateResult(existingScore.getRegistrId());
                            }
                        }
                        return rows;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    logger.warn("获取评分更新锁超时, scoreId: {}", sysScore.getScoreId());
                    throw new IllegalStateException("系统繁忙，请稍后重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("获取评分更新锁被中断", e);
                throw new IllegalStateException("评分更新操作被中断，请稍后重试");
            }
        } catch (IllegalArgumentException e) {
            // 接收业务验证异常并直接抛出
            throw e;
        } catch (Exception e) {
            logger.error("更新评分信息异常", e);
            throw new RuntimeException("更新评分信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量删除评分信息
     *
     * @param scoreIds 需要删除的评分信息主键集合
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteSysScoreByScoreIds(Long[] scoreIds) {
        logger.info("批量删除评分信息, scoreIds: {}", (Object) scoreIds);
        try {
            if (scoreIds == null || scoreIds.length == 0) {
                logger.error("评分ID列表不能为空");
                throw new IllegalArgumentException("评分ID列表不能为空");
            }

            // 获取要删除的所有评分的报名ID，用于后续更新成绩
            List<SysScore> scoreList = new ArrayList<>();
            for (Long scoreId : scoreIds) {
                SysScore score = sysScoreMapper.selectSysScoreByScoreId(scoreId);
                if (score != null) {
                    scoreList.add(score);
                }
            }

            // 使用分布式锁保证批量删除操作的原子性
            String lockKey = "score:delete:batch:" + Arrays.toString(scoreIds);
            RLock lock = redissonClient.getLock(lockKey);
            try {
                if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                    try {
                        // 执行批量删除操作
                        int rows = sysScoreMapper.deleteSysScoreByScoreIds(scoreIds);

                        // 更新相关报名记录的成绩
                        if (rows > 0) {
                            Set<Long> registrIds = new HashSet<>();
                            for (SysScore score : scoreList) {
                                if (score.getRegistrId() != null) {
                                    registrIds.add(score.getRegistrId());
                                }
                            }

                            // 对每个受影响的报名记录重新计算成绩
                            for (Long registrId : registrIds) {
                                calculateAndUpdateResult(registrId);
                            }
                        }

                        return rows;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    logger.warn("获取评分批量删除锁超时");
                    throw new IllegalStateException("系统繁忙，请稍后重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("获取评分批量删除锁被中断", e);
                throw new IllegalStateException("评分删除操作被中断，请稍后重试");
            }
        } catch (IllegalArgumentException e) {
            // 接收业务验证异常并直接抛出
            throw e;
        } catch (Exception e) {
            logger.error("批量删除评分信息异常", e);
            throw new RuntimeException("批量删除评分信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除评分信息信息
     *
     * @param scoreId 评分信息主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteSysScoreByScoreId(Long scoreId) {
        logger.info("删除评分信息, scoreId: {}", scoreId);
        try {
            if (scoreId == null) {
                logger.error("评分ID不能为空");
                throw new IllegalArgumentException("评分ID不能为空");
            }

            // 查询要删除的评分信息
            SysScore score = sysScoreMapper.selectSysScoreByScoreId(scoreId);
            if (score == null) {
                logger.warn("评分信息不存在, scoreId: {}", scoreId);
                return 0;
            }

            // 获取报名ID，用于后续更新成绩
            Long registrId = score.getRegistrId();

            // 使用分布式锁保证删除操作的原子性
            String lockKey = "score:delete:lock:" + scoreId;
            RLock lock = redissonClient.getLock(lockKey);
            try {
                if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                    try {
                        // 执行删除操作
                        int rows = sysScoreMapper.deleteSysScoreByScoreId(scoreId);

                        // 如果删除成功且报名ID不为空，重新计算和更新成绩
                        if (rows > 0 && registrId != null) {
                            calculateAndUpdateResult(registrId);
                        }

                        return rows;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    logger.warn("获取评分删除锁超时, scoreId: {}", scoreId);
                    throw new IllegalStateException("系统繁忙，请稍后重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("获取评分删除锁被中断", e);
                throw new IllegalStateException("评分删除操作被中断，请稍后重试");
            }
        } catch (IllegalArgumentException e) {
            // 接收业务验证异常并直接抛出
            throw e;
        } catch (Exception e) {
            logger.error("删除评分信息异常, scoreId: {}", scoreId, e);
            throw new RuntimeException("删除评分信息失败: " + e.getMessage(), e);
        }
    }
}
