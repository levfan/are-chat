-- H2 内存库建表脚本（仅嵌入式数据源自动执行；MySQL 老库已有同结构表，无需执行）
-- 表结构字段名与老项目保持一致，兼容旧 MySQL 数据

-- smart_collections.admin_audit definition

CREATE TABLE `admin_audit` (
                               `id` varchar(36) NOT NULL,
                               `actor` varchar(64) NOT NULL,
                               `action` varchar(32) NOT NULL,
                               `target` varchar(64) DEFAULT NULL,
                               `detail` varchar(500) DEFAULT NULL,
                               `created` bigint(20) NOT NULL,
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.announcement definition

CREATE TABLE `announcement` (
                                `id` varchar(36) NOT NULL,
                                `content` varchar(500) NOT NULL,
                                `created_by` varchar(64) NOT NULL,
                                `enabled` tinyint(4) DEFAULT 1,
                                `created` bigint(20) NOT NULL,
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.announcement_read definition

CREATE TABLE `announcement_read` (
                                     `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                     `announcement_id` varchar(36) NOT NULL,
                                     `read_at` bigint(20) NOT NULL,
                                     UNIQUE KEY `uq_ann_read` (`username`,`announcement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.app_user definition

CREATE TABLE `app_user` (
                            `id` varchar(36) NOT NULL,
                            `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                            `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                            `nickname` varchar(32) DEFAULT NULL,
                            `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                            `avatar` varchar(16) DEFAULT NULL,
                            `signature` varchar(100) DEFAULT NULL,
                            `presence_status` varchar(16) DEFAULT 'online',
                            `status` varchar(16) NOT NULL DEFAULT 'ACTIVE',
                            `role` varchar(16) NOT NULL DEFAULT 'USER',
                            `created` bigint(20) NOT NULL,
                            `last_login_at` bigint(20) DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uq_app_user_username` (`username`),
                            UNIQUE KEY `uq_app_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.conversation_pin definition

CREATE TABLE `conversation_pin` (
                                    `id` varchar(36) NOT NULL,
                                    `user_a` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                    `user_b` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                    `msg_id` varchar(36) NOT NULL,
                                    `created_by` varchar(64) NOT NULL,
                                    `created` bigint(20) NOT NULL,
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uq_conv_pin` (`user_a`,`user_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.friend definition

CREATE TABLE `friend` (
                          `id` varchar(36) NOT NULL,
                          `owner_username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                          `friend_username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                          `remark` varchar(64) DEFAULT NULL,
                          `tag` varchar(32) DEFAULT '',
                          `pinned` tinyint(4) DEFAULT 0,
                          `muted` tinyint(4) DEFAULT 0,
                          `blocked` tinyint(4) DEFAULT 0,
                          `last_read_at` bigint(20) DEFAULT 0,
                          `last_seen_at` bigint(20) DEFAULT NULL,
                          `created` bigint(20) NOT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uq_friend_pair` (`owner_username`,`friend_username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.friend_request definition

CREATE TABLE `friend_request` (
                                  `id` varchar(36) NOT NULL,
                                  `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                  `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                  `message` varchar(100) DEFAULT NULL,
                                  `status` varchar(16) NOT NULL,
                                  `created` bigint(20) NOT NULL,
                                  `updated_at` bigint(20) DEFAULT NULL,
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.message_reaction definition

CREATE TABLE `message_reaction` (
                                    `id` varchar(36) NOT NULL,
                                    `msg_id` varchar(36) NOT NULL,
                                    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                    `emoji` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
                                    `created` bigint(20) NOT NULL,
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uq_reaction` (`msg_id`,`username`,`emoji`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.message_star definition

CREATE TABLE `message_star` (
                                `id` varchar(36) NOT NULL,
                                `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                `msg_id` varchar(36) NOT NULL,
                                `created` bigint(20) NOT NULL,
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uq_star` (`username`,`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.private_message definition

CREATE TABLE `private_message` (
                                   `id` varchar(36) NOT NULL,
                                   `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                   `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                   `content` varchar(2000) NOT NULL,
                                   `msg_type` varchar(16) NOT NULL,
                                   `status` varchar(16) NOT NULL,
                                   `reply_to_id` varchar(36) DEFAULT NULL,
                                   `read_flag` tinyint(4) DEFAULT 0,
                                   `edited` tinyint(4) DEFAULT 0,
                                   `created` bigint(20) NOT NULL,
                                   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.registration_application definition

CREATE TABLE `registration_application` (
                                            `id` varchar(36) NOT NULL,
                                            `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                            `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                            `nickname` varchar(32) DEFAULT NULL,
                                            `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
                                            `status` varchar(16) NOT NULL,
                                            `reject_reason` varchar(200) DEFAULT NULL,
                                            `created` bigint(20) NOT NULL,
                                            `reviewed_at` bigint(20) DEFAULT NULL,
                                            `reviewed_by` varchar(64) DEFAULT NULL,
                                            PRIMARY KEY (`id`),
                                            KEY `idx_reg_app_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.uploaded_file definition

CREATE TABLE `uploaded_file` (
                                 `id` varchar(36) NOT NULL,
                                 `original_name` varchar(255) NOT NULL,
                                 `stored_path` varchar(255) NOT NULL,
                                 `content_type` varchar(127) DEFAULT NULL,
                                 `size` bigint(20) NOT NULL,
                                 `sha256` varchar(64) NOT NULL,
                                 `uploaded_at` bigint(20) DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uq_uploaded_file_sha256` (`sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- smart_collections.user_profile definition

CREATE TABLE `user_profile` (
                                `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
                                `nickname` varchar(32) DEFAULT NULL,
                                `signature` varchar(100) DEFAULT NULL,
                                `avatar` varchar(16) DEFAULT NULL,
                                `presence_status` varchar(16) DEFAULT 'online',
                                `updated_at` bigint(20) DEFAULT NULL,
                                PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;