package com.cms.common.utils;

import java.beans.PropertyEditorSupport;
import java.util.Date;
import java.util.List;

import com.cms.common.core.domain.AjaxResult;
import com.cms.common.core.domain.model.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.cms.common.constant.HttpStatus;
import com.cms.common.utils.page.PageDomain;
import com.cms.common.utils.page.TableDataInfo;
import com.cms.common.utils.page.TableSupport;
import com.cms.common.utils.sql.SqlUtil;

/**
 * 通用数据处理表现层: 提供了一些常用的数据处理方法，用于简化控制器层的代码。
 * 会被所有控制器层继承，简化控制器层的代码。
 * @author quoteZZZ
 */
public class BaseController
{
    // 日志对象，用于记录日志信息
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 将前台传递过来的日期格式的字符串，自动转化为Date类型
     * 这个方法简化了日期类型的数据绑定，避免了手动转换的繁琐
     */
    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        // Date 类型转换
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport()
        {
            @Override
            public void setAsText(String text)
            {
                setValue(DateUtils.parseDate(text));
            }
        });
    }

    /**
     * 设置请求分页数据
     * 为MyBatis分页插件设置分页参数
     */
    protected void startPage()
    {
        PageUtils.startPage();
    }

    /**
     * 设置请求排序数据
     * 根据前端传入的排序参数，为MyBatis分页插件设置排序规则
     */
    protected void startOrderBy()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        if (StringUtils.isNotEmpty(pageDomain.getOrderBy()))
        {
            String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
            PageHelper.orderBy(orderBy);
        }
    }

    /**
     * 清理分页的线程变量
     * 在分页操作后，清理线程局部变量，避免内存泄漏
     */
    protected void clearPage()
    {
        PageUtils.clearPage();
    }

    /**
     * 响应请求分页数据，封装了分页结果，返回给前端
     *
     * @param list 分页查询结果列表
     * @return 封装了分页数据的TableDataInfo对象
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected TableDataInfo getDataTable(List<?> list)
    {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }

    /**
     * 返回成功
     * 用于返回操作成功的结果
     */
    public AjaxResult success()
    {
        return AjaxResult.success();
    }

    /**
     * 返回失败消息
     * 用于返回操作失败的结果
     */
    public AjaxResult error()
    {
        return AjaxResult.error();
    }

    /**
     * 返回成功消息
     *
     * @param message 成功消息文本
     * @return 封装了成功消息的AjaxResult对象
     */
    public AjaxResult success(String message)
    {
        return AjaxResult.success(message);
    }

    /**
     * 返回成功消息
     *
     * @param data 成功后的数据
     * @return 封装了成功消息和数据的AjaxResult对象
     */
    public AjaxResult success(Object data)
    {
        return AjaxResult.success(data);
    }

    /**
     * 返回失败消息
     *
     * @param message 失败消息文本
     * @return 封装了失败消息的AjaxResult对象
     */
    public AjaxResult error(String message)
    {
        return AjaxResult.error(message);
    }

    /**
     * 返回警告消息
     *
     * @param message 警告消息文本
     * @return 封装了警告消息的AjaxResult对象
     */
    public AjaxResult warn(String message)
    {
        return AjaxResult.warn(message);
    }

    /**
     * 响应返回结果
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected AjaxResult toAjax(int rows)
    {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 响应返回结果
     *
     * @param result 结果
     * @return 操作结果
     */
    protected AjaxResult toAjax(boolean result)
    {
        return result ? success() : error();
    }

    /**
     * 页面跳转(重定向)
     *
     * @param url 重定向的目标URL
     * @return 重定向的URL字符串
     */
    public String redirect(String url)
    {
        return StringUtils.format("redirect:{}", url);
    }

    /**
     * 获取用户缓存信息
     * 从缓存中获取当前登录用户信息
     */
    public LoginUser getLoginUser()
    {
        return SecurityUtils.getLoginUser();
    }

    /**
     * 获取登录用户id
     * 从当前登录用户信息中获取用户ID
     */
    public Long getUserId()
    {
        return getLoginUser().getUserId();
    }


    /**
     * 获取登录用户名
     * 从当前登录用户信息中获取用户名
     */
    public String getUsername()
    {
        return getLoginUser().getUsername();
    }
}
