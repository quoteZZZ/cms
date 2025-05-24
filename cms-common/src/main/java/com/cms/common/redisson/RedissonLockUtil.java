package com.cms.common.redisson;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.cms.common.exception.GlobalException;
import com.cms.common.exception.ServiceException;
import com.cms.common.redis.RedisCacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 Redisson 的分布式锁工具类
 * <p>
 * 提供统一的 executeWithLock 方法来封装加锁、执行带锁操作和释放锁，以便在业务代码中使用分布式锁保证数据一致性。
 * </p>
 */
@Component
public class RedissonLockUtil {

    private static final Logger log = LoggerFactory.getLogger(RedissonLockUtil.class);
    @Autowired
    private RedissonClient redissonClient;

    // 默认分布式锁参数（单位：秒）
    public static final long DEFAULT_LOCK_WAIT_TIME = 10;  // 获取锁的最大等待时间：10秒
    public static final long DEFAULT_LOCK_LEASE_TIME = 5;   // 锁持有租约时间：5秒

    /**
     * 使用默认锁等待和租约时间封装分布式锁执行操作
     * 
     * 该方法提供了一种简单的方式来使用分布式锁，使用默认的等待时间和租约时间。
     * 
     * @param lockKey 锁的 key
     * @param callable 带锁执行的操作
     * @param <T> 返回值类型
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, Callable<T> callable) {
        return executeWithLock(lockKey, DEFAULT_LOCK_WAIT_TIME, DEFAULT_LOCK_LEASE_TIME, TimeUnit.SECONDS, callable);
    }

    /**
     * 封装分布式锁执行操作
     * 
     * 该方法提供了更灵活的方式来使用分布式锁，允许自定义等待时间和租约时间。
     * 它还实现了重试机制，以提高获取锁的成功率。
     * 
     * @param lockKey   锁的 key
     * @param waitTime  获取锁的最大等待时间
     * @param leaseTime 获取锁后自动释放时间
     * @param unit      时间单位
     * @param callable  带锁执行的操作
     * @param <T>       返回值类型
     * @return 执行结果
     * @throws ServiceException 如果业务操作失败或获取锁超时
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Callable<T> callable) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        int retryCount = 0;
        final int MAX_RETRIES = 3; // 最大重试次数

        // 尝试获取锁，最多重试 3 次
        while (!acquired && retryCount < MAX_RETRIES) {
            try {
                acquired = lock.tryLock(waitTime, leaseTime, unit);
                if (!acquired) {
                    retryCount++;
                    Thread.sleep(500); // 重试间隔
                }
            } catch (Exception e) {
                throw new ServiceException("获取锁失败", 500, e.getMessage());
            }
        }

        // 如果未能获取锁，抛出异常
        if (!acquired) {
            throw new ServiceException("获取锁失败，重试次数达到上限", 500);
        }

        // 执行带锁的操作
        try {
            return callable.call();
        } catch (Exception e) {
            throw new ServiceException("执行带锁操作失败", 500, e.getMessage());
        } finally {
            // 确保锁被正确释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 使用分布式锁执行操作，并在操作前后删除指定缓存
     */
    public <T> T executeWithDoubleCacheDeleteForCallable(String lockKey, Callable<T> operation, String cacheKey, RedisCacheUtil redisCacheUtil) {
        return executeWithLock(lockKey, () -> {
            log.info("【开始操作】lockKey={}, cacheKey={}", lockKey, cacheKey);
            // 操作前删除缓存（提前清理）
            redisCacheUtil.deleteObject(cacheKey);
            T result = operation.call();
            // 操作后再次删除缓存（清除脏数据）
            redisCacheUtil.deleteObject(cacheKey);
            log.info("【操作结束】结果：{}, lockKey={}, cacheKey={}", result, lockKey, cacheKey);
            return result;
        });
    }

    /**
     * 使用分布式锁执行操作，并在操作前后删除指定缓存
     */
    public int executeWithDoubleCacheDeleteForInteger(String lockKey, Callable<Integer> operation, String cacheKey, RedisCacheUtil redisCacheUtil) {
        return executeWithDoubleCacheDeleteForCallable(lockKey, operation, cacheKey, redisCacheUtil);
    }

    /**
     * 使用分布式锁执行操作，并在操作前后删除指定缓存
     */
    public int executeWithDoubleCacheDelete(String lockKey, Callable<Integer> operation, String cacheKey, RedisCacheUtil redisCacheUtil) {
        return executeWithDoubleCacheDeleteForInteger(lockKey, operation, cacheKey, redisCacheUtil);
    }

}
