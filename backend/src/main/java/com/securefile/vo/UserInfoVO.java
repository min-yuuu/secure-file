package com.securefile.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户信息VO
 */
@Data
public class UserInfoVO {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 是否已上传公钥
     */
    private Boolean hasPublicKey;
    
    /**
     * 公钥（可选，用于接收者选择时展示）
     */
    private String publicKey;
    
    /**
     * 注册时间
     */
    private LocalDateTime createTime;
}
