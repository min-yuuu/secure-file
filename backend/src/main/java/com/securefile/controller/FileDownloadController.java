package com.securefile.controller;

import com.securefile.common.Result;
import com.securefile.dto.DownloadApplyDTO;
import com.securefile.service.FileDownloadService;
import com.securefile.vo.DownloadApplyVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * 文件下载控制器
 * 职责：三阶段下载（apply/chunk/finish）
 */
@Api(tags = "文件下载模块")
@RestController
@RequestMapping("/api/v1/file/download")
@RequiredArgsConstructor
public class FileDownloadController {

    private final FileDownloadService fileDownloadService;

    @ApiOperation("申请下载（权限校验）")
    @PostMapping("/apply")
    public Result<DownloadApplyVO> apply(@Validated @RequestBody DownloadApplyDTO dto) {
        DownloadApplyVO vo = fileDownloadService.apply(dto);
        return Result.success(vo);
    }

    @ApiOperation("下载分片（返回密文）")
    @GetMapping("/chunk")
    public void downloadChunk(
            @ApiParam(value = "下载会话ID", required = true) @RequestParam String sessionId,
            @ApiParam(value = "分片索引", required = true) @RequestParam Integer chunkIndex,
            HttpServletResponse response) {
        fileDownloadService.downloadChunk(sessionId, chunkIndex, response);
    }

    @ApiOperation("完成下载（扣次数、焚毁、记录日志）")
    @PostMapping("/finish")
    public Result<Void> finish(
            @ApiParam(value = "下载会话ID", required = true) @RequestParam String sessionId) {
        fileDownloadService.finish(sessionId);
        return Result.success();
    }
}
