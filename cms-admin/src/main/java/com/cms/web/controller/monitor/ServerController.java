package com.cms.web.controller.monitor;

import com.cms.common.core.domain.AjaxResult;
import com.cms.framework.web.domain.Server;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务器监控表现层：
 * 用于获取服务器监控信息
 * @author quoteZZZ
 */
@RestController
@RequestMapping("/monitor/server")
public class ServerController
{
    // 获取服务器监控信息
    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping()
    public AjaxResult getInfo() throws Exception
    {
        Server server = new Server();
        server.copyTo();
        return AjaxResult.success(server);
    }
}
