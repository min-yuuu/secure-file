package com.securefile.controller;

import com.securefile.common.PageResult;
import com.securefile.common.Result;
import com.securefile.entity.Log;
import com.securefile.entity.File;
import com.securefile.service.AuditService;
import com.securefile.mapper.FileMapper;
import com.securefile.vo.TransferLogVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审计控制器
 * 职责：提供传输日志查询接口
 */
@Api(tags = "审计模块")
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final FileMapper fileMapper;

    @ApiOperation("分页查询传输日志")
    @GetMapping("/transfer/page")
    public Result<PageResult<TransferLogVO>> pageTransferLogs(
            @ApiParam(value = "当前页", required = true) @RequestParam(defaultValue = "1") Integer current,
            @ApiParam(value = "每页大小", required = true) @RequestParam(defaultValue = "10") Integer size,
            @ApiParam(value = "操作类型(UPLOAD/DOWNLOAD)") @RequestParam(required = false) String action,
            @ApiParam(value = "用户ID") @RequestParam(required = false) Long userId) {
        PageResult<com.securefile.vo.AuditLogVO> result = auditService.pageTransferLogs(current, size, action, userId);
        java.util.List<TransferLogVO> voList = new java.util.ArrayList<>();
        for (com.securefile.vo.AuditLogVO auditLog : result.getList()) {
            TransferLogVO vo = new TransferLogVO();
            vo.setFileId(auditLog.getFileId());
            vo.setFileName(auditLog.getFileName());
            vo.setAction(auditLog.getAction());
            vo.setIp(auditLog.getIp());
            vo.setCreateTime(auditLog.getCreateTime());
            voList.add(vo);
        }
        PageResult<TransferLogVO> wrapped = PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
        return Result.success(wrapped);
    }
}
