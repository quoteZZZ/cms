package com.cms.common.core.domain.entity;

import com.cms.common.annotation.Excel;
import com.cms.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 成绩结果对象 sys_result
 *
 * @author quoteZZZ
 * @date 2025-03-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "成绩结果对象 sys_result")
public class SysResult extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 成绩编号
     */
    @Excel(name = "成绩编号")
    @ApiModelProperty(value = "成绩编号")
    private Long resultId;

    /**
     * 竞赛编号
     */
    @Excel(name = "竞赛编号")
    @ApiModelProperty(value = "竞赛编号")
    private Long compId;

    /**
     * 报名编号
     */
    @Excel(name = "报名编号")
    @ApiModelProperty(value = "报名编号")
    private Long registrId;

    /**
     * 用户ID
     */
    @ApiModelProperty(value = "用户ID")
    private Long userId;

    /**
     * 组织ID
     */
    @ApiModelProperty(value = "组织ID")
    private Long deptId;

    /**
     * 最终得分
     */
    @Excel(name = "最终得分")
    @ApiModelProperty(value = "最终得分")
    private Double finalScore;

    /**
     * 备注信息
     */
    @Excel(name = "备注信息")
    @ApiModelProperty(value = "备注信息")
    private String remark;

    /**
     * 使用状态
     */
    @ApiModelProperty(value = "使用状态：0-正常，1-停用")
    private String status;

    /**
     * 删除标志
     */
    @ApiModelProperty(value = "删除标志：0-存在，2-已删除")
    private String delFlag;
}

