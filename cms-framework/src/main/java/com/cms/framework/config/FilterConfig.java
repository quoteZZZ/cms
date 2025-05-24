package com.cms.framework.config;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.cms.common.filter.RepeatableFilter;
import com.cms.common.filter.XssFilter;
import com.cms.common.utils.StringUtils;

/**
 * Filter过滤器配置
 * 配置并注册两个Servlet过滤器：
 * 防止XSS攻击的过滤器和防止重复提交的过滤器
 * @author quoteZZZ
 */
@Configuration
public class FilterConfig
{
    // XSS过滤器
    @Value("${xss.excludes}")
    private String excludes;

    // XSS过滤开关
    @Value("${xss.urlPatterns}")
    private String urlPatterns;

    // 防止XSS攻击过滤器
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    @ConditionalOnProperty(value = "xss.enabled", havingValue = "true")
    public FilterRegistrationBean xssFilterRegistration()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns(StringUtils.split(urlPatterns, ","));
        registration.setName("xssFilter");
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        Map<String, String> initParameters = new HashMap<String, String>();
        initParameters.put("excludes", excludes);
        registration.setInitParameters(initParameters);
        return registration;
    }
    /*XSS（跨站脚本攻击）是一种安全漏洞，攻击者通过在网页中注入恶意脚本，
    使这些脚本在其他用户的浏览器中执行。当用户浏览包含恶意脚本的网页时，
    这些脚本可以窃取用户的敏感信息（如Cookie、会话令牌等），或者对用户进行其他恶意操作。*/

    // 防止重复提交过滤器
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    public FilterRegistrationBean someFilterRegistration()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new RepeatableFilter());
        registration.addUrlPatterns("/*");
        registration.setName("repeatableFilter");
        registration.setOrder(FilterRegistrationBean.LOWEST_PRECEDENCE);
        return registration;
    }

}
