package com.cms.system.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.cms.common.core.domain.entity.*;
import com.cms.common.exception.ServiceException;
import com.cms.common.utils.DateUtils;
import com.cms.common.utils.uuid.IdGenerator;
import com.cms.system.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cms.system.service.ISysResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private SysDeptMapper sysDeptMapper;

    // 定义日志记录器
    private static final Logger logger = LoggerFactory.getLogger(SysResultServiceImpl.class);

    // -------------------- 对外API方法 --------------------

    /**
     * 查询成绩结果
     * 
     * @param resultId 成绩结果主键
     * @return 成绩结果
     */
    @Override
    public SysResult selectSysResultByResultId(Long resultId)
    {
        logger.info("【开始】查询成绩结果, resultId: {}", resultId);

        if (resultId == null) {
            logger.warn("【错误】查询成绩结果时resultId为空");
            throw new ServiceException("成绩ID不能为空", 400);
        }

        try {
            SysResult result = sysResultMapper.selectSysResultByResultId(resultId);

            if (result != null) {
                logger.debug("【处理】找到成绩结果记录，准备填充额外信息: {}", result);
                // 填充额外信息
                fillResultExtraInfo(result);
                logger.debug("【完成】成绩结果填充额外信息完成: {}", result);
            } else {
                logger.warn("【提示】未找到成绩结果记录, resultId: {}", resultId);
            }

            logger.info("【结束】查询成绩结果完成, resultId: {}", resultId);
            return result;
        } catch (Exception e) {
            logger.error("【异常】查询成绩结果失败, resultId: {}", resultId, e);
            throw new ServiceException("查询成绩结果失败", 500, e.getMessage());
        }
    }

    /**
     * 查询成绩结果列表
     *
     * @param result 成绩结果
     * @return 成绩结果
     */
    @Override
    public List<SysResult> selectSysResultList(SysResult result) {
        logger.info("【开始】查询成绩结果列表, 查询条件: {}", result);

        // 确保成绩数据最新
        try {
            logger.debug("【处理】先处理所有竞赛成绩数据");
            processResults();
            logger.debug("【处理】所有竞赛成绩数据处理完成");
        } catch (Exception e) {
            logger.error("【异常】处理成绩数据出错，但将继续查询以展示现有数据", e);
            // 继续执行查询，确保用户可以看到现有数据
        }

        try {
            List<SysResult> resultList = sysResultMapper.selectSysResultList(result);
            logger.info("【结束】查询到{}条成绩结果记录", resultList.size());
            return resultList;
        } catch (Exception e) {
            logger.error("【异常】查询成绩结果列表失败", e);
            throw new ServiceException("查询成绩结果列表失败", 500, e.getMessage());
        }
    }

    /**
     * 新增成绩结果
     */
    @Override
    @Transactional
    public int insertSysResult(SysResult sysResult) {
        logger.info("【开始】新增成绩结果, sysResult: {}", sysResult);
        try {
            // 参数校验
            if (sysResult == null) {
                logger.warn("【错误】成绩信息不能为空");
                throw new ServiceException("成绩信息不能为空", 400);
            }

            // 校验 registrId 是否存在
            if (sysResult.getRegistrId() != null) {
                logger.debug("【处理】校验报名记录是否存在, registrId: {}", sysResult.getRegistrId());
                SysRegistr registr = sysRegistrMapper.selectSysRegistrByRegistrId(sysResult.getRegistrId());
                if (registr == null) {
                    logger.error("【错误】插入成绩结果失败，报名记录不存在, registrId={}", sysResult.getRegistrId());
                    throw new ServiceException("插入成绩结果失败，报名记录不存在", 400);
                }
                logger.debug("【处理】报名记录校验通过");
            }

            // 生成ID
            if (sysResult.getResultId() == null) {
                sysResult.setResultId(IdGenerator.generateId(0));
                logger.debug("【处理】生成新的结果ID: {}", sysResult.getResultId());
            }

            sysResult.setCreateTime(DateUtils.getNowDate());
            // 确保 finalScore 不为空
            if (sysResult.getFinalScore() != null) {
                // 处理精度
                double originalScore = sysResult.getFinalScore();
                double roundedScore = Math.round(originalScore * 100) / 100.0;
                sysResult.setFinalScore(roundedScore);
                logger.debug("【处理】成绩四舍五入: {} -> {}", originalScore, roundedScore);
            }

            // 检查是否已存在记录
            logger.debug("【处理】检查是否已存在相同报名ID的成绩记录");
            SysResult existingResult = sysResultMapper.selectSysResultByRegistrId(sysResult.getRegistrId());

            int rows;
            if (existingResult != null) {
                logger.debug("【处理】发现已存在成绩记录，将进行更新操作, 已有记录ID: {}", existingResult.getResultId());
                // 更新已存在记录
                existingResult.setFinalScore(sysResult.getFinalScore());
                existingResult.setUpdateTime(DateUtils.getNowDate());
                rows = sysResultMapper.updateSysResult(existingResult);
                logger.info("【结束】更新成绩记录成功: {}, 影响行数: {}", existingResult, rows);
                return rows;
            } else {
                logger.debug("【处理】不存在成绩记录，将进行插入操作");
                // 插入新记录前先填充额外信息
                fillResultExtraInfo(sysResult);

                // 插入新记录
                rows = sysResultMapper.insertSysResult(sysResult);
                logger.info("【结束】插入成绩记录成功: {}, 影响行数: {}", sysResult, rows);
                return rows;
            }
        } catch (ServiceException e) {
            logger.error("【异常】新增成绩结果失败(业务异常): {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【异常】新增或更新成绩结果失败, 详情:", e);
            throw new ServiceException("新增或更新成绩结果失败", 500, e.getMessage());
        }
    }

    /**
     * 更新成绩结果
     */
    @Override
    @Transactional
    public int updateSysResult(SysResult sysResult) {
        logger.info("【开始】更新成绩结果, sysResult: {}", sysResult);
        try {
            // 参数校验
            if (sysResult.getResultId() == null) {
                logger.warn("【错误】成绩ID不能为空");
                throw new ServiceException("成绩ID不能为空", 400);
            }

            sysResult.setUpdateTime(DateUtils.getNowDate());
            // 确保 finalScore 保留两位小数
            if (sysResult.getFinalScore() != null) {
                double originalScore = sysResult.getFinalScore();
                double roundedScore = Math.round(originalScore * 100) / 100.0;
                sysResult.setFinalScore(roundedScore);
                logger.debug("【处理】成绩四舍五入: {} -> {}", originalScore, roundedScore);
            }

            int rows = sysResultMapper.updateSysResult(sysResult);
            logger.info("【结束】更新成绩结果成功, 影响行数: {}", rows);
            return rows;
        } catch (ServiceException e) {
            logger.error("【异常】更新成绩结果失败(业务异常): {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【异常】更新成绩结果失败, sysResult: {}, 详情:", sysResult, e);
            throw new ServiceException("更新成绩结果失败", 500, e.getMessage());
        }
    }

    /**
     * 批量删除成绩结果
     */
    @Override
    @Transactional
    public int deleteSysResultByResultIds(List<Long> resultIds) {
        logger.info("【开始】批量删除成绩结果, resultIds: {}", resultIds);
        try {
            if (resultIds == null || resultIds.isEmpty()) {
                logger.warn("【错误】删除的成绩ID列表不能为空");
                throw new ServiceException("删除的成绩ID不能为空", 400);
            }

            int rows = sysResultMapper.deleteSysResultByResultIds(resultIds);
            logger.info("【结束】批量删除成绩结果成功, 影响行数: {}", rows);
            return rows;
        } catch (ServiceException e) {
            logger.error("【异常】批量删除成绩结果失败(业务异常): {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【异常】批量删除成绩结果失败, resultIds: {}, 详情:", resultIds, e);
            throw new ServiceException("批量删除成绩结果失败", 500, e.getMessage());
        }
    }

    /**
     * 删除成绩结果信息
     */
    @Override
    @Transactional
    public int deleteSysResultByResultId(Long resultId) {
        logger.info("【开始】删除成绩结果信息, resultId: {}", resultId);
        try {
            if (resultId == null) {
                logger.warn("【错误】删除的成绩ID不能为空");
                throw new ServiceException("删除的成绩ID不能为空", 400);
            }

            int rows = sysResultMapper.deleteSysResultByResultId(resultId);
            logger.info("【结束】删除成绩结果信息成功, 影响行数: {}", rows);
            return rows;
        } catch (ServiceException e) {
            logger.error("【异常】删除成绩结果信息失败(业务异常): {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("【异常】删除成绩结果信息失败, resultId: {}, 详情:", resultId, e);
            throw new ServiceException("删除成绩结果信息失败", 500, e.getMessage());
        }
    }

    /**
     * 计算加权平均分
     */
    @SafeVarargs
    public final double calculateWeightedAverage(List<Map.Entry<Double, Double>>... scoreWeights) {
        logger.debug("【开始】计算加权平均分");
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;
        int groupCount = 0;

        for (List<Map.Entry<Double, Double>> scoreWeightList : scoreWeights) {
            if (scoreWeightList == null) {
                logger.debug("【处理】第{}组权重列表为空，跳过", groupCount+1);
                continue; // 防止空列表导致NPE
            }

            logger.debug("【处理】处理第{}组权重列表，包含{}项", ++groupCount, scoreWeightList.size());
            int entryCount = 0;

            for (Map.Entry<Double, Double> entry : scoreWeightList) {
                if (entry == null || entry.getKey() == null) {
                    logger.debug("【处理】第{}组第{}项为空，跳过", groupCount, ++entryCount);
                    continue; // 跳过空条目
                }
                double score = entry.getKey();
                double weight = (entry.getValue() != null) ? entry.getValue() : 0.0;

                totalWeightedScore += score * weight;
                totalWeight += weight;
                logger.debug("【处理】第{}组第{}项: 分数={}, 权重={}, 当前累计加权分={}, 当前累计权重={}",
                        groupCount, ++entryCount, score, weight, totalWeightedScore, totalWeight);
            }
        }

        // 避免除零错误
        if (totalWeight == 0.0) {
            logger.debug("【结束】总权重为0，返回0分");
            return 0.0;
        }

        double result = totalWeightedScore / totalWeight;
        logger.debug("【结束】计算加权平均分完成: {}", result);
        return result;
    }

    // -------------------- 核心处理方法 --------------------

    /**
     * 处理报名数据：查询报名、计算平均分、根据是否已有成绩记录进行新增或更新
     */
    private void processResults() {
        logger.info("【开始】处理所有竞赛的报名数据");

        try {
            // 查询所有竞赛
            logger.debug("【处理】查询所有竞赛");
            List<SysComp> competitions = sysCompMapper.selectSysCompList(new SysComp(), null);
            if (competitions == null || competitions.isEmpty()) {
                logger.warn("【提示】没有找到任何竞赛数据");
                return;
            }
            logger.debug("【处理】找到{}个竞赛", competitions.size());

            for (SysComp comp : competitions) {
                Long compId = comp.getCompId();
                if (compId == null) {
                    logger.warn("【错误】竞赛数据缺少竞赛ID，跳过该竞赛: {}", comp);
                    continue;
                }
                logger.debug("【处理】开始处理竞赛: ID={}, 名称={}", compId, comp.getCompName());
                calculateAndUpdateCompetitionResults(compId);
                logger.debug("【处理】竞赛处理完成: ID={}, 名称={}", compId, comp.getCompName());
            }

            logger.info("【结束】所有竞赛的报名数据处理完成");
        } catch (Exception e) {
            logger.error("【异常】处理报名数据时发生异常:", e);
            throw e; // 向上层传递异常，便于定位
        }
    }

    /**
     * 计算并更新特定竞赛的所有参赛者成绩
     */
    private void calculateAndUpdateCompetitionResults(Long compId) {
        logger.info("【开始】处理竞赛ID={}的成绩", compId);

        // 查询该竞赛下的所有参赛者
        logger.debug("【处理】查询竞赛下所有参赛者, compId={}", compId);
        List<SysRegistr> registrList = sysRegistrMapper.selectSysRegistrListByCompId(compId);
        if (registrList == null || registrList.isEmpty()) {
            logger.warn("【提示】竞赛ID={} 没有参赛者数据", compId);
            return;
        }
        logger.debug("【处理】找到{}名参赛者, compId={}", registrList.size(), compId);

        // 查询该竞赛下的所有评分数据
        logger.debug("【处理】查询竞赛下所有评分数据, compId={}", compId);
        List<SysScore> scores = sysScoreMapper.selectScoresByCompId(compId);
        if (scores == null || scores.isEmpty()) {
            logger.warn("【提示】竞赛ID={} 没有评分数据", compId);
            return;
        }
        logger.debug("【处理】找到{}条评分记录, compId={}", scores.size(), compId);

        // 按参赛者ID分组处理评分
        logger.debug("【处理】按参赛者ID分组评分数据");
        Map<Long, List<SysScore>> scoresByRegistrId = scores.stream()
                .collect(Collectors.groupingBy(SysScore::getRegistrId));
        logger.debug("【处理】评分数据分组完成，共{}个分组", scoresByRegistrId.size());

        // 为每个参赛者计算成绩
        int processedCount = 0;
        int successCount = 0;
        int errorCount = 0;

        for (SysRegistr registr : registrList) {
            Long registrId = registr.getRegistrId();
            Long userId = registr.getUserId();
            processedCount++;

            logger.debug("【处理】处理第{}个参赛者, registrId={}, userId={}",
                    processedCount, registrId, userId);

            try {
                processParticipantScores(compId, registrId, userId, scoresByRegistrId.get(registrId));
                successCount++;
                logger.debug("【处理】参赛者成绩处理成功, registrId={}", registrId);
            } catch (Exception e) {
                errorCount++;
                logger.error("【异常】处理参赛者ID={}的成绩时发生错误:", registrId, e);
                // 继续处理下一个参赛者
            }
        }

        logger.info("【结束】竞赛ID={}的成绩处理完成, 总计处理参赛者:{}, 成功:{}, 失败:{}",
                compId, processedCount, successCount, errorCount);
    }

    /**
     * 处理单个参赛者的评分并更新成绩
     */
    private void processParticipantScores(Long compId, Long registrId, Long userId, List<SysScore> participantScores) {
        logger.info("【开始】处理参赛者评分数据, compId={}, registrId={}, userId={}", compId, registrId, userId);

        // 检查该参赛者是否有评分数据
        if (participantScores == null || participantScores.isEmpty()) {
            logger.info("【提示】参赛者ID={} 没有评分数据，跳过", registrId);
            return;
        }
        logger.debug("【处理】找到{}条评分记录", participantScores.size());

        // 计算平均分
        double totalScore = 0.0;
        int validScoreCount = 0;
        int invalidCount = 0;

        for (SysScore score : participantScores) {
            if (score != null && score.getScore() != null) {
                totalScore += score.getScore();
                validScoreCount++;
                logger.debug("【处理】有效评分: scoreId={}, score={}, 当前总分={}, 有效计数={}",
                        score.getScoreId(), score.getScore(), totalScore, validScoreCount);
            } else {
                invalidCount++;
                logger.debug("【处理】无效评分记录, 忽略");
            }
        }

        // 避免除零错误
        if (validScoreCount == 0) {
            logger.warn("【提示】参赛者ID={} 没有有效的评分数据, 有效评分数=0, 无效评分数={}",
                    registrId, invalidCount);
            return;
        }

        double finalScore = Math.round((totalScore / validScoreCount) * 100.0) / 100.0;
        logger.info("【处理】参赛者ID={}, 计算得到的平均分={}, 总分={}, 有效评分数={}, 无效评分数={}",
                registrId, finalScore, totalScore, validScoreCount, invalidCount);

        try {
            updateParticipantResult(compId, registrId, userId, finalScore);
            logger.info("【结束】参赛者评分处理完成, registrId={}, finalScore={}", registrId, finalScore);
        } catch (Exception e) {
            logger.error("【异常】更新参赛者成绩失败:", e);
            throw e; // 向上抛出异常，便于调用方处理
        }
    }

    /**
     * 更新参赛者成绩记录
     */
    private void updateParticipantResult(Long compId, Long registrId, Long userId, double finalScore) {
        logger.info("【开始】更新参赛者成绩记录, compId={}, registrId={}, userId={}, finalScore={}",
                compId, registrId, userId, finalScore);

        // 检查是否已存在成绩记录
        logger.debug("【处理】检查是否已存在成绩记录");
        SysResult existingResult = sysResultMapper.selectSysResultByRegistrId(registrId);
        if (existingResult != null) {
            logger.debug("【处理】找到已存在的成绩记录: {}", existingResult);
        } else {
            logger.debug("【处理】不存在成绩记录，需要创建新记录");
        }

        try {
            // 获取报名信息，提取部门ID以及用户名
            logger.debug("【处理】获取报名记录信息, registrId={}", registrId);
            SysRegistr registr = sysRegistrMapper.selectSysRegistrByRegistrId(registrId);
            Long deptId = null;
            String userName = null;
            String deptName = null;

            if (registr != null) {
                deptId = registr.getDeptId();
                userName = registr.getUserName();
                logger.debug("【处理】从报名记录获取信息: 部门ID={}, 用户名={}, 报名ID={}",
                        deptId, userName, registrId);

                // 获取部门名称
                if (deptId != null) {
                    logger.debug("【处理】获取部门信息, deptId={}", deptId);
                    SysDept dept = sysDeptMapper.selectDeptById(deptId);
                    if (dept != null) {
                        deptName = dept.getDeptName();
                        logger.debug("【处理】获取到部门名称: {}, 部门ID: {}", deptName, deptId);
                    } else {
                        deptName = "未知部门";
                        logger.warn("【提示】未找到部门信息，设置为默认值: {}, 部门ID: {}", deptName, deptId);
                    }
                } else {
                    logger.warn("【提示】报名记录中部门ID为空, registrId={}", registrId);
                }
            } else {
                logger.warn("【提示】未找到报名记录信息, 报名ID: {}", registrId);
            }

            // 获取竞赛名称
            logger.debug("【处理】获取竞赛信息, compId={}", compId);
            String compName = null;
            SysComp comp = sysCompMapper.selectSysCompByCompId(compId);
            if (comp != null) {
                compName = comp.getCompName();
                logger.debug("【处理】获取到竞赛名称: {}, 竞赛ID: {}", compName, compId);
            } else {
                logger.warn("【提示】未找到竞赛信息, 竞赛ID: {}", compId);
            }

            if (existingResult == null) {
                // 新增成绩记录
                logger.debug("【处理】创建新的成绩记录");
                SysResult newResult = new SysResult();
                Long newResultId = IdGenerator.generateId(0);
                newResult.setResultId(newResultId);
                newResult.setCompId(compId);
                newResult.setRegistrId(registrId);
                newResult.setUserId(userId);
                newResult.setDeptId(deptId); // 设置部门ID
                newResult.setUserName(userName); // 设置用户名
                newResult.setCompName(compName); // 设置竞赛名称
                newResult.setDeptName(deptName); // 设置部门名称
                newResult.setFinalScore(Math.round(finalScore * 100) / 100.0);
                newResult.setCreateTime(DateUtils.getNowDate());
                newResult.setUpdateTime(DateUtils.getNowDate());
                newResult.setStatus('0'); // 默认状态为0
                newResult.setDelFlag('0');

                logger.debug("【处理】准备插入新记录: {}", newResult);
                int rows = sysResultMapper.insertSysResult(newResult);
                logger.info("【结束】成功插入新成绩记录: resultId={}, 用户={}, 竞赛={}, 部门={}, 分数={}, 影响行数={}",
                    newResultId, userName, compName, deptName, newResult.getFinalScore(), rows);
            } else {
                // 更新已存在的成绩记录
                logger.debug("【处理】更新已存在的成绩记录");
                double oldScore = existingResult.getFinalScore() != null ? existingResult.getFinalScore() : 0.0;
                existingResult.setFinalScore(Math.round(finalScore * 100) / 100.0);
                existingResult.setUpdateTime(DateUtils.getNowDate());
                logger.debug("【处理】成绩变更: {} -> {}", oldScore, existingResult.getFinalScore());

                // 如果原记录缺少相关信息但现在能获取到，则更新
                boolean updated = false;

                if (existingResult.getDeptId() == null && deptId != null) {
                    existingResult.setDeptId(deptId);
                    updated = true;
                    logger.debug("【处理】更新成绩记录的部门ID: {}", deptId);
                }

                if (existingResult.getUserName() == null && userName != null) {
                    existingResult.setUserName(userName);
                    updated = true;
                    logger.debug("【处理】更新成绩记录的用户名: {}", userName);
                }

                if (existingResult.getCompName() == null && compName != null) {
                    existingResult.setCompName(compName);
                    updated = true;
                    logger.debug("【处理】更新成绩记录的竞赛名称: {}", compName);
                }

                if (existingResult.getDeptName() == null && deptName != null) {
                    existingResult.setDeptName(deptName);
                    updated = true;
                    logger.debug("【处理】更新成绩记录的部门名称: {}", deptName);
                }

                if (updated) {
                    logger.debug("【处理】成绩记录额外信息已更新");
                }

                logger.debug("【处理】准备更新成绩记录: {}", existingResult);
                int rows = sysResultMapper.updateSysResult(existingResult);
                logger.info("【结束】成功更新成绩记录: resultId={}, 用户={}, 竞赛={}, 部门={}, 分数={}, 影响行数={}",
                    existingResult.getResultId(), existingResult.getUserName(), existingResult.getCompName(),
                    existingResult.getDeptName(), existingResult.getFinalScore(), rows);
            }
        } catch (Exception e) {
            logger.error("【异常】操作数据库更新成绩记录失败:", e);
            throw e;
        }
    }

    /**
     * 计算参赛者的成绩并更新
     */
    private void calculateAndUpdateResult(Long registrId) {
        logger.info("【开始】触发成绩计算, registrId: {}", registrId);
        try {
            // 获取所有该参赛者的评分记录
            logger.debug("【处理】查询参赛者的所有评分记录, registrId={}", registrId);
            List<SysScore> scoreList = sysScoreMapper.selectScoresByRegistrId(registrId);

            if (scoreList == null || scoreList.isEmpty()) {
                logger.warn("【提示】没有找到评分记录，无法计算成绩, registrId: {}", registrId);
                return;
            }
            logger.debug("【处理】找到{}条评分记录", scoreList.size());

            // 计算得分
            double totalScore = 0.0;
            int validScoreCount = 0;
            int invalidCount = 0;

            for (SysScore score : scoreList) {
                if (score.getScore() != null) {
                    totalScore += score.getScore();
                    validScoreCount++;
                    logger.debug("【处理】有效评分: scoreId={}, score={}, 当前总分={}, 有效计数={}",
                            score.getScoreId(), score.getScore(), totalScore, validScoreCount);
                } else {
                    invalidCount++;
                    logger.debug("【处理】无效评分记录, 忽略");
                }
            }

            if (validScoreCount == 0) {
                logger.warn("【提示】没有有效的评分记录, registrId: {}, 无效评分数={}", registrId, invalidCount);
                return;
            }

            // 计算平均分
            double finalScore = totalScore / validScoreCount;
            double roundedScore = Math.round(finalScore * 100) / 100.0;
            logger.info("【处理】计算得到的平均分: {} (四舍五入后: {}), registrId: {}, 总分={}, 有效评分数={}, 无效评分数={}",
                    finalScore, roundedScore, registrId, totalScore, validScoreCount, invalidCount);

            // 获取报名信息
            logger.debug("【处理】获取报名信息, registrId={}", registrId);
            SysRegistr sysRegistr = sysRegistrMapper.selectSysRegistrByRegistrId(registrId);
            if (sysRegistr == null) {
                logger.error("【错误】报名信息不存在, registrId: {}", registrId);
                return;
            }
            logger.debug("【处理】报名信息获取成功: userId={}, compId={}", sysRegistr.getUserId(), sysRegistr.getCompId());

            // 构建成绩结果对象
            SysResult sysResult = new SysResult();
            sysResult.setCompId(sysRegistr.getCompId());
            sysResult.setUserId(sysRegistr.getUserId());
            sysResult.setRegistrId(registrId);
            sysResult.setFinalScore(roundedScore);
            logger.debug("【处理】构建成绩结果对象: {}", sysResult);

            // 调用结果服务保存成绩
            insertSysResult(sysResult);
            logger.info("【结束】成绩更新完成, registrId: {}", registrId);
        } catch (Exception e) {
            logger.error("【异常】计算更新成绩失败:", e);
            // 这里不抛出异常，避免影响主流程
        }
    }

    /**
     * 填充成绩结果的额外信息
     */
    private void fillResultExtraInfo(SysResult result) {
        if (result == null) {
            logger.debug("【跳过】填充额外信息时发现结果对象为空");
            return;
        }

        logger.debug("【开始】填充成绩结果额外信息: resultId={}, registrId={}, compId={}",
                result.getResultId(), result.getRegistrId(), result.getCompId());

        try {
            // 查询报名
            Long registrId = result.getRegistrId();
            if (registrId != null) {
                logger.debug("【处理】查询报名信息, registrId={}", registrId);
                SysRegistr registr = sysRegistrMapper.selectSysRegistrByRegistrId(registrId);
                if (registr != null) {
                    result.setUserName(registr.getUserName());
                    logger.debug("【处理】设置用户名: {}", registr.getUserName());

                    // 根据报名中的部门id获得部门名称再设置到result中
                    Long deptId = registr.getDeptId();
                    if (deptId != null) {
                        logger.debug("【处理】查询部门信息, deptId={}", deptId);
                        SysDept dept = sysDeptMapper.selectDeptById(deptId);
                        if (dept != null) {
                            result.setDeptName(dept.getDeptName());
                            logger.debug("【处理】设置部门名称: {}", dept.getDeptName());
                        } else {
                            result.setDeptName("未知部门");
                            logger.warn("【提示】未找到部门信息, 设置为默认值: 未知部门, deptId={}", deptId);
                        }
                    } else {
                        logger.warn("【提示】报名记录没有部门ID, registrId={}", registrId);
                    }
                } else {
                    logger.warn("【提示】未找到报名记录, registrId={}", registrId);
                }
            } else {
                logger.warn("【提示】成绩结果没有报名ID, resultId={}", result.getResultId());
            }

            // 查询竞赛信息
            Long compId = result.getCompId();
            if (compId != null) {
                logger.debug("【处理】查询竞赛信息, compId={}", compId);
                SysComp comp = sysCompMapper.selectSysCompByCompId(compId);
                if (comp != null) {
                    result.setCompName(comp.getCompName());
                    logger.debug("【处理】设置竞赛名称: {}", comp.getCompName());
                } else {
                    logger.warn("【提示】未找到竞赛信息, compId={}", compId);
                }
            } else {
                logger.warn("【提示】成绩结果没有竞赛ID, resultId={}", result.getResultId());
            }

            logger.debug("【结束】成绩结果额外信息填充完成: {}", result);
        } catch (Exception e) {
            logger.error("【异常】填充成绩额外信息失败, resultId={}", result.getResultId(), e);
            // 捕获异常但不抛出，避免影响主流程
        }
    }
}
