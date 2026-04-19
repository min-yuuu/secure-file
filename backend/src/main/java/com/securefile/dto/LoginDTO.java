package com.securefile.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;
/**
 * @description:
 * @author:zoujingmin
 * @date: 2026/1/26
 */
@ApiModel("登录/注册请求")
@Data
public class LoginDTO {
    @ApiModelProperty(value = "用户名", required = true, example = "alice")
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @ApiModelProperty(value = "密码", required = true, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
