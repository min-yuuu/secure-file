-- 文件上传模块数据库表

-- 文件表
CREATE TABLE `file` (
    `file_id` VARCHAR(64) PRIMARY KEY COMMENT '文件ID',
    `user_id` BIGINT NOT NULL COMMENT '上传者ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `content_type` VARCHAR(128) COMMENT 'MIME类型',
    `chunk_count` INT NOT NULL COMMENT '总分片数',
    `plain_sha256` VARCHAR(64) COMMENT '明文文件SHA256(可选)',
    `status` VARCHAR(20) NOT NULL DEFAULT 'UPLOADING' COMMENT '状态: UPLOADING/COMPLETE/DELETED',
    `share_code` VARCHAR(32) COMMENT '分享码',
    `extract_code` VARCHAR(128) COMMENT '提取码(BCrypt加密)',
    `expiry_time` DATETIME COMMENT '过期时间',
    `download_limit` INT DEFAULT -1 COMMENT '下载次数限制(-1表示无限)',
    `download_count` INT DEFAULT 0 COMMENT '已下载次数',
    `burn_after_reading` TINYINT(1) DEFAULT 0 COMMENT '是否阅后即焚',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_share_code` (`share_code`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- 分片表
CREATE TABLE `chunk` (
    `chunk_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分片ID',
    `file_id` VARCHAR(64) NOT NULL COMMENT '文件ID',
    `chunk_index` INT NOT NULL COMMENT '分片序号(从0开始)',
    `chunk_size` BIGINT NOT NULL COMMENT '分片大小(字节)',
    `cipher_sha256` VARCHAR(64) NOT NULL COMMENT '密文SHA256哈希',
    `storage_path` VARCHAR(512) NOT NULL COMMENT '存储路径',
    `uploaded_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_file_chunk` (`file_id`, `chunk_index`),
    INDEX `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分片表';

-- 文件接收者表
CREATE TABLE `recipient` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    `file_id` VARCHAR(64) NOT NULL COMMENT '文件ID',
    `recipient_user_id` BIGINT NOT NULL COMMENT '接收者ID(包含owner自己)',
    `wrapped_aes_key` TEXT NOT NULL COMMENT '封装后的AES密钥',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_file_id` (`file_id`),
    INDEX `idx_recipient` (`recipient_user_id`),
    UNIQUE KEY `uk_file_recipient` (`file_id`, `recipient_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件接收者表';
