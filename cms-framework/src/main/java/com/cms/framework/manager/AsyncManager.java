package com.cms.framework.manager;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.cms.common.utils.Threads;
import com.cms.common.utils.spring.SpringUtils;

/**
 * 异步任务管理器：
 * 负责管理异步任务，包括执行任务和停止任务线程池。
 * @author quoteZZZ
 */
public class AsyncManager
{
    /**
     * 操作延迟10毫秒
     */
    private final int OPERATE_DELAY_TIME = 10;

    /**
     * 异步操作任务调度线程池
     */
    private ScheduledExecutorService executor = SpringUtils.getBean("scheduledExecutorService");

    /**
     * 单例模式
     */
    private AsyncManager(){}

    //（单例模式：饿汉式）声明并初始化一个静态的 AsyncManager 实例 me，确保在整个应用程序中只有一个 AsyncManager 实例，实现单例模式。
    private static AsyncManager me = new AsyncManager();

    //提供一个静态方法 me()，用于返回 AsyncManager 类的唯一实例 me，从而实现单例模式的全局访问点。
    public static AsyncManager me()
    {
        return me;
    }

    /**
     * 执行任务（使用任务执行器调度任务，延迟10毫秒执行任务）
     * @param task 任务
     */
    public void execute(TimerTask task)
    {
        executor.schedule(task, OPERATE_DELAY_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止任务线程池
     */
    public void shutdown()
    {
        Threads.shutdownAndAwaitTermination(executor);
    }
}
