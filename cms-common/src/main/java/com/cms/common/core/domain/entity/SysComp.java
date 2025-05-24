package com.cms.common.core.domain.entity;

import java.util.Date;

import com.cms.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.cms.common.annotation.Excel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 竞赛信息对象 sys_comp
 *
 * @author quoteZZZ
 * @date 2025-03-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "竞赛信息对象 sys_comp")
public class SysComp extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 竞赛编号
     */
    @Excel(name = "竞赛编号")
    @ApiModelProperty(value = "竞赛编号")
    private Long compId;

    /**
     * 关联的部门ID
     */
    @ApiModelProperty(value = "关联的部门ID")
    private Long deptId;

    /**
     * 竞赛名称
     */
    @Excel(name = "竞赛名称")
    @ApiModelProperty(value = "竞赛名称")
    private String compName;

    /**
     * 竞赛图片
     */
    @Excel(name = "竞赛图片")
    @ApiModelProperty(value = "竞赛图片")
    private String compImageUrl;


    /**
     * 竞赛类别
     */
    @Excel(name = "竞赛类别", readConverterExp = "0=其他类型,1=文学艺术,2=体育竞技,3=学术竞赛,4=创业创新,5=公益社会,6=科技创新")
    @ApiModelProperty(value = "竞赛类别")
    private Character compCategory;

    /**
     * 竞赛模式
     */
    @Excel(name = "竞赛模式", readConverterExp = "0=线上个人赛,1=线下个人赛,2=线上团队赛,3=线下团队赛,4=混合模式赛")
    @ApiModelProperty(value = "竞赛模式")
    private Character compMode;

    /**
     * 竞赛状态
     */
    @Excel(name = "竞赛状态", readConverterExp = "0=未开始,1=进行中,2=已结束")
    @ApiModelProperty(value = "竞赛状态")
    private Character compStatus;

    /**
     * 竞赛阶段
     */
    @Excel(name = "竞赛阶段", readConverterExp = "0=报名,1=初赛,2=复赛,3=决赛,4=评审,5=公示")
    @ApiModelProperty(value = "竞赛阶段")
    private Character stageStatus;

    /**
     * 竞赛开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Excel(name = "竞赛开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "竞赛开始时间")
    private Date compStartTime;

    /**
     * 竞赛结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Excel(name = "竞赛结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "竞赛结束时间")
    private Date compEndTime;

    /**
     * 推荐标志：0-不推荐，1-推荐
     */
    @ApiModelProperty(value = "推荐标志：0-不推荐，1-推荐，默认值为'0'")
    private Character isRecommended; // 注释补充：明确默认值为'0'

    /**
     * 访问频率
     */
    @Excel(name = "访问频率")
    @ApiModelProperty(value = "访问频率")
    private Integer accessFrequency; // 修改：将Long改为Integer以匹配SQL中的int类型

    /**
     * 备注信息：审批备注或其他说明
     */
    @Excel(name = "备注信息")
    @ApiModelProperty(value = "备注信息：审批备注或其他说明")
    private String remark;

    /**
     * 使用状态：0-正常，1-停用
     */
    @Excel(name = "使用状态", readConverterExp = "0=正常,1=停用")
    @ApiModelProperty(value = "使用状态：0-正常，1-停用，默认值为'0'")
    private Character status; // 注释补充：明确默认值为'0'

    // 新增：强制包含status字段的toString实现
    @Override
    public String toString() {
        return "SysComp{" +
            "compId=" + compId +
            ", compName='" + compName + '\'' +
            ", status=" + status +
            ", compCategory=" + compCategory +
            ", compMode=" + compMode +
            ", compStatus=" + compStatus +
            '}';
    }

    /**
     * 删除标志：0-存在，2-已删除
     */
    @ApiModelProperty(value = "删除标志：0-存在，2-已删除")
    private Character delFlag;

    /**
     * 创建者
     */
    @Excel(name = "创建者")
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新者
     */
    @Excel(name = "更新者")
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时��", width = 30, dateFormat = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }
}
