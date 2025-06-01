package com.cms.common.core.domain.entity;

import com.cms.common.annotation.Excel;
import com.cms.common.annotation.Xss;
import com.cms.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 通知公告表 sys_notice的实体类：
 * 用于公告信息
 * @author quoteZZZ
 * @date 2025-05-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "通知公告表：发布竞赛相关通知、公告")
public class SysNotice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 公告ID：自增主键 */
    @ApiModelProperty(value = "公告ID：自增主键")
    private Integer noticeId;  // 修改为Integer以匹配数据库中的int类型

    /** 公告标题：如"关于XX竞赛的通知" */
    @Excel(name = "公告标题")
    @ApiModelProperty(value = "公告标题：如\"关于XX竞赛的通知\"")
    @Xss(message = "公告标题不能包含脚本字符")
    @NotBlank(message = "公告标题不能为空")
    @Size(min = 0, max = 50, message = "公告标题不能超过50个字符")
    private String noticeTitle;

    /** 公告类型：1通知(如赛事通知)/2公告(如获奖公示) */
    @Excel(name = "公告类型", readConverterExp = "1=通知,2=公告")
    @ApiModelProperty(value = "公告类型：1通知(如赛事通知)/2公告(如获奖公示)")
    private Character noticeType;

    /** 公告内容：支持富文本格式 */
    @Excel(name = "公告内容")
    @ApiModelProperty(value = "公告内容：支持富文本格式")
    private String noticeContent;

    /** 公告内容字符串(用于前端展示) */
    @ApiModelProperty(value = "公告内容字符串(用于前端展示)")
    private transient String noticeContentString;

    /** 公告状态：0正常/1关闭 */
    @Excel(name = "公告状态", readConverterExp = "0=正常,1=关闭")
    @ApiModelProperty(value = "公告状态：0正常/1关闭")
    private Character status;

    /** 发布用户：关联用户表ID */
    @Excel(name = "发布用户")
    @ApiModelProperty(value = "发布用户：关联用户表ID")
    private Long userId;

    /** 发布部门：关联部门表ID */
    @Excel(name = "发布部门")
    @ApiModelProperty(value = "发布部门：关联部门表ID")
    private Long deptId;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("noticeId", getNoticeId())
            .append("noticeTitle", getNoticeTitle())
            .append("noticeType", getNoticeType())
            .append("noticeContent", getNoticeContent())
            .append("status", getStatus())
            .append("userId", getUserId())
            .append("deptId", getDeptId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
