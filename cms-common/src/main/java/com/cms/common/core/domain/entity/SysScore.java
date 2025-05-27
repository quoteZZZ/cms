package com.cms.common.core.domain.entity;

import com.cms.common.annotation.Excel;
import com.cms.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * 评分信息对象 sys_score
 *
 * @author quoteZZZ
 * @date 2025-03-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "评分信息对象 sys_score")
public class SysScore extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 评分编号
     */
    @ApiModelProperty(value = "评分编号")
    private Long scoreId;

    /**
     * 成绩编号
     */
    @Excel(name = "成绩编号")
    @ApiModelProperty(value = "成绩编号")
    private Long resultId;

    /**
     * 报名编号
     */
    @Excel(name = "报名编号")
    @ApiModelProperty(value = "报名编号")
    private Long registrId;

    /**
     * 评委编号
     */
    @Excel(name = "评委编号")
    @ApiModelProperty(value = "评委编号")
    private Long judgeId;

    /**
     * 分数结果
     */
    @Excel(name = "分数结果")
    @ApiModelProperty(value = "分数结果")
    private Double score;

    /**
     * 评委名称
     */
    @Excel(name = "评委名称")
    @ApiModelProperty(value = "评委名称")
    private String judgeName;

    /**
     * 参赛者名称
     */
    @Excel(name = "参赛者名称")
    @ApiModelProperty(value = "参赛者名称")
    private String userName;

    /**
     * 竞赛名称
     */
    @Excel(name = "竞赛名称")
    @ApiModelProperty(value = "竞赛名称")
    private String compName;

    /**
     * 评分时间
     */
    @Excel(name = "评分时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "评分时间")
    private Date scoreTime;

    /**
     * 评语
     */
    @Excel(name = "评语")
    @ApiModelProperty(value = "评语")
    private String comment;

    /**
     * 使用状态：0-正常，1-停用
     */
    @Excel(name = "使用状态", readConverterExp = "0=正常,1=停用")
    @ApiModelProperty(value = "使用状态：0-正常，1-停用")
    private String status;

    /**
     * 删除标志：0-存在，2-已删除
     */
    @ApiModelProperty(value = "删除标志：0-存在，2-已删除")
    private String delFlag;

    /**
     * 用户ID
     */
    @ApiModelProperty(value = "参赛者ID")
    private Long userId;

    /**
     * 组织ID
     */
    @ApiModelProperty(value = "组织ID")
    private Long deptId;
}
