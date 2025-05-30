package com.cms.system.service.impl;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.cms.common.constant.CacheConstants;
import com.cms.common.exception.ServiceException;
import com.cms.common.utils.DateUtils;
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
import java.util.Random;
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
     */
    @PostConstruct
    public void cachePreheat() {
        new Thread(() -> {
            try {
                List<Long> hotCompIds = sysCompMapper.selectHotCompIds();
                logger.info("获取热门竞赛ID列表：{}", hotCompIds);
                
                // 创建新列表存储有效compId
                List<Long> validCompIds = new ArrayList<>();
                
                for (Long compId : hotCompIds) {
                    String cacheKey = CacheConstants.COMP_INFO_KEY + compId;
                    
                    if (!redisCacheUtil.hasKey(cacheKey)) {
                        SysComp sysComp = sysCompMapper.selectSysCompByCompId(compId);
                        if (sysComp != null && sysComp.getCompId() != null) {
                            int ttl = CacheConstants.DEFAULT_CACHE_TTL + new Random().nextInt(300);
                            redisCacheUtil.setCacheObject(cacheKey, sysComp, ttl, TimeUnit.SECONDS);
                            validCompIds.add(compId); // 记录有效compId
                        } else {
                            logger.warn("竞赛信息为空或已失效，compId: {}，将从热门列表移除", compId);
                            // 删除无效compId
                            sysCompMapper.deleteSysCompByCompId(compId); 
                        }
                    } else {
                        validCompIds.add(compId);
                    }
                }
                
            } catch (Exception e) {
                logger.error("缓存预热失败", e);
            }
        }).start();
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
     *
     * @param compId 竞赛ID，不能为空
     * @return 竞赛信息
     * @throws ServiceException 如果 compId 为 null 或查询失败
     */
    @Override
    public SysComp selectSysCompByCompId(Long compId) {
        if (compId == null) throw new ServiceException("竞赛ID不能为空", 400);

        String cacheKey = CacheConstants.COMP_INFO_KEY + compId;

        // 1. 旁路缓存模式：先查缓存
        SysComp sysComp = redisCacheUtil.getCacheObject(cacheKey);

        if (sysComp == null) {
            // 2. 缓存空值处理：双重检查（防止缓存穿透）
            RLock lock = redissonClient.getLock("lock:" + cacheKey);
            try {
                if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                    sysComp = redisCacheUtil.getCacheObject(cacheKey);
                    if (sysComp == null) {
                        // 3. 回源查询数据库
                        sysComp = sysCompMapper.selectSysCompByCompId(compId);

                        if (sysComp != null) {
                            // 4. 设置随机TTL（基础TTL + 随机偏移）
                            int ttl = CacheConstants.DEFAULT_CACHE_TTL + new Random().nextInt(300);
                            redisCacheUtil.setCacheObject(cacheKey, sysComp, ttl, TimeUnit.SECONDS);
                        } else {
                            // 5. 缓存空值防止穿透
                            redisCacheUtil.setCacheObject(cacheKey, "", 60, TimeUnit.SECONDS);
                        }
                    }
                }
            } catch (Exception e) {
                throw new ServiceException("缓存重建失败", 500, e.getMessage());
            } finally {
                if (lock.isHeldByCurrentThread()) lock.unlock();
            }
        } else {
            // 6. 检查逻辑过期时间（异步更新）
            Long ttl = redisCacheUtil.getExpire(cacheKey);
            if (ttl != null && ttl < CacheConstants.LOGICAL_EXPIRE_CHECK_WINDOW) {
                // 触发异步更新
                asyncUpdateCache(cacheKey, compId);
            }
        }

        return sysComp;
    }

    /**
     * 查询竞赛信息列表
     *
     * 功能描述：
     * 1. 生成基于查询参数的MD5缓存键（优化点）
     * 2. 缓存空列表防止穿透
     * 3. 随机TTL防止缓存雪崩
     *
     * @param sysComp 竞赛信息查询条件（需包含有效的查询参数）
     * @return 竞赛信息列表（永不为null，空时返回空列表）
     */
    @Override
    public List<SysComp> selectSysCompList(SysComp sysComp, String order) {
        // 记录传入的查询条件
        logger.info("查询竞赛信息列表, 查询条件: {}, 排序参数: {}", sysComp, order);

        // 如果 order 为空，默认按 comp_id 排序
        if (order == null || order.isEmpty()) {
            order = "comp_id DESC";
            logger.info("排序参数为空，默认按 comp_id 排序");
        }

        // 设置 status 默认值为 '0'，确保查询条件一致性
        if (sysComp.getStatus() == null) {
            sysComp.setStatus('0');
        }

        // 生成基于查询条件的缓存键
        String cacheKey = generateCacheKey(sysComp, order);

        // 1. 从缓存中获取数据
        List<SysComp> cachedResult = redisCacheUtil.getCacheList(cacheKey);
        if (cachedResult != null && !cachedResult.isEmpty() && isValidCacheData(cachedResult)) {
            logger.info("缓存命中，返回缓存结果，cacheKey: {}", cacheKey);
            return cachedResult;
        }

        // 2. 缓存未命中，查询数据库
        List<SysComp> result = sysCompMapper.selectSysCompList(sysComp, order);
        logger.info("数据库查询结果: {}", result);

        if (result == null || result.isEmpty()) {
            // 数据库查询结果为空，缓存空列表防止穿透
            logger.warn("数据库查询结果为空，查询条件: {}", sysComp);
            result = Collections.emptyList();
        } else {
            // 数据库查询结果有效，写入缓存
            int ttl = CacheConstants.DEFAULT_CACHE_TTL + new Random().nextInt(300);
            redisCacheUtil.setCacheList(cacheKey, result, ttl, TimeUnit.SECONDS);
            logger.info("缓存更新，cacheKey: {}", cacheKey);
        }

        return result;
    }

    /**
     * 生成基于查询条件的缓存键
     * @param sysComp 查询条件
     * @param order 排序字段
     * @return 缓存键
     */
    private String generateCacheKey(SysComp sysComp, String order) {
        StringBuilder keyBuilder = new StringBuilder(CacheConstants.COMP_LIST_KEY);
        keyBuilder.append("compId=").append(sysComp.getCompId() != null ? sysComp.getCompId() : "")
              .append("&compName=").append(sysComp.getCompName() != null ? sysComp.getCompName() : "")
              .append("&compCategory=").append(sysComp.getCompCategory() != null ? sysComp.getCompCategory() : "")
              .append("&compStatus=").append(sysComp.getCompStatus() != null ? sysComp.getCompStatus() : "")
              .append("&status=").append(sysComp.getStatus() != null ? sysComp.getStatus() : "")
              .append("&order=").append(order != null ? order : "");
        return DigestUtils.md5Hex(keyBuilder.toString());
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
     * 1. 生成唯一竞赛ID
     * 2. 设置创建时间
     * 3. 初始化访问频率为0
     * 4. 创建关联部门
     * 5. 为评委角色创建与此部门的关联
     * 6. 执行数据库插入操作
     * 7. 清理相关缓存
     *
     * @param sysComp 竞赛信息，不能为空
     * @return 插入结果
     * @throws ServiceException 如果 sysComp 为 null 或插入失败
     */
    @Override
    @Transactional
    public int insertSysComp(SysComp sysComp) {
        if (sysComp == null) throw new ServiceException("竞赛信息不能为空", 400);  // 参数校验

        try {
            // 1. 生成唯一竞赛ID
            sysComp.setCompId(IdGenerator.generateId(0));
            
            // 2. 设置创建时间
            sysComp.setCreateTime(DateUtils.getNowDate());
            
            // 3. 初始化访问频率为0
            sysComp.setAccessFrequency(0);

            // 4. 创建关联部门
            SysDept dept = new SysDept();
            dept.setDeptName(sysComp.getCompName());
            dept.setParentId(0L); // 直接挂在根部门下
            dept.setLeader(sysComp.getCreateBy()); // 可以根据实际情况设置负责人
            // 其他部门属性可以根据需要设置
            deptService.insertDept(dept); // 调用插入部门的方法
            sysComp.setDeptId(dept.getDeptId()); // 保存新创建的部门ID

            // 5. 为评委角色(roleId=3)创建与此部门的关联
            SysRoleDept roleDept = new SysRoleDept();
            roleDept.setRoleId(3L); // 评委角色ID
            roleDept.setDeptId(dept.getDeptId());
            roleDeptMapper.insertRoleDept(roleDept);

            // 6. 执行数据库插入操作
            int result = sysCompMapper.insertSysComp(sysComp);
            
            // 7. 清理相关缓存
            clearCompCache(sysComp.getCompId());//为了清除与该竞赛相关的所有缓存，确保后续查询能获取最新数据，维护缓存一致性。
            return result;
        } catch (Exception e) {
            throw new ServiceException("新增竞赛信息失败", 500, e.getMessage());  // 异常处理
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
    public int deleteSysCompByCompIds(Long[] compIds) {
        if (compIds == null || compIds.length == 0) {
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
     * 统一缓存清理逻辑
     *
     * 功能描述：
     * 1. 精确删除竞赛信息缓存
     * 2. 精确删除关联列表缓存（优化通配符匹配）
     * 3. 优化推荐缓存清理：按compId清理相关推荐缓存
     *
     * @param compId 竞赛ID
     */
    private void clearCompCache(Long compId) {
        if (compId != null) {
            // 精确删除竞赛信息缓存
            redisCacheUtil.deleteObject(CacheConstants.COMP_INFO_KEY + compId);

            // 精确删除关联列表缓存（优化通配符匹配）
            String compIdStr = compId.toString();
            redisCacheUtil.deleteKeysByPattern(CacheConstants.COMP_LIST_KEY + "*" + compIdStr + "*");

            // 优化推荐缓存清理：按compId清理相关推荐缓存
            redisCacheUtil.deleteKeysByPattern(CacheConstants.COMP_RECOMMEND_KEY + "*:" + compIdStr + "*");
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

}
