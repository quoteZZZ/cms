package com.cms.framework.web.service;

import com.cms.common.constant.CacheConstants;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.core.redis.RedisCache;
import com.cms.common.exception.user.UserPasswordNotMatchException;
import com.cms.common.exception.user.UserPasswordRetryLimitExceedException;
import com.cms.common.utils.SecurityUtils;
import com.cms.framework.security.context.AuthenticationContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 登录密码服务类：
 * 用于处理用户登录时的密码校验逻辑，包括错误次数限制和缓存管理。
 * @author quoteZZZ
 */
@Component
public class SysPasswordService {

    // Redis 缓存工具类，用于管理登录错误次数和锁定机制
    @Autowired
    private RedisCache redisCache;

    // 最大登录密码错误次数，从配置文件中读取
    @Value(value = "${user.password.maxRetryCount}")
    private int maxRetryCount;

    // 登录错误锁定时间（分钟），从配置文件中读取
    @Value(value = "${user.password.lockTime}")
    private int lockTime;

    /**
     * 构建redis缓存键，用于存储登录账户密码错误次数
     * @param username 用户名
     * @return 缓存键名
     */
    private String getCacheKey(String username) {
        return CacheConstants.PWD_ERR_CNT_KEY + username;
    }

    /**
     * 验证用户密码
     * 根据用户名和密码进行验证，处理错误次数限制和锁定机制。
     * @param user 用户实体对象
     */
    public void validate(SysUser user) {
        // 从上下文中获取认证信息（包含用户名和密码）
        Authentication usernamePasswordAuthenticationToken = AuthenticationContextHolder.getContext();
        String username = usernamePasswordAuthenticationToken.getName();
        String password = usernamePasswordAuthenticationToken.getCredentials().toString();
        // 从 Redis 获取当前用户名的登录错误次数
        Integer retryCount = redisCache.getCacheObject(getCacheKey(username));
        // 如果缓存中无记录，初始化为 0
        if (retryCount == null) {
            retryCount = 0;
        }
        // 检查错误次数是否超出限制
        if (retryCount >= maxRetryCount) {
            throw new UserPasswordRetryLimitExceedException(maxRetryCount, lockTime);
        }
        // 验证密码是否匹配
        if (!matches(user, password)) {
            // 密码不匹配，增加错误次数并存入 Redis，设置锁定时间
            retryCount = retryCount + 1;
            redisCache.setCacheObject(getCacheKey(username), retryCount, lockTime, TimeUnit.MINUTES);
            throw new UserPasswordNotMatchException();//抛出密码不匹配异常
        } else {
            // 密码匹配，清除缓存中的错误记录
            clearLoginRecordCache(username);
        }
    }

    /**
     * 校验用户密码是否匹配
     * 使用工具类进行密码比对。
     * @param user 用户实体对象
     * @param rawPassword 输入的原始密码
     * @return true 如果密码匹配，false 否则
     */
    public boolean matches(SysUser user, String rawPassword) {
        // 使用 Spring Security 的工具类进行密码比对
        return SecurityUtils.matchesPassword(rawPassword, user.getPassword());
    }

    /**
     * 清除用户登录错误记录
     * 成功登录或解锁时，删除 Redis 中的错误记录。
     * @param loginName 用户名
     */
    public void clearLoginRecordCache(String loginName) {
        if (redisCache.hasKey(getCacheKey(loginName))) {
            redisCache.deleteObject(getCacheKey(loginName)); // 删除对应的缓存记录
        }
    }
}

