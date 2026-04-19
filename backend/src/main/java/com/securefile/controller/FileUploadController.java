package com.securefile.controller;

import com.securefile.common.Result;
import com.securefile.service.FileUploadService;
import com.securefile.dto.UploadCompleteDTO;
import com.securefile.dto.UploadInitDTO;
import com.securefile.vo.UploadInitVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 * 职责：接收密文分片、支持断点续传
 */
@Api(tags = "文件上传模块")
@Slf4j
@RestController
@RequestMapping("/api/v1/file/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @ApiOperation("初始化上传")
    @PostMapping("/init")
    public Result<UploadInitVO> init(@Validated @RequestBody UploadInitDTO dto) {
        UploadInitVO vo = fileUploadService.init(dto);
        return Result.success(vo);
    }

    @ApiOperation("上传分片（密文）")
    @PostMapping("/chunk")
    public Result<Void> uploadChunk(
            @ApiParam(value = "文件ID", required = true) @RequestParam("fileId") String fileId,
            @ApiParam(value = "分片序号", required = true) @RequestParam("chunkIndex") Integer chunkIndex,
            @ApiParam(value = "密文SHA256", required = true) @RequestParam("cipherSha256") String cipherSha256,
            @ApiParam(value = "分片文件（密文）", required = true) @RequestParam("blob") MultipartFile blob) {
        fileUploadService.uploadChunk(fileId, chunkIndex, cipherSha256, blob);
        return Result.success();
    }

    @ApiOperation("完成上传（保存wrappedKey）")
    @PostMapping("/complete")
    public Result<Void> complete(@Validated @RequestBody UploadCompleteDTO dto) {
        fileUploadService.complete(dto);
        return Result.success();
    }
}
