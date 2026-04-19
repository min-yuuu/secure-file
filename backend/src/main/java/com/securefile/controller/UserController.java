package com.securefile.controller;

import com.securefile.common.Result;
import com.securefile.dto.LoginDTO;
import com.securefile.service.UserService;
import com.securefile.vo.UserProfileVO;
import com.securefile.common.SecurityContext;
import com.securefile.entity.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author:zoujingmin
 * @date: 2026/1/26
 */
@Api(tags = "用户认证模块")
@RestController
@RequestMapping("/api/v1/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result<String> register(@Validated @RequestBody LoginDTO loginDTO) {
        String msg = userService.register(loginDTO.getUsername(), loginDTO.getPassword());
        return Result.success(msg);
    }

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<String> login(@Validated @RequestBody LoginDTO loginDTO) {
        String token = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
        return Result.success(token);
    }

    @ApiOperation("当前用户信息")
    @GetMapping("/me")
    public Result<UserProfileVO> me() {
        Long userId = SecurityContext.getUserId();
        User user = userService.getById(userId);
        UserProfileVO vo = new UserProfileVO();
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setCreateTime(user.getCreateTime());
            vo.setHasPublicKey(user.getPublicKey() != null && !user.getPublicKey().isEmpty());
        }
        return Result.success(vo);
    }
}
