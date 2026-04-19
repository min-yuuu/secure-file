package com.securefile.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 上传完成请求
 */
@ApiModel("上传完成请求")
@Data
public class UploadCompleteDTO {
    @ApiModelProperty(value = "文件ID", required = true, example = "a1b2c3d4e5f6")
    @NotBlank(message = "文件ID不能为空")
    private String fileId;

    @ApiModelProperty(value = "明文文件SHA256", example = "abc123def456...")
    private String plainSha256;

    @ApiModelProperty(value = "接收者封装密钥列表", required = true)
    @NotNull(message = "接收者密钥列表不能为空")
    private List<RecipientKeyDTO> recipientsWrappedKeys;
}
