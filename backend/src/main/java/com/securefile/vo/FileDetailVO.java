package com.securefile.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件详情VO
 */
@ApiModel("文件详情信息")
@Data
public class FileDetailVO {
    @ApiModelProperty("文件ID")
    private String fileId;
    
    @ApiModelProperty("上传者ID")
    private Long userId;
    
    @ApiModelProperty("文件名")
    private String fileName;
    
    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;
    
    @ApiModelProperty("MIME类型")
    private String contentType;
    
    @ApiModelProperty("分片总数")
    private Integer chunkCount;
    
    @ApiModelProperty("明文文件SHA256")
    private String plainSha256;
    
    @ApiModelProperty("状态：UPLOADING/COMPLETE/DELETED")
    private String status;

    @ApiModelProperty("过期时间")
    private LocalDateTime expiryTime;
    
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
    
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
