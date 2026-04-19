package com.securefile.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TransferLogVO {
    private String fileId;
    private String fileName;
    private String action;
    private String ip;
    private LocalDateTime createTime;
}
