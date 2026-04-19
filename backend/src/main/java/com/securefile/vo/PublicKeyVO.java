package com.securefile.vo;
/**
 * @description:
 * @author:zoujingmin
 * @date: 2026/1/26
 */
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用于向前端返回目标用户的公钥信息
 */
@ApiModel("用户公钥信息")
@Data
public class PublicKeyVO {
    @ApiModelProperty("用户ID")
    private String userId;
    
    @ApiModelProperty("用户名")
    private String username;
    
    @ApiModelProperty("RSA公钥（Base64编码字符串）")
    private String publicKey;

    @ApiModelProperty("注册时间")
    private java.time.LocalDateTime createTime;
}
