package com.securefile.controller;

import com.securefile.common.ErrorCode;
import com.securefile.common.Result;
import com.securefile.common.SecurityContext;
import com.securefile.dto.PublicKeyDTO;
import com.securefile.entity.User;
import com.securefile.exception.BizException;
import com.securefile.service.UserService;
import com.securefile.vo.PublicKeyVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author:zoujingmin
 * @date: 2026/1/26
 */

@Api(tags = "密钥管理模块")
@RestController
@RequestMapping("/api/v1/key")
public class KeyController {
    @Autowired
    private UserService userService;

    @ApiOperation("更新当前登录用户的公钥")
    @PostMapping("/my")
    public Result<Boolean> updateMyKey(@Validated @RequestBody PublicKeyDTO dto) {
        Long currentUserId = SecurityContext.getUserId();
        boolean success = userService.saveUserPublicKey(currentUserId, dto.getPublicKey());
        return Result.success(success);
    }

    @ApiOperation("获取当前登录用户的公钥信息")
    @GetMapping("/my")
    public Result<PublicKeyVO> getMyKey() {
        Long currentUserId = SecurityContext.getUserId();
        PublicKeyVO vo = userService.getUserPublicKey(currentUserId);
        return Result.success(vo);
    }

    @ApiOperation("获取指定接收者的公钥（用于文件分享加密）")
    @GetMapping("/user/{userId}")
    public Result<PublicKeyVO> getTargetKey(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long userId) {
        String publicKey = userService.getPublicKey(userId);

        // 业务逻辑校验：如果没有上传公钥，无法加密
        if (publicKey == null) {
            throw new BizException(ErrorCode.KEY_NOT_UPLOADED);
        }

        User user = userService.getById(userId);
        PublicKeyVO vo = new PublicKeyVO();
        vo.setUserId(String.valueOf(user.getId()));
        vo.setUsername(user.getUsername());
        vo.setPublicKey(publicKey);

        return Result.success(vo);
    }
}
