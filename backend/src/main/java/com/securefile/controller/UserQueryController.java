package com.securefile.controller;

import com.securefile.common.Result;
import com.securefile.service.UserService;
import com.securefile.vo.PublicKeyVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户查询控制器（用于按用户名搜索接收者并返回其公钥）
 */
@Api(tags = "用户查询模块")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserQueryController {

    private final UserService userService;

    @ApiOperation("按用户名搜索用户并返回其公钥（仅返回已上传公钥的用户）")
    @GetMapping("/search")
    public Result<List<PublicKeyVO>> search(
            @ApiParam(value = "用户名关键词", required = true) @RequestParam String keyword,
            @ApiParam(value = "返回数量上限", example = "10") @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        List<PublicKeyVO> list = userService.searchPublicKeys(keyword, limit == null ? 10 : limit);
        return Result.success(list);
    }
}
