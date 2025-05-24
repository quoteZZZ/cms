package com.cms.common.core.domain.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户和竞赛关联 sys_user_comp表的实体类：
 * 用于封装用户和竞赛关联信息
 * @author quoteZZZ
 */
public class SysUserComp
{
    /** 用户ID */
    private Long userId;
    
    /** 竞赛ID */
    private Long compId;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getCompId()
    {
        return compId;
    }

    public void setCompId(Long roleId)
    {
        this.compId = roleId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("compId", getCompId())
            .toString();
    }
}
