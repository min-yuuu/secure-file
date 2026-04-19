package com.securefile.service;

import com.securefile.dto.DownloadApplyDTO;
import com.securefile.vo.DownloadApplyVO;

import javax.servlet.http.HttpServletResponse;

/**
 * 文件下载服务接口
 * 职责：三阶段下载（apply/chunk/finish）
 */
public interface FileDownloadService {

    /**
     * 申请下载（权限校验）
     * 返回：downloadSessionId + wrappedAesKey + chunkMeta
     */
    DownloadApplyVO apply(DownloadApplyDTO dto);

    /**
     * 下载分片（返回密文）
     */
    void downloadChunk(String sessionId, Integer chunkIndex, HttpServletResponse response);

    /**
     * 完成下载（扣次数、焚毁、记录日志）
     */
    void finish(String sessionId);
}
