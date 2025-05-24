package com.cms.framework.manager.factory;

import java.util.TimerTask;

import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.core.domain.entity.SysLogininfor;
import com.cms.common.core.domain.entity.SysOperLog;
import com.cms.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cms.common.constant.Constants;
import com.cms.common.utils.LogUtils;
import com.cms.common.utils.ServletUtils;
import com.cms.common.utils.StringUtils;
import com.cms.common.utils.ip.AddressUtils;
import com.cms.common.utils.ip.IpUtils;
import com.cms.common.utils.spring.SpringUtils;
import com.cms.system.service.ISysLogininforService;
import com.cms.system.service.ISysOperLogService;
import eu.bitwalker.useragentutils.UserAgent;

/**
 * 异步工厂类：（产生任务用）
 * 生成异步任务，主要负责记录系统的登录信息和操作日志，通过定时任务的方式将相关信息插入数据库。
 * @author quoteZZZ
 */
public class AsyncFactory
{
    //为后续的日志记录操作准备一个专门的日志记录器实例
    private static final Logger sys_user_logger = LoggerFactory.getLogger("sys-user");

    /**
     * 记录用户登录信息日志的定时任务
     * @param username 用户名
     * @param status 登录状态（成功/失败）
     * @param message 登录的提示消息（i18N里的）
     * @param args 其他可选参数列表
     * @return 任务task（返回一个实现了定时任务的TimerTask对象，用于异步记录登录信息）
     */
    public static TimerTask recordLogininfor(final String username, final String status, final String message,
            final Object... args)
    {
        //解析请求头中的User-Agent信息（用户代理信息），用于获取客户端操作系统和浏览器信息
        final UserAgent userAgent = UserAgent.parseUserAgentString(ServletUtils.getRequest().getHeader("User-Agent"));
        //获取客户端IP地址
        final String ip = IpUtils.getIpAddr();
        return new TimerTask()
        {
            @Override
            public void run()
            {
                //通过IP地址获取真实地址（用户所在地理位置信息）
                String address = AddressUtils.getRealAddressByIP(ip);
                //构建拼接日志信息字符串
                StringBuilder s = new StringBuilder();
                s.append(LogUtils.getBlock(ip));
                s.append(address);
                s.append(LogUtils.getBlock(username));
                s.append(LogUtils.getBlock(status));
                s.append(LogUtils.getBlock(message));
                // 打印信息到日志，输出到控制台
                sys_user_logger.info(s.toString(), args);
                // 获取客户端操作系统
                String os = userAgent.getOperatingSystem().getName();
                // 获取客户端浏览器
                String browser = userAgent.getBrowser().getName();
                // 封装对象（和日志表一一对应）
                SysLogininfor logininfor = new SysLogininfor();
                logininfor.setUserName(username);
                logininfor.setIpaddr(ip);
                logininfor.setLoginLocation(address);
                logininfor.setBrowser(browser);
                logininfor.setOs(os);
                logininfor.setMsg(message);
                // 根据登录状态设置记录状态
                if (StringUtils.equalsAny(status, Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER))
                {
                    logininfor.setStatus(Constants.SUCCESS);//成功设置为0
                }
                else if (Constants.LOGIN_FAIL.equals(status))
                {
                    logininfor.setStatus(Constants.FAIL);//失败设置为1
                }
                //新增功能（用于数据权限过滤）：设置登录部门ID，用户ID
                SysUser sysUser = SpringUtils.getBean(ISysUserService.class).selectUserByUserName(username);//根据用户名查询用户信息（用户id和部门id）
                if(StringUtils.isNotNull(sysUser)){//用户存在
                    logininfor.setUserId(sysUser.getUserId());//设置用户ID
                    logininfor.setDeptId(sysUser.getUserId());///设置部门ID
                }
                // 插入数据，插入到数据库
                SpringUtils.getBean(ISysLogininforService.class).insertLogininfor(logininfor);
            }
        };
    }

    /**
     * 操作日志记录
     * @param operLog 操作日志信息
     * @return 任务task
     */
    public static TimerTask recordOper(final SysOperLog operLog)
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                // 远程查询操作地点
                operLog.setOperLocation(AddressUtils.getRealAddressByIP(operLog.getOperIp()));
                SpringUtils.getBean(ISysOperLogService.class).insertOperlog(operLog);
            }
        };
    }
}
