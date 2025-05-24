package com.cms.framework.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.cms.framework.config.properties.RedisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redisson 配置类
 */
@Configuration
public class RedissonConfig {
    private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);

    @Autowired
    private RedisProperties redisProperties;

    /**
     * 创建 RedissonClient 实例（单机模式）
     * 
     * 该方法用于初始化 RedissonClient，支持单机模式的 Redis 配置。
     * 它从 RedisProperties 中读取配置，并设置 Redisson 的连接参数。
     * 
     * @return RedissonClient 实例
     * @throws IllegalArgumentException 如果 Redis 主机或端口配置不正确
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        // 检查 Redis 主机和端口配置
        if (redisProperties.getHost() == null || redisProperties.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("Redis host is not configured in application.yml");
        }
        if (redisProperties.getPort() <= 0) {
            throw new IllegalArgumentException("Redis port is not configured or invalid in application.yml");
        }

        // 构建 Redisson 配置
        Config config = new Config();
        String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();

        // 解析超时配置
        int timeoutValue = parseTimeout(redisProperties.getTimeout(), 10000);

        // 获取 Lettuce 和 Pool 配置，避免空指针异常
        RedisProperties.LettuceProperties lettuce = redisProperties.getLettuce();
        RedisProperties.PoolProperties pool = (lettuce != null && lettuce.getPool() != null) ? lettuce.getPool() : new RedisProperties.PoolProperties();

        // 设置连接池参数
        int maxWaitTime = parseTimeout(pool.getMaxWait(), 500);
        int poolSize = pool.getMaxActive();
        int minIdle = pool.getMinIdle();
        int maxIdle = pool.getMaxIdle();

        // 确保连接池大小不小于最小空闲连接数
        if (poolSize < minIdle) {
            log.warn("Redis连接池大小 ({}), 小于最小空闲连接数 ({}), 已调整", poolSize, minIdle);
            poolSize = minIdle;
        }

        // 配置 Redisson 连接参数
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase())
                .setConnectionPoolSize(poolSize)
                .setConnectionMinimumIdleSize(minIdle)
                .setTimeout(timeoutValue)
                .setConnectTimeout(maxWaitTime);

        // 如果有密码，设置密码
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().trim().isEmpty()) {
            config.useSingleServer().setPassword(redisProperties.getPassword());
        }

        // 创建并返回 RedissonClient 实例
        return Redisson.create(config);
    }

    /**
     * 解析时间字符串 (如 "10s" or "500ms" 或 "5m")，转换为毫秒
     */
    private int parseTimeout(String timeout, int defaultValue) {
        if (timeout == null || timeout.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            // 使用正则表达式提取数字部分
            String numberPart = timeout.replaceAll("[^\\d]", "");
            int timeoutValue = Integer.parseInt(numberPart);

            // 处理时间单位（秒、毫秒、分钟）
            if (timeout.endsWith("s")) {
                return timeoutValue * 1000; // 秒转毫秒
            } else if (timeout.endsWith("ms")) {
                return timeoutValue; // 毫秒直接返回
            } else if (timeout.endsWith("m")) { // 处理分钟
                return timeoutValue * 60 * 1000; // 分钟转毫秒
            } else {
                return timeoutValue; // 默认按毫秒处理
            }
        } catch (NumberFormatException e) {
            log.error("解析 Redis 超时配置失败: " + timeout, e);
            return defaultValue;
        }
    }

}
