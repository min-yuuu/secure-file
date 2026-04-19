package com.securefile.dto;
/**
 * @description:
 * @author:zoujingmin
 * @date: 2026/1/26
 */
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 公钥上传DTO
 */
@ApiModel("公钥上传请求")
@Data
public class PublicKeyDTO {
    @ApiModelProperty(value = "RSA公钥（PEM格式）", required = true, example = "-----BEGIN PUBLIC KEY-----\\nMIIBIjAN...\\n-----END PUBLIC KEY-----")
    @NotBlank(message = "公钥不能为空")
    private String publicKey;
}
