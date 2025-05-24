package com.cms.web.controller.system;

import com.cms.common.config.CmsConfig;
import com.cms.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页提示表现层：
 * 用于访问首页提示语
 * @author quoteZZZ
 */
@RestController
public class SysIndexController
{
    /** 系统基础配置 */
    @Autowired
    private CmsConfig cmsConfig;

    /**
     * 访问首页，提示语
     */
    @RequestMapping("/")
    public String index()
    {
        return StringUtils.format("欢迎使用{}后台管理框架，当前版本：v{}，请通过前端地址访问。", cmsConfig.getName(), cmsConfig.getVersion());
    }
}
