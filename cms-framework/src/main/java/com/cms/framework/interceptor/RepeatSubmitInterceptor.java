package com.cms.framework.interceptor;

import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.cms.common.core.domain.AjaxResult;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import com.alibaba.fastjson2.JSON;
import com.cms.common.annotation.RepeatSubmit;
import com.cms.common.utils.ServletUtils;

/**
 * 防止表单重复提交的通用拦截器（抽象基类）：
 * 主要用途：
 * 通过拦截请求，避免用户短时间内重复提交表单（如重复下单、重复保存等操作）。
 * 作为 抽象基类，定义防止重复提交的通用逻辑，具体规则由子类实现（如 SameUrlDataInterceptor）。
 * 实现逻辑：
 * 1.拦截请求：
 * 在处理 HTTP 请求前 (preHandle 方法)，判断目标方法是否标注了 @RepeatSubmit 注解。
 * 如果没有该注解，则不进行防重复提交逻辑。
 * 2.重复提交验证：
 * 如果方法上标注了 @RepeatSubmit 注解：
 * 调用 isRepeatSubmit 方法（由子类实现）进行重复提交检测。
 * 如果检测到重复提交，则返回统一的错误响应，并终止后续处理流程。
 * 3.统一响应：
 * 当检测到重复提交时，返回 JSON 格式的错误信息（由 AjaxResult 封装）。
 * 通过工具类 ServletUtils 将响应直接写入到 HttpServletResponse。
 * @author quoteZZZ
 * 可以通过继承该类，实现基于 Redis、内存缓存等方式的重复提交检测。
 * 例如：
 * SameUrlDataInterceptor：基于 Redis，按照 URL 和参数进行防重复提交。
 */
@Component
public abstract class RepeatSubmitInterceptor implements HandlerInterceptor
{
    // 重复拦截器
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if (handler instanceof HandlerMethod)
        {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            RepeatSubmit annotation = method.getAnnotation(RepeatSubmit.class);
            if (annotation != null)
            {
                if (this.isRepeatSubmit(request, annotation))
                {
                    AjaxResult ajaxResult = AjaxResult.error(annotation.message());
                    ServletUtils.renderString(response, JSON.toJSONString(ajaxResult));
                    return false;
                }
            }
            return true;
        }
        else
        {
            return true;
        }
    }

    /**
     * 验证是否重复提交由子类实现具体的防重复提交的规则
     * @param request 请求信息
     * @param annotation 防重复注解参数
     * @return 结果
     * @throws Exception
     */
    public abstract boolean isRepeatSubmit(HttpServletRequest request, RepeatSubmit annotation);
}
