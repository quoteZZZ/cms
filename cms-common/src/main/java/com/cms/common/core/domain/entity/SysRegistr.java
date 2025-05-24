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
 * 报名信息对象 sys_registr
 *
 * @author quoteZZZ
 * @date 2025-03-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "报名信息对象 sys_registr")
public class SysRegistr extends BaseEntity {
private static final long serialVersionUID = 1L;

        /**
         * 报名编码
         */
        @Excel(name = "报名编码")
        @ApiModelProperty(value = "报名编码")
        private Long registrId;

        /**
         * 竞赛编码
         */
        @Excel(name = "竞赛编码")
        @ApiModelProperty(value = "竞赛编码")
        private Long compId;

        /**
         * 用户编码
         */
        @Excel(name = "用户编码")
        @ApiModelProperty(value = "用户编码")
        private Long userId;

        /**
         * 组织ID
         */
        @Excel(name = "组织ID")
        @ApiModelProperty(value = "组织ID")
        private Long deptId;

        /**
         * 报名状态
         */
        @Excel(name = "报名状态",readConverterExp = "0=待审批,1=已通过,2=未通过")
        @ApiModelProperty(value = "报名状态")
        private String registrStatus;

        /**
         * 竞赛作品
         */
        @Excel(name = "竞赛作品")
        @ApiModelProperty(value = "竞赛作品")
        private String materialUrl;

        /**
         * 竞赛名称
         */
        @Excel(name = "竞赛名称")
        @ApiModelProperty(value = "竞赛名称")
        private String compName;

        /**
         * 用户名称
         */
        @Excel(name = "用户名称")
        @ApiModelProperty(value = "用户名称")
        private String userName;

        /**
         * 评分频率
         */
        @Excel(name = "评分频率")
        @ApiModelProperty(value = "评分频率")
        private Long scoreCount;

        /**
         * 使用状态：0-正常，1-停用
         */
        @Excel(name = "使用状态",readConverterExp = "0=正常,1=停用")
        @ApiModelProperty(value = "使用状态：0-正常，1-停用")
        private String status;

        /**
         * 删除标志：0-存在，2-已删除
         */
        @ApiModelProperty(value = "删除标志：0-存在，2-已删除")
        private String delFlag;
}

