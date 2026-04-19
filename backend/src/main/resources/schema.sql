-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    public_key TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 文件表
CREATE TABLE IF NOT EXISTS `file` (
    file_id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(128),
    chunk_count INT NOT NULL,
    plain_sha256 VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING',
    share_code VARCHAR(32),
    extract_code VARCHAR(128),
    expiry_time DATETIME,
    download_limit INT DEFAULT -1,
    download_count INT DEFAULT 0,
    burn_after_reading TINYINT(1) DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_share_code (share_code),
    INDEX idx_created_at (created_at)
);

-- 分片表
CREATE TABLE IF NOT EXISTS `chunk` (
    chunk_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id VARCHAR(64) NOT NULL,
    chunk_index INT NOT NULL,
    chunk_size BIGINT NOT NULL,
    cipher_sha256 VARCHAR(64) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_file_chunk (file_id, chunk_index),
    INDEX idx_file_id (file_id)
);

-- 接收者表
CREATE TABLE IF NOT EXISTS `recipient` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id VARCHAR(64) NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    wrapped_aes_key TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file_id (file_id),
    INDEX idx_recipient (recipient_user_id),
    UNIQUE KEY uk_file_recipient (file_id, recipient_user_id)
);

-- 下载会话表
CREATE TABLE IF NOT EXISTS `session` (
    session_id VARCHAR(64) PRIMARY KEY,
    file_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    wrapped_key TEXT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_time DATETIME NOT NULL,
    INDEX idx_file_id (file_id),
    INDEX idx_user_id (user_id),
    INDEX idx_expire_time (expire_time)
);

-- 传输日志表
CREATE TABLE IF NOT EXISTS `log` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    ip VARCHAR(64),
    user_agent VARCHAR(512),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file_id (file_id),
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_create_time (create_time)
);
