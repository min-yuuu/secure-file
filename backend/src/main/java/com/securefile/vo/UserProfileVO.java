package com.securefile.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserProfileVO {
    private String username;
    private LocalDateTime createTime;
    private Boolean hasPublicKey;
}
