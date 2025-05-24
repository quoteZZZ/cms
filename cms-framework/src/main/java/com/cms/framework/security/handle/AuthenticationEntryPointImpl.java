package com.cms.framework.security.handle;

import com.alibaba.fastjson2.JSON;
import com.cms.common.constant.HttpStatus;
import com.cms.common.core.domain.AjaxResult;
import com.cms.common.utils.ServletUtils;
import com.cms.common.utils.StringUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;

/**
 * 认证失败处理类： 返回未授权
 * 当用户尝试访问受保护的资源但未通过认证时，
 * 该类会返回一个未授权的响应，并在响应体中包含详细的错误信息。
 * @author quoteZZZ
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint, Serializable
{
    private static final long serialVersionUID = -8970718410437077606L;

    /**
     * @param request 请求对象，包含用户尝试访问的URL、请求方法等信息
     * @param response 响应对象，用于向客户端发送响应，错误信息等
     * @param e 认证异常对象，包含认证失败的原因和详细信息
     * @throws IOException 如果在向客户端发送响应时发生IO异常
     * @Description：
     * 处理未通过身份认证的请求，返回未授权响应。
     * 当用户尝试访问需要身份认证的资源但未通过认证时，此方法被调用。
     * 该方法会返回一个未授权(包含错误信息)的HTTP响应，并在响应体中包含详细的错误信息，表示认证失败。
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException
    {
        //Http状态码为401，表示未授权
        int code = HttpStatus.UNAUTHORIZED;
        //构建错误信息，包括尝试访问的URL
        String msg = StringUtils.format("请求访问：{}，认证失败，无法访问系统资源", request.getRequestURI());
        //将错误信息转换为JSON格式，向客户端发送响应（错误信息）
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(code, msg)));
    }
}
