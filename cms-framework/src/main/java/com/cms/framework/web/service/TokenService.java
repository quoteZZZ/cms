package com.cms.framework.web.service;

import com.cms.common.constant.CacheConstants;
import com.cms.common.constant.Constants;
import com.cms.common.core.domain.model.LoginUser;
import com.cms.common.core.redis.RedisCache;
import com.cms.common.utils.ServletUtils;
import com.cms.common.utils.StringUtils;
import com.cms.common.utils.ip.AddressUtils;
import com.cms.common.utils.ip.IpUtils;
import com.cms.common.utils.uuid.IdUtils;
import eu.bitwalker.useragentutils.UserAgent;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Token验证处理服务类：
 * 用于处理用户Token验证和管理的服务类，负责管理用户Token的创建、验证、刷新和删除。
 * Token验证：从HTTP请求中提取并解析Token，获取用户身份信息。
 * Token创建与刷新：生成新的Token，并在接近过期时自动刷新Token的有效期。
 * 用户信息管理：将用户登录信息存储到Redis缓存中，并提供设置和删除用户信息的功能。
 * @author quoteZZZ
 */
@Component
public class TokenService {
    // 日志记录器（初始化一个静态的 Logger 对象，用于记录 TokenService 类的日志信息。）
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    // 令牌自定义标识（从配置文件中读取，用于在HTTP请求头中标识令牌。）
    @Value("${token.header}")
    private String header;

    // 令牌秘钥（从配置文件中读取生成和验证令牌的秘钥，确保令牌的安全性。）
    @Value("${token.secret}")
    private String secret;

    // 令牌有效期（默认30分钟，从配置文件中读取，控制令牌的过期时间。）
    @Value("${token.expireTime}")
    private int expireTime;

    // 时间常量定义
    protected static final long MILLIS_SECOND = 1000;// 秒
    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;// 分钟
    private static final Long MILLIS_MINUTE_TEN = 20 * 60 * 1000L;// 20分钟

    // Redis缓存实例，用于存储用户登录信息
    @Autowired
    private RedisCache redisCache;

    /**
     * 根据请求中的token，获取用户身份信息。
     * 从请求中提取Token并解析出用户信息，如果Token有效则返回用户信息，否则返回null。
     * @param request HTTP请求对象
     * @return 用户信息对象
     */
    public LoginUser getLoginUser(HttpServletRequest request) {
        // 获取请求携带的令牌
        String token = getToken(request);
        // 解析Token中的用户信息，如果Token有效则返回用户信息，否则返回null。
        if (StringUtils.isNotEmpty(token)) {
            try {
                // 解析Token中的Claims，并返回Claims对象。
                Claims claims = parseToken(token);
                // 获取用户唯一标识UUID，用于构建Redis键。
                String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
                // 构建Redis键，用于在Redis中存储用户信息。
                String userKey = getTokenKey(uuid);
                // 从Redis中获取用户信息，如果存在则返回用户信息，否则返回null。
                LoginUser user = redisCache.getCacheObject(userKey);
                return user;// 返回用户信息对象
            } catch (Exception e) {
                // 记录错误日志
                log.error("获取用户信息异常'{}'", e.getMessage());
            }
        }
        return null;// Token无效或不存在，返回null
    }

    /**
     * 设置用户身份信息。
     * 如果用户信息不为空且包含有效的Token，则刷新Token的有效期。
     * @param loginUser 用户信息对象
     */
    public void setLoginUser(LoginUser loginUser) {
        // 判断用户信息是否为空且包含有效的Token，如果不为空且包含有效的Token，则刷新Token的有效期。
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken())) {
            // 刷新Token有效期
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户身份信息。
     * 根据Token构建Redis键并删除对应的用户信息。
     * @param token 用户Token
     */
    public void delLoginUser(String token) {
        // 判断Token是否为空，如果不为空则删除对应的用户信息。
        if (StringUtils.isNotEmpty(token)) {
            // 构建Redis键，作为key用于在Redis中删除用户信息。
            String userKey = getTokenKey(token);
            // 删除Redis中的用户信息
            redisCache.deleteObject(userKey);
        }
    }

    /**
     * 创建令牌Token。
     * 生成唯一的Token，并设置用户代理信息，刷新Token有效期，最后返回JWT格式的Token。
     * @param loginUser 用户信息对象
     * @return 生成的Token字符串
     */
    public String createToken(LoginUser loginUser) {
        // 生成唯一Token，用一个快速UUID生成
        String token = IdUtils.fastUUID();
        // 将Token存入用户信息对象中
        loginUser.setToken(token);
        // 设置用户代理信息（IP地址、浏览器、操作系统等）
        setUserAgent(loginUser);
        // 刷新Token有效期（redis中的token）
        refreshToken(loginUser);
        // 构建Claims，用于生成JWT Token。（Claims是JWT中用于存储用户或实体相关信息的负载部分。）
        Map<String, Object> claims = new HashMap<>();// Claims是一个Map，用于存储用户信息。
        // 将token存入Claims中
        claims.put(Constants.LOGIN_USER_KEY, token);
        // 生成JWT Token
        return createToken(claims);
    }

    /**
     * 验证令牌有效期。
     * 如果Token距离过期时间不足20分钟，则自动刷新Token有效期。
     * @param loginUser 用户信息对象
     */
    public void verifyToken(LoginUser loginUser) {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        // 判断是否需要刷新Token
        if (expireTime - currentTime <= MILLIS_MINUTE_TEN) {
            // 刷新Token有效期
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期。
     * 更新用户的登录时间和过期时间，并将用户信息存入Redis缓存。
     * @param loginUser 用户信息对象
     */
    public void refreshToken(LoginUser loginUser) {
        // 更新登录时间
        loginUser.setLoginTime(System.currentTimeMillis());
        //根据新的登录时间计算出令牌的过期时间（默认30分钟）
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        // 构建Redis键
        String userKey = getTokenKey(loginUser.getToken());
        // 使用新的有效期重新缓存用户对象
        redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    /**
     * 设置用户代理信息。
     * 记录用户的IP地址、地理位置、浏览器和操作系统等信息。
     * @param loginUser 用户信息对象
     */
    public void setUserAgent(LoginUser loginUser) {
        // 获取User-Agent信息，解析HTTP请求头中的User-Agent字符串，获取用户代理对象
        UserAgent userAgent = UserAgent.parseUserAgentString(ServletUtils.getRequest().getHeader("User-Agent"));
        // 获取IP地址
        String ip = IpUtils.getIpAddr();
        // 设置用户信息
        loginUser.setIpaddr(ip);// 设置登录用户的IP地址
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));//通过IP地址获取真实地理位置，将登录地点设置到登录用户对象中
        loginUser.setBrowser(userAgent.getBrowser().getName());//获取并设置登录用户的浏览器信息
        loginUser.setOs(userAgent.getOperatingSystem().getName());//获取并设置登录用户的操作系统信息
    }

    /**
     * 从数据声明生成令牌。
     * 使用JWT库根据Claims生成签名后的Token。
     * @param claims 数据声明
     * @return 生成的Token字符串
     */
    private String createToken(Map<String, Object> claims) {
        // 使用HMAC SHA-512算法签名并生成Token
        String token = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
        return token;
    }

    /**
     * 从令牌中获取数据声明。
     * 解析Token并返回其中的Claims。
     * @param token 令牌字符串
     * @return 数据声明对象
     */
    private Claims parseToken(String token) {
        // 解析Token并验证签名
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从令牌中获取用户名。
     * 解析Token并返回其中的Subject字段（通常为用户名）。
     * @param token 令牌字符串
     * @return 用户名字符串
     */
    public String getUsernameFromToken(String token) {
        // 解析Token并返回Subject字段
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 获取请求中的Token。
     * 从HTTP请求头中提取Token，并去除前缀。
     * @param request HTTP请求对象
     * @return 提取的Token字符串
     */
    private String getToken(HttpServletRequest request) {
        // 获取请求头中的Token
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX)) {
            // 去除Token前缀
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    /**
     * 构建Token对应的Redis键。
     * 根据UUID生成Redis键，用于存储用户信息。
     * @param uuid 用户唯一标识
     * @return Redis键字符串
     */
    private String getTokenKey(String uuid) {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }
}
