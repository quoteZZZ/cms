package com.cms.framework.interceptor.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.cms.common.annotation.RepeatSubmit;
import com.cms.common.constant.CacheConstants;
import com.cms.common.redis.RedisCacheUtil;
import com.cms.common.filter.RepeatedlyRequestWrapper;
import com.cms.common.utils.StringUtils;
import com.cms.common.utils.http.HttpHelper;
import com.cms.framework.interceptor.RepeatSubmitInterceptor;

/**
 * 验证是否重复提交拦截器（具体实现类）：
 * 主要用途：
 * 防止客户端在短时间内重复提交相同的数据（如表单提交）。
 * 通过对比上一次提交的请求地址、参数和时间，实现重复提交的检测和拦截。
 * 实现逻辑：
 * 使用 Redis 缓存存储请求的参数和提交时间。
 * 对比当前请求和缓存中存储的上一次请求：
 * 参数是否相同：通过请求体（body）或参数列表进行比对。
 * 时间间隔是否小于设定的间隔时间：默认有效时间为 10 秒。
 * 如果两者均满足，则认定为重复提交，拦截本次请求。
 * @author quoteZZZ
 */
@Component
public class SameUrlDataInterceptor extends RepeatSubmitInterceptor
{
    // 参数和消息键值对
    public final String REPEAT_PARAMS = "repeatParams";

    // 参数和消息键值对
    public final String REPEAT_TIME = "repeatTime";

    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    // redis缓存
    @Autowired
    private RedisCacheUtil redisCacheUtil;

    // 拦截器方法
    @SuppressWarnings("unchecked")
    @Override
    public boolean isRepeatSubmit(HttpServletRequest request, RepeatSubmit annotation)
    {
        String nowParams = "";
        if (request instanceof RepeatedlyRequestWrapper)
        {
            RepeatedlyRequestWrapper repeatedlyRequest = (RepeatedlyRequestWrapper) request;
            nowParams = HttpHelper.getBodyString(repeatedlyRequest);
        }

        // body参数为空，获取Parameter的数据
        if (StringUtils.isEmpty(nowParams))
        {
            nowParams = JSON.toJSONString(request.getParameterMap());
        }
        Map<String, Object> nowDataMap = new HashMap<String, Object>();
        nowDataMap.put(REPEAT_PARAMS, nowParams);
        nowDataMap.put(REPEAT_TIME, System.currentTimeMillis());

        // 请求地址（作为存放cache的key值）
        String url = request.getRequestURI();

        // 唯一值（没有消息头则使用请求地址）
        String submitKey = StringUtils.trimToEmpty(request.getHeader(header));

        // 唯一标识（指定key + url + 消息头）
        String cacheRepeatKey = CacheConstants.REPEAT_SUBMIT_KEY + url + submitKey;

        Object sessionObj = redisCacheUtil.getCacheObject(cacheRepeatKey);
        if (sessionObj != null)
        {
            Map<String, Object> sessionMap = (Map<String, Object>) sessionObj;
            if (sessionMap.containsKey(url))
            {
                Map<String, Object> preDataMap = (Map<String, Object>) sessionMap.get(url);
                if (compareParams(nowDataMap, preDataMap) && compareTime(nowDataMap, preDataMap, annotation.interval()))
                {
                    return true;
                }
            }
        }
        Map<String, Object> cacheMap = new HashMap<String, Object>();
        cacheMap.put(url, nowDataMap);
        redisCacheUtil.setCacheObject(cacheRepeatKey, cacheMap, annotation.interval(), TimeUnit.MILLISECONDS);
        return false;
    }

    /**
     * 判断参数是否相同
     */
    private boolean compareParams(Map<String, Object> nowMap, Map<String, Object> preMap)
    {
        String nowParams = (String) nowMap.get(REPEAT_PARAMS);
        String preParams = (String) preMap.get(REPEAT_PARAMS);
        return nowParams.equals(preParams);
    }

    /**
     * 判断两次间隔时间
     */
    private boolean compareTime(Map<String, Object> nowMap, Map<String, Object> preMap, int interval)
    {
        long time1 = (Long) nowMap.get(REPEAT_TIME);
        long time2 = (Long) preMap.get(REPEAT_TIME);
        if ((time1 - time2) < interval)
        {
            return true;
        }
        return false;
    }
}
