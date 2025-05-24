package com.cms.framework.security.filter;

import com.cms.common.core.domain.model.LoginUser;
import com.cms.common.utils.SecurityUtils;
import com.cms.common.utils.StringUtils;
import com.cms.framework.web.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * token过滤器类，
 * 用于验证HTTP请求中的token有效性。
 * 是一个安全过滤器，负责验证HTTP请求中的JWT有效性，
 * 并将合法用户的认证信息设置到Spring Security上下文对象（SS）中，
 * 确保只有经过验证的用户可以访问受保护的资源。
 * @author quoteZZZ
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    // 自动注入TokenService，用于处理token相关操作
    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 从请求中的令牌获取登录用户信息
        LoginUser loginUser = tokenService.getLoginUser(request);
        // 检查是否已登录且SS当前没有认证对象。如果用户信息存在且当前没有认证信息，则进行token验证
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication())) {
            // 验证token的有效性
            tokenService.verifyToken(loginUser);
            // 创建认证令牌（认证对象的详细信息，这些信息基于web的认证细节）
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            //并将认证对象设置到Spring Security上下文对象(SS)中，这样其他部分就可以访问用户信息
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        // 放行操作，继续执行过滤链中的下一个过滤器或目标servlet
        chain.doFilter(request, response);
    }
}

