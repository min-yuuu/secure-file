package com.securefile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件实体
 */
@Data
@TableName("file")
public class File {
    @TableId
    private String fileId;
    private Long userId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Integer chunkCount;
    private String plainSha256;
    private String status;
    private String shareCode;
    private String extractCode;
    private LocalDateTime expiryTime;  // 添加这个字段
    private Integer downloadLimit;
    private Integer downloadCount;
    private Boolean burnAfterReading;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}