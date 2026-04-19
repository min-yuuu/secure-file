package com.securefile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件分片实体
 */
@Data
@TableName("chunk")
public class Chunk {
    @TableId(type = IdType.AUTO)
    private Long chunkId;
    private String fileId;
    private Integer chunkIndex;
    private Long chunkSize;
    private String cipherSha256;
    private String storagePath;
    private LocalDateTime uploadedAt;
}
