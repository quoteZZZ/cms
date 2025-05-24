package com.cms.framework.security.handle;

import com.alibaba.fastjson2.JSON;
import com.cms.common.constant.Constants;
import com.cms.common.core.domain.AjaxResult;
import com.cms.common.core.domain.model.LoginUser;
import com.cms.common.utils.MessageUtils;
import com.cms.common.utils.ServletUtils;
import com.cms.common.utils.StringUtils;
import com.cms.framework.manager.AsyncManager;
import com.cms.framework.manager.factory.AsyncFactory;
import com.cms.framework.web.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义退出处理类： 返回成功
 * 用户在退出时，系统能够正确清理用户信息并记录日志，同时给客户端返回明确的成功提示。
 * @author quoteZZZ
 */
@Configuration
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler
{
    @Autowired
    private TokenService tokenService;

    /**
     * 退出处理
     * 
     * @return
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException
    {
        //获取当前登录用户信息：（登录成功后会在redis存储login的token）
        LoginUser loginUser = tokenService.getLoginUser(request);
        // 判断用户是否已经登录：
        if (StringUtils.isNotNull(loginUser))
        {
            //获取当前登录用户名：
            String userName = loginUser.getUsername();
            // 删除用户缓存记录（redis里）
            tokenService.delLoginUser(loginUser.getToken());
            // 记录用户退出日志，使用异步执行，避免阻塞主线程
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.LOGOUT, MessageUtils.message("user.logout.success")));
        }
        //返回成功响应：（返回Json格式的提示信息：user.logout.success在I18N的配置文件中）
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.success(MessageUtils.message("user.logout.success"))));
    }
}
