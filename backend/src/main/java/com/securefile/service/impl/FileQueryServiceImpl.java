package com.securefile.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.securefile.common.ErrorCode;
import com.securefile.common.PageResult;
import com.securefile.common.SecurityContext;
import com.securefile.exception.BizException;
import com.securefile.entity.File;
import com.securefile.entity.Recipient;
import com.securefile.mapper.FileMapper;
import com.securefile.mapper.FileRecipientMapper;
import com.securefile.service.FileQueryService;
import com.securefile.vo.FileDetailVO;
import com.securefile.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件查询服务实现
 * 职责：文件列表查询、详情查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileQueryServiceImpl implements FileQueryService {

    private final FileMapper fileMapper;
    private final FileRecipientMapper recipientMapper;

    @Override
    public PageResult<FileVO> page(int current, int size, String keyword) {
        Long userId = SecurityContext.getUserId();

        Page<File> page = new Page<>(current, size);

        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .ne(File::getStatus, "DELETED");

        // 关键词搜索
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(File::getFileName, keyword);
        }

        wrapper.orderByDesc(File::getCreatedAt);

        Page<File> result = fileMapper.selectPage(page, wrapper);

        List<FileVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), (long) current, (long) size);
    }

    @Override
    public PageResult<FileVO> sharedPage(int current, int size) {
        Long userId = SecurityContext.getUserId();

        // 查询当前用户作为接收者的所有文件ID
        List<Recipient> recipients = recipientMapper.selectList(
                new LambdaQueryWrapper<Recipient>()
                        .eq(Recipient::getRecipientUserId, userId)
        );

        if (recipients.isEmpty()) {
            return PageResult.of(new ArrayList<>(), 0L, (long) current, (long) size);
        }

        List<String> fileIds = recipients.stream()
                .map(Recipient::getFileId)
                .distinct()
                .collect(Collectors.toList());

        // 分页查询这些文件（排除自己上传的）
        Page<File> page = new Page<>(current, size);
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(File::getFileId, fileIds)
                .ne(File::getUserId, userId)  // 排除自己上传的
                .ne(File::getStatus, "DELETED")
                .orderByDesc(File::getCreatedAt);

        Page<File> result = fileMapper.selectPage(page, wrapper);

        List<FileVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), (long) current, (long) size);
    }

    @Override
    public FileDetailVO detail(String fileId) {
        File file = fileMapper.selectById(fileId);

        if (file == null || "DELETED".equals(file.getStatus())) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }

        // 权限校验：只能查看自己的文件或有权限的文件
        Long userId = SecurityContext.getUserId();
        if (!file.getUserId().equals(userId)) {
            // 检查是否在接收者列表中
            Recipient recipient = recipientMapper.selectOne(
                    new LambdaQueryWrapper<Recipient>()
                            .eq(Recipient::getFileId, fileId)
                            .eq(Recipient::getRecipientUserId, userId)
            );
            
            if (recipient == null) {
                throw new BizException(ErrorCode.FORBIDDEN);
            }
        }

        return convertToDetailVO(file);
    }

    private FileVO convertToVO(File file) {
        FileVO vo = new FileVO();
        BeanUtil.copyProperties(file, vo);
        return vo;
    }

    private FileDetailVO convertToDetailVO(File file) {
        FileDetailVO vo = new FileDetailVO();
        BeanUtil.copyProperties(file, vo);
        return vo;
    }

    @Override
    public Object getRecipients(String fileId) {
        // 查询文件的接收者列表
        List<Recipient> recipients = recipientMapper.selectList(
                new LambdaQueryWrapper<Recipient>()
                        .eq(Recipient::getFileId, fileId)
        );
        return recipients;
    }
}
