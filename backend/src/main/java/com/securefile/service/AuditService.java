package com.securefile.service;

import com.securefile.common.PageResult;
import com.securefile.vo.AuditLogVO;  // 导入 VO

/**
 * 审计服务接口
 * 职责：记录传输日志、提供审计查询
 */
public interface AuditService {

    /**
     * 记录上传日志
     */
    void logUpload(String fileId, Long userId, String ip, String userAgent);

    /**
     * 记录下载日志
     */
    void logDownload(String fileId, Long userId, String ip, String userAgent);

    /**
     * 分页查询传输日志
     * 返回值改为 AuditLogVO
     */
    PageResult<AuditLogVO> pageTransferLogs(Integer current, Integer size, String action, Long userId);
}