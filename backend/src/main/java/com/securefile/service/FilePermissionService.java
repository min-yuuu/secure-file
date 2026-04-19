package com.securefile.service;

import com.securefile.entity.File;

/**
 * 文件权限控制服务接口
 * 职责：集中所有权限校验逻辑
 */
public interface FilePermissionService {

    /**
     * 校验下载权限（集中校验）
     * @param file 文件实体
     * @param userId 当前用户ID
     * @param extractCode 提取码
     * @throws com.securefile.exception.BizException 权限校验失败时抛出
     */
    void checkDownloadPermission(File file, Long userId, String extractCode);

    /**
     * 校验提取码
     */
    void checkExtractCode(File file, String extractCode);

    /**
     * 校验有效期
     */
    void checkExpiry(File file);

    /**
     * 校验下载次数
     */
    void checkDownloadLimit(File file);

    /**
     * 校验接收者身份
     */
    void checkRecipient(String fileId, Long userId);
}
