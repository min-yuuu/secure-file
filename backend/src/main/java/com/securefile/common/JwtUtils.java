package com.securefile.common;

/**
 * @description:
 * @author:zoujingmin
 * @date: 2026/1/26
 */

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import java.util.HashMap;
import java.util.Map;

public class JwtUtils {
    // 签名密钥
    private static final byte[] KEY = "mZ7qP2vS5hL8nB4dX1kC9tJ0wY6fR3zG2aV1uI7oP9kL0jH5gT4sE2rD1fC8vB9".getBytes();

    public static String createToken(Long userId, String username) {
        Map<String, Object> map = new HashMap<>();
        // 将 Long 类型的 userId 转为字符串，避免前端精度丢失
        map.put("userId", String.valueOf(userId));  // 修改这里
        map.put("username", username);
        map.put("expire_time", System.currentTimeMillis() + 1000 * 60 * 60 * 24); // 24小时过期
        return JWTUtil.createToken(map, KEY);
    }

    public static boolean validate(String token) {
        try {
            return JWTUtil.verify(token.replace("Bearer ", ""), KEY);
        } catch (Exception e) {
            return false;
        }
    }

    public static Long getUserId(String token) {
        JWT jwt = JWTUtil.parseToken(token.replace("Bearer ", ""));
        // 从 JWT 中获取 userId（现在是字符串），转为 Long
        Object userIdObj = jwt.getPayload("userId");
        if (userIdObj instanceof String) {
            return Long.parseLong((String) userIdObj);
        }
        return Long.parseLong(userIdObj.toString());
    }
}