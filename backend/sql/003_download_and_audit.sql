-- 下载会话表
CREATE TABLE `session` (
    `session_id` VARCHAR(64) PRIMARY KEY COMMENT '会话ID',
    `file_id` VARCHAR(64) NOT NULL COMMENT '文件ID',
    `user_id` BIGINT NOT NULL COMMENT '下载用户ID',
    `wrapped_key` TEXT NOT NULL COMMENT '封装后的AES密钥',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    INDEX `idx_file_id` (`file_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下载会话';

-- 传输日志表
CREATE TABLE `log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    `file_id` VARCHAR(64) NOT NULL COMMENT '文件ID',
    `user_id` BIGINT NOT NULL COMMENT '操作用户ID',
    `action` VARCHAR(20) NOT NULL COMMENT '操作类型: UPLOAD/DOWNLOAD',
    `ip` VARCHAR(64) COMMENT 'IP地址',
    `user_agent` VARCHAR(512) COMMENT '用户代理',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX `idx_file_id` (`file_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_action` (`action`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传输日志';
