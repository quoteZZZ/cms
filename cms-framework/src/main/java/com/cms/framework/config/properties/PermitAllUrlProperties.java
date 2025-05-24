package com.cms.framework.config.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.cms.common.annotation.Anonymous;

/**
 * 设置Anonymous注解允许匿名访问的url配置类：
 * 用于：匿名用户可访问的url，默认情况下，所有需要认证的url自动加上了@Anonymous注解，
 * @author quoteZZZ
 */
@Configuration
public class PermitAllUrlProperties implements InitializingBean, ApplicationContextAware
{
    // 正则匹配，例如：/user/{id}
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    // 上下文
    private ApplicationContext applicationContext;

    // 匿名访问的url
    private List<String> urls = new ArrayList<>();

    // 通配符
    public String ASTERISK = "*";

    // 初始化方法
    /*
    * 获取方法上的注解：检查方法上是否有 @Anonymous 注解。
    替换路径变量为*：如果有 @Anonymous 注解，将 URL 中的路径变量替换为通配符 *。
    添加URL到列表：将处理后的 URL 添加到允许匿名访问的 URL 列表中。
    获取类上的注解：检查类上是否有 @Anonymous 注解，重复上述步骤。*/
    @Override
    public void afterPropertiesSet()
    {
        // 获取所有url
        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        // 获取url与类和方法的对应信息
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
        // 遍历所有url与类和方法的对应信息
        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);// 获取方法
            // 获取方法上边的注解 替代path variable 为 *
            Anonymous method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Anonymous.class);
            Optional.ofNullable(method).ifPresent(anonymous -> Objects.requireNonNull(info.getPatternsCondition().getPatterns())
                    .forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK))));
            // 获取类上边的注解, 替代path variable 为 *
            Anonymous controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class);
            Optional.ofNullable(controller).ifPresent(anonymous -> Objects.requireNonNull(info.getPatternsCondition().getPatterns())
                    .forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK))));
        });
    }

    // 获取上下文
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException
    {
        this.applicationContext = context;// 设置上下文
    }

    public List<String> getUrls()
    {
        return urls;
    }// 获取url

    public void setUrls(List<String> urls)
    {
        this.urls = urls;
    }// 设置url
}
