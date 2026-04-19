package com.securefile.controller;

import com.securefile.common.PageResult;
import com.securefile.common.Result;
import com.securefile.service.FileManageService;
import com.securefile.service.FileQueryService;
import com.securefile.vo.FileDetailVO;
import com.securefile.vo.FileVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 文件管理控制器
 * 职责：文件列表查询、详情查询、删除
 */
@Api(tags = "文件管理模块")
@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class FileController {

    private final FileQueryService fileQueryService;
    private final FileManageService fileManageService;

    @ApiOperation("分页查询文件列表")
    @GetMapping("/page")
    public Result<PageResult<FileVO>> page(
            @ApiParam(value = "当前页码", example = "1") @RequestParam(defaultValue = "1") int current,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @ApiParam(value = "文件名关键词") @RequestParam(required = false) String keyword) {
        PageResult<FileVO> result = fileQueryService.page(current, size, keyword);
        return Result.success(result);
    }

    @ApiOperation("分页查询共享给我的文件列表")
    @GetMapping("/shared/page")
    public Result<PageResult<FileVO>> pageShared(
            @ApiParam(value = "当前页码", example = "1") @RequestParam(defaultValue = "1") int current,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size) {
        PageResult<FileVO> result = fileQueryService.sharedPage(current, size);
        return Result.success(result);
    }

    @ApiOperation("查询文件详情")
    @GetMapping("/detail")
    public Result<FileDetailVO> detail(
            @ApiParam(value = "文件ID", required = true) @RequestParam String fileId) {
        FileDetailVO vo = fileQueryService.detail(fileId);
        return Result.success(vo);
    }

    @ApiOperation("删除文件")
    @PostMapping("/delete")
    public Result<Void> delete(
            @ApiParam(value = "文件ID", required = true) @RequestParam String fileId) {
        fileManageService.delete(fileId);
        return Result.success();
    }
}
