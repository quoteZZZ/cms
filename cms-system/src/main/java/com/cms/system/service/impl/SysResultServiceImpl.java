package com.cms.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.cms.common.core.domain.entity.SysComp;
import com.cms.common.exception.ServiceException;
import com.cms.common.redis.RedisCacheUtil;
import com.cms.common.redisson.RedissonLockUtil;
import com.cms.common.utils.DateUtils;
import com.cms.common.utils.uuid.IdGenerator;
import com.cms.common.core.domain.entity.SysRegistr;
import com.cms.common.core.domain.entity.SysScore;
import com.cms.system.mapper.SysCompMapper;
import com.cms.system.mapper.SysRegistrMapper;
import com.cms.system.mapper.SysScoreMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cms.system.mapper.SysResultMapper;
import com.cms.common.core.domain.entity.SysResult;
import com.cms.system.service.ISysResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 成绩结果Service业务层处理
 * 
 * @author quoteZZZ
 * @date 2025-03-09
 */
@Service
public class SysResultServiceImpl implements ISysResultService 
{
    @Autowired
    private SysResultMapper sysResultMapper;

    @Autowired
    private SysScoreMapper sysScoreMapper;

    @Autowired
    private SysRegistrMapper sysRegistrMapper;

    @Autowired
    private SysCompMapper sysCompMapper;

    @Autowired
    private SysRegistrServiceImpl sysRegistrService;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Resource
    private RedissonClient redissonClient;

    @Autowired(required = false)
    private RedissonLockUtil redissonLockUtil;

    // 定义日志记录器
    private static final Logger logger = LoggerFactory.getLogger(SysResultServiceImpl.class);

    // 缓存相关常量
    private static final String RESULT_CACHE_PREFIX = "sys:result:";
    private static final String RESULT_LIST_CACHE_KEY = "sys:result:list";
    private static final String SCORE_CALC_LOCK_PREFIX = "lock:score_calculation:comp:";
    private static final int CACHE_EXPIRE_TIME = 30; // 缓存过期时间（分钟）
    private static final int CACHE_RANDOM_OFFSET = 5; // 随机过期偏移（分钟）

    /**
     * 查询成绩结果
     * 
     * @param resultId 成绩结果主键
     * @return 成绩结果
     */
    @Override
    public SysResult selectSysResultByResultId(Long resultId)
    {
        if (resultId == null) {
            logger.warn("查询成绩结果时resultId为空");
            throw new ServiceException("成绩ID不能为空", 400);
        }

        logger.info("查询成绩结果, resultId: {}", resultId);
        // 尝试从缓存获取
        String cacheKey = RESULT_CACHE_PREFIX + resultId;
        SysResult result = redisCacheUtil.getCacheObject(cacheKey);

        if (result != null) {
            logger.info("从缓存获取成绩结果, resultId: {}", resultId);
            return result;
        }

        // 缓存未命中，从数据库查询（使用分布式锁避免缓存击穿）
        RLock lock = redissonClient.getLock("lock:" + cacheKey);
        try {
            // 尝试获取锁，等待10秒，锁过期时间5秒
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                try {
                    // 双重检查
                    result = redisCacheUtil.getCacheObject(cacheKey);
                    if (result == null) {
                        // 从数据库查询
                        result = sysResultMapper.selectSysResultByResultId(resultId);

                        // 存入缓存（即使为null也缓存，防止缓存穿透）
                        if (result != null) {
                            // 随机过期时间，避免缓存雪崩
                            int randomExpire = CACHE_EXPIRE_TIME + new Random().nextInt(CACHE_RANDOM_OFFSET);
                            redisCacheUtil.setCacheObject(cacheKey, result, randomExpire, TimeUnit.MINUTES);
                            logger.info("成绩结果已加入缓存, resultId: {}, 过期时间: {}分钟", resultId, randomExpire);
                        } else {
                            // 缓存空值，防止缓存穿透，过期时间较短
                            redisCacheUtil.setCacheObject(cacheKey, new SysResult(), 2, TimeUnit.MINUTES);
                            logger.info("成绩结果不存在，已缓存空对象, resultId: {}", resultId);
                        }
                    }
                } finally {
                    // 确保锁被释放
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取成绩结果分布式锁被中断, resultId: {}", resultId, e);
        } catch (Exception e) {
            logger.error("查询成绩结果异常, resultId: {}", resultId, e);
        }

        return result;
    }

    /**
     * 查询成绩结果列表
     *
     * @param result 成绩结果
     * @return 成绩结果
     */
    @Override
    public List<SysResult> selectSysResultList(SysResult result) {
        // 确保成绩数据最新
        try {
            processResults();
        } catch (Exception e) {
            logger.error("处理成绩数据出错", e);
            // 继续执行查询，确保用户可以看到现有数据
        }

        // 构建查询的缓存键
        String cacheKey = buildResultListCacheKey(result);

        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<SysResult> resultList = redisCacheUtil.getCacheObject(cacheKey);

        if (resultList != null) {
            logger.info("从缓存获取成绩结果列表, 数量: {}", resultList.size());
            return resultList;
        }

        // 缓存未命中，从数据库查询
        resultList = sysResultMapper.selectSysResultList(result);
        logger.info("查询到{}条成绩结果记录", resultList.size());

        // 存入缓存
        if (resultList != null && !resultList.isEmpty()) {
            redisCacheUtil.setCacheObject(cacheKey, resultList, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        }

        return resultList;
    }

    /**
     * 构建结果列表的缓存键
     */
    private String buildResultListCacheKey(SysResult result) {
        StringBuilder keySb = new StringBuilder(RESULT_LIST_CACHE_KEY);

        if (result != null) {
            if (result.getCompId() != null) {
                keySb.append(":comp:").append(result.getCompId());
            }
            if (result.getUserId() != null) {
                keySb.append(":user:").append(result.getUserId());
            }
            if (result.getRegistrId() != null) {
                keySb.append(":registr:").append(result.getRegistrId());
            }
        }

        return keySb.toString();
    }

    /**
     * 新增成绩结果
     */
    @Override
    @Transactional
    public int insertSysResult(SysResult sysResult) {
        logger.info("新增成绩结果, sysResult: {}", sysResult);
        try {
            // 参数校验
            if (sysResult == null) {
                throw new ServiceException("成绩信息不能为空", 400);
            }

            // 校验 registrId 是否存在
            if (sysResult.getRegistrId() != null) {
                SysRegistr registr = sysRegistrMapper.selectSysRegistrByRegistrId(sysResult.getRegistrId());
                if (registr == null) {
                    logger.error("插入成绩结果失败，registrId={} 在 sys_registr 表中不存在", sysResult.getRegistrId());
                    throw new ServiceException("插入成绩结果失败，报名记录不存在", 400);
                }
            }

            // 生成ID
            if (sysResult.getResultId() == null) {
                sysResult.setResultId(IdGenerator.generateId(0));
            }

            sysResult.setCreateTime(DateUtils.getNowDate());
            sysResult.setFinalScore(sysResult.getFinalScore().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP));

            // 使用分布式锁确保操作原子性
            String lockKey = SCORE_CALC_LOCK_PREFIX + "registr:" + sysResult.getRegistrId();
            RLock lock = redissonClient.getLock(lockKey);

            try {
                if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                    try {
                        // 检查是否已存在记录
                        SysResult existingResult = sysResultMapper.selectSysResultByRegistrId(sysResult.getRegistrId());

                        // 删除可能存在的缓存（先删缓存）
                        if (existingResult != null) {
                            String cacheKey = RESULT_CACHE_PREFIX + existingResult.getResultId();
                            redisCacheUtil.deleteObject(cacheKey);
                        }

                        int rows;
                        if (existingResult != null) {
                            // 更新已存在记录
                            existingResult.setFinalScore(sysResult.getFinalScore());
                            existingResult.setUpdateTime(DateUtils.getNowDate());
                            rows = sysResultMapper.updateSysResult(existingResult);
                            logger.info("更新成绩记录: {}, 返回: {}", existingResult, rows);

                            // 清除相关列表缓存
                            redisCacheUtil.deleteKeysByPattern(RESULT_LIST_CACHE_KEY + "*");

                            // 延时双删策略
                            final String finalCacheKey = RESULT_CACHE_PREFIX + existingResult.getResultId();
                            new Thread(() -> {
                                try {
                                    // 延时100-500ms再次删除缓存
                                    Thread.sleep(new Random().nextInt(400) + 100);
                                    redisCacheUtil.deleteObject(finalCacheKey);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    logger.error("延时删除缓存被中断", e);
                                }
                            }).start();

                            return rows;
                        } else {
                            // 插入新记录
                            rows = sysResultMapper.insertSysResult(sysResult);
                            logger.info("插入成绩记录: {}, 返回: {}", sysResult, rows);

                            // 清除相关列表缓存
                            redisCacheUtil.deleteKeysByPattern(RESULT_LIST_CACHE_KEY + "*");

                            return rows;
                        }
                    } finally {
                        lock.unlock();
                    }
                } else {
                    logger.warn("获取分布式锁超时，无法保证成绩操作原子性");
                    throw new ServiceException("系统繁忙，请稍后重试", 500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("获取分布式锁被中断", e);
                throw new ServiceException("操作被中断", 500);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("新增或更新成绩结果失败, sysResult: {}", sysResult, e);
            throw new ServiceException("新增或更新成绩结果失败", 500, e.getMessage());
        }
    }

    /**
     * 处理报名数据：查询报名、计算平均分、根据是否已有成绩记录进行新增或更新
     */
    private void processResults() {
        logger.info("开始处理报名数据");

        try {
            // 查询所有竞赛
            List<SysComp> competitions = sysCompMapper.selectSysCompList(new SysComp(), null);
            if (competitions == null || competitions.isEmpty()) {
                logger.warn("没有找到任何竞赛数据");
                return;
            }

            for (SysComp comp : competitions) {
                Long compId = comp.getCompId();
                if (compId == null) {
                    logger.warn("竞赛数据缺少竞赛ID，跳过该竞赛: {}", comp);
                    continue;
                }

                // 使用分布式锁确保同一时间只有一个实例在处理同一个竞赛的成绩
                String lockKey = SCORE_CALC_LOCK_PREFIX + compId;

                try {
                    // 使用分布式锁执行操作
                    redissonLockUtil.executeWithLock(lockKey, () -> {
                        calculateAndUpdateCompetitionResults(compId);
                        return null;
                    });
                } catch (Exception e) {
                    logger.error("处理竞赛ID={}的成绩时发生错误: {}", compId, e.getMessage(), e);
                }
            }

            logger.info("所有报名数据处理完成");
        } catch (Exception e) {
            logger.error("处理报名数据时发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 计算并更新特定竞赛的所有参赛者成绩
     */
    private void calculateAndUpdateCompetitionResults(Long compId) {
        logger.info("开始处理竞赛ID={}的成绩", compId);

        // 查询该竞赛下的所有参赛者
        List<SysRegistr> registrList = sysRegistrMapper.selectSysRegistrListByCompId(compId);
        if (registrList == null || registrList.isEmpty()) {
            logger.warn("竞赛ID={} 没有参赛者数据", compId);
            return;
        }

        // 查询该竞赛下的所有评分数据
        List<SysScore> scores = sysScoreMapper.selectScoresByCompId(compId);
        if (scores == null || scores.isEmpty()) {
            logger.warn("竞赛ID={} 没有评分数据", compId);
            return;
        }

        // 按参赛者ID分组处理评分
        Map<Long, List<SysScore>> scoresByRegistrId = scores.stream()
                .collect(Collectors.groupingBy(SysScore::getRegistrId));

        // 为每个参赛者计算成绩
        for (SysRegistr registr : registrList) {
            Long registrId = registr.getRegistrId();
            Long userId = registr.getUserId();

            try {
                processParticipantScores(compId, registrId, userId, scoresByRegistrId.get(registrId));
            } catch (Exception e) {
                logger.error("处理参赛者ID={}的成绩时发生错误: {}", registrId, e.getMessage(), e);
            }
        }
    }

    /**
     * 处理单个参赛者的评分并更新成绩
     */
    private void processParticipantScores(Long compId, Long registrId, Long userId, List<SysScore> participantScores) {
        // 检查该参赛者是否有评分数据
        if (participantScores == null || participantScores.isEmpty()) {
            logger.info("参赛者ID={} 没有评分数据，跳过", registrId);
            return;
        }

        // 计算平均分
        double totalScore = 0.0;
        int validScoreCount = 0;

        for (SysScore score : participantScores) {
            if (score != null && score.getScore() != null) {
                totalScore += score.getScore();
                validScoreCount++;
            }
        }

        // 避免除零错误
        if (validScoreCount == 0) {
            logger.warn("参赛者ID={} 没有有效的评分数据", registrId);
            return;
        }

        double finalScore = Math.round((totalScore / validScoreCount) * 100.0) / 100.0;
        logger.info("参赛者ID={}, 计算得到的平均分={}", registrId, finalScore);

        // 使用分布式锁更新成绩
        String lockKey = SCORE_CALC_LOCK_PREFIX + "registr:" + registrId;
        try {
            redissonLockUtil.executeWithLock(lockKey, () -> {
                updateParticipantResult(compId, registrId, userId, finalScore);
                return null;
            });
        } catch (Exception e) {
            logger.error("更新参赛者ID={}的成绩时发生错误: {}", registrId, e.getMessage(), e);
        }
    }

    /**
     * 更新参赛者成绩记录
     */
    private void updateParticipantResult(Long compId, Long registrId, Long userId, double finalScore) {
        // 检查是否已存在成绩记录
        SysResult existingResult = sysResultMapper.selectSysResultByRegistrId(registrId);
        String cacheKey = RESULT_CACHE_PREFIX + (existingResult != null ? existingResult.getResultId() : "");

        try {
            // 删除缓存（双删策略第一次删除）
            if (existingResult != null) {
                redisCacheUtil.deleteObject(cacheKey);
            }

            if (existingResult == null) {
                // 新增成绩记录
                SysResult newResult = new SysResult();
                newResult.setResultId(IdGenerator.generateId(0));
                newResult.setCompId(compId);
                newResult.setRegistrId(registrId);
                newResult.setUserId(userId);
                newResult.setFinalScore(BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP));
                newResult.setCreateTime(DateUtils.getNowDate());
                newResult.setUpdateTime(DateUtils.getNowDate());
                newResult.setStatus("0");
                newResult.setDelFlag("0");

                sysResultMapper.insertSysResult(newResult);
                logger.info("成功插入新成绩记录: {}", newResult);

                // 更新缓存
                redisCacheUtil.setCacheObject(RESULT_CACHE_PREFIX + newResult.getResultId(),
                        newResult, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            } else {
                // 更新已存在的成绩记录
                existingResult.setFinalScore(BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP));
                existingResult.setUpdateTime(DateUtils.getNowDate());

                sysResultMapper.updateSysResult(existingResult);
                logger.info("成功更新成绩记录: {}", existingResult);

                // 删除结果列表缓存
                redisCacheUtil.deleteKeysByPattern(RESULT_LIST_CACHE_KEY + "*");

                // 延时双删策略，防止缓存不一致
                redisCacheUtil.deleteWithDelay(cacheKey, 500);
            }
        } catch (Exception e) {
            logger.error("操作数据库更新成绩记录失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 更新成绩结果
     */
    @Override
    @Transactional
    public int updateSysResult(SysResult sysResult) {
        logger.info("更新成绩结果, sysResult: {}", sysResult);
        try {
            // 参数校验
            if (sysResult.getResultId() == null) {
                throw new ServiceException("成绩ID不能为空", 400);
            }

            sysResult.setUpdateTime(DateUtils.getNowDate());
            // 确保 finalScore 以两位小数精度存储
            if (sysResult.getFinalScore() != null) {
                sysResult.setFinalScore(sysResult.getFinalScore().setScale(2, RoundingMode.HALF_UP));
            }

            String lockKey = SCORE_CALC_LOCK_PREFIX + "result:" + sysResult.getResultId();
            String cacheKey = RESULT_CACHE_PREFIX + sysResult.getResultId();

            return redissonLockUtil.executeWithDoubleCacheDelete(lockKey, () -> {
                int rows = sysResultMapper.updateSysResult(sysResult);
                if (rows > 0) {
                    // 更新成功后，清除相关的结果列表缓存
                    redisCacheUtil.deleteKeysByPattern(RESULT_LIST_CACHE_KEY + "*");
                }
                return rows;
            }, cacheKey, redisCacheUtil);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("更新成绩结果失败, sysResult: {}", sysResult, e);
            throw new ServiceException("更新成绩结果失败", 500, e.getMessage());
        }
    }

    /**
     * 批量删除成绩结果
     */
    @Override
    @Transactional
    public int deleteSysResultByResultIds(Long[] resultIds) {
        logger.info("批量删除成绩结果, resultIds: {}", resultIds);
        try {
            if (resultIds == null || resultIds.length == 0) {
                throw new ServiceException("删除的成绩ID不能为空", 400);
            }

            String lockKey = SCORE_CALC_LOCK_PREFIX + "batch:" + Arrays.toString(resultIds);

            return redissonLockUtil.executeWithLock(lockKey, () -> {
                int rows = sysResultMapper.deleteSysResultByResultIds(resultIds);
                if (rows > 0) {
                    // 删除每个成绩的缓存
                    for (Long resultId : resultIds) {
                        redisCacheUtil.deleteObject(RESULT_CACHE_PREFIX + resultId);
                    }
                    // 清除结果列表缓存
                    redisCacheUtil.deleteKeysByPattern(RESULT_LIST_CACHE_KEY + "*");
                }
                return rows;
            });
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("批量删除成绩结果失败, resultIds: {}", resultIds, e);
            throw new ServiceException("批量删除成绩结果失败", 500, e.getMessage());
        }
    }

    /**
     * 删除成绩结果信息
     */
    @Override
    @Transactional
    public int deleteSysResultByResultId(Long resultId) {
        logger.info("删除成绩结果信息, resultId: {}", resultId);
        try {
            if (resultId == null) {
                throw new ServiceException("删除的成绩ID不能为空", 400);
            }

            String lockKey = SCORE_CALC_LOCK_PREFIX + "result:" + resultId;
            String cacheKey = RESULT_CACHE_PREFIX + resultId;

            return redissonLockUtil.executeWithDoubleCacheDelete(lockKey, () -> {
                int rows = sysResultMapper.deleteSysResultByResultId(resultId);
                if (rows > 0) {
                    // 清除结果列表缓存
                    redisCacheUtil.deleteKeysByPattern(RESULT_LIST_CACHE_KEY + "*");
                }
                return rows;
            }, cacheKey, redisCacheUtil);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("删除成绩结果信息失败, resultId: {}", resultId, e);
            throw new ServiceException("删除成绩结果信息失败", 500, e.getMessage());
        }
    }

    // 添加@SafeVarargs注解
    @SafeVarargs
    public final BigDecimal calculateWeightedAverage(List<Map.Entry<BigDecimal, Double>>... scoreWeights) {
        BigDecimal totalWeightedScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        for (List<Map.Entry<BigDecimal, Double>> scoreWeightList : scoreWeights) {
            if (scoreWeightList == null) {
                continue; // 防止空列表导致NPE
            }
            for (Map.Entry<BigDecimal, Double> entry : scoreWeightList) {
                if (entry == null || entry.getKey() == null) {
                    continue; // 跳过空条目
                }
                BigDecimal score = entry.getKey();
                // 修改: 确保 entry.getValue() 不为 null 并转换为 BigDecimal
                Double weightValue = entry.getValue();
                BigDecimal weight = (weightValue != null) ? BigDecimal.valueOf(weightValue) : BigDecimal.ZERO;
                
                totalWeightedScore = totalWeightedScore.add(score.multiply(weight));
                totalWeight = totalWeight.add(weight);
            }
        }
        
        // 避免除零错误
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalWeightedScore.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }

    private void someMethod() { // 假设错误发生在某个方法内部
        // 示例代码
        BigDecimal aBigDecimal = new BigDecimal("10.5"); // 示例值
        double aDouble = 2.5; // 示例值
        BigDecimal result = aBigDecimal.multiply(BigDecimal.valueOf(aDouble));
        // 示例代码结束
    }
}
