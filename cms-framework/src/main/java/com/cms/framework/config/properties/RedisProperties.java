package com.cms.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 配置属性类
 */
@Component
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {

    private String host;
    private int port;
    private int database;
    private String password;
    private String timeout;

    // 初始化 lettuce 属性以避免空指针异常
    private LettuceProperties lettuce = new LettuceProperties();

    public static class LettuceProperties {
        private PoolProperties pool = new PoolProperties(); // 初始化 pool 属性

        public PoolProperties getPool() {
            return pool;
        }

        public void setPool(PoolProperties pool) {
            this.pool = pool;
        }
    }

    public static class PoolProperties {
        private int minIdle;
        private int maxIdle;
        private int maxActive;
        private String maxWait;

        // Getter & Setter
        public int getMinIdle() {
            return minIdle;
        }
        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }
        public int getMaxIdle() {
            return maxIdle;
        }
        public void setMaxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
        }
        public int getMaxActive() {
            return maxActive;
        }
        public void setMaxActive(int maxActive) {
            this.maxActive = maxActive;
        }
        public String getMaxWait() {
            return maxWait;
        }
        public void setMaxWait(String maxWait) {
            this.maxWait = maxWait;
        }
    }

    // Getter & Setter
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public int getDatabase() {
        return database;
    }
    public void setDatabase(int database) {
        this.database = database;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getTimeout() {
        return timeout;
    }
    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }
    public LettuceProperties getLettuce() {
        return lettuce;
    }
    public void setLettuce(LettuceProperties lettuce) {
        this.lettuce = lettuce;
    }
}
