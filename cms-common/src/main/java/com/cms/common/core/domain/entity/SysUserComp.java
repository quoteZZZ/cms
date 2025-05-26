package com.cms.common.core.domain.entity;

import com.cms.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户和竞赛关联对象 sys_user_comp
 *
 * @author quoteZZZ
 * @date 2025-05-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "用户和竞赛关联对象 sys_user_comp")
public class SysUserComp extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @ApiModelProperty(value = "用户ID")
    private Long userId;
    
    /** 竞赛ID */
    @ApiModelProperty(value = "竞赛ID")
    private Long compId;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("compId", getCompId())
            .toString();
    }
}
