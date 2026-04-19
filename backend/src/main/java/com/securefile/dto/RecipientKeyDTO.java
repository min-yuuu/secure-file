package com.securefile.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 接收者封装密钥DTO
 */
@ApiModel("接收者封装密钥")
@Data
public class RecipientKeyDTO {
    @ApiModelProperty(value = "接收者用户ID", required = true, example = "1")
    @NotNull(message = "接收者ID不能为空")
    private Long recipientUserId;

    @ApiModelProperty(value = "用接收者公钥加密的AES密钥", required = true, example = "encrypted_aes_key_base64")
    @NotBlank(message = "封装密钥不能为空")
    private String wrappedAesKey;
}
