package com.securefile.service;

import com.securefile.dto.UploadCompleteDTO;
import com.securefile.dto.UploadInitDTO;
import com.securefile.vo.UploadInitVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 * 负责：接收密文分片、断点续传、保存wrappedKey
 */
public interface FileUploadService {

    /**
     * 初始化上传
     * 创建文件记录，返回fileId和已上传分片列表（用于断点续传）
     */
    UploadInitVO init(UploadInitDTO dto);

    /**
     * 上传分片
     * 接收密文分片，做hash校验，落盘保存
     */
    void uploadChunk(String fileId, Integer chunkIndex, String cipherSha256, MultipartFile blob);

    /**
     * 完成上传
     * 确认分片齐全，保存wrappedKey，文件状态置为COMPLETE
     */
    void complete(UploadCompleteDTO dto);
}
