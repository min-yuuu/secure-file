package com.securefile.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.securefile.common.ErrorCode;
import com.securefile.common.SecurityContext;
import com.securefile.exception.BizException;
import com.securefile.entity.Chunk;
import com.securefile.entity.File;
import com.securefile.entity.Recipient;
import com.securefile.mapper.FileChunkMapper;
import com.securefile.mapper.FileMapper;
import com.securefile.mapper.FileRecipientMapper;
import com.securefile.service.AuditService;
import com.securefile.service.FileUploadService;
import com.securefile.storage.LocalFileStorage;
import com.securefile.dto.*;
import com.securefile.vo.UploadInitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传服务实现
 * 职责：只负责接收密文分片、断点续传、保存wrappedKey
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final FileMapper fileMapper;
    private final FileChunkMapper chunkMapper;
    private final FileRecipientMapper recipientMapper;
    private final LocalFileStorage localStorage;
    private final AuditService auditService;
    private final HttpServletRequest request;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadInitVO init(UploadInitDTO dto) {
        // 获取当前用户ID
        Long userId = SecurityContext.getUserId();

        // 生成文件ID
        String fileId = IdUtil.fastSimpleUUID();

        // 创建文件记录，状态设为UPLOADING
        File file = new File();
        file.setFileId(fileId);
        file.setUserId(userId);
        file.setFileName(dto.getFileName());
        file.setFileSize(dto.getFileSize());
        file.setContentType(dto.getContentType());
        file.setChunkCount(dto.getChunkCount());
        file.setPlainSha256(dto.getPlainSha256());
        file.setStatus("UPLOADING");
        
        // 设置提取码（只存hash）
        if (dto.getExtractCode() != null && !dto.getExtractCode().isEmpty()) {
            file.setExtractCode(BCrypt.hashpw(dto.getExtractCode(), BCrypt.gensalt()));
        }
        
        // 设置有效期
        if (dto.getExpiryHours() != null && dto.getExpiryHours() > 0) {
            file.setExpiryTime(LocalDateTime.now().plusHours(dto.getExpiryHours()));
        }
        
        // 设置下载限制
        file.setDownloadLimit(dto.getDownloadLimit() != null ? dto.getDownloadLimit() : -1);
        file.setDownloadCount(0);
        file.setBurnAfterReading(dto.getBurnAfterReading() != null ? dto.getBurnAfterReading() : false);
        
        file.setCreatedAt(LocalDateTime.now());
        file.setUpdatedAt(LocalDateTime.now());
        fileMapper.insert(file);

        // 查询已上传的分片列表（用于断点续传）
        List<Integer> uploadedChunks = chunkMapper.selectUploadedIndexes(fileId);

        UploadInitVO vo = new UploadInitVO();
        vo.setFileId(fileId);
        vo.setUploadedChunkIndexList(uploadedChunks);

        log.info("初始化上传成功, fileId={}, fileName={}, uploadedChunks={}",
                fileId, dto.getFileName(), uploadedChunks.size());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadChunk(String fileId, Integer chunkIndex, String cipherSha256, MultipartFile blob) {
        // 1. 校验文件是否存在
        File file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        // 2. 权限校验：只能上传自己的文件
        Long userId = SecurityContext.getUserId();
        if (!file.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        // 3. 校验文件状态
        if ("COMPLETE".equals(file.getStatus()) || "DELETED".equals(file.getStatus())) {
            throw new BizException(ErrorCode.FILE_STATUS_ERROR);
        }

        // 4. 幂等性检查：如果分片已存在且hash一致，直接返回
        Chunk existingChunk = chunkMapper.selectOne(
                new LambdaQueryWrapper<Chunk>()
                        .eq(Chunk::getFileId, fileId)
                        .eq(Chunk::getChunkIndex, chunkIndex));

        if (existingChunk != null && existingChunk.getCipherSha256().equals(cipherSha256)) {
            log.info("分片已存在, fileId={}, chunkIndex={}", fileId, chunkIndex);
            return;
        }

        try {
            // 5. 读取文件数据并计算SHA256
            byte[] bytes = blob.getBytes();
            String actualHash = DigestUtil.sha256Hex(bytes);

            // 6. 校验hash
            if (!actualHash.equals(cipherSha256)) {
                throw new BizException(ErrorCode.CHUNK_HASH_MISMATCH);
            }

            // 7. 保存密文分片到磁盘
            String storagePath = localStorage.saveChunk(fileId, chunkIndex, bytes);

            // 8. 保存分片记录
            Chunk chunk = new Chunk();
            chunk.setFileId(fileId);
            chunk.setChunkIndex(chunkIndex);
            chunk.setChunkSize((long) bytes.length);
            chunk.setCipherSha256(cipherSha256);
            chunk.setStoragePath(storagePath);
            chunk.setUploadedAt(LocalDateTime.now());

            if (existingChunk != null) {
                chunk.setChunkId(existingChunk.getChunkId());
                chunkMapper.updateById(chunk);
            } else {
                chunkMapper.insert(chunk);
            }

            log.info("分片上传成功, fileId={}, chunkIndex={}, size={}",
                    fileId, chunkIndex, bytes.length);

        } catch (Exception e) {
            log.error("分片上传失败", e);
            throw new BizException(ErrorCode.STORAGE_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(UploadCompleteDTO dto) {
        String fileId = dto.getFileId();

        // 1. 校验文件是否存在
        File file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        // 2. 权限校验：只能完成自己的文件上传
        Long userId = SecurityContext.getUserId();
        if (!file.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        // 3. 校验分片是否齐全
        Integer uploadedCount = chunkMapper.countByFileId(fileId);
        if (!uploadedCount.equals(file.getChunkCount())) {
            throw new BizException(ErrorCode.CHUNK_INCOMPLETE);
        }

        // 4. 写入t_file_recipient（包含owner自己一条）
        List<Recipient> recipients = new ArrayList<>();

        // 添加owner自己
        Long ownerId = file.getUserId();
        boolean ownerIncluded = false;

        for (RecipientKeyDTO keyDTO : dto.getRecipientsWrappedKeys()) {
            Recipient recipient = new Recipient();
            recipient.setFileId(fileId);
            recipient.setRecipientUserId(keyDTO.getRecipientUserId());
            recipient.setWrappedAesKey(keyDTO.getWrappedAesKey());
            recipient.setCreatedAt(LocalDateTime.now());
            recipients.add(recipient);

            if (keyDTO.getRecipientUserId().equals(ownerId)) {
                ownerIncluded = true;
            }
        }

        // 如果owner不在列表中，需要添加
        if (!ownerIncluded) {
            log.warn("接收者列表中未包含owner，需要确保owner有访问权限");
        }

        // 批量插入
        recipients.forEach(recipientMapper::insert);

        // 5. 更新文件状态为COMPLETE
        file.setStatus("COMPLETE");
        file.setPlainSha256(dto.getPlainSha256());
        file.setUpdatedAt(LocalDateTime.now());
        fileMapper.updateById(file);

        // 6. 记录上传日志
        String ip = getClientIp();
        String userAgent = request.getHeader("User-Agent");
        auditService.logUpload(fileId, userId, ip, userAgent);

        log.info("上传完成, fileId={}, recipientCount={}", fileId, recipients.size());
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp() {
        String ip = request.getHeader("X-Client-Public-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 取第一个真实IP
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getHeader("X-Real-IP");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        }
        // 本地IPv6回环转换为IPv4显示
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}
