package com.cms.framework.aspectj;

import com.cms.common.annotation.DataSource;
import com.cms.common.utils.StringUtils;
import com.cms.framework.datasource.DynamicDataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 多数据源切换切面类：（自定义注解DataSource）
 * 主要作用如下：
 * 切面拦截：通过AOP（面向切面编程）拦截带有 @DataSource 注解的方法或类。
 * 数据源切换：在方法执行前根据注解配置切换数据源，并在方法执行完毕后清除数据源配置。
 * 优先级设置：通过 @Order(1) 确保该切面的优先级，保证其在其他切面之前执行。
 * 简而言之，该文件实现了动态数据源切换的逻辑，确保系统可以在不同数据源之间灵活切换。
 * @author quoteZZZ
 */
@Aspect // 表示这是一个切面类
@Order(1) // 设置切面优先级，值越小，优先级越高
@Component
public class DataSourceAspect
{
    // 日志记录器
    protected Logger logger = LoggerFactory.getLogger(getClass());

    // 定义切点
    @Pointcut("@annotation(com.cms.common.annotation.DataSource)"
            + "|| @within(com.cms.common.annotation.DataSource)")
    public void dsPointCut()
    {

    }

    // 环绕通知
    @Around("dsPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable
    {
        DataSource dataSource = getDataSource(point);

        if (StringUtils.isNotNull(dataSource))
        {
            DynamicDataSourceContextHolder.setDataSourceType(dataSource.value().name());
        }

        try
        {
            return point.proceed();
        }
        finally
        {
            // 销毁数据源 在执行方法之后
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }

    /**
     * 获取需要切换的数据源
     */
    public DataSource getDataSource(ProceedingJoinPoint point)
    {
        MethodSignature signature = (MethodSignature) point.getSignature();
        DataSource dataSource = AnnotationUtils.findAnnotation(signature.getMethod(), DataSource.class);
        if (Objects.nonNull(dataSource))
        {
            return dataSource;
        }

        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), DataSource.class);
    }
}
