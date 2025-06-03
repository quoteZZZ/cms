package com.cms.system.service.impl;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

import com.cms.common.constant.CacheConstants;
import com.cms.common.core.domain.model.LoginUser;
import com.cms.common.exception.ServiceException;
import com.cms.common.utils.DateUtils;
import com.cms.common.utils.SecurityUtils;
import com.cms.common.utils.uuid.IdGenerator;
import com.cms.common.core.domain.entity.SysUserComp;
import com.cms.system.mapper.SysUserCompMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import com.cms.system.mapper.SysCompMapper;
import com.cms.common.core.domain.entity.SysComp;
import com.cms.system.service.ISysCompService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cms.common.redis.RedisCacheUtil;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import com.cms.system.service.ISysDeptService;
import com.cms.common.core.domain.entity.SysDept;
import com.cms.system.mapper.SysRoleDeptMapper;
import com.cms.common.core.domain.entity.SysRoleDept;
import com.cms.system.service.ISysUserService;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.system.service.ISysUserCompService;

/**
 * 竞赛信息Service业务层处理
 */
@Service
public class SysCompServiceImpl implements ISysCompService {

    @Resource(name = "threadPoolTaskExecutor")
    private Executor asyncExecutor; // 引入线程池

    @Resource
    private SysCompMapper sysCompMapper;

    @Resource
    private SysUserCompMapper userCompMapper;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ISysDeptService deptService; // 注入ISysDeptService

    @Resource
    private SysRoleDeptMapper roleDeptMapper; // 注入SysRoleDeptMapper

    @Resource
    private ISysUserService sysUserService; // 注入ISysUserService

    @Resource
    private ISysUserCompService userCompService; // 注入ISysUserCompService

    // 定义日志记录器
    Logger logger = LoggerFactory.getLogger(SysCompServiceImpl.class);

    // ========== 初始化方法 ==========
    /**
     * 系统启动时缓存预热
     *
     * 功能描述：
     * 1. 加载热点竞赛数据到缓存
     * 2. 使用异步线程执行，避免阻塞主线程
     * 3. 采用线程池处理，改进预热性能
     */
    @PostConstruct
    public void cachePreheat() {
        // 使用线程池而不是直接创建新线程
        asyncExecutor.execute(() -> {
            try {
                logger.info("开始执行竞赛数据缓存预热...");
                List<Long> hotCompIds = sysCompMapper.selectHotCompIds();
                logger.info("获取热门竞赛ID列表：{}", hotCompIds);

                // 创建新列表存储有效compId
                List<Long> validCompIds = new ArrayList<>();
                int preheatedCount = 0;

                for (Long compId : hotCompIds) {
                    String cacheKey = CacheConstants.COMP_INFO_KEY + compId;

                    if (!redisCacheUtil.hasKey(cacheKey)) {
                        SysComp sysComp = sysCompMapper.selectSysCompByCompId(compId);
                        if (sysComp != null && sysComp.getCompId() != null) {
                            // 使用基础TTL + 随机偏移，防止雪崩
                            int ttl = CacheConstants.DEFAULT_CACHE_TTL + new Random().nextInt(300);
                            redisCacheUtil.setCacheObject(cacheKey, sysComp, ttl, TimeUnit.SECONDS);
                            validCompIds.add(compId);
                            preheatedCount++;
                        } else {
                            logger.warn("竞赛信息为空或已失效，compId: {}，将从热门列表移除", compId);
                            // 删除无效compId
                            sysCompMapper.deleteSysCompByCompId(compId);
                        }
                    } else {
                        validCompIds.add(compId);
                        logger.debug("竞赛 {} 已在缓存中，跳过预热", compId);
                    }
                }

                logger.info("竞赛数据缓存预热完成，成功预热 {} 条数据", preheatedCount);

                // 预热常用的竞赛列表查询
                preheatCommonQueries();

            } catch (Exception e) {
                logger.error("缓存预热失败", e);
            }
        });
    }

    /**
     * 预热常用查询条件的列表缓存
     */
    private void preheatCommonQueries() {
        try {
            // 预热全部竞赛列表
            SysComp queryCond = new SysComp();
            queryCond.setStatus('0'); // 正常状态
            List<SysComp> allComps = sysCompMapper.selectSysCompList(queryCond, "comp_id DESC");
            String allCacheKey = CacheConstants.COMP_LIST_KEY + ":all";
            if (allComps != null && !allComps.isEmpty()) {
                int ttl = CacheConstants.DEFAULT_CACHE_TTL + new Random().nextInt(300);
                redisCacheUtil.setCacheList(allCacheKey, allComps, ttl, TimeUnit.SECONDS);
                logger.info("预热全部竞赛列表缓存，共 {} 条数据", allComps.size());
            }

            // 预热各类别的竞赛列表
            for (char category = '1'; category <= '5'; category++) {
                SysComp categoryQuery = new SysComp();
                categoryQuery.setStatus('0');
                categoryQuery.setCompCategory(category);
                List<SysComp> categoryComps = sysCompMapper.selectSysCompList(categoryQuery, "comp_id DESC");
                if (categoryComps != null && !categoryComps.isEmpty()) {
                    String categoryCacheKey = CacheConstants.COMP_LIST_KEY + ":category:" + category;
                    int ttl = CacheConstants.DEFAULT_CACHE_TTL + new Random().nextInt(300);
                    redisCacheUtil.setCacheList(categoryCacheKey, categoryComps, ttl, TimeUnit.SECONDS);
                    logger.info("预热类别 {} 竞赛列表缓存，共 {} 条数据", category, categoryComps.size());
                }
            }

            // 预热推荐竞赛
            List<SysComp> recommendedComps = recommendCompetitions("latest", null, 10);
            logger.info("预热推荐竞赛列表缓存，共 {} 条数据", recommendedComps.size());

        } catch (Exception e) {
            logger.error("预热常用查询缓存失败", e);
        }
    }

    // ========== 查询相关方法 ==========
    /**
     * 根据id查询竞赛信息
     *
     * 功能描述：
     * 1. 缓存空值处理防止穿透
     * 2. 随机TTL防止雪崩
     * 3. 逻辑过期策略实现异步更新
     * 4. 旁路缓存模式处理缓存缺失
     * 5. 使用分布式锁防止击穿
     *
     * @param compId 竞赛ID，不能为空
     * @return 竞赛信息
     * @throws ServiceException 如果 compId 为 null 或查询失败
     */
    @Override
    public SysComp selectSysCompByCompId(Long compId) {
        // 参数校验
        if (compId == null) {
            throw new ServiceException("竞赛ID不能为空", 400);
        }

        // 构建缓存键
        String cacheKey = CacheConstants.COMP_INFO_KEY + compId;

        long startTime = System.currentTimeMillis();
        boolean isCacheHit = true;

        try {
            // 1. 旁路缓存模式：先查缓存
            Object cacheObject = redisCacheUtil.getCacheObject(cacheKey);

            // 命中缓存：正常数据或空值标记
            if (cacheObject != null) {
                // 判断是否是空值标记
                if (cacheObject instanceof String && "".equals(cacheObject)) {
                    // 缓存了空值（防止缓存穿透）
                    logger.debug("竞赛信息缓存命中空值标记, compId: {}", compId);
                    return null;
                } else if (cacheObject instanceof SysComp) {
                    SysComp sysComp = (SysComp) cacheObject;

                    // 检查逻辑过期时间，如果快过期则进行异步更新
                    Long ttl = redisCacheUtil.getExpire(cacheKey);
                    if (ttl != null && ttl < CacheConstants.LOGICAL_EXPIRE_CHECK_WINDOW) {
                        // 异步更新缓存，不阻塞当前请求
                        asyncUpdateCache(cacheKey, compId);
                    }

                    logger.debug("竞赛信息缓存命中, compId: {}, 耗时: {}ms", compId, System.currentTimeMillis() - startTime);
                    return sysComp;
                }
            }

            // 缓存未命中，需要查询数据库
            isCacheHit = false;

            // 2. 使用分布式锁防止缓存击穿
            // 对于热点数据，可能会同时有大量请求同时查询数据库
            RLock lock = redissonClient.getLock(CacheConstants.COMP_LOCK_KEY + compId);
            boolean locked = false;

            try {
                // 尝试获取锁，最多等待500ms，持有锁CacheConstants.LOCK_TIMEOUT秒
                locked = lock.tryLock(500, CacheConstants.LOCK_TIMEOUT * 1000, TimeUnit.MILLISECONDS);

                if (locked) {
                    // 双重检查，防止其他线程已经重建了缓存
                    Object recheck = redisCacheUtil.getCacheObject(cacheKey);
                    if (recheck != null) {
                        if (recheck instanceof String && "".equals(recheck)) {
                            return null;
                        } else if (recheck instanceof SysComp) {
                            return (SysComp) recheck;
                        }
                    }

                    // 3. 从数据库获取竞赛信息
                    logger.info("竞赛信息缓存未命中，查询数据库, compId: {}", compId);
                    SysComp sysComp = sysCompMapper.selectSysCompByCompId(compId);

                    // 4. 将查询结果存入缓存
                    if (sysComp != null) {
                        // 使用随机TTL防止缓存雪崩
                        int randomTtl = CacheConstants.DEFAULT_CACHE_TTL + new Random().nextInt(300);
                        redisCacheUtil.setCacheObject(cacheKey, sysComp, randomTtl, TimeUnit.SECONDS);
                        logger.info("竞赛信息已加入缓存, compId: {}, TTL: {}秒", compId, randomTtl);
                        return sysComp;
                    } else {
                        // 5. 缓存空值防止缓存穿透
                        // 对于不存在的数据，缓存空值，但TTL较短
                        redisCacheUtil.setCacheObject(cacheKey, "", CacheConstants.EMPTY_CACHE_TTL, TimeUnit.SECONDS);
                        logger.info("竞赛信息不存在，已缓存空值, compId: {}, TTL: {}秒", compId, CacheConstants.EMPTY_CACHE_TTL);
                        return null;
                    }
                } else {
                    // 获取锁失败，降级处理：直接查询数据库
                    logger.warn("获取竞赛信息分布式锁失败，直接查询数据库, compId: {}", compId);
                    return sysCompMapper.selectSysCompByCompId(compId);
                }
            } finally {
                // 释放锁
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            logger.error("查询竞赛信息异常, compId: {}", compId, e);

            // 出现异常时降级处理：直接查询数据库
            try {
                return sysCompMapper.selectSysCompByCompId(compId);
            } catch (Exception ex) {
                throw new ServiceException("查询竞赛信息失败", 500, e.getMessage());
            }
        } finally {
            // 记录查询耗时
            long costTime = System.currentTimeMillis() - startTime;
            if (costTime > 200) {
                logger.warn("查询竞赛信息耗时较长: {}ms, 缓存{}, compId: {}",
                        costTime, isCacheHit ? "命中" : "未命中", compId);
            }
        }
    }

    /**
     * 查询竞赛信息列表
     *
     * 功能描述：
     * 1. 生成基于查询条件的结构化缓存键
     * 2. 缓存空列表防止穿透
     * 3. 使用分布式锁防止击穿
     * 4. 随机TTL防止缓存雪崩
     * 5. 性能监控与日志记录
     * 6. 保留分页信息以修复分页问题
     *
     * @param sysComp 竞赛信息查询条件
     * @param order 排序参数
     * @return 竞赛信息列表（永不为null，空时返回空列表）
     */
    @Override
    public List<SysComp> selectSysCompList(SysComp sysComp, String order) {
        // 记录查询开始时间
        long startTime = System.currentTimeMillis();
        boolean isCacheHit = true;

        try {
            // 获取当前线程的分页信息
            com.github.pagehelper.Page<Object> page = com.github.pagehelper.PageHelper.getLocalPage();
            boolean isPaging = page != null;

            if (isPaging) {
                // 如果启用了分页，则直接查询数据库以确保分页正确
                logger.debug("检测到分页请求，跳过缓存直接查询数据库，页码: {}, 每页大小: {}",
                        page.getPageNum(), page.getPageSize());
                List<SysComp> result = sysCompMapper.selectSysCompList(sysComp, order);
                return result != null ? result : Collections.emptyList();
            }

            // 如果未分页，走原有缓存逻辑
            // 如果 order 为空，默认按 comp_id 排序
            if (order == null || order.isEmpty()) {
                order = "comp_id DESC";
            }

            // 设置 status 默认值为 '0'，确保查询条件一致性
            if (sysComp == null) {
                sysComp = new SysComp();
                sysComp.setStatus('0');
            } else if (sysComp.getStatus() == null) {
                sysComp.setStatus('0');
            }

            // 1. 生成结构化缓存键
            String cacheKey = generateCacheKey(sysComp, order);
            logger.debug("生成查询缓存键: {}", cacheKey);

            // 2. 从缓存中获取数据 - 直接尝试获取Object类型，处理可能存储的空标记
            Object cachedObject = redisCacheUtil.getCacheObject(cacheKey);

            // 3. 缓存命中的情况处理
            if (cachedObject != null) {
                // 处理空值标记（字符串类型的空值标记）
                if (cachedObject instanceof String && "EMPTY_LIST".equals(cachedObject)) {
                    logger.debug("缓存命中空值标记，key: {}", cacheKey);
                    return Collections.emptyList();
                }
                // 处理正常List
                else if (cachedObject instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<SysComp> result = (List<SysComp>) cachedObject;
                    if (isValidCacheData(result)) {
                        logger.debug("缓存命中，key: {}, 结果数量: {}", cacheKey, result.size());
                        return result;
                    } else {
                        // 缓存数据格式无效，删除此缓存
                        logger.warn("缓存数据格式无效，删除缓存: {}", cacheKey);
                        redisCacheUtil.deleteObject(cacheKey);
                    }
                }
            }

            // 4. 缓存未命中或无效，需要查询数据库
            isCacheHit = false;

            // 使用分布式锁防止缓存击穿 (多个请求同时查询数据库)
            RLock lock = redissonClient.getLock("lock:complist:" + cacheKey.hashCode());
            boolean locked = false;

            try {
                // 尝试获取锁，最多等待300ms，持有锁5秒
                locked = lock.tryLock(300, 5000, TimeUnit.MILLISECONDS);

                if (locked) {
                    // 5. 双重检查，可能在等待锁的过程中其他线程已经重建了缓存
                    Object recheckCached = redisCacheUtil.getCacheObject(cacheKey);

                    if (recheckCached != null) {
                        if (recheckCached instanceof String && "EMPTY_LIST".equals(recheckCached)) {
                            return Collections.emptyList();
                        }
                        else if (recheckCached instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<SysComp> result = (List<SysComp>) recheckCached;
                            if (isValidCacheData(result)) {
                                logger.debug("锁等待期间缓存已重建，直接返回，key: {}", cacheKey);
                                return result;
                            }
                        }
                    }

                    // 6. 查询数据库
                    logger.info("缓存未命中，查询数据库, 查询条件: {}, 排序: {}", sysComp, order);
                    List<SysComp> result = sysCompMapper.selectSysCompList(sysComp, order);

                    // 7. 将结果写入缓存（特殊处理空结果）
                    if (result == null || result.isEmpty()) {
                        // 如果查询结果为空，使用特殊标记值存储
                        redisCacheUtil.setCacheObject(cacheKey, "EMPTY_LIST", CacheConstants.EMPTY_CACHE_TTL, TimeUnit.SECONDS);
                        logger.info("查询结果为空，缓存空标记, key: {}, TTL: {}秒",
                                cacheKey, CacheConstants.EMPTY_CACHE_TTL);
                        return Collections.emptyList();  // 返回空列表而非null
                    } else {
                        // 非空结果使用随机TTL防止缓存雪崩
                        int ttl = CacheConstants.DEFAULT_CACHE_TTL + new Random().nextInt(300);
                        redisCacheUtil.setCacheObject(cacheKey, result, ttl, TimeUnit.SECONDS);
                        logger.info("数据库查询结果已写入缓存, key: {}, 结果数: {}, TTL: {}秒",
                                cacheKey, result.size(), ttl);
                        return result;
                    }
                } else {
                    // 8. 获取锁失败，降级策略：直接查询数据库但不更新缓存
                    logger.warn("获取分布式锁失败，直接查询数据库, key: {}", cacheKey);
                    List<SysComp> result = sysCompMapper.selectSysCompList(sysComp, order);
                    return result != null ? result : Collections.emptyList();
                }
            } finally {
                // 释放锁
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            logger.error("查询竞赛列表异常, 条件: {}, 排序: {}, 错误: {}", sysComp, order, e.getMessage());

            // 出现异常时降级处理：直接查询数据库
            try {
                List<SysComp> result = sysCompMapper.selectSysCompList(sysComp, order);
                return result != null ? result : Collections.emptyList();
            } catch (Exception ex) {
                logger.error("降级查询也失败, 错误: {}", ex.getMessage());
                return Collections.emptyList(); // 确保返回空列表而不是null
            }
        } finally {
            // 记录查询耗时
            long costTime = System.currentTimeMillis() - startTime;

            // 根据耗时情况记录不同级别的日志
            if (costTime > 500) {
                logger.warn("查询竞赛列表耗时较长: {}ms, 缓存{}",
                        costTime, isCacheHit ? "命中" : "未命中");
            } else if (costTime > 200) {
                logger.info("查询竞赛列表耗时: {}ms, 缓存{}",
                        costTime, isCacheHit ? "命中" : "未命中");
            } else {
                logger.debug("查询竞赛列表耗时: {}ms, 缓存{}",
                        costTime, isCacheHit ? "命中" : "未命中");
            }
        }
    }

    /**
     * 生成基于查询条件的缓存键
     * @param sysComp 查询条件
     * @param order 排序字段
     * @return 缓存键
     */
    private String generateCacheKey(SysComp sysComp, String order) {
        // 构建更结构化的缓存键，使用格式：list:condition1:value1:condition2:value2...
        StringBuilder keyBuilder = new StringBuilder(CacheConstants.COMP_LIST_KEY);

        // 添加主要过滤条件标识
        if (sysComp.getCompId() != null) {
            keyBuilder.append(":compId:").append(sysComp.getCompId());
        }

        if (sysComp.getCompName() != null && !sysComp.getCompName().isEmpty()) {
            keyBuilder.append(":name:").append(DigestUtils.md5Hex(sysComp.getCompName()));
        }

        if (sysComp.getCompCategory() != null) {
            keyBuilder.append(":category:").append(sysComp.getCompCategory());
        }

        if (sysComp.getCompStatus() != null) {
            keyBuilder.append(":status:").append(sysComp.getCompStatus());
        }

        if (sysComp.getStatus() != null) {
            keyBuilder.append(":dataStatus:").append(sysComp.getStatus());
        }

        // 添加排序条件
        if (order != null && !order.isEmpty()) {
            keyBuilder.append(":order:").append(DigestUtils.md5Hex(order));
        }

        // 如果没有任何条件，添加一个标识表示查询全部
        if (keyBuilder.length() == CacheConstants.COMP_LIST_KEY.length()) {
            keyBuilder.append(":all");
        }

        return keyBuilder.toString();
    }

    /**
     * 判断缓存数据是否有效
     * @param data 缓存中的数据
     * @return 如果数据有效返回 true，否则返回 false
     */
    private boolean isValidCacheData(List<SysComp> data) {
        // 遍历缓存数据，检查是否存在无效的 SysComp 对象
        for (SysComp comp : data) {
            if (comp.getCompId() == null || comp.getCompName() == null || comp.getStatus() == null) {
                // 如果 compId 或 compName 或 status 为 null，表示缓存数据无效
                return false;
            }
        }
        return true;
    }



    // ========== CRUD操作相关方法 ==========
    /**
     * 新增竞赛信息
     *
     * 功能描述：
     * 1. 全面参数校验，确保必要字段不为空
     * 2. 生成唯一竞赛ID
     * 3. 设置默认值（创建信息、状态、推荐标志等）
     * 4. 创建关联部门
     * 5. 为评委角色创建与此部门的关联
     * 6. 执行数据库插入操作
     * 7. 清理相关缓存
     *
     * @param sysComp 竞赛信息，不能为空
     * @return 插入结果
     * @throws ServiceException 如果参数无效或插入失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertSysComp(SysComp sysComp) {
        // 参数校验 - 更全面的校验
        if (sysComp == null) {
            throw new ServiceException("竞赛信息不能为空", 400);
        }
        if (sysComp.getCompName() == null || sysComp.getCompName().trim().isEmpty()) {
            throw new ServiceException("竞赛名称不能为空", 400);
        }
        if (sysComp.getCompCategory() == null) {
            throw new ServiceException("竞赛类别不能为空", 400);
        }
        if (sysComp.getCompMode() == null) {
            throw new ServiceException("竞赛模式不能为空", 400);
        }

        logger.info("开始创建新竞赛: {}", sysComp.getCompName());

        try {
            // 1. 生成唯一竞赛ID
            Long compId = IdGenerator.generateId(0);
            sysComp.setCompId(compId);
            logger.debug("为竞赛生成ID: {}", compId);

            // 2. 设置默认值和创建信息
            Date now = DateUtils.getNowDate();
            sysComp.setCreateTime(now);
            sysComp.setUpdateTime(now);

            // 设置创建者为当前登录用户，如果为空则使用系统账号

            String creator = sysComp.getCreateBy();
            if (creator == null || creator.trim().isEmpty()) {
                // 尝试从安全上下文获取当前登录用户
                LoginUser currentUser = SecurityUtils.getLoginUser();
                if (currentUser != null && currentUser.getUsername() != null) {
                    creator = currentUser.getUsername();
                } else {
                    creator = "system"; // 如果没有登录用户，则使用系统账号
                    logger.warn("未指定创建者，使用默认系统账号: {}", creator);
                }

            }
            sysComp.setCreateBy(creator);

            // 3. 设置默认值
            sysComp.setAccessFrequency(0); // 初始化访问频率为0

            // 设置默认状态（如果未指定）
            if (sysComp.getCompStatus() == null) {
                sysComp.setCompStatus('0'); // 默认未开始
            }

            if (sysComp.getStageStatus() == null) {
                sysComp.setStageStatus('0'); // 默认为报名阶段
            }

            if (sysComp.getIsRecommended() == null) {
                sysComp.setIsRecommended('0'); // 默认不推荐
            }

            if (sysComp.getStatus() == null) {
                sysComp.setStatus('0'); // 默认正常状态
            }

            if (sysComp.getDelFlag() == null) {
                sysComp.setDelFlag('0'); // 默认未删除
            }

            // 4. 创建关联部门
            SysDept dept = createCompetitionDepartment(sysComp);

            // 确保部门ID已成功生成
            if (dept.getDeptId() == null) {
                throw new ServiceException("创建部门失败，无法获取部门ID");
            }

            sysComp.setDeptId(dept.getDeptId()); // 保存新创建的部门ID
            logger.info("为竞赛创建关联部门成功，竞赛ID: {}，部门ID: {}", compId, dept.getDeptId());

            // 5. 为评委角色(roleId=3)创建与此部门的关联
            createRoleDeptRelation(dept.getDeptId());

            // 6. 执行数据库插入操作
            int result = sysCompMapper.insertSysComp(sysComp);
            if (result <= 0) {
                throw new ServiceException("插入竞赛信息到数据库失败");
            }

            // 7. 清理相关缓存
            clearCompCache(sysComp.getCompId());

            logger.info("竞赛创建成功: ID={}, 名称={}", sysComp.getCompId(), sysComp.getCompName());
            return result;
        } catch (Exception e) {
            // 记录详细的错误信息和堆栈跟踪
            logger.error("新增竞赛信息失败: {}", e.getMessage(), e);
            throw new ServiceException("新增竞赛信息失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 创建竞赛关联部门
     *
     * @param sysComp 竞赛信息
     * @return 创建的部门对象
     */
    private SysDept createCompetitionDepartment(SysComp sysComp) {
        SysDept dept = new SysDept();
        dept.setDeptId(IdGenerator.generateId(0)); // 生成部门ID
        dept.setDeptName(sysComp.getCompName());
        dept.setParentId(1L); // 直接挂在竞赛管理系统部门下

        // 设置部门负责人信息
        String leaderUsername = sysComp.getCreateBy();
        dept.setLeader(leaderUsername);

        // 尝试从负责人获取电话和邮箱
        try {
            if (leaderUsername != null && !leaderUsername.isEmpty()) {
                SysUser leader = sysUserService.selectUserByUserName(leaderUsername);
                if (leader != null) {
                    dept.setPhone(leader.getPhonenumber());
                    dept.setEmail(leader.getEmail());
                    logger.info("从负责人[{}]获取联系方式，电话: {}, 邮箱: {}",
                            leaderUsername, leader.getPhonenumber(), leader.getEmail());
                }
            }
        } catch (Exception e) {
            // 如果获取失败，记录警告但继续执行
            logger.warn("无法获取竞赛联系信息: {}", e.getMessage());
        }

        dept.setOrderNum(100); // 设置较大显示顺序，避免排在前面
        dept.setStatus("0"); // 设置状态为正常
        dept.setDelFlag("0"); // 设置删除标志为存在

        // 设置创建者和创建时间
        dept.setCreateBy(sysComp.getCreateBy());
        dept.setCreateTime(sysComp.getCreateTime());

        // 调用插入部门的方法
        deptService.insertDept(dept);

        return dept;
    }

    /**
     * 创建角色部门关联
     *
     * @param deptId 部门ID
     */
    private void createRoleDeptRelation(Long deptId) {
        // 创建竞赛评委角色与部门的关联
        SysRoleDept roleDept = new SysRoleDept();
        roleDept.setRoleId(3L); // 评委角色ID
        roleDept.setDeptId(deptId);

        int result = roleDeptMapper.insertRoleDept(roleDept);
        if (result <= 0) {
            logger.warn("创建角色部门关联失败，roleId=3, deptId={}", deptId);
            // 这里不抛出异常，让主流程继续
        } else {
            logger.debug("成功创建角色部门关联，roleId=3, deptId={}", deptId);
        }
    }

    /**
     * 修改竞赛信息
     *
     * 功能描述：
     * 1. 设置更新时间
     * 2. 执行数据库更新操作
     * 3. 清理相关缓存
     *
     * @param sysComp 竞赛信息，不能为空
     * @return 更新结果
     * @throws ServiceException 如果 sysComp 或 compId 为 null 或更新失败
     */
    @Override
    @Transactional
    public int updateSysComp(SysComp sysComp) {
        if (sysComp == null || sysComp.getCompId() == null) {
            throw new ServiceException("竞赛信息或ID不能为空", 400);  // 参数校验
        }

        try {
            // 1. 设置更新时间
            sysComp.setUpdateTime(DateUtils.getNowDate());

            // 2. 执行数据库更新操作
            int result = sysCompMapper.updateSysComp(sysComp);

            // 3. 清理相关缓存
            clearCompCache(sysComp.getCompId());
            return result;
        } catch (Exception e) {
            throw new ServiceException("修改竞赛信息失败", 500, e.getMessage());  // 异常处理
        }
    }

    // ========== 删除相关方法 ==========
    /**
     * 删除竞赛信息
     *
     * 功能描述：
     * 1. 获取竞赛信息，以便获取关联的部门ID
     * 2. 执行数据库删除操作
     * 3. 逻辑删除关联的部门
     * 4. 删除角色与该部门的关联
     * 5. 清理相关缓存
     * 6. 清理用户与竞赛关联
     *
     * @param compId 竞赛信息主键，不能为空
     * @return 删除结果
     * @throws ServiceException 如果 compId 为 null 或删除失败
     */
    @Override
    @Transactional
    public int deleteSysCompByCompId(Long compId) {
        logger.info("删除竞赛信息信息, compId: {}", compId);  // 日志补充说明

        if (compId == null) {
            logger.error("删除竞赛信息失败，compId为null");  // 错误日志
            throw new ServiceException("竞赛ID不能为空", 400);  // 参数校验
        }

        try {
            // 0. 获取竞赛信息，以便获取关联的部门ID
            SysComp sysComp = sysCompMapper.selectSysCompByCompId(compId);
            if (sysComp == null) {
                logger.warn("竞赛信息不存在, compId: {}", compId);
                return 0; // 或者根据实际情况返回
            }
            Long deptId = sysComp.getDeptId();

            // 清理用户与竞赛关联
            userCompService.deleteCompetitionUsers(compId);

            // 1. 执行数据库删除操作 (逻辑删除竞赛)
            int result = sysCompMapper.deleteSysCompByCompId(compId);
            logger.info("删除竞赛信息信息结果: {}", result);  // 操作结果日志

            // 2. 逻辑删除关联的部门
            if (deptId != null) {
                deptService.deleteDeptById(deptId); // 调用部门逻辑删除方法
                // 3. 删除角色与该部门的关联
                roleDeptMapper.deleteRoleDeptByDeptId(deptId);
            }

            // 4. 使用统一缓存清理逻辑（修改点）
            clearCompCache(compId);

            return result;
        } catch (Exception e) {
            logger.error("删除竞赛信息信息失败, compId: {}", compId, e);  // 异常日志
            throw new ServiceException("删除竞赛信息失败", 500, e.getMessage());  // 异常处理
        }
    }

    /**
     * 批量删除竞赛信息并清理相关缓存
     *
     * 功能描述：
     * 1. 提取缓存清理为独立方法
     * 2. 清理用户与竞赛关联
     *
     * @param compIds 竞赛ID列表，不能为空
     * @return 删除结果
     * @throws ServiceException 如果 compIds 为 null 或删除失败
     */
    @Override
    @Transactional
    public int deleteSysCompByCompIds(List<Long> compIds) {
        if (compIds == null || compIds.isEmpty()) {
            throw new ServiceException("竞赛ID列表不能为空", 400);  // 参数校验
        }

        try {
            // 清理用户与竞赛关联
            for (Long compId : compIds) {
                userCompService.deleteCompetitionUsers(compId);
            }

            // 1. 执行数据库删除操作
            int result = sysCompMapper.deleteSysCompByCompIds(compIds);

            // 2. 批量清理每个竞赛的缓存
            for (Long compId : compIds) clearCompCache(compId);

            return result;
        } catch (Exception e) {
            logger.error("批量删除竞赛信息失败", e);
            throw new ServiceException("批量删除竞赛信息失败", 500, e.getMessage());
        }
    }

    // ========== 用户授权相关方法 ==========
    /**
     * 取消授权用户竞赛
     *
     * 功能描述：
     * 1. 删除用户竞赛关联信息
     * 2. 根据情况重置用户部门
     *
     * @param userComp 用户和竞赛关联信息
     * @return 删除结果
     */
    @Override
    @Transactional
    public int deleteAuthUser(SysUserComp userComp) {
        if (userComp == null || userComp.getUserId() == null || userComp.getCompId() == null) {
            throw new ServiceException("用户竞赛关联信息不完整", 400);
        }

        try {
            // 获取竞赛信息，确认部门ID
            SysComp sysComp = selectSysCompByCompId(userComp.getCompId());
            if (sysComp != null && sysComp.getDeptId() != null) {
                // 获取用户信息
                SysUser user = sysUserService.selectUserById(userComp.getUserId());
                if (user != null && user.getDeptId() != null &&
                        user.getDeptId().equals(sysComp.getDeptId())) {

                    logger.info("用户 {} 从竞赛 {} 取消授权，当前用户部门为竞赛部门 {}，将重置用户部门",
                            userComp.getUserId(), userComp.getCompId(), user.getDeptId());

                    // 重置用户部门为默认部门或其他适当部门（这里以系统默认部门ID 100为例）
                    // 实际应用中，可能需要根据业务逻辑选择一个合适的部门
                    user.setDeptId(100L); // 系统默认部门，根据实际情况修改
                    sysUserService.updateUser(user);
                }
            }

            // 删除用户竞赛关联信息
            return userCompMapper.deleteUserCompInfo(userComp);
        } catch (Exception e) {
            logger.error("取消授权用户竞赛失败", e);
            throw new ServiceException("取消授权用户竞赛失败", 500, e.getMessage());
        }
    }

    /**
     * 批量取消授权用户竞赛
     *
     * 功能描述：
     * 1. 批量删除用户竞赛关联信息
     * 2. 根据情况重置用户部门
     *
     * @param compId 竞赛ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 删除结果
     */
    @Override
    @Transactional
    public int deleteAuthUsers(Long compId, Long[] userIds) {
        if (compId == null || userIds == null || userIds.length == 0) {
            throw new ServiceException("竞赛ID或用户ID列表不能为空", 400);
        }

        try {
            // 获取竞赛信息，确认部门ID
            SysComp sysComp = selectSysCompByCompId(compId);
            if (sysComp != null && sysComp.getDeptId() != null) {
                Long deptId = sysComp.getDeptId();

                // 处理每个用户的部门关系
                for (Long userId : userIds) {
                    SysUser user = sysUserService.selectUserById(userId);
                    if (user != null && user.getDeptId() != null &&
                            user.getDeptId().equals(deptId)) {

                        logger.info("用户 {} 从竞赛 {} 批量取消授权，当前用户部门为竞赛部门 {}，将重置用户部门",
                                userId, compId, user.getDeptId());

                        // 重置用户部门为默认部门或其他适当部门
                        user.setDeptId(100L); // 系统默认部门，根据实际情况修改
                        sysUserService.updateUser(user);
                    }
                }
            }

            // 批量删除用户竞赛关联信息
            return userCompMapper.deleteUserCompInfos(compId, userIds);
        } catch (Exception e) {
            logger.error("批量取消授权用户竞赛失败", e);
            throw new ServiceException("批量取消授权用户竞赛失败", 500, e.getMessage());
        }
    }

    /**
     * 批量选择授权用户竞赛
     *
     * 功能描述：
     * 1. 设置创建时间
     * 2. 批量插入用户竞赛关联信息
     * 3. 更新用户的部门ID为竞赛关联的部门ID
     *
     * @param compId 竞赛ID
     * @param userIds 需要授权的用户数据ID
     * @return 插入结果
     * @throws ServiceException 如果 compId 或 userIds 为 null 或插入失败
     */
    @Override
    @Transactional
    public int insertAuthUsers(Long compId, Long[] userIds) {
        if (compId == null || userIds == null || userIds.length == 0) {
            throw new ServiceException("竞赛ID和用户ID列表不能为空", 400);
        }

        // 获取竞赛详情，含部门ID
        SysComp sysComp = selectSysCompByCompId(compId);
        Long competitionDeptId = null;

        if (sysComp != null && sysComp.getDeptId() != null) {
            competitionDeptId = sysComp.getDeptId();
        } else {
            logger.warn("竞赛 {} 不存在或没有关联的部门ID，将无法更新用户部门。", compId);
            // 根据业务需求可以决定是否抛出异常或继续处理
        }

        List<SysUserComp> list = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long userId : userIds) {
            try {
                // 创建用户-竞赛关联
                SysUserComp ur = new SysUserComp();
                ur.setUserId(userId);
                ur.setCompId(compId);
                list.add(ur);

                // 如果竞赛有关联部门，则更新用户部门
                if (competitionDeptId != null) {
                    SysUser user = sysUserService.selectUserById(userId);
                    if (user != null) {
                        Long oldDeptId = user.getDeptId();
                        if (oldDeptId == null || !oldDeptId.equals(competitionDeptId)) {
                            logger.info("为竞赛 {} 分配用户 {} 时，将其部门从 {} 更新为竞赛部门 {}",
                                    compId, user.getUserId(), oldDeptId, competitionDeptId);

                            user.setDeptId(competitionDeptId);
                            sysUserService.updateUser(user);
                            successCount++;
                        } else {
                            logger.debug("用户 {} 已属于竞赛部门 {}, 无需更新", userId, competitionDeptId);
                        }
                    } else {
                        logger.warn("为竞赛 {} 分配用户时，未找到用户ID {}，跳过部门更新。", compId, userId);
                        failCount++;
                    }
                }
            } catch (Exception e) {
                logger.error("为竞赛 {} 分配用户 {} 失败", compId, userId, e);
                failCount++;
                // 不抛出异常，继续处理其他用户
            }
        }

        if (failCount > 0) {
            logger.warn("批量选择授权用户竞赛完成，成功: {}，失败: {}", successCount, failCount);
        }

        // 批量插入用户-竞赛关联
        return userCompMapper.batchUserComp(list);
    }

    // ========== 推荐竞赛相关方法 ==========
    /**
     * 查询推荐竞赛
     *
     * 功能描述：
     * 1. 根据参数决定推荐方式
     * 2. 支持随机推荐、类别推荐、访问频率推荐、最新竞赛推荐
     * 3. 直接从数据库加载数据，避免缓存干扰
     * 4. 新增类别推荐逻辑处理
     *
     * @param type 推荐类型（random、category、access、latest）
     * @param category 竞赛类别（仅在 type=category 时有效）
     * @param count 推荐数量
     * @return 推荐竞赛列表
     * @throws ServiceException 如果参数无效
     */
    @Override
    public List<SysComp> recommendCompetitions(String type, Character category, int count) {
        logger.info("推荐竞赛, type: {}, category: {}, count: {}", type, category, count);
        if (count <= 0) {
            throw new ServiceException("推荐数量必须大于0", 400);
        }

        // 生成基于推荐类型和参数的缓存键
        String cacheKey = CacheConstants.COMP_RECOMMEND_KEY + type + ":" + (category != null ? category : "") + ":" + count;

        // 优先从缓存中获取推荐结果
        List<SysComp> result = redisCacheUtil.getCacheList(cacheKey);

        if (result == null || result.isEmpty()) {
            logger.info("缓存未命中，查询数据库，cacheKey: {}", cacheKey);

            SysComp queryCondition = new SysComp();
            String orderBy = "comp_id"; // 默认排序字段

            // 根据推荐类型设置查询条件和排序字段
            switch (type.toLowerCase()) {
                case "latest":
                    orderBy = "comp_start_time DESC";
                    break;
                case "access":
                    orderBy = "access_frequency DESC";
                    break;
                case "random":
                    orderBy = "RAND()";
                    break;
                case "category":
                    if (category == null) {
                        throw new ServiceException("类别参数不能为空", 400);
                    }
                    queryCondition.setCompCategory(category);
                    orderBy = "access_frequency DESC";
                    break;
                case "id":
                    orderBy = "comp_id DESC";
                    break;
                default:
                    throw new ServiceException("无效的推荐类型", 400);
            }

            // 直接从数据库加载数据
            result = sysCompMapper.selectSysCompList(queryCondition, orderBy);

            // 去重并限制返回结果数量
            result = result.stream()
                    .distinct()
                    .limit(count)
                    .toList();

            // 将结果写入缓存
            int ttl = CacheConstants.DEFAULT_CACHE_TTL + new Random().nextInt(300);
            redisCacheUtil.setCacheList(cacheKey, result, ttl, TimeUnit.SECONDS);
        } else {
            logger.info("缓存命中，返回缓存结果，cacheKey: {}", cacheKey);
        }

        return result;
    }

    // ========== 异步任务相关方法 ==========
    /**
     * 智能缓存清理逻辑
     *
     * 功能描述：
     * 1. 精确清除单个竞赛详情缓存
     * 2. 智能清除与该竞赛相关的列表缓存（基于结构化缓存键）
     * 3. 智能清除与该竞赛分类相关的推荐缓存
     * 4. 异常处理与日志记录
     *
     * @param compId 竞赛ID
     */
    private void clearCompCache(Long compId) {
        if (compId == null) {
            return;
        }

        logger.info("开始智能清理竞赛ID {} 相关的缓存", compId);

        try {
            // 1. 首先获取竞赛信息，用于后续针对性清理
            SysComp sysComp = null;
            try {
                sysComp = sysCompMapper.selectSysCompByCompId(compId);
            } catch (Exception e) {
                logger.warn("获取竞赛信息失败，将执行全面缓存清理", e);
            }

            // 2. 精确删除竞赛信息缓存（详情页面使用）
            String infoKey = CacheConstants.COMP_INFO_KEY + compId;
            boolean infoDeleted = redisCacheUtil.deleteObject(infoKey);
            logger.info("清理竞赛详情缓存 {}: {}", infoKey, infoDeleted ? "成功" : "不存在");

            // 3. 智能清理列表缓存
            clearRelatedListCache(compId, sysComp);

            // 4. 智能清理推荐缓存
            clearRelatedRecommendCache(compId, sysComp);

        } catch (Exception e) {
            // 缓存清理失败不应影响主业务流程
            logger.error("清理竞赛缓存失败", e);
        }
    }

    /**
     * 智能清理与指定竞赛相关的列表缓存
     *
     * @param compId 竞赛ID
     * @param sysComp 竞赛信息（可能为null）
     */
    private void clearRelatedListCache(Long compId, SysComp sysComp) {
        try {
            List<String> patterns = new ArrayList<>();

            // 1. 清理包含此竞赛ID的列表缓存
            patterns.add(CacheConstants.COMP_LIST_KEY + ":compId:" + compId);

            // 2. 如果有竞赛对象，根据其属性清理更多相关缓存
            if (sysComp != null) {
                // 清理同类别的竞赛列表缓存
                if (sysComp.getCompCategory() != null) {
                    patterns.add(CacheConstants.COMP_LIST_KEY + ":category:" + sysComp.getCompCategory());
                }

                // 清理同状态的竞赛列表缓存
                if (sysComp.getCompStatus() != null) {
                    patterns.add(CacheConstants.COMP_LIST_KEY + ":status:" + sysComp.getCompStatus());
                }
            }

            // 3. 清理全部列表的缓存（重要的缓存键模式）
            patterns.add(CacheConstants.COMP_LIST_KEY + ":all");

            // 4. 执行多模式匹配删除
            int deletedCount = 0;
            for (String pattern : patterns) {
                Collection<String> keys = redisCacheUtil.keys(pattern + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisCacheUtil.deleteObject(keys);
                    deletedCount += keys.size();
                    logger.info("清理列表缓存模式 {}, 删除 {} 个缓存键", pattern, keys.size());
                }
            }

            // 如果智能清理没找到匹配的键，回退到全模式清理
            if (deletedCount == 0) {
                Collection<String> allListKeys = redisCacheUtil.keys(CacheConstants.COMP_LIST_KEY + "*");
                if (allListKeys != null && !allListKeys.isEmpty()) {
                    redisCacheUtil.deleteObject(allListKeys);
                    logger.info("智能清理未找到相关缓存，执行全面清理，共删除 {} 个列表缓存键", allListKeys.size());
                }
            }
        } catch (Exception e) {
            logger.error("清理列表缓存失败", e);
            // 失败时，尝试清理所有列表缓存
            Collection<String> allListKeys = redisCacheUtil.keys(CacheConstants.COMP_LIST_KEY + "*");
            if (allListKeys != null && !allListKeys.isEmpty()) {
                redisCacheUtil.deleteObject(allListKeys);
            }
        }
    }

    /**
     * 智能清理与指定竞赛相关的推荐缓存
     *
     * @param compId 竞赛ID
     * @param sysComp 竞赛信息（可能为null）
     */
    private void clearRelatedRecommendCache(Long compId, SysComp sysComp) {
        try {
            List<String> patterns = new ArrayList<>();

            // 1. 所有random和latest类型的推荐缓存都需要清理
            patterns.add(CacheConstants.COMP_RECOMMEND_KEY + "random:*");
            patterns.add(CacheConstants.COMP_RECOMMEND_KEY + "latest:*");
            patterns.add(CacheConstants.COMP_RECOMMEND_KEY + "id:*");

            // 2. 根据竞赛特性清理特定推荐缓存
            if (sysComp != null) {
                // 访问频率相关的推荐缓存
                patterns.add(CacheConstants.COMP_RECOMMEND_KEY + "access:*");

                // 如果有类别信息，清理该类别的推荐缓存
                if (sysComp.getCompCategory() != null) {
                    patterns.add(CacheConstants.COMP_RECOMMEND_KEY + "category:" + sysComp.getCompCategory() + ":*");
                }
            } else {
                // 如果没有竞赛对象，清理所有类别相关的推荐缓存
                patterns.add(CacheConstants.COMP_RECOMMEND_KEY + "category:*");
                patterns.add(CacheConstants.COMP_RECOMMEND_KEY + "access:*");
            }

            // 3. 执行多模式清理
            int deletedCount = 0;
            for (String pattern : patterns) {
                Collection<String> keys = redisCacheUtil.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisCacheUtil.deleteObject(keys);
                    deletedCount += keys.size();
                    logger.info("清理推荐缓存模式 {}, 删除 {} 个缓存键", pattern, keys.size());
                }
            }

            // 如果智能清理没找到匹配的键，记录信息
            if (deletedCount == 0) {
                logger.info("未找到需要清理的推荐缓存");
            }
        } catch (Exception e) {
            logger.error("清理推荐缓存失败", e);
            // 失败时，尝试清理所有推荐缓存
            Collection<String> allRecommendKeys = redisCacheUtil.keys(CacheConstants.COMP_RECOMMEND_KEY + "*");
            if (allRecommendKeys != null && !allRecommendKeys.isEmpty()) {
                redisCacheUtil.deleteObject(allRecommendKeys);
            }
        }
    }

    /**
     * 异步更新缓存（竞赛信息）
     *
     * 功能描述：
     * 1. 使用线程池管理异步任务
     * 2. 更新单个竞赛信息
     *
     * @param cacheKey 缓存键
     * @param compId 竞赛ID
     */
    private void asyncUpdateCache(String cacheKey, Long compId) {
        asyncExecutor.execute(() -> {
            RLock lock = redissonClient.getLock("lock:" + cacheKey);
            try {
                if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                    // 更新竞赛信息
                    SysComp newData = sysCompMapper.selectSysCompByCompId(compId);
                    if (newData != null) {
                        int ttl = CacheConstants.DEFAULT_CACHE_TTL + new Random().nextInt(300);
                        redisCacheUtil.setCacheObject(cacheKey, newData, ttl, TimeUnit.SECONDS);
                    }
                }
            } catch (Exception e) {
                logger.error("异步缓存更新失败，cacheKey: {}", cacheKey, e);
            } finally {
                if (lock.isHeldByCurrentThread()) lock.unlock();
            }
        });
    }

    @Override
    public List<SysComp> selectMyAssignedCompetitions(Long userId) {
        return sysCompMapper.selectMyAssignedCompetitions(userId);
    }

    @Override
    public List<SysComp> selectUnassignedCompetitions(Long userId) {
        return sysCompMapper.selectUnassignedCompetitions(userId);
    }

    /**
     * 递增竞赛访问频率
     *
     * 功能描述：
     * 1. 更新数据库中的访问频率
     * 2. 如果缓存中存在竞赛信息，同步更新缓存
     *
     * @param compId 竞赛ID
     * @return 更新结果
     * @throws ServiceException 如果 compId 为 null 或更新失败
     */
    @Override
    public int incrementAccessFrequency(Long compId) {
        if (compId == null) {
            throw new ServiceException("竞赛ID不能为空", 400);
        }

        try {
            // 1. 更新数据库中的访问频率
            int result = sysCompMapper.incrementAccessFrequency(compId);

            // 2. 如果缓存中存在竞赛信息，同步更新缓存
            String cacheKey = CacheConstants.COMP_INFO_KEY + compId;
            SysComp cachedComp = redisCacheUtil.getCacheObject(cacheKey);
            if (cachedComp != null) {
                // 递增缓存中的访问频率
                Integer currentFreq = cachedComp.getAccessFrequency();
                if (currentFreq != null) {
                    cachedComp.setAccessFrequency(currentFreq + 1);
                    // 更新缓存
                    int ttl = CacheConstants.DEFAULT_CACHE_TTL + new Random().nextInt(300);
                    redisCacheUtil.setCacheObject(cacheKey, cachedComp, ttl, TimeUnit.SECONDS);
                    logger.info("更新缓存中竞赛[{}]的访问频率为{}", compId, currentFreq + 1);
                }
            }

            return result;
        } catch (Exception e) {
            logger.error("递增竞赛访问频率失败, compId: {}", compId, e);
            throw new ServiceException("递增竞赛访问频率失败", 500, e.getMessage());
        }
    }

}
