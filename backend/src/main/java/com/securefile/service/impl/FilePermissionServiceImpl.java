package com.securefile.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.securefile.common.ErrorCode;
import com.securefile.entity.File;
import com.securefile.entity.Recipient;
import com.securefile.exception.BizException;
import com.securefile.mapper.FileRecipientMapper;
import com.securefile.mapper.FileMapper;
import com.securefile.service.FilePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 文件权限控制服务实现
 * 职责：集中所有权限校验逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilePermissionServiceImpl implements FilePermissionService {

    private final FileRecipientMapper recipientMapper;
    private final FileMapper fileMapper;

    @Override
    public void checkDownloadPermission(File file, Long userId, String extractCode) {
        // 1. 检查文件状态
        if (!"COMPLETE".equals(file.getStatus())) {
            throw new BizException(ErrorCode.FILE_STATUS_ERROR);
        }

        // 2. 检查有效期
        checkExpiry(file);

        // 3. 检查下载次数
        checkDownloadLimit(file);

        // 4. 检查接收者身份（如果不是owner）
        if (!file.getUserId().equals(userId)) {
            checkRecipient(file.getFileId(), userId);
        }

        // 5. 检查提取码
        checkExtractCode(file, extractCode);

        log.info("权限校验通过, fileId={}, userId={}", file.getFileId(), userId);
    }

    @Override
    public void checkExtractCode(File file, String extractCode) {
        if (file.getExtractCode() != null && !file.getExtractCode().isEmpty()) {
            if (extractCode == null || !BCrypt.checkpw(extractCode, file.getExtractCode())) {
                log.warn("提取码错误, fileId={}", file.getFileId());
                throw new BizException(ErrorCode.EXTRACT_CODE_ERROR);
            }
        }
    }

    @Override
    public void checkExpiry(File file) {
        if (file.getExpiryTime() != null && LocalDateTime.now().isAfter(file.getExpiryTime())) {
            file.setStatus("EXPIRED");
            fileMapper.updateById(file);
            log.warn("文件已过期, fileId={}, expiryTime={}", file.getFileId(), file.getExpiryTime());
            throw new BizException(ErrorCode.FILE_EXPIRED);
        }
    }

    @Override
    public void checkDownloadLimit(File file) {
        if (file.getDownloadLimit() != null && file.getDownloadLimit() > 0) {
            if (file.getDownloadCount() >= file.getDownloadLimit()) {
                file.setStatus("LIMIT_REACHED");
                fileMapper.updateById(file);
                log.warn("下载次数已达上限, fileId={}, limit={}, count={}",
                        file.getFileId(), file.getDownloadLimit(), file.getDownloadCount());
                throw new BizException(ErrorCode.DOWNLOAD_LIMIT_EXCEEDED);
            }
        }
    }

    @Override
    public void checkRecipient(String fileId, Long userId) {
        Recipient recipient = recipientMapper.selectOne(
                new LambdaQueryWrapper<Recipient>()
                        .eq(Recipient::getFileId, fileId)
                        .eq(Recipient::getRecipientUserId, userId)
        );

        if (recipient == null) {
            log.warn("用户不在接收者列表中, fileId={}, userId={}", fileId, userId);
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}
