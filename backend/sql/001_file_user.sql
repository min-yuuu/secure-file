CREATE TABLE `user` (
                        `id` bigint NOT NULL AUTO_INCREMENT,
                        `username` varchar(64) NOT NULL UNIQUE COMMENT '用户名',
                        `password` varchar(255) NOT NULL COMMENT '哈希加密后的密码',
                        `public_key` text COMMENT 'RSA公钥',
                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;