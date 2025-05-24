package com.cms.common.core.domain.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录参数对象
 * （对应前端传过来的用户登录参数）
 * @author quoteZZZ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(value = "LoginBody", description = "用户登录参数对象")
public class LoginBody {

    /**
     * 用户名
     */
    @ApiModelProperty(value = "用户名", required = true, example = "user123")
    private String username;

    /**
     * 用户密码
     */
    @ApiModelProperty(value = "用户密码", required = true, example = "password123")
    private String password;

    /**
     * 输入验证码
     */
    @ApiModelProperty(value = "用户输入的验证码", required = true, example = "abcd1234")
    private String code;

    /**
     * 正确验证码唯一标识（作为key，找到在redis的正确验证码，与输入验证码比对）
     */
    @ApiModelProperty(value = "验证码唯一标识", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
    private String uuid;
}
