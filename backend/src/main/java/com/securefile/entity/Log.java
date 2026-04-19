package com.securefile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 传输日志实体
 */
@Data
@TableName("log")
public class Log {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileId;
    private Long userId;
    private String action; // UPLOAD/DOWNLOAD
    private String ip;
    private String userAgent;
    private LocalDateTime createTime;
}
