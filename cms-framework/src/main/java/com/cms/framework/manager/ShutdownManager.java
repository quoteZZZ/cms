package com.cms.framework.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.PreDestroy;

/**
 * 关闭后台线程类：
 * 确保应用退出时能关闭后台线程
 * @author quoteZZZ
 */
@Component
public class ShutdownManager
{
    // 日志
    private static final Logger logger = LoggerFactory.getLogger("sys-user");

    // 关闭应用时执行
    @PreDestroy
    public void destroy()
    {
        shutdownAsyncManager();
    }

    /**
     * 停止异步执行任务
     */
    private void shutdownAsyncManager()
    {
        try
        {
            logger.info("====关闭后台任务任务线程池====");
            AsyncManager.me().shutdown();
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
    }
}
