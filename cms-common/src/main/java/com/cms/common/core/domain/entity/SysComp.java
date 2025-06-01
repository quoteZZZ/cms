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
    @ApiModelProperty(value = "竞赛编号：系统生成的唯一标识符")
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
    @ApiModelProperty(value = "竞赛名称：如\"大学生创新创业大赛\"")
    private String compName;

    /**
     * 竞赛图片
     */
    @Excel(name = "竞赛图片")
    @ApiModelProperty(value = "竞赛图片：大赛宣传海报或Logo图片的URL")
    private String compImageUrl;

    /**
     * 竞赛类别
     */
    @Excel(name = "竞赛类别", readConverterExp = "0=其他类型,1=文学艺术,2=体育竞技,3=学术竞赛,4=创业创新,5=公益社会,6=科技创新")
    @ApiModelProperty(value = "竞赛类别：0其他类型/1文学艺术/2体育竞技/3学术竞赛/4创业创新/5公益社会/6科技创新")
    private Character compCategory;

    /**
     * 竞赛模式
     */
    @Excel(name = "竞赛模式", readConverterExp = "0=线上个人赛,1=线下个人赛,2=线上团队赛,3=线下团队赛,4=混合模式赛")
    @ApiModelProperty(value = "竞赛模式：0线上个人赛/1线下个人赛/2线上团队赛/3线下团队赛/4混合模式赛")
    private Character compMode;

    /**
     * 竞赛状态
     */
    @Excel(name = "竞赛状态", readConverterExp = "0=未开始,1=进行中,2=已结束")
    @ApiModelProperty(value = "竞赛状态：0未开始/1进行中/2已结束")
    private Character compStatus;

    /**
     * 竞赛阶段
     */
    @Excel(name = "竞赛阶段", readConverterExp = "0=报名,1=初赛,2=复赛,3=决赛,4=评审,5=公示")
    @ApiModelProperty(value = "竞赛阶段：0报名/1初赛/2复赛/3决赛/4评审/5公示")
    private Character stageStatus;

    /**
     * 竞赛开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Excel(name = "竞赛开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "开始时间：竞赛报名开始时间")
    private Date compStartTime;

    /**
     * 竞赛结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @Excel(name = "竞赛结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "结束时间：竞赛最终结束时间")
    private Date compEndTime;

    /**
     * 推荐标志：0-不推荐，1-推荐
     */
    @ApiModelProperty(value = "推荐标志：0普通赛事/1推荐赛事")
    private Character isRecommended;

    /**
     * 访问频率
     */
    @Excel(name = "访问频率")
    @ApiModelProperty(value = "访问频率：竞赛详情页访问计数")
    private Integer accessFrequency;

    /**
     * 备注信息：审批备注或其他说明
     */
    @Excel(name = "备注信息")
    @ApiModelProperty(value = "竞赛备注：包括评审要求、奖项设置等补充说明")
    private String remark;

    /**
     * 使用状态：0-正常，1-停用
     */
    @Excel(name = "使用状态", readConverterExp = "0=正常,1=停用")
    @ApiModelProperty(value = "状态标志：0正常/1停用")
    private Character status;

    /**
     * 删除标志：0-存在，2-已删除
     */
    @ApiModelProperty(value = "删除标志：0存在/2删除")
    private Character delFlag;

}
