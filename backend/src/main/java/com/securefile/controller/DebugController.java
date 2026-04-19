package com.securefile.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.securefile.common.Result;
import com.securefile.entity.Recipient;
import com.securefile.mapper.FileRecipientMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "调试接口")
@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
public class DebugController {

    private final FileRecipientMapper recipientMapper;

    private final com.securefile.mapper.FileMapper fileMapper;

    @ApiOperation("查询所有接收者记录")
    @GetMapping("/recipients")
    public Result<List<Recipient>> listAllRecipients() {
        List<Recipient> list = recipientMapper.selectList(null);
        return Result.success(list);
    }

    @ApiOperation("全能诊断：检查用户为何看不到文件")
    @GetMapping("/diagnose")
    public Result<Object> diagnose(@org.springframework.web.bind.annotation.RequestParam Long userId) {
        java.util.Map<String, Object> report = new java.util.HashMap<>();
        report.put("checkingUserId", userId);

        // 1. 查 recipient 表
        List<Recipient> recList = recipientMapper.selectList(
                new LambdaQueryWrapper<Recipient>().eq(Recipient::getRecipientUserId, userId)
        );
        report.put("recipientRecordsCount", recList.size());
        report.put("recipientRecords", recList);

        if (recList.isEmpty()) {
            report.put("conclusion", "该用户在 recipient 表中没有任何记录。请检查上传时是否选对了人，或者 recipient_user_id 是否写入正确。");
            return Result.success(report);
        }

        // 2. 查 file 表
        List<String> fileIds = new java.util.ArrayList<>();
        for (Recipient r : recList) {
            fileIds.add(r.getFileId());
        }
        report.put("fileIdsFromRecipient", fileIds);

        List<com.securefile.entity.File> files = fileMapper.selectBatchIds(fileIds);
        report.put("foundFilesCount", files.size());
        report.put("foundFiles", files);

        if (files.size() < fileIds.size()) {
            report.put("warning", "找到的记录数少于文件ID数，说明有些文件ID在 file 表里不存在！");
        }
        
        if (files.isEmpty()) {
             report.put("conclusion", "虽然在 recipient 表里有记录，但在 file 表里找不到对应的文件。可能是文件上传未完成或已删除。");
        } else {
             report.put("conclusion", "看起来一切正常。如果列表还不显示，请检查前端分页参数或状态过滤条件。");
        }

        return Result.success(report);
    }
}
