-- 测试专用 Flyway 基线：结构来自退役的 schema.sql（与 deploy 合并 SQL 同一套 18 表），H2/MariaDB 双兼容
-- 仅建表不导数据；CREATE TABLE IF NOT EXISTS 幂等。置于 test classpath，只作用于 H2 测试库，生产仍由 init-scripts 建表。

CREATE TABLE IF NOT EXISTS `admin_audit` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `actor` varchar(64) NOT NULL COMMENT '操作人用户名（管理员）',
    `action` varchar(32) NOT NULL COMMENT '操作类型：APPROVE 通过注册 / REJECT 拒绝注册 / ENABLE 启用账号 / DISABLE 禁用账号 / RESET_PASSWORD 重置密码',
    `target` varchar(64) DEFAULT NULL COMMENT '操作对象，通常为目标用户名',
    `detail` varchar(500) DEFAULT NULL COMMENT '操作详情说明',
    `created` bigint(20) NOT NULL COMMENT '操作时间（毫秒时间戳）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理员操作审计表：审批/启用禁用/重置密码等敏感动作全部留痕';

CREATE TABLE IF NOT EXISTS `announcement` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `content` varchar(500) NOT NULL COMMENT '公告内容',
    `created_by` varchar(64) NOT NULL COMMENT '发布人用户名（管理员）',
    `enabled` tinyint(4) DEFAULT 1 COMMENT '是否启用：1 启用（横幅展示）/ 0 停用',
    `created` bigint(20) NOT NULL COMMENT '发布时间（毫秒时间戳）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='全站公告表：管理员发布，所有登录用户顶部横幅展示';

CREATE TABLE IF NOT EXISTS `announcement_read` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '已读用户名（区分大小写）',
    `announcement_id` varchar(36) NOT NULL COMMENT '公告ID，关联 announcement.id',
    `read_at` bigint(20) NOT NULL COMMENT '确认已读时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_ann_read` (`username`,`announcement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告已读记录表：用户点「我知道了」后不再展示该条公告';

CREATE TABLE IF NOT EXISTS `app_user` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '用户名，全局唯一且区分大小写',
    `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '手机号，全局唯一，注册与登录标识',
    `nickname` varchar(32) DEFAULT NULL COMMENT '昵称（注册时默认取用户名）',
    `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '登录密码哈希（PBKDF2）',
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

CREATE TABLE IF NOT EXISTS `conversation_pin` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `user_a` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '会话双方用户名之一（字典序较小者）',
    `user_b` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '会话双方用户名之一（字典序较大者）',
    `msg_id` varchar(36) NOT NULL COMMENT '被置顶的消息ID',
    `created_by` varchar(64) NOT NULL COMMENT '置顶操作人用户名',
    `created` bigint(20) NOT NULL COMMENT '置顶时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_conv_pin` (`user_a`,`user_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会话置顶消息表：每个双人会话最多一条置顶消息，双方共享可见';

CREATE TABLE IF NOT EXISTS `friend` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `owner_username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '拥有者用户名（关系归属方）',
    `friend_username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '好友用户名',
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

CREATE TABLE IF NOT EXISTS `friend_request` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '发起方用户名（区分大小写）',
    `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '接收方用户名（区分大小写）',
    `message` varchar(100) DEFAULT NULL COMMENT '申请留言',
    `status` varchar(16) NOT NULL COMMENT '申请状态：PENDING 待处理 / ACCEPTED 已接受 / REJECTED 已拒绝',
    `created` bigint(20) NOT NULL COMMENT '申请时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近处理时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_fr_to_user_status` (`to_user`, `status`),
    KEY `idx_fr_from_to` (`from_user`, `to_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='好友申请表：对方同意后双方各建一条 friend 关系';

CREATE TABLE IF NOT EXISTS `message_reaction` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `msg_id` varchar(36) NOT NULL COMMENT '被回应的消息ID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '回应人用户名（区分大小写）',
    `emoji` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '表情符号',
    `created` bigint(20) NOT NULL COMMENT '回应时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_reaction` (`msg_id`,`username`,`emoji`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息表情回应表：消息+用户+表情三元组唯一，重复提交为取消（toggle）';

CREATE TABLE IF NOT EXISTS `message_star` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '收藏人用户名（区分大小写）',
    `msg_id` varchar(36) NOT NULL COMMENT '收藏的消息ID',
    `created` bigint(20) NOT NULL COMMENT '收藏时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_star` (`username`,`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息收藏表：个人视角，跨会话收藏消息';

CREATE TABLE IF NOT EXISTS `private_message` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID（图片/文件消息的下载地址为 /api/files/{id}/download）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '发送方用户名（区分大小写）',
    `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '接收方用户名（区分大小写）',
    `content` varchar(2000) NOT NULL COMMENT '消息内容：text 为文本；image 为站内下载地址；card/location/file 为 JSON',
    `msg_type` varchar(16) NOT NULL COMMENT '消息类型：text 文本 / image 图片 / poke 拍一拍 / system 系统 / card 名片 / location 位置 / file 文件',
    `status` varchar(16) NOT NULL COMMENT '消息状态：SENT 已发送 / RECALLED 已撤回（发送2分钟内可撤回）',
    `reply_to_id` varchar(36) DEFAULT NULL COMMENT '引用回复的消息ID',
    `read_flag` tinyint(4) DEFAULT 0 COMMENT '是否已读：1 已读 / 0 未读',
    `edited` tinyint(4) DEFAULT 0 COMMENT '内容是否已编辑：1 已编辑 / 0 未编辑',
    `heart_at` bigint(20) DEFAULT NULL COMMENT '心动时刻标记时间（毫秒时间戳，null = 未标记）',
    `created` bigint(20) NOT NULL COMMENT '发送时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_pm_from_to_created` (`from_user`, `to_user`, `created`),
    KEY `idx_pm_to_from_created` (`to_user`, `from_user`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='点对点私聊消息表：支持文本/图片/拍一拍/系统/名片/位置/文件消息';

CREATE TABLE IF NOT EXISTS `registration_application` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '申请手机号（区分大小写）',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '申请用户名（区分大小写）',
    `nickname` varchar(32) DEFAULT NULL COMMENT '注册时填写的昵称（审批通过后写入 app_user/user_profile，历史申请可能为空）',
    `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '登录密码哈希（PBKDF2，审批通过时原样搬入 app_user）',
    `status` varchar(16) NOT NULL COMMENT '审批状态：PENDING 待审批 / APPROVED 已通过 / REJECTED 已拒绝',
    `reject_reason` varchar(200) DEFAULT NULL COMMENT '拒绝原因',
    `created` bigint(20) NOT NULL COMMENT '申请时间（毫秒时间戳）',
    `reviewed_at` bigint(20) DEFAULT NULL COMMENT '审批时间（毫秒时间戳）',
    `reviewed_by` varchar(64) DEFAULT NULL COMMENT '审批人用户名（管理员）',
    PRIMARY KEY (`id`),
    KEY `idx_reg_app_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='注册申请表：管理员审批通过后才真正创建 app_user 账号';

CREATE TABLE IF NOT EXISTS `uploaded_file` (
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

CREATE TABLE IF NOT EXISTS `couple_space` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `user_a` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '情侣双方用户名之一（字典序较小者）',
    `user_b` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '情侣双方用户名之一（字典序较大者）',
    `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '空间状态：ACTIVE 开启 / DISSOLVED 已解除',
    `anniversary` varchar(10) DEFAULT NULL COMMENT '在一起纪念日（yyyy-MM-dd，用于计算在一起天数，可空默认取建立时间）',
    `city_a` varchar(50) DEFAULT NULL COMMENT '用户 A 所在城市（手填，匹配内置城市库计算时差/距离）',
    `city_b` varchar(50) DEFAULT NULL COMMENT '用户 B 所在城市（手填，匹配内置城市库计算时差/距离）',
    `nick_a` varchar(30) DEFAULT NULL COMMENT 'user_a 的专属爱称（由对方设置，如「宝宝」「猪猪」）',
    `nick_b` varchar(30) DEFAULT NULL COMMENT 'user_b 的专属爱称（由对方设置）',
    `slogan` varchar(60) DEFAULT NULL COMMENT '我们的宣言：只有彼此懂的一句话（60 字内）',
    `theme` varchar(20) NOT NULL DEFAULT 'classic' COMMENT '空间主题：classic 经典粉 / cherry 樱花 / ocean 海盐 / forest 森绿 / night 星夜',
    `created` bigint(20) NOT NULL COMMENT '建立时间（毫秒时间戳）',
    `dissolved_at` bigint(20) DEFAULT NULL COMMENT '解除时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_space_a` (`user_a`),
    KEY `idx_couple_space_b` (`user_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣空间表：一对一关系，user_a/user_b 为规范化排序的双方用户名';

CREATE TABLE IF NOT EXISTS `couple_invite` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '发起方用户名（区分大小写）',
    `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '接收方用户名（区分大小写）',
    `message` varchar(100) DEFAULT NULL COMMENT '邀请留言',
    `status` varchar(16) NOT NULL COMMENT '邀请状态：PENDING 待处理 / ACCEPTED 已同意 / REJECTED 已拒绝 / CANCELED 已取消',
    `created` bigint(20) NOT NULL COMMENT '邀请时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近处理时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_invite_to` (`to_user`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣空间邀请表：对方同意后创建 couple_space';

CREATE TABLE IF NOT EXISTS `user_profile` (
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '用户名，主键，关联 app_user.username（区分大小写）',
    `nickname` varchar(32) DEFAULT NULL COMMENT '昵称',
    `signature` varchar(100) DEFAULT NULL COMMENT '个性签名',
    `avatar` varchar(16) DEFAULT NULL COMMENT '头像标识（内置头像色档编号，如 c0）',
    `presence_status` varchar(16) DEFAULT 'online' COMMENT '在线状态：online 在线 / busy 忙碌 / away 离开',
    `birthday` varchar(10) DEFAULT NULL COMMENT '生日（yyyy-MM-dd，允许只填 MM-dd 表达不在意年份）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '资料更新时间（毫秒时间戳）',
    PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户资料表：与 app_user 一一对应（注册时同步创建），存昵称/签名/头像/在线状态';

CREATE TABLE IF NOT EXISTS `couple_notify` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收件人用户名（区分大小写）',
    `event` varchar(40) NOT NULL COMMENT '事件名（如 letter-created / countdown-reminder）',
    `actor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '触发人用户名（system = 定时任务）',
    `detail` varchar(300) NOT NULL COMMENT '通知文案',
    `read_flag` tinyint(4) DEFAULT 0 COMMENT '是否已读：1 已读 / 0 未读',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_notify_user` (`username`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣空间通知中心表：每次事件推送都给收件人存档一条，登录后可补看与标记已读';

CREATE TABLE IF NOT EXISTS `couple_period` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `start_day` varchar(10) NOT NULL COMMENT '经期开始日期（yyyy-MM-dd，自然日）',
    `end_day` varchar(10) DEFAULT NULL COMMENT '经期结束日期（yyyy-MM-dd；还没结束为 NULL）',
    `recorder_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名',
    `created` bigint(20) NOT NULL COMMENT '记录时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近修改时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple10_period_start` (`space_id`,`start_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='经期记录表：一行=一次经期的开始日（结束日可后补），平均周期与下次预测读时算';

CREATE TABLE IF NOT EXISTS `couple_intimacy_log` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `day` varchar(10) NOT NULL COMMENT '发生日期（yyyy-MM-dd，自然日）',
    `kind` varchar(10) NOT NULL COMMENT '类型：EDGE 边缘亲昵 / SEX 性生活',
    `note` varchar(60) DEFAULT NULL COMMENT '备注（选填，最多 60 字）',
    `recorder_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名',
    `created` bigint(20) NOT NULL COMMENT '记录时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple11_intimacy_space_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='亲密记录表：一行=一次边缘亲昵或性生活，同一天可多条，双方共享';
