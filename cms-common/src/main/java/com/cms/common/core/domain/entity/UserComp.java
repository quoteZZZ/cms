package com.cms.common.core.domain.entity;

import com.cms.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

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
public class UserComp extends BaseEntity {
private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        @ApiModelProperty(value = "用户ID")
        private Long userId;

        /**
         * 竞赛ID
         */
        @ApiModelProperty(value = "竞赛ID")
        private Long compId;

}

