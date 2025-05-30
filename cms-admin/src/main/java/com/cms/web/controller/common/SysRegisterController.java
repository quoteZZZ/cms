package com.cms.web.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.cms.common.core.controller.BaseController;
import com.cms.common.core.domain.AjaxResult;
import com.cms.common.core.domain.model.RegisterBody;
import com.cms.common.utils.StringUtils;
import com.cms.framework.web.service.SysRegisterService;
import com.cms.system.service.ISysConfigService;

/**
 * 注册验证表现层：
 * 用于处理注册请求
 * @author quoteZZZ
 */
@RestController
public class SysRegisterController extends BaseController
{
    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private ISysConfigService configService;

    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterBody user)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("当前系统没有开启注册功能！");
        }
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}