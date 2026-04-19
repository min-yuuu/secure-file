package com.securefile.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.securefile.common.ErrorCode;
import com.securefile.common.SecurityContext;
import com.securefile.dto.DownloadApplyDTO;
import com.securefile.entity.*;
import com.securefile.exception.BizException;
import com.securefile.mapper.*;
import com.securefile.service.AuditService;
import com.securefile.service.FileDownloadService;
import com.securefile.service.FilePermissionService;
import com.securefile.vo.DownloadApplyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件下载服务实现
 * 职责：三阶段下载（apply/chunk/finish）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadServiceImpl implements FileDownloadService {

    private final FileMapper fileMapper;
    private final FileChunkMapper chunkMapper;
    private final FileRecipientMapper recipientMapper;
    private final DownloadSessionMapper sessionMapper;
    private final FilePermissionService permissionService;
    private final AuditService auditService;
    private final HttpServletRequest request;

    // 下载会话有效期：30分钟
    private static final int SESSION_EXPIRE_MINUTES = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DownloadApplyVO apply(DownloadApplyDTO dto) {
        Long userId = SecurityContext.getUserId();
        String fileId = dto.getFileId();

        // 1. 获取文件信息
        File file = fileMapper.selectById(fileId);
        if (file == null || "DELETED".equals(file.getStatus())) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        // 2. 权限校验（集中在这里）
        permissionService.checkDownloadPermission(file, userId, dto.getExtractCode());

        // 3. 获取接收者的wrappedKey
        Recipient recipient = recipientMapper.selectOne(
                new LambdaQueryWrapper<Recipient>()
                        .eq(Recipient::getFileId, fileId)
                        .eq(Recipient::getRecipientUserId, userId)
        );

        // 修改开始：如果是上传者且没有接收者记录，尝试查找其他接收者的密钥
        if (recipient == null && file.getUserId().equals(userId)) {
            log.info("上传者下载文件，尝试查找其他接收者的密钥: fileId={}, userId={}", fileId, userId);

            // 查找任意一个接收者的记录
            recipient = recipientMapper.selectOne(
                    new LambdaQueryWrapper<Recipient>()
                            .eq(Recipient::getFileId, fileId)
                            .last("LIMIT 1")
            );

            if (recipient == null) {
                log.error("文件没有接收者记录: fileId={}", fileId);
                throw new BizException(ErrorCode.FORBIDDEN);
            }

            log.info("上传者使用其他接收者的密钥: fileId={}, recipientUserId={}",
                    fileId, recipient.getRecipientUserId());
        } else if (recipient == null) {
            // 既不是上传者，也不是接收者
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        // 修改结束

        // 4. 创建下载会话
        String sessionId = IdUtil.fastSimpleUUID();
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setFileId(fileId);
        session.setUserId(userId);
        session.setWrappedKey(recipient.getWrappedAesKey());
        session.setCreateTime(LocalDateTime.now());
        session.setExpireTime(LocalDateTime.now().plusMinutes(SESSION_EXPIRE_MINUTES));
        sessionMapper.insert(session);

        // 5. 获取分片元信息
        List<Chunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<Chunk>()
                        .eq(Chunk::getFileId, fileId)
                        .orderByAsc(Chunk::getChunkIndex)
        );

        List<DownloadApplyVO.ChunkMeta> chunkMetaList = chunks.stream()
                .map(chunk -> {
                    DownloadApplyVO.ChunkMeta meta = new DownloadApplyVO.ChunkMeta();
                    meta.setChunkIndex(chunk.getChunkIndex());
                    meta.setChunkSize(chunk.getChunkSize());
                    meta.setCipherSha256(chunk.getCipherSha256());
                    return meta;
                })
                .collect(Collectors.toList());

        // 6. 构建响应
        DownloadApplyVO vo = new DownloadApplyVO();
        vo.setDownloadSessionId(sessionId);
        vo.setWrappedAesKey(recipient.getWrappedAesKey());
        vo.setFileName(file.getFileName());
        vo.setFileSize(file.getFileSize());
        vo.setChunkCount(file.getChunkCount());
        vo.setChunkMetaList(chunkMetaList);

        log.info("下载申请成功, fileId={}, userId={}, sessionId={}", fileId, userId, sessionId);
        return vo;
    }

    @Override
    public void downloadChunk(String sessionId, Integer chunkIndex, HttpServletResponse response) {
        // 1. 校验会话
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        // 2. 校验会话是否过期
        if (LocalDateTime.now().isAfter(session.getExpireTime())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        // 3. 获取分片信息
        Chunk chunk = chunkMapper.selectOne(
                new LambdaQueryWrapper<Chunk>()
                        .eq(Chunk::getFileId, session.getFileId())
                        .eq(Chunk::getChunkIndex, chunkIndex)
        );

        if (chunk == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        // 4. 读取密文分片并返回
        try {
            byte[] chunkData = Files.readAllBytes(Paths.get(chunk.getStoragePath()));

            response.setContentType("application/octet-stream");
            response.setContentLengthLong(chunkData.length);
            response.setHeader("Content-Disposition", "attachment; filename=chunk_" + chunkIndex + ".bin");

            OutputStream outputStream = response.getOutputStream();
            outputStream.write(chunkData);
            outputStream.flush();

            log.info("分片下载成功, sessionId={}, chunkIndex={}", sessionId, chunkIndex);

        } catch (IOException e) {
            log.error("分片下载失败", e);
            throw new BizException(ErrorCode.STORAGE_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(String sessionId) {
        // 1. 获取会话信息
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        String fileId = session.getFileId();
        Long userId = session.getUserId();

        // 2. 获取文件信息
        File file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        // 3. 扣减下载次数
        file.setDownloadCount(file.getDownloadCount() + 1);
        file.setUpdatedAt(LocalDateTime.now());
        fileMapper.updateById(file);

        // 4. 阅后即焚
        if (file.getBurnAfterReading() != null && file.getBurnAfterReading()) {
            file.setStatus("DELETED");
            fileMapper.updateById(file);
            log.info("阅后即焚触发, fileId={}", fileId);
        }

        // 5. 删除会话
        sessionMapper.deleteById(sessionId);

        // 6. 记录下载日志
        String ip = getClientIp();
        String userAgent = request.getHeader("User-Agent");
        auditService.logDownload(fileId, userId, ip, userAgent);

        log.info("下载完成, fileId={}, userId={}, downloadCount={}",
                fileId, userId, file.getDownloadCount());
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
