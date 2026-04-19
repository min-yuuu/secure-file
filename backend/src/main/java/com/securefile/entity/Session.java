package com.securefile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 下载会话实体
 */
@Data
@TableName("session")
public class Session {
    @TableId
    private String sessionId;
    private String fileId;
    private Long userId;
    private String wrappedKey;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
}
