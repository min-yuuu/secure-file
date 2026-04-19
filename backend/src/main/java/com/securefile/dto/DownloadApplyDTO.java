package com.securefile.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 下载申请请求
 */
@ApiModel("下载申请请求")
@Data
public class DownloadApplyDTO {
    @ApiModelProperty(value = "文件ID", required = true, example = "a1b2c3d4e5f6")
    @NotBlank(message = "文件ID不能为空")
    private String fileId;

    @ApiModelProperty(value = "提取码（如果设置了）", example = "123456")
    private String extractCode;
}
