package com.cms.framework.config;

import com.cms.common.utils.FastJson2JsonRedisSerializer;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * redis配置类：
 * 用于配置RedisTemplate，用于操作Redis数据库。
 * 主要功能包括：
 * 1. 自定义RedisTemplate，支持FastJson序列化。
 * 2. 提供限流脚本的配置。
 *
 * @author quoteZZZ
 */
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {

    /**
     * 自定义redisTemplate
     * 配置RedisTemplate，使用FastJson2JsonRedisSerializer进行序列化和反序列化。
     *
     * @param connectionFactory Redis连接工厂
     * @return 配置好的RedisTemplate对象
     */
    @Bean
    @SuppressWarnings(value = { "unchecked", "rawtypes" })// 表示对类中的所有方法进行参数和返回值类型的检查，防止类型转换错误。
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建RedisTemplate对象
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // 使用fastjson序列化
        FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);
        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        // Hash的key也采用StringRedisSerializer的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        // 开启自动转换数据操作
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 限流脚本配置
     * 提供限流脚本的DefaultRedisScript对象。
     *
     * @return 配置好的DefaultRedisScript对象
     */
    @Bean
    public DefaultRedisScript<Long> limitScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(limitScriptText());
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 限流脚本
     * Lua脚本实现限流逻辑，支持按指定时间窗口限制访问次数。
     *
     * @return Lua脚本文本
     */
    private String limitScriptText() {
        return "local key = KEYS[1]\n" +
                "local count = tonumber(ARGV[1])\n" +
                "local time = tonumber(ARGV[2])\n" +
                "local current = redis.call('get', key);\n" +
                "if current and tonumber(current) > count then\n" +
                "    return tonumber(current);\n" +
                "end\n" +
                "current = redis.call('incr', key)\n" +
                "if tonumber(current) == 1 then\n" +
                "    redis.call('expire', key, time)\n" +
                "end\n" +
                "return tonumber(current);";
    }

}
