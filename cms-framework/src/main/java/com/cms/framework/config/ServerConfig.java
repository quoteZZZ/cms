package com.cms.framework.config;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import com.cms.common.utils.ServletUtils;

/**
 * 服务相关配置：
 * 用于获取项目相关配置，如：项目路径、请求域名、请求地址等
 * @author quoteZZZ
 */
@Component
public class ServerConfig
{
    /**
     * 获取完整的请求路径，包括：域名，端口，上下文访问路径
     * @return 服务地址
     */
    public String getUrl()
    {
        HttpServletRequest request = ServletUtils.getRequest();
        return getDomain(request);
    }

    public static String getDomain(HttpServletRequest request)
    {
        StringBuffer url = request.getRequestURL();
        String contextPath = request.getServletContext().getContextPath();
        return url.delete(url.length() - request.getRequestURI().length(), url.length()).append(contextPath).toString();
    }
}
