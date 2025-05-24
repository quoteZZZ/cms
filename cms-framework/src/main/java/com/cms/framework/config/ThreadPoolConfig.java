package com.cms.framework.config;

import com.cms.common.utils.Threads;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类：
 * 用于创建线程池，并配置线程池参数：
 * @author quoteZZZ
 **/
@Configuration
public class ThreadPoolConfig
{
    // 核心线程池大小
    private int corePoolSize = 50;

    // 最大可创建的线程数
    private int maxPoolSize = 200;

    // 队列最大长度
    private int queueCapacity = 1000;

    // 线程池维护线程所允许的空闲时间
    private int keepAliveSeconds = 300;

    // 线程池配置
    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor()
    {
        // 创建线程池
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);//设置最大可创建的线程数
        executor.setCorePoolSize(corePoolSize);//设置核心线程池大小
        executor.setQueueCapacity(queueCapacity);//设置队列最大长度
        executor.setKeepAliveSeconds(keepAliveSeconds);//设置线程池维护线程所允许的空闲时间
        // 线程池对拒绝任务(无线程可用)的处理策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;//返回线程池对象
    }

    /**
     * 执行周期性或定时任务
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService()
    {
        //创建一个具有核心线程数量的线程池ScheduledThreadPoolExecutor
        //使用BasicThreadFactory构建器定制线程工厂：设置线程命名模式为"schedule-pool-%d"和守护线程为true
        //设置拒绝策略为ThreadPoolExecutor.CallerRunsPolicy，即当线程池无法处理任务时，由调用者线程处理该任务
        return new ScheduledThreadPoolExecutor(corePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy())
        {
            //在执行任务后，如果发生异常，则调用Threads.printException方法打印异常信息
            @Override
            protected void afterExecute(Runnable r, Throwable t)
            {
                super.afterExecute(r, t);//调用父类方法，执行任务后，如果发生异常，则调用Threads.printException方法打印异常信息
                Threads.printException(r, t);//调用Threads.printException方法打印异常信息
            }
        };
    }
}
