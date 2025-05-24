package com.cms;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Servlet 容器部署初始化类：web容器中进行部署
 * 用于在传统的 Servlet 容器中部署 Spring Boot 应用程序，通过指定应用程序的主类来配置启动。
 * @author quoteZZ
 */
public class CmsServletInitializer extends SpringBootServletInitializer
{
    // 重写父类方法，用于配置应用程序的启动
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application)
    {
        // 返回应用程序的启动类，用于启动应用程序
        return application.sources(CmsApplication.class);
    }
}
