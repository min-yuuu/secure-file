package com.securefile.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@ApiModel("审计日志VO")
@Data
public class AuditLogVO {
    @ApiModelProperty("日志ID")
    private Long id;

    @ApiModelProperty("文件ID")
    private String fileId;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("操作类型")
    private String action;

    @ApiModelProperty("IP地址")
    private String ip;

    @ApiModelProperty("用户代理")
    private String userAgent;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}