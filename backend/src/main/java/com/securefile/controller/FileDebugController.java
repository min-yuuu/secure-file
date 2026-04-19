package com.securefile.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.securefile.common.Result;
import com.securefile.entity.Recipient;
import com.securefile.mapper.FileRecipientMapper;
import com.securefile.common.SecurityContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "调试")
@RestController
@RequestMapping("/api/v1/file/debug")
@RequiredArgsConstructor
public class FileDebugController {

    private final FileRecipientMapper recipientMapper;

    @ApiOperation("查看文件接收者列表")
    @GetMapping("/recipients")
    public Result<List<Recipient>> recipients(
            @ApiParam(value = "文件ID", required = true) @RequestParam String fileId) {
        List<Recipient> list = recipientMapper.selectList(
                new LambdaQueryWrapper<Recipient>().eq(Recipient::getFileId, fileId));
        return Result.success(list);
    }

    @ApiOperation("当前用户ID")
    @GetMapping("/whoami")
    public Result<Long> whoami() {
        return Result.success(SecurityContext.getUserId());
    }
}
