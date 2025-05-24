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
     * 竞赛ID
     */
    @Excel(name = "竞赛ID")
    @ApiModelProperty(value = "竞赛ID")
    private Long compId;

    /**
     * 竞赛名称
     */
    @Excel(name = "竞赛名称")
    @ApiModelProperty(value = "竞赛名称")
    private String compName;

    /**
     * 竞赛类别
     */
    @Excel(name = "竞赛类别", readConverterExp = "0=其他类型,1=文学艺术,2=体育竞技,3=学术竞赛,4=创业创新,5=公益社会,6=科技创新")
    @ApiModelProperty(value = "竞赛类别")
    private String compCategory;

    /**
     * 竞赛模式
     */
    @Excel(name = "竞赛模式", readConverterExp = "0=线上个人赛,1=线下个人赛,2=线上团队赛,3=线下团队赛,4=混合模式赛")
    @ApiModelProperty(value = "竞赛模式")
    private String compMode;

    /**
     * 竞赛状态
     */
    @Excel(name = "竞赛状态", readConverterExp = "0=未开始,1=进行中,2=已结束")
    @ApiModelProperty(value = "竞赛状态")
    private String compStatus;

    /**
     * 竞赛开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "竞赛开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "竞赛开始时间")
    private Date startTime;

    /**
     * 竞赛结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "竞赛结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "竞赛结束时间")
    private Date endTime;

    /**
     * 竞赛地点
     */
    @Excel(name = "竞赛地点")
    @ApiModelProperty(value = "竞赛地点")
    private String location;

    /**
     * 竞赛规模
     */
    @Excel(name = "竞赛规模")
    @ApiModelProperty(value = "竞赛规模")
    private Integer scale;

    /**
     * 竞赛费用
     */
    @Excel(name = "竞赛费用")
    @ApiModelProperty(value = "竞赛费用")
    private Double fee;

    /**
     * 竞赛联系人
     */
    @Excel(name = "竞赛联系人")
    @ApiModelProperty(value = "竞赛联系人")
    private String contact;

    /**
     * 竞赛联系人电话
     */
    @Excel(name = "竞赛联系人电话")
    @ApiModelProperty(value = "竞赛联系人电话")
    private String contactPhone;

    /**
     * 竞赛联系人邮箱
     */
    @Excel(name = "竞赛联系人邮箱")
    @ApiModelProperty(value = "竞赛联系人邮箱")
    private String contactEmail;

    /**
     * 竞赛详情
     */
    @Excel(name = "竞赛详情")
    @ApiModelProperty(value = "竞赛详情")
    private String details;

    /**
     * 推荐标志：0-不推荐，1-推荐
     */
    @ApiModelProperty(value = "推荐标志：0-不推荐，1-推荐")
    private String isRecommended;

    /**
     * 访问频率 (int -> Integer)
     */
    @Excel(name = "访问频率")
    @ApiModelProperty(value = "访问频率")
    private Integer accessFrequency; // 从Long改为Integer

    /**
     * 使用状态：0-正常，1-停用
     */
    @Excel(name = "使用状态", readConverterExp = "0=正常,1=停用")
    @ApiModelProperty(value = "使用状态：0-正常，1-停用")
    private String status;

    /**
     * 删除标志：0-存在，2-已删除 (Character -> String)
     */
    @ApiModelProperty(value = "删除标志：0-存在，2-已删除")
    private String delFlag; // 从Character改为String

    /**
     * 是否随机推荐
     */
    @ApiModelProperty(value = "是否随机推荐")
    private Boolean isRandom;

    /**
     * 是否按访问频率排序
     */
    @ApiModelProperty(value = "是否按访问频率排序")
    private Boolean orderByAccessFrequency;

    /**
     * 是否按最新竞赛排序
     */
    @ApiModelProperty(value = "是否按最新竞赛排序")
    private Boolean orderByLatest;
}