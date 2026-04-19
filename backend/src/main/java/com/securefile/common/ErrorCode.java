package com.securefile.common;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    // 通用错误
    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统错误"),
    
    // 文件业务错误 (10xxx)
    FILE_NOT_FOUND(10001, "文件不存在"),
    FILE_STATUS_ERROR(10002, "文件状态异常"),
    CHUNK_HASH_MISMATCH(10003, "分片哈希校验失败"),
    CHUNK_INCOMPLETE(10004, "分片未上传完整"),
    FILE_ALREADY_EXISTS(10005, "文件已存在"),
    STORAGE_ERROR(10006, "存储失败"),
    FILE_EXPIRED(10007, "文件已过期"),
    DOWNLOAD_LIMIT_EXCEEDED(10008, "下载次数已达上限"),
    EXTRACT_CODE_ERROR(10009, "提取码错误"),

    // 用户与鉴权错误 (20xxx)
    USER_NOT_FOUND(20001, "用户不存在"),
    USER_ALREADY_EXISTS(20002, "用户名已存在"),
    INVALID_PASSWORD(20003, "用户名或密码错误"),
    KEY_NOT_UPLOADED(20004, "目标用户未上传公钥，无法对其加密");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
