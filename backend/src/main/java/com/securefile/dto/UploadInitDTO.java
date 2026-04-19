package com.securefile.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 上传初始化请求
 */
@ApiModel("上传初始化请求")
@Data
public class UploadInitDTO {
    @ApiModelProperty(value = "文件名", required = true, example = "document.pdf")
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @ApiModelProperty(value = "文件大小（字节）", required = true, example = "1048576")
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;

    @ApiModelProperty(value = "MIME类型", example = "application/pdf")
    private String contentType;

    @ApiModelProperty(value = "分片总数", required = true, example = "10")
    @NotNull(message = "分片数量不能为空")
    @Min(value = 1, message = "分片数量必须大于0")
    private Integer chunkCount;

    @ApiModelProperty(value = "明文文件SHA256（可选）", example = "abc123def456...")
    private String plainSha256;

    @ApiModelProperty(value = "提取码（可选）", example = "123456")
    private String extractCode;

    @ApiModelProperty(value = "有效期（小时，-1表示永久）", example = "24")
    private Integer expiryHours;

    @ApiModelProperty(value = "下载次数限制（-1表示无限）", example = "5")
    private Integer downloadLimit;

    @ApiModelProperty(value = "是否阅后即焚", example = "false")
    private Boolean burnAfterReading;
}
