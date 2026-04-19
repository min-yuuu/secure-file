package com.securefile.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件列表VO
 */
@ApiModel("文件列表信息")
@Data
public class FileVO {
    @ApiModelProperty("文件ID")
    private String fileId;
    
    @ApiModelProperty("文件名")
    private String fileName;
    
    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;
    
    @ApiModelProperty("MIME类型")
    private String contentType;
    
    @ApiModelProperty("状态：UPLOADING/COMPLETE/DELETED")
    private String status;
    
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
    
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;

    @ApiModelProperty("过期时间")
    private LocalDateTime expiryTime;
    
    @ApiModelProperty("下载次数限制（-1表示不限制）")
    private Integer downloadLimit;
    
    @ApiModelProperty("已下载次数")
    private Integer downloadCount;
    
    @ApiModelProperty("是否阅后即焚")
    private Boolean burnAfterReading;
}
