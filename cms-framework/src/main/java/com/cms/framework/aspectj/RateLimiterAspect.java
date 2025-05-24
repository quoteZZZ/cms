package com.cms.framework.aspectj;

import com.cms.common.annotation.RateLimiter;
import com.cms.common.enums.LimitType;
import com.cms.common.exception.ServiceException;
import com.cms.common.utils.StringUtils;
import com.cms.common.utils.ip.IpUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * 限流处理切面类：（自定义注解RateLimiter）
 * 用于限流控制，通过Redis实现
 * @author quoteZZZ
 */
@Aspect
@Component
public class RateLimiterAspect
{
    // 日志对象
    private static final Logger log = LoggerFactory.getLogger(RateLimiterAspect.class);

    // RedisTemplate对象
    private RedisTemplate<Object, Object> redisTemplate;

    // RedisScript对象
    private RedisScript<Long> limitScript;

    // 注入RedisTemplate对象
    @Autowired
    public void setRedisTemplate1(RedisTemplate<Object, Object> redisTemplate)
    {
        this.redisTemplate = redisTemplate;
    }

    // 注入RedisScript对象
    @Autowired
    public void setLimitScript(RedisScript<Long> limitScript)
    {
        this.limitScript = limitScript;
    }

    // 前置通知
    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable //参数：JoinPoint连接点，RateLimiter注解对象
    {
        int time = rateLimiter.time();// 时间，秒
        int count = rateLimiter.count();// 最大访问次数

        String combineKey = getCombineKey(rateLimiter, point);// 获取注解中定义的key
        List<Object> keys = Collections.singletonList(combineKey);// 将key放入List集合中
        try
        {
            Long number = redisTemplate.execute(limitScript, keys, count, time);// 执行RedisScript脚本，获取访问次数
            if (StringUtils.isNull(number) || number.intValue() > count)// 如果访问次数大于最大访问次数，则抛出异常
            {
                throw new ServiceException("访问过于频繁，请稍候再试");// 抛出异常
            }
            log.info("限制请求'{}',当前请求'{}',缓存key'{}'", count, number.intValue(), combineKey);// 记录日志
        }
        catch (ServiceException e)
        {
            throw e;// 抛出异常
        }
        catch (Exception e)
        {
            throw new RuntimeException("服务器限流异常，请稍候再试");// 抛出异常
        }
    }

    // 获取注解中定义的key
    public String getCombineKey(RateLimiter rateLimiter, JoinPoint point)
    {
        StringBuffer stringBuffer = new StringBuffer(rateLimiter.key());// 创建StringBuffer对象
        if (rateLimiter.limitType() == LimitType.IP)// 如果限流类型为IP
        {
            stringBuffer.append(IpUtils.getIpAddr()).append("-");// 将IP地址拼接到StringBuffer对象中
        }
        MethodSignature signature = (MethodSignature) point.getSignature();// 获取方法签名
        Method method = signature.getMethod();// 获取方法
        Class<?> targetClass = method.getDeclaringClass();// 获取类
        stringBuffer.append(targetClass.getName()).append("-").append(method.getName());// 将类名和方法名拼接到StringBuffer对象中
        return stringBuffer.toString();// 返回StringBuffer对象
    }
}
