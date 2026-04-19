package com.securefile.service.impl;

import com.securefile.common.ErrorCode;
import com.securefile.common.SecurityContext;
import com.securefile.exception.BizException;
import com.securefile.entity.File;
import com.securefile.mapper.FileMapper;
import com.securefile.service.FileManageService;
import com.securefile.storage.LocalFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 文件管理服务实现
 * 职责：文件删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileManageServiceImpl implements FileManageService {

    private final FileMapper fileMapper;
    private final LocalFileStorage localStorage;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String fileId) {
        File file = fileMapper.selectById(fileId);

        if (file == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        // 权限校验：只有文件所有者可以删除
        Long userId = SecurityContext.getUserId();
        if (!file.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        // 逻辑删除
        file.setStatus("DELETED");
        file.setUpdatedAt(LocalDateTime.now());
        fileMapper.updateById(file);

        // 物理删除文件（可选，也可以异步处理）
        try {
            localStorage.deleteFileDir(fileId);
        } catch (Exception e) {
            log.error("物理删除文件失败, fileId={}", fileId, e);
        }

        log.info("文件删除成功, fileId={}, userId={}", fileId, userId);
    }
}
