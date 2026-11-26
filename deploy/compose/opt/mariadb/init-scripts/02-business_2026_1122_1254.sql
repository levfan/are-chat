-- =============================================================
-- 数据库：are_chat
-- 说明：在原导出脚本基础上补充：
--   1. CREATE DATABASE IF NOT EXISTS 建库语句
--   2. USE are_chat 切库语句
--   3. 建表前按依赖倒序 DROP TABLE IF EXISTS 条件删表
--   4. 已去重：原文件建表语句重复两遍，本版仅保留一份
--   5. 字符集统一：库/表/字段全部 utf8mb4_general_ci
--   6. 修复：原 DROP 段遗漏 app_user，会导致重跑时 CREATE TABLE app_user 报表已存在而中断
-- 适用：MySQL 8.0+（InnoDB + utf8mb4 / utf8mb4_general_ci）
-- 注意：utf8mb4_general_ci 仅 MySQL 8.0 支持，MariaDB 与 MySQL 5.7 不支持
--
-- ⚠️ 危险提示：下面的 DROP TABLE IF EXISTS 会删除同名表及其中全部数据。
--    如需保留数据，请先把所有 DROP TABLE 行注释掉。
--    若库里存在本脚本未列出的其他表，不会被动删除。
-- =============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `are_chat`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `are_chat`;

-- -------------------------------------------------------------
-- 清理旧表：按“子表 -> 父表 / 业务表 -> Flyway 基线”的顺序删除
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `message_reaction`;
DROP TABLE IF EXISTS `message_star`;
DROP TABLE IF EXISTS `conversation_pin`;
DROP TABLE IF EXISTS `private_message`;

DROP TABLE IF EXISTS `announcement_read`;
DROP TABLE IF EXISTS `announcement`;

DROP TABLE IF EXISTS `friend_request`;
DROP TABLE IF EXISTS `friend`;

DROP TABLE IF EXISTS `couple_intimacy_log`;
DROP TABLE IF EXISTS `couple_period`;
DROP TABLE IF EXISTS `couple_notify`;
DROP TABLE IF EXISTS `couple_invite`;
DROP TABLE IF EXISTS `couple_space`;

DROP TABLE IF EXISTS `registration_application`;
DROP TABLE IF EXISTS `uploaded_file`;
DROP TABLE IF EXISTS `user_profile`;
DROP TABLE IF EXISTS `app_user`;
DROP TABLE IF EXISTS `admin_audit`;

-- Flyway 必须最后删：避免重建后历史记录与真实表结构不一致
DROP TABLE IF EXISTS `flyway_schema_history`;

-- =============================================================
-- 以下为建表 DDL（已去重 + 字符集统一为 utf8mb4_general_ci）
-- =============================================================

-- are_chat.admin_audit definition

CREATE TABLE `admin_audit` (
                               `id` varchar(36) NOT NULL COMMENT '主键UUID',
                               `actor` varchar(64) NOT NULL COMMENT '操作人用户名（管理员）',
                               `action` varchar(32) NOT NULL COMMENT '操作类型：APPROVE 通过注册 / REJECT 拒绝注册 / ENABLE 启用账号 / DISABLE 禁用账号 / RESET_PASSWORD 重置密码',
                               `target` varchar(64) DEFAULT NULL COMMENT '操作对象，通常为目标用户名',
                               `detail` varchar(500) DEFAULT NULL COMMENT '操作详情说明',
                               `created` bigint(20) NOT NULL COMMENT '操作时间（毫秒时间戳）',
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理员操作审计表：审批/启用禁用/重置密码等敏感动作全部留痕';


-- are_chat.announcement definition

CREATE TABLE `announcement` (
                                `id` varchar(36) NOT NULL COMMENT '主键UUID',
                                `content` varchar(500) NOT NULL COMMENT '公告内容',
                                `created_by` varchar(64) NOT NULL COMMENT '发布人用户名（管理员）',
                                `enabled` tinyint(4) DEFAULT 1 COMMENT '是否启用：1 启用（横幅展示）/ 0 停用',
                                `created` bigint(20) NOT NULL COMMENT '发布时间（毫秒时间戳）',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='全站公告表：管理员发布，所有登录用户顶部横幅展示';


-- are_chat.announcement_read definition

CREATE TABLE `announcement_read` (
                                     `username` varchar(50) DEFAULT NULL COMMENT '已读用户名（区分大小写）',
                                     `announcement_id` varchar(36) NOT NULL COMMENT '公告ID，关联 announcement.id',
                                     `read_at` bigint(20) NOT NULL COMMENT '确认已读时间（毫秒时间戳）',
                                     `id` varchar(36) NOT NULL COMMENT '主键UUID',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uq_ann_read` (`username`,`announcement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告已读记录表：用户点「我知道了」后不再展示该条公告';


-- are_chat.app_user definition

CREATE TABLE `app_user` (
                            `id` varchar(36) NOT NULL COMMENT '主键UUID',
                            `username` varchar(50) DEFAULT NULL COMMENT '用户名，全局唯一且区分大小写',
                            `phone` varchar(20) DEFAULT NULL COMMENT '手机号，全局唯一，注册与登录标识',
                            `nickname` varchar(32) DEFAULT NULL COMMENT '昵称（注册时默认取用户名）',
                            `password_hash` varchar(255) DEFAULT NULL COMMENT '登录密码哈希（PBKDF2）',
                            `avatar` varchar(16) DEFAULT NULL COMMENT '头像标识（内置头像色档编号，如 c0）',
                            `signature` varchar(100) DEFAULT NULL COMMENT '个性签名',
                            `presence_status` varchar(16) DEFAULT 'online' COMMENT '在线状态：online/busy/away，注册时初始化，实时状态以 user_profile 为准',
                            `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态：ACTIVE 正常 / DISABLED 禁用 / CLOSED 自助注销（保留占位，禁止登录、不可被搜索加好友）',
                            `role` varchar(16) NOT NULL DEFAULT 'USER' COMMENT '角色：USER 普通用户 / ADMIN 管理员（可审批注册、管理用户与公告）',
                            `created` bigint(20) NOT NULL COMMENT '注册时间（毫秒时间戳）',
                            `last_login_at` bigint(20) DEFAULT NULL COMMENT '最近登录时间（毫秒时间戳）',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uq_app_user_username` (`username`),
                            UNIQUE KEY `uq_app_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统合法用户表：注册申请审批通过后创建，用户名与手机号均唯一';


-- are_chat.conversation_pin definition

CREATE TABLE `conversation_pin` (
                                    `id` varchar(36) NOT NULL COMMENT '主键UUID',
                                    `user_a` varchar(50) DEFAULT NULL COMMENT '会话双方用户名之一（字典序较小者）',
                                    `user_b` varchar(50) DEFAULT NULL COMMENT '会话双方用户名之一（字典序较大者）',
                                    `msg_id` varchar(36) NOT NULL COMMENT '被置顶的消息ID',
                                    `created_by` varchar(64) NOT NULL COMMENT '置顶操作人用户名',
                                    `created` bigint(20) NOT NULL COMMENT '置顶时间（毫秒时间戳）',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uq_conv_pin` (`user_a`,`user_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会话置顶消息表：每个双人会话最多一条置顶消息，双方共享可见';


-- are_chat.couple_intimacy_log definition

CREATE TABLE `couple_intimacy_log` (
                                       `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键UUID',
                                       `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
                                       `day` varchar(10) NOT NULL COMMENT '发生日期（yyyy-MM-dd，自然日）',
                                       `kind` varchar(10) NOT NULL COMMENT '类型：EDGE 边缘亲昵 / SEX 性生活',
                                       `note` varchar(60) DEFAULT NULL COMMENT '备注（选填，最多 60 字）',
                                       `recorder_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '记录人用户名',
                                       `created` bigint(20) NOT NULL COMMENT '记录时间（毫秒时间戳）',
                                       PRIMARY KEY (`id`),
                                       KEY `idx_couple11_intimacy_space_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='亲密记录表：一行=一次边缘亲昵或性生活，同一天可多条，双方共享';


-- are_chat.couple_invite definition

CREATE TABLE `couple_invite` (
                                 `id` varchar(36) NOT NULL COMMENT '主键UUID',
                                 `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发起方用户名（区分大小写）',
                                 `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接收方用户名（区分大小写）',
                                 `message` varchar(100) DEFAULT NULL COMMENT '邀请留言',
                                 `status` varchar(16) NOT NULL COMMENT '邀请状态：PENDING 待处理 / ACCEPTED 已同意 / REJECTED 已拒绝 / CANCELED 已取消',
                                 `created` bigint(20) NOT NULL COMMENT '邀请时间（毫秒时间戳）',
                                 `updated_at` bigint(20) DEFAULT NULL COMMENT '最近处理时间（毫秒时间戳）',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_couple_invite_to` (`to_user`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣空间邀请表：对方同意后创建 couple_space';


-- are_chat.couple_notify definition

CREATE TABLE `couple_notify` (
                                 `id` varchar(36) NOT NULL COMMENT '主键UUID',
                                 `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '收件人用户名（区分大小写）',
                                 `event` varchar(40) NOT NULL COMMENT '事件名（如 letter-created / countdown-reminder）',
                                 `actor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发人用户名（system = 定时任务）',
                                 `detail` varchar(300) NOT NULL COMMENT '通知文案',
                                 `read_flag` tinyint(4) DEFAULT 0 COMMENT '是否已读：1 已读 / 0 未读',
                                 `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_couple_notify_user` (`username`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣空间通知中心表：每次事件推送都给收件人存档一条，登录后可补看与标记已读';


-- are_chat.couple_period definition

CREATE TABLE `couple_period` (
                                 `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键UUID',
                                 `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
                                 `start_day` varchar(10) NOT NULL COMMENT '经期开始日期（yyyy-MM-dd，自然日）',
                                 `end_day` varchar(10) DEFAULT NULL COMMENT '经期结束日期（yyyy-MM-dd；还没结束为 NULL）',
                                 `recorder_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '记录人用户名',
                                 `created` bigint(20) NOT NULL COMMENT '记录时间（毫秒时间戳）',
                                 `updated_at` bigint(20) DEFAULT NULL COMMENT '最近修改时间（毫秒时间戳）',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_couple10_period_start` (`space_id`,`start_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='经期记录表：一行=一次经期的开始日（结束日可后补），平均周期与下次预测读时算';


-- are_chat.couple_space definition

CREATE TABLE `couple_space` (
                                `id` varchar(36) NOT NULL COMMENT '主键UUID',
                                `user_a` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '情侣双方用户名之一（字典序较小者）',
                                `user_b` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '情侣双方用户名之一（字典序较大者）',
                                `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '空间状态：ACTIVE 开启 / DISSOLVED 已解除',
                                `anniversary` varchar(10) DEFAULT NULL COMMENT '在一起纪念日（yyyy-MM-dd，用于计算在一起天数，可空默认取建立时间）',
                                `created` bigint(20) NOT NULL COMMENT '建立时间（毫秒时间戳）',
                                `dissolved_at` bigint(20) DEFAULT NULL COMMENT '解除时间（毫秒时间戳）',
                                `city_a` varchar(50) DEFAULT NULL COMMENT '用户 A 所在城市（手填，匹配内置城市库计算时差/距离）',
                                `city_b` varchar(50) DEFAULT NULL COMMENT '用户 B 所在城市（手填，匹配内置城市库计算时差/距离）',
                                `nick_a` varchar(30) DEFAULT NULL COMMENT 'user_a 的专属爱称（由对方设置，如「宝宝」「猪猪」）',
                                `nick_b` varchar(30) DEFAULT NULL COMMENT 'user_b 的专属爱称（由对方设置）',
                                `slogan` varchar(60) DEFAULT NULL COMMENT '我们的宣言：只有彼此懂的一句话（60 字内）',
                                `theme` varchar(20) NOT NULL DEFAULT 'classic' COMMENT '空间主题：classic 经典粉 / cherry 樱花 / ocean 海盐 / forest 森绿 / night 星夜',
                                PRIMARY KEY (`id`),
                                KEY `idx_couple_space_a` (`user_a`),
                                KEY `idx_couple_space_b` (`user_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣空间表：一对一关系，user_a/user_b 为规范化排序的双方用户名';



-- are_chat.friend definition

CREATE TABLE `friend` (
                          `id` varchar(36) NOT NULL COMMENT '主键UUID',
                          `owner_username` varchar(50) DEFAULT NULL COMMENT '拥有者用户名（关系归属方）',
                          `friend_username` varchar(50) DEFAULT NULL COMMENT '好友用户名',
                          `remark` varchar(64) DEFAULT NULL COMMENT '好友备注名',
                          `tag` varchar(32) DEFAULT '' COMMENT '好友分组标签',
                          `pinned` tinyint(4) DEFAULT 0 COMMENT '是否置顶联系人：1 置顶 / 0 否',
                          `muted` tinyint(4) DEFAULT 0 COMMENT '是否消息免打扰：1 开启 / 0 关闭',
                          `blocked` tinyint(4) DEFAULT 0 COMMENT '是否拉黑：1 已拉黑 / 0 未拉黑',
                          `last_read_at` bigint(20) DEFAULT 0 COMMENT '最近已读时间（毫秒时间戳，用于未读红点计算）',
                          `last_seen_at` bigint(20) DEFAULT NULL COMMENT '最近在线时间（毫秒时间戳）',
                          `created` bigint(20) NOT NULL COMMENT '成为好友时间（毫秒时间戳）',
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uq_friend_pair` (`owner_username`,`friend_username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='好友关系表：双向各存一行，owner 为拥有者视角';


-- are_chat.friend_request definition

CREATE TABLE `friend_request` (
                                  `id` varchar(36) NOT NULL COMMENT '主键UUID',
                                  `from_user` varchar(50) DEFAULT NULL COMMENT '发起方用户名（区分大小写）',
                                  `to_user` varchar(50) DEFAULT NULL COMMENT '接收方用户名（区分大小写）',
                                  `message` varchar(100) DEFAULT NULL COMMENT '申请留言',
                                  `status` varchar(16) NOT NULL COMMENT '申请状态：PENDING 待处理 / ACCEPTED 已接受 / REJECTED 已拒绝',
                                  `created` bigint(20) NOT NULL COMMENT '申请时间（毫秒时间戳）',
                                  `updated_at` bigint(20) DEFAULT NULL COMMENT '最近处理时间（毫秒时间戳）',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_fr_to_user_status` (`to_user`,`status`),
                                  KEY `idx_fr_from_to` (`from_user`,`to_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='好友申请表：对方同意后双方各建一条 friend 关系';


-- are_chat.message_reaction definition

CREATE TABLE `message_reaction` (
                                    `id` varchar(36) NOT NULL,
                                    `msg_id` varchar(36) NOT NULL,
                                    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                                    `emoji` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                    `created` bigint(20) NOT NULL,
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uq_reaction` (`msg_id`,`username`,`emoji`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- are_chat.message_star definition

CREATE TABLE `message_star` (
                                `id` varchar(36) NOT NULL COMMENT '主键UUID',
                                `username` varchar(50) DEFAULT NULL COMMENT '收藏人用户名（区分大小写）',
                                `msg_id` varchar(36) NOT NULL COMMENT '收藏的消息ID',
                                `created` bigint(20) NOT NULL COMMENT '收藏时间（毫秒时间戳）',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uq_star` (`username`,`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息收藏表：个人视角，跨会话收藏消息';


-- are_chat.private_message definition

CREATE TABLE `private_message` (
                                   `id` varchar(36) NOT NULL COMMENT '主键UUID（图片/文件消息的下载地址为 /api/files/{id}/download）',
                                   `from_user` varchar(50) DEFAULT NULL COMMENT '发送方用户名（区分大小写）',
                                   `to_user` varchar(50) DEFAULT NULL COMMENT '接收方用户名（区分大小写）',
                                   `content` varchar(2000) NOT NULL COMMENT '消息内容：text 为文本；image 为站内下载地址；card/location/file 为 JSON',
                                   `msg_type` varchar(16) NOT NULL COMMENT '消息类型：text 文本 / image 图片 / poke 拍一拍 / system 系统 / card 名片 / location 位置 / file 文件',
                                   `status` varchar(16) NOT NULL COMMENT '消息状态：SENT 已发送 / RECALLED 已撤回（发送2分钟内可撤回）',
                                   `reply_to_id` varchar(36) DEFAULT NULL COMMENT '引用回复的消息ID',
                                   `read_flag` tinyint(4) DEFAULT 0 COMMENT '是否已读：1 已读 / 0 未读',
                                   `edited` tinyint(4) DEFAULT 0 COMMENT '内容是否已编辑：1 已编辑 / 0 未编辑',
                                   `heart_at` bigint(20) DEFAULT NULL COMMENT '心动时刻标记时间（毫秒时间戳，null = 未标记）',
                                   `created` bigint(20) NOT NULL COMMENT '发送时间（毫秒时间戳）',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_pm_from_to_created` (`from_user`,`to_user`,`created`),
                                   KEY `idx_pm_to_from_created` (`to_user`,`from_user`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='点对点私聊消息表：支持文本/图片/拍一拍/系统/名片/位置/文件消息';


-- are_chat.registration_application definition

CREATE TABLE `registration_application` (
                                            `id` varchar(36) NOT NULL COMMENT '主键UUID',
                                            `phone` varchar(20) DEFAULT NULL COMMENT '申请手机号（区分大小写）',
                                            `username` varchar(50) DEFAULT NULL COMMENT '申请用户名（区分大小写）',
                                            `nickname` varchar(32) DEFAULT NULL COMMENT '注册时填写的昵称（审批通过后写入 app_user/user_profile，历史申请可能为空）',
                                            `password_hash` varchar(255) DEFAULT NULL COMMENT '登录密码哈希（PBKDF2，审批通过时原样搬入 app_user）',
                                            `status` varchar(16) NOT NULL COMMENT '审批状态：PENDING 待审批 / APPROVED 已通过 / REJECTED 已拒绝',
                                            `reject_reason` varchar(200) DEFAULT NULL COMMENT '拒绝原因',
                                            `created` bigint(20) NOT NULL COMMENT '申请时间（毫秒时间戳）',
                                            `reviewed_at` bigint(20) DEFAULT NULL COMMENT '审批时间（毫秒时间戳）',
                                            `reviewed_by` varchar(64) DEFAULT NULL COMMENT '审批人用户名（管理员）',
                                            PRIMARY KEY (`id`),
                                            KEY `idx_reg_app_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='注册申请表：管理员审批通过后才真正创建 app_user 账号';


-- are_chat.uploaded_file definition

CREATE TABLE `uploaded_file` (
                                 `id` varchar(36) NOT NULL COMMENT '主键UUID（站内下载地址为 /api/files/{id}/download）',
                                 `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
                                 `stored_path` varchar(255) NOT NULL COMMENT '服务端存储路径',
                                 `content_type` varchar(127) DEFAULT NULL COMMENT 'MIME 类型',
                                 `size` bigint(20) NOT NULL COMMENT '文件大小（字节）',
                                 `sha256` varchar(64) NOT NULL COMMENT '文件内容 SHA-256，全局唯一，用于秒传去重',
                                 `uploaded_at` bigint(20) DEFAULT NULL COMMENT '上传时间（毫秒时间戳）',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uq_uploaded_file_sha256` (`sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='上传文件档案表：按 SHA-256 内容寻址，天然去重';


-- are_chat.user_profile definition

CREATE TABLE `user_profile` (
                                `username` varchar(50) NOT NULL COMMENT '用户名，主键，关联 app_user.username（区分大小写）',
                                `nickname` varchar(32) DEFAULT NULL COMMENT '昵称',
                                `signature` varchar(100) DEFAULT NULL COMMENT '个性签名',
                                `avatar` varchar(16) DEFAULT NULL COMMENT '头像标识（内置头像色档编号，如 c0）',
                                `presence_status` varchar(16) DEFAULT 'online' COMMENT '在线状态：online 在线 / busy 忙碌 / away 离开',
                                `birthday` varchar(10) DEFAULT NULL COMMENT '生日（yyyy-MM-dd，允许只填 MM-dd 表达不在意年份）',
                                `updated_at` bigint(20) DEFAULT NULL COMMENT '资料更新时间（毫秒时间戳）',
                                PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户资料表：与 app_user 一一对应（注册时同步创建），存昵称/签名/头像/在线状态';

-- =============================================================
-- 以下为原始导出数据（INSERT），建表语句已去重，仅保留一份
-- 注意：app_user 含密码哈希（PBKDF2），请按最小权限控制本文件访问
-- =============================================================
INSERT INTO admin_audit (id,actor,`action`,target,detail,created) VALUES
                                                                      ('33206eab-1660-49ba-9b6f-11b823f0ed82','admin','RESET_PASSWORD','liufan','重置用户密码（指定密码）',1791044475144),
                                                                      ('6a04fd9b-e169-409f-b5b2-ff67f7c3dd0f','admin','RESET_PASSWORD','liufan','重置用户密码（指定密码）',1790448923206),
                                                                      ('a07655d7-1013-4df7-a22b-61931c7dbbe5','admin','RESET_PASSWORD','kaylazho','重置用户密码',1790446502408),
                                                                      ('aefc11c8-abd9-4247-814e-5dca9b2d5d27','admin','RESET_PASSWORD','kaylazho','重置用户密码（指定密码）',1790448912812),
                                                                      ('c91f2b82-faf9-448f-be0e-a67aa827ca94','admin','RESET_PASSWORD','admin','重置用户密码（指定密码）',1790448948676),
                                                                      ('restore-audit-kaylazho-0001','admin','APPROVE','kaylazho','通过注册申请 restore-reg-kaylazho-000001',1790425217000),
                                                                      ('restore-audit-liufan-0001','admin','APPROVE','liufan','通过注册申请 restore-reg-liufan-000001',1790423861000);
INSERT INTO announcement (id,content,created_by,enabled,created) VALUES
    ('43c2dd73-7cb6-4f48-8348-9099fc4bd43d','明天正常上班','admin',1,1790506616449);
INSERT INTO announcement_read (username,announcement_id,read_at,id) VALUES
                                                                        ('admin','43c2dd73-7cb6-4f48-8348-9099fc4bd43d',1790506622079,'726d1c01-de71-46ed-9b65-678beddb3380'),
                                                                        ('liufan','43c2dd73-7cb6-4f48-8348-9099fc4bd43d',1790519361388,'8a00b2df-935f-4721-8808-223a90cc15a4');
INSERT INTO app_user (id,username,phone,nickname,password_hash,avatar,signature,presence_status,status,`role`,created,last_login_at) VALUES
                                                                                                                                         ('29912c20-5c59-46c9-9b31-9fd76395933f','liufan','13772019579','爸爸','pbkdf2$60000$8p+EQaInBsZM097vRWdFcw==$+CS3/8iLtgOHApZ4l+PRWLt2Xt1i6fkCGO+npiORNrQ=','c0',NULL,'online','ACTIVE','USER',1790425510784,1792516532168),
                                                                                                                                         ('c13efd57-f2bf-450d-a291-536b87dbac94','kaylazho','153****3818','赵碧涵','pbkdf2$60000$hHukkedL4dtVrbbqr1LcDQ==$wbI5azLoSzI+V49giYUVGXNExF29cUtmsxNcNt65N5U=',NULL,NULL,'online','ACTIVE','USER',1790425510784,1790434384976),
                                                                                                                                         ('e36c3ecd-7592-426c-9262-110de62a742b','admin','00000000000','爸爸','pbkdf2$60000$o3DarZAw8J6UwA959g7q/Q==$ti8IjynPa51HXJ1IWaDNoY6W+FHBE2YKZIlgj46PZ/I=','c0','','online','ACTIVE','ADMIN',1790431371145,1792514155800);
INSERT INTO conversation_pin (id,user_a,user_b,msg_id,created_by,created) VALUES
    ('0bd8f48a-6918-48d0-9899-993172b85055','admin','liufan','016a5140-5510-4e7e-9d56-39016fee9967','admin',1792514396091);
INSERT INTO couple_invite (id,from_user,to_user,message,status,created,updated_at) VALUES
                                                                                       ('74ca139f-3375-4688-9d00-5fa0383e7480','admin','liufan',NULL,'ACCEPTED',1791091817695,1791091858756),
                                                                                       ('d6a4de29-189b-4114-b0dc-8f0215cd9a19','admin','liufan',NULL,'ACCEPTED',1791044514291,1791044635809);
INSERT INTO couple_notify (id,username,event,actor,detail,read_flag,created) VALUES
                                                                                 ('00ec07ad-6a10-472c-83a9-3c8e961a78fe','liufan','comfort-sent','admin','🫂 TA 说 TA 现在有点难过 😢，需要一个抱抱——去 TA 那里给 TA 一点温柔吧',0,1791124613763),
                                                                                 ('03317956-7c71-4d38-bba8-9869b6a11576','admin','bond-action','liufan','TA 蹭了蹭你 😚 好黏人',0,1791124757676),
                                                                                 ('0aebfbc2-fe56-4c7e-8fe6-cbe115cdfbb4','liufan','scratch-scratched','admin','TA 刮开了你送的刮刮乐，抽中了「十句不重样的夸夸」🎟️ 快准备兑现吧！',0,1791124578122),
                                                                                 ('0bf52897-299c-480a-8849-ec23ad45ccec','liufan','comfort-sent','admin','🫂 TA 说 TA 现在有点委屈 🥺，需要一个抱抱——去 TA 那里给 TA 一点温柔吧',0,1791124613346),
                                                                                 ('105588d8-eca3-4b65-b6ab-9a33a3fb8c39','liufan','bond-milestone','liufan','第 1 次「亲亲」达成 🎉💋 你们好甜！',0,1791091901651),
                                                                                 ('236c4dd5-c740-49ca-8278-dfb944151707','liufan','comfort-sent','admin','🫂 TA 说 TA 现在有点委屈 🥺，需要一个抱抱——去 TA 那里给 TA 一点温柔吧',0,1791124608161),
                                                                                 ('24471dee-a26b-4f1c-a8eb-02115d0525c0','admin','anniversary-updated','liufan','TA 更新了你们「在一起」的日子 📅',0,1791124801497),
                                                                                 ('2687cca3-5359-4928-9ea4-bf3ac250f13b','admin','bond-action','liufan','TA 亲了你一口 💋 嘻嘻',0,1791124745595),
                                                                                 ('27b5855b-fdaf-4999-aa53-9543f90ef3dd','liufan','comfort-sent','admin','🫂 TA 说 TA 现在有点委屈 🥺，需要一个抱抱——去 TA 那里给 TA 一点温柔吧',0,1791124612659),
                                                                                 ('35582356-fe02-4286-a4da-49874155050b','admin','challenge-checked','liufan','⚡ TA 已完成今日挑战：「今天不催 TA 任何事」就差你啦！',0,1791044678346);
INSERT INTO couple_notify (id,username,event,actor,detail,read_flag,created) VALUES
                                                                                 ('3b35598f-f1ed-48bb-b55c-bee6dce92e7d','admin','dissolved','liufan','对方解除了情侣空间 😢',0,1791044716211),
                                                                                 ('3e39505e-92ad-45d0-a428-001c9859f17f','liufan','quest-move-done','liufan','✅ 「」打包完了，0 箱。搬家这件事，我们又少怕了一点。',0,1791044656283),
                                                                                 ('3e484310-7588-4d47-abdb-5b0f040d1164','admin','bond-action','liufan','TA 给了你一个大大的拥抱 🤗 快抱回去！',0,1791091900765),
                                                                                 ('41c0fc18-5fa9-4229-8c77-7a41ff3ebc57','admin','invite-accepted','liufan','对方同意啦！你们的情侣空间已开启 🎉',0,1791044635875),
                                                                                 ('5129b95d-d5ff-40b3-b198-0d3531addac1','liufan','invite','admin','TA 邀请你开启情侣空间 💕',0,1791091817751),
                                                                                 ('5c6ea134-0350-4074-b23b-e900dab48ec0','admin','bond-action','liufan','TA 戳了戳你 👉 快回戳！',0,1791091902420),
                                                                                 ('63bac4f1-80f8-41a1-9d10-707f2463a7c4','admin','bond-milestone','liufan','第 1 次「亲亲」达成 🎉💋 你们好甜！',0,1791091901681),
                                                                                 ('65f69a3c-46b0-4076-96b7-0c625e708cb8','admin','bond-action','liufan','TA 挠你痒痒 🤭 哈哈哈别跑！',0,1791091903494),
                                                                                 ('74009277-0e06-46b2-900a-bb040e155d78','admin','bond-action','liufan','TA 说 TA 在想你 💌 现在立刻马上',0,1791091902011),
                                                                                 ('7c34ea26-b156-48c1-a424-55f49f3fb2ac','admin','passbook-deposit','liufan','💰 TA 在恋爱存折里存了一笔：「200」',0,1791044688486);
INSERT INTO couple_notify (id,username,event,actor,detail,read_flag,created) VALUES
                                                                                 ('7c8f2bc2-31df-4a79-82ab-fc4ff192590e','liufan','bond-milestone','liufan','第 1 次「抱抱」达成 🎉🤗 你们好甜！',0,1791091900833),
                                                                                 ('7cb9c8c7-2506-4772-b932-52d0963bf525','admin','bond-action','liufan','TA 戳了戳你 👉 快回戳！',0,1791124748333),
                                                                                 ('82bc2feb-3665-4603-ae56-750c6f4e8fa4','admin','bond-action','liufan','TA 捏了捏你的脸 🫳 好软',0,1791124749582),
                                                                                 ('8e6312a7-b28e-4ebf-b2e5-cbc2a1db6e8d','admin','bond-action','liufan','TA 捏了捏你的脸 🫳 好软',0,1791091902805),
                                                                                 ('93d4bcd5-294e-4bad-b76c-07167c8c999e','liufan','comfort-sent','admin','🫂 TA 说 TA 现在有点焦虑 😖，需要一个抱抱——去 TA 那里给 TA 一点温柔吧',0,1791124609535),
                                                                                 ('9b9fd219-0750-4ffd-940a-db3fc9576413','admin','bond-action','liufan','TA 给了你一个大大的拥抱 🤗 快抱回去！',0,1791124744743),
                                                                                 ('9c4e4e2c-b644-49fe-aa21-c1c23efaf15c','liufan','comfort-sent','admin','🫂 TA 说 TA 现在有点好累 😮‍💨，需要一个抱抱——去 TA 那里给 TA 一点温柔吧',0,1791124609240),
                                                                                 ('b6744aed-14c7-45df-8a34-d7229c3280f5','admin','bond-milestone','liufan','第 1 次「在想你」达成 🎉💌 你们好甜！',0,1791091902106),
                                                                                 ('b92b8475-882f-4e6c-8f53-616ea2251849','admin','quest-move-done','liufan','✅ 「」打包完了，0 箱。搬家这件事，我们又少怕了一点。',0,1791044656252),
                                                                                 ('bc049eb9-e793-4846-b2f6-7a77e0a9eed0','liufan','scratch-scratched','admin','TA 刮开了你送的刮刮乐，抽中了「十句不重样的夸夸」🎟️ 快准备兑现吧！',0,1791124577333);
INSERT INTO couple_notify (id,username,event,actor,detail,read_flag,created) VALUES
                                                                                 ('be9c4d2b-ed48-408d-be6e-3579785658d1','admin','bond-action','liufan','TA 亲了你一口 💋 嘻嘻',0,1791091901591),
                                                                                 ('c7ffbd71-1273-4f88-8f53-53d5089878dc','liufan','comfort-sent','admin','🫂 TA 说 TA 现在有点emo 🌧️，需要一个抱抱——去 TA 那里给 TA 一点温柔吧',0,1791124609916),
                                                                                 ('cb55d4c4-ae21-49cd-b3cd-d0fb0fae6e51','liufan','bond-milestone','liufan','第 1 次「在想你」达成 🎉💌 你们好甜！',0,1791091902074),
                                                                                 ('d1ac72e6-b27b-4d61-a669-a75bbcc7d358','liufan','comfort-sent','admin','🫂 TA 说 TA 现在有点好累 😮‍💨，需要一个抱抱——去 TA 那里给 TA 一点温柔吧',0,1791124612512),
                                                                                 ('dca1179a-606b-433e-85b0-9f1d64c3620b','liufan','comfort-sent','admin','🫂 TA 说 TA 现在有点难过 😢，需要一个抱抱——去 TA 那里给 TA 一点温柔吧',0,1791124608397),
                                                                                 ('dcfa60da-883f-4870-95b7-4c8fc4ac4c03','admin','invite-accepted','liufan','对方同意啦！你们的情侣空间已开启 🎉',0,1791091858819),
                                                                                 ('df61a323-83c5-480d-a6f4-b9ff676b8c08','admin','quest-move-claim','liufan','📦 「」这块 liufan 认领了。剩下的格子谁来？',0,1791044652280),
                                                                                 ('e2ceb6ff-f2a8-4bea-a298-f9efc45290d9','liufan','comfort-sent','admin','🫂 TA 说 TA 现在有点焦虑 😖，需要一个抱抱——去 TA 那里给 TA 一点温柔吧',0,1791124611125),
                                                                                 ('e532c3af-a8c3-4df2-9f20-11216d59c8f0','admin','bond-action','liufan','TA 蹭了蹭你 😚 好黏人',0,1791091903165),
                                                                                 ('e96ec641-1212-4453-b9bd-33caff5d370f','admin','bond-action','liufan','TA 说 TA 在想你 💌 现在立刻马上',0,1791124746664);
INSERT INTO couple_notify (id,username,event,actor,detail,read_flag,created) VALUES
                                                                                 ('eb0859c2-70ce-41a5-9550-905acf8468ac','admin','bond-milestone','liufan','第 1 次「抱抱」达成 🎉🤗 你们好甜！',0,1791091900864),
                                                                                 ('f29e03e3-a707-42bd-8458-818d206c0c15','liufan','invite','admin','TA 邀请你开启情侣空间 💕',0,1791044514332);
INSERT INTO couple_space (id,user_a,user_b,status,anniversary,created,dissolved_at,city_a,city_b,nick_a,nick_b,slogan,theme) VALUES
                                                                                                                                 ('aa048baf-2d06-4a50-b84b-f140461bf233','admin','liufan','ACTIVE','2026-10-01',1791091858785,NULL,NULL,NULL,NULL,NULL,NULL,'classic'),
                                                                                                                                 ('e9e89ced-4883-49e9-ae7b-c4ecb00316a5','admin','liufan','DISSOLVED',NULL,1791044635838,1791044716176,NULL,NULL,NULL,NULL,NULL,'classic');
INSERT INTO friend (id,owner_username,friend_username,remark,tag,pinned,muted,blocked,last_read_at,last_seen_at,created) VALUES
                                                                                                                             ('35ad8a1c-2d48-428e-a00f-ae55e6cf2984','liufan','kaylazho','小赵','',0,0,0,1790430985308,NULL,1790425510784),
                                                                                                                             ('a87f56ff-ee8a-4889-aa71-3ba8865a566d','liufan','admin','','',0,0,0,1792516536249,1792514423353,1790449238586),
                                                                                                                             ('bb2425d1-bcbc-488f-979c-902f0953643f','admin','liufan','刘帆','',0,0,0,1790449255216,1792516934589,1790449238582),
                                                                                                                             ('c7529f83-2b50-4d3f-896a-5c5ab61dbf8c','kaylazho','liufan',NULL,'',0,0,0,1790430026646,1792516934589,1790425510784);
INSERT INTO friend_request (id,from_user,to_user,message,status,created,updated_at) VALUES
    ('3cb155e7-b2a5-49d9-be80-7b798a0741b9','admin','liufan',NULL,'ACCEPTED',1790449206262,1790449238577);
INSERT INTO message_reaction (id,msg_id,username,emoji,created) VALUES
                                                                    ('0e1caee9-af72-4160-859c-b69485b3a274','016a5140-5510-4e7e-9d56-39016fee9967','liufan','🔥',1790519375534),
                                                                    ('1f0043fe-ec36-4453-b150-184ba35bf551','016a5140-5510-4e7e-9d56-39016fee9967','liufan','👍',1791126908265),
                                                                    ('2bc055a1-77ec-4530-beac-fe938c39b775','016a5140-5510-4e7e-9d56-39016fee9967','admin','👍',1790506669811),
                                                                    ('2c023d22-1775-4652-be4c-bda4c52c76b2','016a5140-5510-4e7e-9d56-39016fee9967','admin','🎉',1790506668302),
                                                                    ('2edad24a-7d48-49a2-8924-a99c6a185e1e','016a5140-5510-4e7e-9d56-39016fee9967','admin','😍',1790506681482),
                                                                    ('3a5ee89a-bcdd-4253-9bce-75923ef9638e','169d0486-4b80-4b45-9e0d-5752caab32e0','kaylazho','😂',1790427668616),
                                                                    ('3cd94a19-f78c-4f0a-b650-0cacd3a407e5','016a5140-5510-4e7e-9d56-39016fee9967','liufan','😍',1790519374881),
                                                                    ('3e72b704-9399-4744-b9c2-cf93240eed38','cc21c55b-28c0-42e2-8b7a-29a1835a3a54','admin','🤗',1790489731925),
                                                                    ('4bb994e2-8843-4153-b67f-3faadc570b1e','76667cfc-9ec2-400b-8ea8-7a2dbe9d98db','kaylazho','🔥',1790426957786),
                                                                    ('549b12fa-6eb4-4e06-abdf-fbb87e967320','016a5140-5510-4e7e-9d56-39016fee9967','admin','🔥',1790506668681);
INSERT INTO message_reaction (id,msg_id,username,emoji,created) VALUES
                                                                    ('5b2c0e9e-3652-460a-8b59-282eadcd6548','349882ca-b426-4a59-bf53-e466b9c4555a','liufan','👍',1790427008715),
                                                                    ('5cf1f27f-efa9-4e8d-8bf9-f56f95de7e60','cc21c55b-28c0-42e2-8b7a-29a1835a3a54','admin','🔥',1790489723806),
                                                                    ('609bd8b5-9d67-4d55-8bdd-595503f0669a','27d0e24e-1262-419b-84fd-1c2315b9ff4d','admin','🎉',1791044406822),
                                                                    ('74abe6bc-1418-43d0-956d-a2efa9bb0321','016a5140-5510-4e7e-9d56-39016fee9967','admin','😮',1790506666651),
                                                                    ('79cc8d1e-6789-4ce9-bae8-f287a76b28d3','016a5140-5510-4e7e-9d56-39016fee9967','liufan','😢',1791126910821),
                                                                    ('808c99e6-cf6f-41c0-9df5-126e4220e279','169d0486-4b80-4b45-9e0d-5752caab32e0','kaylazho','👍',1790427668616),
                                                                    ('853fb9a7-7b58-4abc-a47e-d9723fd3986c','d11291dd-c568-43e1-b324-ba7ac28547ae','kaylazho','😢',1790429558158),
                                                                    ('8702be4d-7ff3-497a-9d74-3b603895ba01','63ffe72c-a41c-462d-8e0f-167674c51c5e','liufan','😢',1790426666968),
                                                                    ('9cea323a-9938-4944-8879-ddd7ad7a52ae','42ad26db-cfc3-4566-aa15-79b9c7992bcf','kaylazho','👍',1790427059644),
                                                                    ('9e424d0e-8fe1-45cd-b488-d7c385d57089','cc21c55b-28c0-42e2-8b7a-29a1835a3a54','admin','🥰',1790489725408);
INSERT INTO message_reaction (id,msg_id,username,emoji,created) VALUES
                                                                    ('a3bd5390-eff7-4820-82c1-e2fed903fc1f','29d7354d-2e6f-4974-a836-873af15a6e60','kaylazho','👍',1790427131186),
                                                                    ('af88b1ce-78db-43bf-96a3-1ac647a605a5','27d0e24e-1262-419b-84fd-1c2315b9ff4d','admin','👍',1791044412490),
                                                                    ('b167e71b-a158-455e-b842-7323ba06a89c','016a5140-5510-4e7e-9d56-39016fee9967','admin','💕',1792514388770),
                                                                    ('b52dce4c-ae01-45d7-8a75-b5129b459a4f','016a5140-5510-4e7e-9d56-39016fee9967','admin','❤️',1790506669294),
                                                                    ('b787332b-4a67-4a0a-bf15-1387951b9d78','8348e385-7163-4809-9f87-b4d41571d3bb','liufan','👍',1790427510934),
                                                                    ('c38b2b21-c59c-4fc5-9d2c-ecbc706f88c3','5ace757e-ac91-4ca0-8f79-0ee1f898a0a6','liufan','😂',1790427272343),
                                                                    ('c6f2c8ed-a243-4726-b9c7-a1ab21197e02','016a5140-5510-4e7e-9d56-39016fee9967','admin','😢',1790506667433),
                                                                    ('c6fb6fcd-cb0b-46ab-b9de-afe290015492','f55ded19-3d15-4059-b000-0831c5047820','liufan','😢',1790428584082),
                                                                    ('d0f1231a-ebfd-4cad-8324-b9c484c80256','ab3b75ac-59cb-46fa-bfcb-6252dad19625','liufan','😢',1790426778842),
                                                                    ('d90a6a5f-0702-45a1-a8ed-9640a7faea7b','29d7354d-2e6f-4974-a836-873af15a6e60','liufan','👍',1790427131186);
INSERT INTO message_reaction (id,msg_id,username,emoji,created) VALUES
                                                                    ('df11419d-9a98-4a99-81ff-abcc3c01f9a5','016a5140-5510-4e7e-9d56-39016fee9967','liufan','🎉',1792516752533),
                                                                    ('e1eba6e7-b663-465b-9a2a-489f7af960d8','cc21c55b-28c0-42e2-8b7a-29a1835a3a54','admin','🎉',1790489722945),
                                                                    ('f4220017-0523-47bd-9c83-a9f4050f91cb','016a5140-5510-4e7e-9d56-39016fee9967','admin','🌹',1790506672135),
                                                                    ('f47ff2e3-211c-45d6-a385-a52b085c096d','016a5140-5510-4e7e-9d56-39016fee9967','admin','🥰',1790506667844),
                                                                    ('f8e4fb42-5e5f-4549-be6f-66e677337db1','d82d8829-25bc-4895-bb9c-05e88930ad6e','liufan','👍',1790426762995),
                                                                    ('ff4ad846-e5fb-4a37-a687-d578f1791e89','7b2b422c-e45d-4a86-8675-7b13287b4c35','liufan','❤️',1790426288923);
INSERT INTO message_star (id,username,msg_id,created) VALUES
    ('707b4e26-90e9-4e81-b3a4-5c51d0d79f73','admin','cc21c55b-28c0-42e2-8b7a-29a1835a3a54',1791044386287);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
                                                                                                                             ('016a5140-5510-4e7e-9d56-39016fee9967','admin','liufan','📞☎️💧⭕🦀','text','SENT',NULL,1,0,NULL,1790506664163),
                                                                                                                             ('03810e0f-e586-4a71-bf8c-f3e3ee5331cf','kaylazho','liufan','可爱的帆宝','text','SENT',NULL,1,0,NULL,1790426274398),
                                                                                                                             ('055e39e5-be81-405c-b24e-5b53e9e1453a','liufan','kaylazho','你能看到了吗？','text','SENT',NULL,1,0,NULL,1790426316108),
                                                                                                                             ('0758590f-aab1-448c-bd7f-50118fbac5ec','kaylazho','liufan','✨','text','SENT',NULL,1,0,NULL,1790429520748),
                                                                                                                             ('0a2b6a99-9a64-42c3-bc77-5db7abdeaeaa','kaylazho','liufan','🎉','text','SENT',NULL,1,0,NULL,1790429492339),
                                                                                                                             ('0b027662-ceec-4889-bb33-2657be346271','kaylazho','liufan','💔','text','SENT',NULL,1,0,NULL,1790429499440),
                                                                                                                             ('0cefde68-24fa-4808-8288-7cd27cf921e3','kaylazho','liufan','有一天，猪和老虎在外面山洞过夜','text','SENT',NULL,1,0,NULL,1790428145506),
                                                                                                                             ('0e702eae-ecb5-42da-a04a-a5c67d770537','admin','liufan','摸摸头 🫳','poke','SENT',NULL,1,0,NULL,1791124527589),
                                                                                                                             ('105f13d4-d22d-4e04-b6fe-0cad916e14b7','liufan','kaylazho','辛苦了','text','SENT',NULL,1,0,NULL,1790427684818),
                                                                                                                             ('112b6e29-3ee5-44ee-aadd-813262426929','liufan','kaylazho','🎈','text','SENT',NULL,0,0,NULL,1790519186409);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
                                                                                                                             ('169d0486-4b80-4b45-9e0d-5752caab32e0','liufan','kaylazho','{"name":"酒店-0909-0911.jpg","size":624181,"url":"/api/files/cb1a31e8-40e1-4610-a771-1604ed1ae062/download"}','file','SENT',NULL,1,0,NULL,1790427668616),
                                                                                                                             ('1bb453b3-34e4-440e-8d3b-231fdcdd3abf','liufan','kaylazho','就是乱发的','text','SENT',NULL,1,0,NULL,1790427858215),
                                                                                                                             ('1c4c6323-ab89-41fc-8dfb-27fca0ff86b9','liufan','kaylazho','你冰雪聪明','text','SENT',NULL,1,0,NULL,1790427627926),
                                                                                                                             ('1f972d69-90cc-479d-8993-af952e77fda7','liufan','kaylazho','看了','text','SENT',NULL,1,0,NULL,1790426271353),
                                                                                                                             ('21925d5d-07dd-4bef-b395-4c4167aa7771','kaylazho','liufan','😄','text','SENT',NULL,1,0,NULL,1790426204060),
                                                                                                                             ('2192da07-3683-4e7a-892d-9156d432f56b','kaylazho','liufan','哦，还可以引用','text','SENT',NULL,1,0,NULL,1790427154844),
                                                                                                                             ('219e0f66-1587-44d8-9de0-b0cbbb44dc4f','kaylazho','liufan','讲笑话会触发什么奖励机制','text','SENT',NULL,1,0,NULL,1790428015406),
                                                                                                                             ('23a33fa6-8c66-4282-bfa8-773c26d9dbde','liufan','kaylazho','10分钟','text','SENT',NULL,1,0,NULL,1790429990898),
                                                                                                                             ('27d0e24e-1262-419b-84fd-1c2315b9ff4d','admin','liufan','{"name":"北京·天安门广场","address":"北京市东城区东长安街"}','location','SENT',NULL,1,0,NULL,1791044404450),
                                                                                                                             ('28244fef-f744-4846-96b2-155e7e359e4e','kaylazho','liufan','不会讲笑话','text','SENT',NULL,1,0,NULL,1790427916563);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
                                                                                                                             ('29d7354d-2e6f-4974-a836-873af15a6e60','kaylazho','liufan','这还能撤回消息，咋弄的','text','SENT',NULL,1,0,NULL,1790427131186),
                                                                                                                             ('2bd8e426-62f0-4186-8ba4-4c1f5d7bbe4a','kaylazho','liufan','🌹','text','SENT',NULL,1,0,NULL,1790429524673),
                                                                                                                             ('2e9c7429-9bb1-4872-8115-f50a5f320db2','liufan','kaylazho','你先上个厕所','text','SENT',NULL,1,0,NULL,1790429996626),
                                                                                                                             ('2e9f5fc0-d2e5-4d06-aa8f-c51806609c1d','liufan','kaylazho','在部署程序','text','SENT',NULL,1,0,NULL,1790429987799),
                                                                                                                             ('3125b215-4916-463f-9a5b-fb9c54ee6e07','liufan','kaylazho','你琢磨一下','text','SENT',NULL,1,0,NULL,1790427624164),
                                                                                                                             ('32bf8ed7-04fd-4223-8b41-b6833d134cd1','liufan','kaylazho','❤️','text','SENT',NULL,1,0,NULL,1790429976202),
                                                                                                                             ('349882ca-b426-4a59-bf53-e466b9c4555a','kaylazho','liufan','因为我冰雪聪明','text','SENT',NULL,1,0,NULL,1790427008715),
                                                                                                                             ('39588c45-2196-493a-bdd2-f47022edc865','liufan','kaylazho','收到','text','SENT',NULL,1,0,NULL,1790427679081),
                                                                                                                             ('3a601077-d655-41b0-95ec-abf9a5b1c16e','liufan','kaylazho','嘿嘿','text','SENT',NULL,1,0,NULL,1790426308959),
                                                                                                                             ('3db97e7a-7947-4bbd-a5b5-0ed3067a224b','liufan','kaylazho','触发奖励机制','text','SENT',NULL,1,0,NULL,1790428087747);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
                                                                                                                             ('42ad26db-cfc3-4566-aa15-79b9c7992bcf','liufan','kaylazho','牛批','text','SENT',NULL,1,0,NULL,1790427059644),
                                                                                                                             ('46a48594-f4fe-485a-8e47-896786547d47','liufan','kaylazho','老虎被猪打死了？','text','SENT',NULL,1,0,NULL,1790428181843),
                                                                                                                             ('4944bcfb-4f0d-485c-8087-8aea64b8e3e9','kaylazho','liufan','🎁','text','SENT',NULL,1,0,NULL,1790429528779),
                                                                                                                             ('506145d2-4f8c-4aec-a18a-88d0c82b08e1','kaylazho','liufan','是不是猜不出来','text','SENT',NULL,1,0,NULL,1790428828618),
                                                                                                                             ('5067ecae-9d57-4d06-848b-89c9ae7fed5c','kaylazho','liufan','🐷也猜不到呀','text','SENT',NULL,1,0,NULL,1790428909347),
                                                                                                                             ('545287bb-cf23-40c6-929b-c582ff0800c3','kaylazho','liufan','[拍一拍]','poke','SENT',NULL,1,0,NULL,1790425875296),
                                                                                                                             ('54db10fd-7617-4148-bd29-5397f648fd8f','kaylazho','liufan','看到了','text','SENT',NULL,1,0,NULL,1790427907453),
                                                                                                                             ('54f6d25a-5e18-4d59-97d2-f4f82df012df','kaylazho','liufan','高冷女王','text','SENT',NULL,1,0,NULL,1790427963945),
                                                                                                                             ('582db41d-e4bb-4fc6-83f8-27e1906e7acd','kaylazho','liufan','不是的','text','SENT',NULL,1,0,NULL,1790428436823),
                                                                                                                             ('5ace757e-ac91-4ca0-8f79-0ee1f898a0a6','kaylazho','liufan','回应是干啥的','text','SENT',NULL,1,0,NULL,1790427272343);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
                                                                                                                             ('610c2c7c-a048-40f5-b911-bf4239e91fc1','kaylazho','liufan','没事，你部署吧','text','SENT',NULL,1,0,NULL,1790430013133),
                                                                                                                             ('63ffe72c-a41c-462d-8e0f-167674c51c5e','kaylazho','liufan','帅哥，你qq号多少','text','SENT',NULL,1,0,NULL,1790426666968),
                                                                                                                             ('6e5240b9-b376-4a04-b027-9479bf4c162f','liufan','kaylazho','就是 cute 一下','text','RECALLED',NULL,1,0,NULL,1790427417829),
                                                                                                                             ('7285eddb-e90a-45b8-8225-d472a60f9543','liufan','kaylazho','好的','text','SENT',NULL,0,0,NULL,1790430985308),
                                                                                                                             ('76667cfc-9ec2-400b-8ea8-7a2dbe9d98db','liufan','kaylazho','对，你咋知道','text','SENT',NULL,1,0,NULL,1790426957786),
                                                                                                                             ('766833b5-b995-49b4-8109-8d8d0a4cd0ea','kaylazho','liufan','不是','text','SENT',NULL,1,0,NULL,1790428817237),
                                                                                                                             ('78010695-074a-40d9-9b98-fea1541d464b','liufan','kaylazho','不用这个app','text','SENT',NULL,1,0,NULL,1790426711922),
                                                                                                                             ('7801f499-ef3c-46e0-aafa-2f3e040df15c','kaylazho','liufan','😜','text','SENT',NULL,1,0,NULL,1790429005841),
                                                                                                                             ('791365b4-14c2-4fcb-bc39-cef244df6b8a','admin','liufan','抱抱你 🤗','poke','SENT',NULL,1,0,NULL,1791124521821),
                                                                                                                             ('7b2a7f1f-67e2-4c4e-a973-f9a8af1fa7a8','kaylazho','liufan','哦哦，实验呢','text','SENT',NULL,1,0,NULL,1790427878739);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
                                                                                                                             ('7b2b422c-e45d-4a86-8675-7b13287b4c35','kaylazho','liufan','❤️','text','SENT',NULL,1,0,NULL,1790426288923),
                                                                                                                             ('82ceea0a-828a-48d0-a96d-34d7ffa530c4','kaylazho','liufan','搓手手，期待','text','SENT',NULL,1,0,NULL,1790429341556),
                                                                                                                             ('8348e385-7163-4809-9f87-b4d41571d3bb','kaylazho','liufan','没看出来咋玩的','text','SENT',NULL,1,0,NULL,1790427510934),
                                                                                                                             ('834bc16d-82ae-4505-862b-965159b31a19','liufan','kaylazho','我部署一下','text','SENT',NULL,1,0,NULL,1790429240175),
                                                                                                                             ('859691a4-0391-4819-85dd-e9689631ce4d','kaylazho','liufan','猜猜这是为啥','text','SENT',NULL,1,0,NULL,1790428190562),
                                                                                                                             ('8696b715-5bda-41dd-9cfa-19b690906256','kaylazho','liufan','不是','text','SENT','46a48594-f4fe-485a-8e47-896786547d47',1,0,NULL,1790428203811),
                                                                                                                             ('86b05f6c-56d1-462e-a6db-da6a6cfb244f','kaylazho','liufan','再猜','text','SENT',NULL,1,0,NULL,1790428446678),
                                                                                                                             ('87584f19-bd0d-49b7-8d1d-61ec7d477863','kaylazho','liufan','🤣','text','SENT',NULL,1,0,NULL,1790428916458),
                                                                                                                             ('88f0d498-be7e-4acd-96e1-e0cd9bcf8b28','kaylazho','liufan','你不是让我给你讲个笑话，我讲了','text','SENT',NULL,1,0,NULL,1790429220460),
                                                                                                                             ('891a7ea8-6c4e-4f32-91df-df0d3dd3a946','liufan','kaylazho','老虎被猪臭死了','text','SENT',NULL,1,0,NULL,1790428570543);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
                                                                                                                             ('895cefe6-3a60-4935-bcec-76fc6ddaf892','liufan','kaylazho','{"name":"61a1cbe8000f853a2b3b3fceafc8b15d_6349480863603700657_m_读书分享活动（格式模板）.doc","size":13824,"url":"/api/files/cf692ccd-8b02-4bdb-85a8-755408fe3941/download"}','file','SENT',NULL,0,0,NULL,1792516590120),
                                                                                                                             ('8ac8ac31-18bc-4d71-aea8-79adaf8db225','admin','liufan','1','text','SENT','016a5140-5510-4e7e-9d56-39016fee9967',1,0,NULL,1792514400387),
                                                                                                                             ('8f9b7c4e-4b67-46fe-9bd6-5327f6b1db15','admin','liufan','举高高 🙌','poke','SENT',NULL,1,0,NULL,1791124534014),
                                                                                                                             ('92abfcc4-5b6a-4e71-897b-8b6675e42149','kaylazho','liufan','⭐','text','SENT',NULL,1,0,NULL,1790429511972),
                                                                                                                             ('96dbe1cd-9f1b-4112-a7cf-915c7794d374','admin','liufan','⏱','text','SENT',NULL,1,0,NULL,1790506654994),
                                                                                                                             ('97ee6ba9-3406-4dfd-8a14-a70f32d70288','liufan','kaylazho','被压死了','text','SENT',NULL,1,0,NULL,1790428663224),
                                                                                                                             ('9f488d23-00d3-4c2e-a8c1-5f7f3486a2a3','liufan','kaylazho','老虎被猪吃了','text','SENT',NULL,1,0,NULL,1790428391432),
                                                                                                                             ('9f695f2c-c730-45c1-8a42-42bf89920961','liufan','kaylazho','在部署啊','text','SENT',NULL,1,0,NULL,1790429450334),
                                                                                                                             ('a17363c3-c602-44c2-a150-0129659666cc','liufan','kaylazho','牛屁','text','RECALLED',NULL,1,0,NULL,1790427050609),
                                                                                                                             ('a5ee3b72-f030-4852-bfe9-b4295117286e','liufan','kaylazho','给你发呢','text','SENT',NULL,1,0,NULL,1790427743662);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
                                                                                                                             ('aa68d34c-3d51-41a0-87e4-cb1cc6497a32','kaylazho','liufan','你今天一天就是手搓了一个网站嘛','text','SENT',NULL,1,0,NULL,1790426868704),
                                                                                                                             ('ab3b75ac-59cb-46fa-bfcb-6252dad19625','kaylazho','liufan','把你打成🐷','text','SENT',NULL,1,0,NULL,1790426778842),
                                                                                                                             ('abd143e4-ed08-4aac-942a-bd44376d5523','liufan','kaylazho','❤️','text','SENT',NULL,0,0,NULL,1790519175505),
                                                                                                                             ('accb6fd1-bf9e-4ec8-80e5-92adabdf7e4e','kaylazho','liufan','结果，第2天，老虎死了。','text','SENT',NULL,1,0,NULL,1790428162683),
                                                                                                                             ('b19c5dca-a025-4d7a-b1f9-413828c2f29c','kaylazho','liufan','天生严肃','text','SENT',NULL,1,0,NULL,1790427925018),
                                                                                                                             ('b5935a98-6db7-444f-b48b-711f2375a07d','kaylazho','liufan','这个界面感觉怪怪的','text','SENT',NULL,1,0,NULL,1790425676451),
                                                                                                                             ('b5a78c00-1040-4627-901a-fff91ab649a3','admin','liufan','🆗','text','SENT',NULL,1,0,NULL,1790489760213),
                                                                                                                             ('b692e69c-edba-4fd8-90aa-4eb9b1cb5be7','kaylazho','liufan','嘿嘿','text','SENT',NULL,1,0,NULL,1790429320133),
                                                                                                                             ('be03782a-70c9-4711-aad0-ea84548550a5','liufan','kaylazho','。。。。','text','SENT',NULL,1,0,NULL,1790428937713),
                                                                                                                             ('c094dbca-1323-4807-a858-2a8fcd2564ad','kaylazho','liufan','奖励啥','text','SENT',NULL,1,0,NULL,1790429230277);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
                                                                                                                             ('c2c5bbb1-1b26-405f-91a5-ada7d2a10d95','admin','liufan','[拍一拍]','poke','SENT',NULL,1,0,NULL,1791044377724),
                                                                                                                             ('c30bec27-faf0-42eb-82f1-929d50dbf299','liufan','kaylazho','弄了两天了','text','SENT',NULL,1,0,NULL,1790426882189),
                                                                                                                             ('c449abf4-5f5b-4a63-9216-fbc3fd3fdd02','liufan','admin','我们已经成为好友，现在开始聊天吧！','system','SENT',NULL,1,0,NULL,1790449238587),
                                                                                                                             ('c50295b0-7ada-4010-89e0-7b5ca68851d7','liufan','kaylazho','山洞塌陷了','text','SENT',NULL,1,0,NULL,1790428659902),
                                                                                                                             ('c572615f-ec09-4973-81ea-fc5cc6933454','liufan','kaylazho','下次狠狠 ** 你','text','SENT',NULL,1,0,NULL,1790428957599),
                                                                                                                             ('c60c3ae4-a5cf-458f-bc08-0bb904cac171','kaylazho','liufan','还能针对消息发表情，哦哟，厉害','text','SENT',NULL,1,0,NULL,1790427206931),
                                                                                                                             ('c7bb26ce-8940-479b-9d82-fbd2546a52b9','kaylazho','liufan','❤️','text','SENT',NULL,1,0,NULL,1790429506017),
                                                                                                                             ('c99dd17f-f483-4ee3-b071-5d341a487621','kaylazho','liufan','猪也在山洞，猪也没死呀','text','SENT',NULL,1,0,NULL,1790428744966),
                                                                                                                             ('cb4ebbc6-eefa-46f6-8f45-59aab0cee4ca','kaylazho','liufan','弄好了，我再过来玩','text','SENT',NULL,1,0,NULL,1790430026646),
                                                                                                                             ('cc21c55b-28c0-42e2-8b7a-29a1835a3a54','admin','liufan','你好','text','SENT',NULL,1,0,NULL,1790449269759);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
                                                                                                                             ('cdb4ab08-9a4f-49fb-a77b-88daf876ec6e','liufan','kaylazho','看到通知公告没有？','text','SENT',NULL,1,0,NULL,1790427865372),
                                                                                                                             ('cfd5688b-348a-47c3-a678-07da64855c65','kaylazho','liufan','哈哈哈哈，你给谁发呢','text','SENT',NULL,1,0,NULL,1790427707708),
                                                                                                                             ('d11291dd-c568-43e1-b324-ba7ac28547ae','kaylazho','liufan','只有礼花和红心有特效','text','SENT',NULL,1,0,NULL,1790429558158),
                                                                                                                             ('d76a04b4-8c31-4fd8-b01d-ad26e2a3349c','liufan','kaylazho','现在社交软件，都有这个功能','text','SENT',NULL,1,0,NULL,1790427439622),
                                                                                                                             ('d82d8829-25bc-4895-bb9c-05e88930ad6e','kaylazho','liufan','🐷','text','SENT',NULL,1,0,NULL,1790426762995),
                                                                                                                             ('db4e84b8-9ae2-4029-ab7d-dbe3b5cc4d34','admin','liufan','😁','text','SENT',NULL,1,0,NULL,1790489750936),
                                                                                                                             ('db7434ea-bc34-41c4-b80f-70e706e8a2c6','kaylazho','liufan','🔥','text','SENT',NULL,1,0,NULL,1790429517230),
                                                                                                                             ('dbb2c192-2af6-4413-8a71-c00cbd54ab7d','kaylazho','liufan','哦哟，你这是要赚外快去嘛','text','SENT',NULL,1,0,NULL,1790426911853),
                                                                                                                             ('de898ee8-5aa4-4c3b-926b-034ba5114452','liufan','kaylazho','就是 care 一下','text','SENT',NULL,1,0,NULL,1790427431946),
                                                                                                                             ('dfa457c4-3619-458e-990e-7d7a49b50f91','liufan','kaylazho','我没qq啊','text','SENT',NULL,1,0,NULL,1790426700700);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
                                                                                                                             ('e6fe3dba-bbae-41d9-a477-0b78ca71162f','liufan','kaylazho','我猜不到','text','SENT',NULL,1,0,NULL,1790428875460),
                                                                                                                             ('ecf63724-2121-4424-a83d-8b01fe6649da','liufan','kaylazho','我要修复bug','text','SENT',NULL,1,0,NULL,1790428093884),
                                                                                                                             ('ed1738cb-2712-4538-9bb2-102cdf8e48ed','kaylazho','liufan','猜得到吗','text','SENT',NULL,1,0,NULL,1790428275645),
                                                                                                                             ('ee6c584f-8145-44fd-95ce-3a317f97a5b1','kaylazho','liufan','你现在在干嘛呀','text','SENT',NULL,1,0,NULL,1790429421074),
                                                                                                                             ('f187e71b-274c-4280-b2b4-4adb77a888c1','liufan','kaylazho','刚好压住老虎了','text','SENT',NULL,1,0,NULL,1790428778571),
                                                                                                                             ('f21ce322-04a6-4839-8d11-bced45a26072','liufan','kaylazho','我们已经成为好友，现在开始聊天吧！','system','SENT',NULL,1,0,NULL,1790425510784),
                                                                                                                             ('f4c7ed37-8616-4ca2-96c6-bf782708b83a','kaylazho','liufan','[拍一拍]','poke','SENT',NULL,1,0,NULL,1790425873149),
                                                                                                                             ('f4fd32fa-7201-43b6-8882-476c08b7e432','admin','liufan','{"name":"酒店-0922-0923.jpg","size":617567,"url":"/api/files/073bec68-3c5a-475f-978e-d29636740e41/download"}','file','SENT',NULL,1,0,NULL,1791044374987),
                                                                                                                             ('f55ded19-3d15-4059-b000-0831c5047820','kaylazho','liufan','也不是','text','SENT',NULL,1,0,NULL,1790428584082),
                                                                                                                             ('f755fcd5-2a67-452e-bf93-d4a4e2b1311d','kaylazho','liufan','就我手机看着就很怪','text','SENT',NULL,1,0,NULL,1790426322335);
INSERT INTO private_message (id,from_user,to_user,content,msg_type,status,reply_to_id,read_flag,edited,heart_at,created) VALUES
    ('f8f78db3-8016-4033-bab8-9352dce5a947','kaylazho','liufan','你给我发的文档干啥的','text','SENT',NULL,1,0,NULL,1790427786296);
INSERT INTO registration_application (id,phone,username,nickname,password_hash,status,reject_reason,created,reviewed_at,reviewed_by) VALUES
                                                                                                                                         ('5bbf3e5c-86b5-45d7-bb22-b473104bb795','13900009999','newbie','小新','pbkdf2$60000$8E8k9/Z6id9CGzxu4Z/eag==$qA8A2INO78U2Z6oRK8jxwK+5PYEFSq9AQV+TRGB5LXI=','PENDING',NULL,1790438386649,NULL,NULL),
                                                                                                                                         ('f678e39c-4dc1-469c-a185-5e3f4a5e5431','13772019579','heihei','嘿嘿','pbkdf2$60000$vYpDBGke6O/lQg4yojyHZw==$P+VYEVZUnN1eTXyQjsSXxbKIXSNBLIK1eHsmTE/VAOc=','PENDING',NULL,1790441916955,NULL,NULL),
                                                                                                                                         ('restore-reg-kaylazho-000001','153****3818','kaylazho',NULL,'pbkdf2$60000$WCLcEIXKMGKFvBqNUhM8BQ==$hBEVxAUMNz6o6EqzhXN/xSWFWGz3ye4JhZJpOuBy6NA=','APPROVED',NULL,1790425181000,1790425217000,'admin'),
                                                                                                                                         ('restore-reg-liufan-000001','137****9579','liufan',NULL,'pbkdf2$60000$ibH0Y+8vDC2TRG1NIWlkdw==$yAQfZwcXPb3wabMGnBbN3mebuiH/ilE4CjVB5N2Ol7s=','APPROVED',NULL,1790423816000,1790423861000,'admin');
INSERT INTO uploaded_file (id,original_name,stored_path,content_type,`size`,sha256,uploaded_at) VALUES
                                                                                                    ('073bec68-3c5a-475f-978e-d29636740e41','酒店-0922-0923.jpg','9c/9c922382dbc98d31a167a216abb57246eecd1d974b243d0c48fae01f19f57fcb.jpg','image/jpeg',617567,'9c922382dbc98d31a167a216abb57246eecd1d974b243d0c48fae01f19f57fcb',1791044374764),
                                                                                                    ('cf692ccd-8b02-4bdb-85a8-755408fe3941','61a1cbe8000f853a2b3b3fceafc8b15d_6349480863603700657_m_读书分享活动（格式模板）.doc','7c/7c197375665de133f37412fe5a95936ee93196d93821c824fe2f4c64c2ac2bb7.doc','application/msword',13824,'7c197375665de133f37412fe5a95936ee93196d93821c824fe2f4c64c2ac2bb7',1792516590073);
INSERT INTO user_profile (username,nickname,signature,avatar,presence_status,birthday,updated_at) VALUES
                                                                                                      ('admin','爸爸','','c0','online',NULL,1792514365973),
                                                                                                      ('kaylazho','kaylazho',NULL,NULL,'online',NULL,NULL),
                                                                                                      ('liufan','爸爸','','c0','online',NULL,1792516532623);

SET FOREIGN_KEY_CHECKS = 1;