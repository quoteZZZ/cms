package com.cms.common.core.domain.entity;

import java.math.BigDecimal;
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
 * @date 2025-05-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "成绩结果对象 sys_result")
public class SysResult extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 成绩ID：使用雪花算法生成
     */
    @Excel(name = "成绩ID：使用雪花算法生成")
    @ApiModelProperty(value = "成绩ID：使用雪花算法生成")
    private Long resultId;

    /**
     * 竞赛ID：所属竞赛编号
     */
    @Excel(name = "竞赛ID：所属竞赛编号")
    @ApiModelProperty(value = "竞赛ID：所属竞赛编号")
    private Long compId;

    /**
     * 报名ID：对应报名记录编号
     */
    @Excel(name = "报名ID：对应报名记录编号")
    @ApiModelProperty(value = "报名ID：对应报名记录编号")
    private Long registrId;

    /**
     * 参赛者ID：关联用户表ID
     */
    @Excel(name = "参赛者ID：关联用户表ID")
    @ApiModelProperty(value = "参赛者ID：关联用户表ID")
    private Long userId;

    /**
     * 部门ID：关联部门表ID
     */
    @Excel(name = "部门ID：关联部门表ID")
    @ApiModelProperty(value = "部门ID：关联部门表ID")
    private Long deptId;

    /**
     * 参赛者名称：冗余字段，便于列表展示
     */
    @Excel(name = "参赛者名称：冗余字段，便于列表展示")
    @ApiModelProperty(value = "参赛者名称：冗余字段，便于列表展示")
    private String userName;

    /**
     * 竞赛名称：冗余字段，便于列表展示
     */
    @Excel(name = "竞赛名称：冗余字段，便于列表展示")
    @ApiModelProperty(value = "竞赛名称：冗余字段，便于列表展示")
    private String compName;

    /**
     * 部门名称：冗余字段，便于列表展示
     */
    @Excel(name = "部门名称：冗余字段，便于列表展示")
    @ApiModelProperty(value = "部门名称：冗余字段，便于列表展示")
    private String deptName;

    /**
     * 最终得分：参赛作品的综合得分
     */
    @Excel(name = "最终得分：参赛作品的综合得分")
    @ApiModelProperty(value = "最终得分：参赛作品的综合得分")
    private BigDecimal finalScore;

    /**
     * 排名：在本竞赛中的名次
     */
    @Excel(name = "排名：在本竞赛中的名次")
    @ApiModelProperty(value = "排名：在本竞赛中的名次")
    private Long rankNum;

    /**
     * 获奖等级：如一等奖/二等奖/三等奖
     */
    @Excel(name = "获奖等级：如一等奖/二等奖/三等奖")
    @ApiModelProperty(value = "获奖等级：如一等奖/二等奖/三等奖")
    private String awardLevel;

    /**
     * 使用状态：0正常/1停用
     */
    @Excel(name = "使用状态：0正常/1停用")
    @ApiModelProperty(value = "使用状态：0正常/1停用")
    private String status;

    /**
     * 删除标志：0存在/2删除
     */
    @ApiModelProperty(value = "删除标志：0存在/2删除")
    private String delFlag;
}

