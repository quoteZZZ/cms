package com.cms.framework.web.service;

import com.cms.common.constant.CacheConstants;
import com.cms.common.constant.Constants;
import com.cms.common.constant.UserConstants;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.core.domain.model.LoginUser;
import com.cms.common.core.redis.RedisCache;
import com.cms.common.exception.ServiceException;
import com.cms.common.exception.user.*;
import com.cms.common.utils.DateUtils;
import com.cms.common.utils.MessageUtils;
import com.cms.common.utils.StringUtils;
import com.cms.common.utils.ip.IpUtils;
import com.cms.framework.manager.AsyncManager;
import com.cms.framework.manager.factory.AsyncFactory;
import com.cms.framework.security.context.AuthenticationContextHolder;
import com.cms.system.service.ISysConfigService;
import com.cms.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 登录校验服务类：
 * 该类负责处理用户的登录验证、验证码校验、登录前置校验、以及记录用户登录信息，返回前端token。
 * 1. 验证用户输入的用户名和密码是否匹配。
 * 2. 校验验证码是否正确，防止暴力破解。
 * 3. 进行一些登录前的校验（用户名、密码范围检查、IP黑名单检查等）。
 * 4. 登录成功后生成并返回一个令牌token。
 * 5. 异常捕获并记录登录日志。
 * @author quoteZZZ
 */
@Component
public class SysLoginService
{
    // 注入 Token 服务，用于生成和管理令牌
    @Autowired
    private TokenService tokenService;

    // 注入 AuthenticationManager（认证管理器） 进行用户认证
    @Resource
    private AuthenticationManager authenticationManager;

    // 注入 Redis 缓存服务，用于存储和获取验证码
    @Autowired
    private RedisCache redisCache;

    // 注入用户服务，用于更新用户信息
    @Autowired
    private ISysUserService userService;

    // 注入系统配置服务，用于获取配置项
    @Autowired
    private ISysConfigService configService;

    /**
     * 登录验证
     * 验证用户的用户名、密码、验证码，校验通过后生成并返回访问令牌。
     * @param username 用户名
     * @param password 密码
     * @param code 输入的验证码
     * @param uuid 正确验证码唯一标识key
     * @return 结果，返回生成的访问令牌
     */
    public String login(String username, String password, String code, String uuid)
    {
        // 验证码校验
        validateCaptcha(username, code, uuid);
        // 登录前置校验（前端校验后，后端要进行二次校验）：用户名和密码是否符合规范
        loginPreCheck(username, password);
        // 用户验证
        Authentication authentication = null;
        try
        {
            // 创建一个身份验证对象（用户密码）
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            // 设置用户密码放入认证上下文
            AuthenticationContextHolder.setContext(authenticationToken);
            // 调用AuthenticationManager（认证管理器） 进行用户认证（该方法去调用UserDetailServiceImpl.loadUserByUsername）
            authentication = authenticationManager.authenticate(authenticationToken);
        }
        catch (Exception e)
        {
            // 认证失败的处理
            if (e instanceof BadCredentialsException)
            {
                // 记录登录失败信息
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();//抛出自定义异常（用户名或密码不匹配异常）
            }
            else
            {
                // 记录失败的错误信息
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new ServiceException(e.getMessage());//抛出自定义异常（系统异常）
            }
        }
        finally
        {
            // 无论登录成功还是失败，都清理认证上下文（用户密码）
            AuthenticationContextHolder.clearContext();
        }
        // 登录成功后，记录登录信息日志（调用异步线程，传入用户名，登录状态：成功）
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        // 获取登录用户的详细信息（通过spring security）
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        // 更新用户的登录信息
        recordLoginInfo(loginUser.getUserId());
        // 生成访问令牌token并返回前端
        return tokenService.createToken(loginUser);
    }

    /**
     * 校验验证码
     * 校验验证码是否正确。验证码存在且正确时，才允许继续登录。
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        // 判断是否启用验证码
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            // 获取验证码存储的 key
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
            String captcha = redisCache.getCacheObject(verifyKey);
            // 如果验证码过期或不存在
            if (captcha == null)
            {
                // 记录验证码过期信息
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
                throw new CaptchaExpireException();// 抛出验证码过期异常
            }
            // 删除验证码缓存
            redisCache.deleteObject(verifyKey);
            // 验证用户输入的验证码是否正确，与缓存中的验证码进行比较（不区分大小写）
            if (!code.equalsIgnoreCase(captcha))
            {
                // 记录验证码错误信息（不匹配）
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
                throw new CaptchaException();// 抛出验证码不匹配异常
            }
        }
    }

    /**
     * 登录前置校验
     * 校验用户名、密码的合法性，例如空值检查、长度检查等。
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password)
    {
        // 用户名或密码为空时，抛出异常
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            // 记录用户名或密码为空的错误信息
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();// 抛出用户名或密码不存在异常
        }

        // 密码长度不符合要求，抛出异常（5~20）
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            // 记录密码不匹配的错误日志信息
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();//抛出用户密码不正确或不符合规范异常
        }

        // 用户名长度不符合要求，抛出异常（2~20）
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            // 记录用户名不匹配的错误信息
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();// 抛出用户名或密码不正确或不符合规范异常
        }

        // IP 黑名单校验
        //从系统配置服务中获取黑名单IP列表
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        //判断当前用户的IP是否在黑名单中
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            // 记录黑名单 IP 登录失败的信息（记录访问IP以列入系统黑名单日志信息）
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("login.blocked")));
            // 抛出黑名单异常
            throw new BlackListException();
        }
    }

    /**
     * 更新登录信息（更新登录IP和时间）
     * 记录用户的登录时间、登录 IP 等信息。
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        SysUser sysUser = new SysUser();// 用户对象
        sysUser.setUserId(userId); // 设置用户ID
        sysUser.setLoginIp(IpUtils.getIpAddr()); // 设置登录IP
        sysUser.setLoginDate(DateUtils.getNowDate()); // 设置登录时间
        userService.updateUserProfile(sysUser); // 更新用户登录信息
    }
}
