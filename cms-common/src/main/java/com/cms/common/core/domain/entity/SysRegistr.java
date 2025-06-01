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
 * 参赛报名对象 sys_registr
 *
 * @author quoteZZZ
 * @date 2025-05-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "参赛报名对象 sys_registr")
public class SysRegistr extends BaseEntity {
private static final long serialVersionUID = 1L;

        /**
         * 报名ID：使用雪花算法生成
         */
        @Excel(name = "报名ID：使用雪花算法生成")
        @ApiModelProperty(value = "报名ID：使用雪花算法生成")
        private Long registrId;

        /**
         * 竞赛ID：关联竞赛表ID
         */
        @Excel(name = "竞赛ID：关联竞赛表ID")
        @ApiModelProperty(value = "竞赛ID：关联竞赛表ID")
        private Long compId;

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
         * 报名状态：0待审核/1已通过/2已拒绝/3已提交/4已评分
         */
        @Excel(name = "报名状态：0待审核/1已通过/2已拒绝/3已提交/4已评分")
        @ApiModelProperty(value = "报名状态：0待审核/1已通过/2已拒绝/3已提交/4已评分")
        private Character registrStatus;

        /**
         * 评分次数：记录作品被评委评分的次数
         */
        @Excel(name = "评分次数：记录作品被评委评分的次数")
        @ApiModelProperty(value = "评分次数：记录作品被评委评分的次数")
        private Integer scoreCount;

        /**
         * 作品链接：参赛作品文件的存储地址
         */
        @Excel(name = "作品链接：参赛作品文件的存储地址")
        @ApiModelProperty(value = "作品链接：参赛作品文件的存储地址")
        private String materialUrl;

        /**
         * 竞赛名称：冗余字段，便于查询显示
         */
        @Excel(name = "竞赛名称：冗余字段，便于查询显示")
        @ApiModelProperty(value = "竞赛名称：冗余字段，便于查询显示")
        private String compName;

        /**
         * 参赛者：冗余字段，便于查询显示
         */
        @Excel(name = "参赛者：冗余字段，便于查询显示")
        @ApiModelProperty(value = "参赛者：冗余字段，便于查询显示")
        private String userName;

        /**
         * 备注信息：包含报名说明、评审意见等
         */
        @Excel(name = "备注信息：包含报名说明、评审意见等")
        @ApiModelProperty(value = "备注信息：包含报名说明、评审意见等")
        private String remark;

        /**
         * 使用状态：0正常/1停用
         */
        @Excel(name = "使用状态：0正常/1停用")
        @ApiModelProperty(value = "使用状态：0正常/1停用")
        private Character status;

        /**
         * 删除标志：0存在/2删除
         */
        @ApiModelProperty(value = "删除标志：0存在/2删除")
        private Character delFlag;
}

