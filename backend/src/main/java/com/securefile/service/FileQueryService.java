package com.securefile.service;

import com.securefile.common.PageResult;
import com.securefile.vo.FileDetailVO;
import com.securefile.vo.FileVO;

/**
 * 文件查询服务接口
 * 职责：文件列表查询、详情查询
 */
public interface FileQueryService {

    /**
     * 分页查询文件列表（我上传的）
     */
    PageResult<FileVO> page(int current, int size, String keyword);

    /**
     * 分页查询共享给我的文件列表
     */
    PageResult<FileVO> sharedPage(int current, int size);

    /**
     * 查询文件详情
     */
    FileDetailVO detail(String fileId);

    /**
     * 调试：获取文件的接收者列表
     */
    Object getRecipients(String fileId);
}
