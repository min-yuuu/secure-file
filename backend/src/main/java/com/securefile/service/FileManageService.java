package com.securefile.service;

/**
 * 文件管理服务接口
 * 职责：文件删除
 */
public interface FileManageService {

    /**
     * 删除文件（逻辑删除）
     */
    void delete(String fileId);
}
