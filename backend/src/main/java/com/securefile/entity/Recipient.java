package com.securefile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件接收者实体
 */
@Data
@TableName("recipient")
public class Recipient {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String fileId;
    
    @TableField("recipient_user_id")
    private Long recipientUserId;
    
    private String wrappedAesKey;
    
    private LocalDateTime createdAt;
}
