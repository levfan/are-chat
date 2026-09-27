-- are-chat 全量结构文档（MySQL / H2 MODE=MySQL 双兼容写法，所有表与字段均带 COMMENT 注释）
-- 表结构统一由 Flyway 自动执行 db/ 下 V 脚本创建与管理：V1__add_couple_space.sql（couple 系列表）
-- + V2__legacy_tables_baseline.sql（存量业务表基线）+ V3__add_couple_mood.sql（心情日记表）
-- + V4__add_couple_letter.sql（悄悄话信箱表）+ V5__add_couple_pact_city_fund.sql（恋爱条约/城市列/心愿基金），
-- 运行时不再执行本文件（spring.sql.init.mode=never）
-- 修改表结构时：新增 V 脚本 + 同步更新本文件，保证文档与真实结构一致
-- 注意：区分大小写的列（用户名/手机号）用列级 CHARACTER SET utf8mb4 COLLATE utf8mb4_bin 声明，不写 DEFAULT（可空为默认，语法对 H2/MySQL/MariaDB 通用）

-- smart_collections.admin_audit definition

CREATE TABLE `admin_audit` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `actor` varchar(64) NOT NULL COMMENT '操作人用户名（管理员）',
    `action` varchar(32) NOT NULL COMMENT '操作类型：APPROVE 通过注册 / REJECT 拒绝注册 / ENABLE 启用账号 / DISABLE 禁用账号 / RESET_PASSWORD 重置密码',
    `target` varchar(64) DEFAULT NULL COMMENT '操作对象，通常为目标用户名',
    `detail` varchar(500) DEFAULT NULL COMMENT '操作详情说明',
    `created` bigint(20) NOT NULL COMMENT '操作时间（毫秒时间戳）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理员操作审计表：审批/启用禁用/重置密码等敏感动作全部留痕';


-- smart_collections.announcement definition

CREATE TABLE `announcement` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `content` varchar(500) NOT NULL COMMENT '公告内容',
    `created_by` varchar(64) NOT NULL COMMENT '发布人用户名（管理员）',
    `enabled` tinyint(4) DEFAULT 1 COMMENT '是否启用：1 启用（横幅展示）/ 0 停用',
    `created` bigint(20) NOT NULL COMMENT '发布时间（毫秒时间戳）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='全站公告表：管理员发布，所有登录用户顶部横幅展示';


-- smart_collections.announcement_read definition

CREATE TABLE `announcement_read` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '已读用户名（区分大小写）',
    `announcement_id` varchar(36) NOT NULL COMMENT '公告ID，关联 announcement.id',
    `read_at` bigint(20) NOT NULL COMMENT '确认已读时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_ann_read` (`username`,`announcement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告已读记录表：用户点「我知道了」后不再展示该条公告';


-- smart_collections.app_user definition

CREATE TABLE `app_user` (
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


-- smart_collections.conversation_pin definition

CREATE TABLE `conversation_pin` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `user_a` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '会话双方用户名之一（字典序较小者）',
    `user_b` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '会话双方用户名之一（字典序较大者）',
    `msg_id` varchar(36) NOT NULL COMMENT '被置顶的消息ID',
    `created_by` varchar(64) NOT NULL COMMENT '置顶操作人用户名',
    `created` bigint(20) NOT NULL COMMENT '置顶时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_conv_pin` (`user_a`,`user_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会话置顶消息表：每个双人会话最多一条置顶消息，双方共享可见';


-- smart_collections.friend definition

CREATE TABLE `friend` (
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


-- smart_collections.friend_request definition

CREATE TABLE `friend_request` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '发起方用户名（区分大小写）',
    `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '接收方用户名（区分大小写）',
    `message` varchar(100) DEFAULT NULL COMMENT '申请留言',
    `status` varchar(16) NOT NULL COMMENT '申请状态：PENDING 待处理 / ACCEPTED 已接受 / REJECTED 已拒绝',
    `created` bigint(20) NOT NULL COMMENT '申请时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近处理时间（毫秒时间戳）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='好友申请表：对方同意后双方各建一条 friend 关系';


-- smart_collections.message_reaction definition

CREATE TABLE `message_reaction` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `msg_id` varchar(36) NOT NULL COMMENT '被回应的消息ID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '回应人用户名（区分大小写）',
    `emoji` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '表情符号',
    `created` bigint(20) NOT NULL COMMENT '回应时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_reaction` (`msg_id`,`username`,`emoji`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息表情回应表：消息+用户+表情三元组唯一，重复提交为取消（toggle）';


-- smart_collections.message_star definition

CREATE TABLE `message_star` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '收藏人用户名（区分大小写）',
    `msg_id` varchar(36) NOT NULL COMMENT '收藏的消息ID',
    `created` bigint(20) NOT NULL COMMENT '收藏时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_star` (`username`,`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息收藏表：个人视角，跨会话收藏消息';


-- smart_collections.private_message definition

CREATE TABLE `private_message` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID（图片/文件消息的下载地址为 /api/files/{id}/download）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '发送方用户名（区分大小写）',
    `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '接收方用户名（区分大小写）',
    `content` varchar(2000) NOT NULL COMMENT '消息内容：text 为文本；image 为站内下载地址；card/location/file 为 JSON',
    `msg_type` varchar(16) NOT NULL COMMENT '消息类型：text 文本 / image 图片 / poke 拍一拍 / system 系统 / card 名片 / location 位置 / file 文件',
    `status` varchar(16) NOT NULL COMMENT '消息状态：SENT 已发送 / RECALLED 已撤回（发送2分钟内可撤回）',
    `reply_to_id` varchar(36) DEFAULT NULL COMMENT '引用回复的消息ID',
    `read_flag` tinyint(4) DEFAULT 0 COMMENT '是否已读：1 已读 / 0 未读',
    `edited` tinyint(4) DEFAULT 0 COMMENT '内容是否已编辑：1 已编辑 / 0 未编辑',
    `created` bigint(20) NOT NULL COMMENT '发送时间（毫秒时间戳）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='点对点私聊消息表：支持文本/图片/拍一拍/系统/名片/位置/文件消息';


-- smart_collections.registration_application definition

CREATE TABLE `registration_application` (
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


-- smart_collections.uploaded_file definition

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


-- smart_collections.couple_space definition

CREATE TABLE `couple_space` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `user_a` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '情侣双方用户名之一（字典序较小者）',
    `user_b` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '情侣双方用户名之一（字典序较大者）',
    `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '空间状态：ACTIVE 开启 / DISSOLVED 已解除',
    `anniversary` varchar(10) DEFAULT NULL COMMENT '在一起纪念日（yyyy-MM-dd，用于计算在一起天数，可空默认取建立时间）',
    `city_a` varchar(50) DEFAULT NULL COMMENT '用户 A 所在城市（手填，匹配内置城市库计算时差/距离）',
    `city_b` varchar(50) DEFAULT NULL COMMENT '用户 B 所在城市（手填，匹配内置城市库计算时差/距离）',
    `created` bigint(20) NOT NULL COMMENT '建立时间（毫秒时间戳）',
    `dissolved_at` bigint(20) DEFAULT NULL COMMENT '解除时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_space_a` (`user_a`),
    KEY `idx_couple_space_b` (`user_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣空间表：一对一关系，user_a/user_b 为规范化排序的双方用户名';


-- smart_collections.couple_invite definition

CREATE TABLE `couple_invite` (
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


-- smart_collections.couple_promise definition

CREATE TABLE `couple_promise` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `promiser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '承诺人用户名（答应做事的一方）',
    `creditor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '受益人用户名（被承诺的一方）',
    `content` varchar(200) NOT NULL COMMENT '承诺内容，如「明天给你带奶茶」',
    `due_at` bigint(20) DEFAULT NULL COMMENT '承诺截止时间（毫秒时间戳，空表示不设期限）',
    `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '承诺状态：PENDING 待兑现 / DONE 已兑现',
    `done_at` bigint(20) DEFAULT NULL COMMENT '兑现打卡时间（毫秒时间戳）',
    `last_remind_day` varchar(10) DEFAULT NULL COMMENT '最近一次逾期提醒日期（yyyy-MM-dd，防止重复打扰）',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_promise_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣约定/承诺卡表：把口头承诺变成可追踪的甜蜜记录';


-- smart_collections.couple_checkin definition

CREATE TABLE `couple_checkin` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '打卡人用户名（区分大小写）',
    `kind` varchar(8) NOT NULL COMMENT '打卡类型：MORNING 早安 / NIGHT 晚安',
    `checkin_day` varchar(10) NOT NULL COMMENT '打卡日期（yyyy-MM-dd，按自然日去重）',
    `created` bigint(20) NOT NULL COMMENT '打卡时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_checkin` (`space_id`,`username`,`kind`,`checkin_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日小仪式打卡表：互道早晚安解锁当日专属背景/贴纸，晚安连续天数=streak';


-- smart_collections.couple_answer definition

CREATE TABLE `couple_answer` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `answer_day` varchar(10) NOT NULL COMMENT '问题日期（yyyy-MM-dd，每天一问）',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '回答人用户名（区分大小写）',
    `answer` varchar(300) NOT NULL COMMENT '回答内容',
    `created` bigint(20) NOT NULL COMMENT '回答时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_answer` (`space_id`,`answer_day`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='今日一问回答表：每天自动推一个情侣问题，双方回答后拼在一起看';


-- smart_collections.couple_item definition

CREATE TABLE `couple_item` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `kind` varchar(16) NOT NULL COMMENT '清单类型：MOVIE 想看的电影 / FOOD 想吃的餐厅 / TRIP 想去的旅行 / TODO 共同待办',
    `title` varchar(100) NOT NULL COMMENT '事项标题',
    `note` varchar(300) DEFAULT NULL COMMENT '补充说明',
    `due_date` varchar(10) DEFAULT NULL COMMENT '计划日期（yyyy-MM-dd，可空，显示在共同日历上）',
    `done` tinyint(4) DEFAULT 0 COMMENT '是否完成：1 完成 / 0 未完成',
    `done_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '完成操作人用户名',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间（毫秒时间戳）',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '创建人用户名',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_item_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='共享清单表：一起的行程/想看的电影/想去的餐厅，双方可增删改与打卡';


-- smart_collections.couple_anniversary definition

CREATE TABLE `couple_anniversary` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `title` varchar(60) NOT NULL COMMENT '纪念日名称，如「领证纪念日」/「TA 的生日」',
    `event_date` varchar(10) NOT NULL COMMENT '纪念日日期（yyyy-MM-dd）',
    `yearly` tinyint(4) DEFAULT 1 COMMENT '是否每年重复：1 每年 / 0 仅当年',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '创建人用户名',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_anniv_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='共同日历纪念日表：纪念日/生日/约会日，双方都能看到和编辑';


-- smart_collections.couple_mood definition

CREATE TABLE `couple_mood` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '记录人用户名（区分大小写）',
    `mood_day` varchar(10) NOT NULL COMMENT '心情日期（yyyy-MM-dd，按自然日去重）',
    `mood` varchar(16) NOT NULL COMMENT '心情键：LOVE 恋爱中 / HAPPY 开心 / CALM 平静 / BUSY 好忙 / TIRED 累了 / SICK 生病 / SAD 难过 / ANGRY 生气',
    `note` varchar(200) DEFAULT NULL COMMENT '一句话心情',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近修改时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_mood` (`space_id`,`username`,`mood_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣心情日记表：每天每人记录一条心情，双方互相可见，用于绘制双人心情曲线';


-- smart_collections.couple_letter definition

CREATE TABLE `couple_letter` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `sender` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '发件人用户名（区分大小写）',
    `recipient` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '收件人用户名（区分大小写）',
    `content` varchar(300) NOT NULL COMMENT '悄悄话内容（1-300 字的小纸条）',
    `deliver_at` bigint(20) DEFAULT NULL COMMENT '可拆封时间（毫秒时间戳，空=立即可拆；慢递最长 7 天）',
    `status` varchar(16) NOT NULL DEFAULT 'SEALED' COMMENT '信件状态：SEALED 未拆封 / OPENED 已拆封',
    `opened_at` bigint(20) DEFAULT NULL COMMENT '拆封时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '发信时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_letter_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣悄悄话信箱表：写给 TA 的小纸条，支持慢递（到点才能拆），拆封后双方可见';


-- smart_collections.couple_pact definition

CREATE TABLE `couple_pact` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `content` varchar(100) NOT NULL COMMENT '条约内容，如「吵架不过夜」「每周一次约会日」',
    `proposed_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '提出人用户名（区分大小写）',
    `accepted_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '盖章人用户名（空=待对方盖章）',
    `accepted_at` bigint(20) DEFAULT NULL COMMENT '盖章时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '提出时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_pact_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣恋爱条约表：双方共同签署的甜蜜公约，一方提出、另一方盖章后生效';


-- smart_collections.couple_fund definition

CREATE TABLE `couple_fund` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `title` varchar(60) NOT NULL COMMENT '心愿名称，如「一起去北海道旅行」「换一台大电视」',
    `target_amount` bigint(20) NOT NULL COMMENT '目标金额（分，1 元 = 100 分）',
    `saved_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '已存金额（分）',
    `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '基金状态：ACTIVE 攒钱中 / REACHED 已达成',
    `done_at` bigint(20) DEFAULT NULL COMMENT '达成时间（毫秒时间戳）',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '创建人用户名',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_fund_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣心愿基金表：共同存钱目标，双方都能往里存钱，攒够自动庆祝';


-- smart_collections.couple_fund_deposit definition

CREATE TABLE `couple_fund_deposit` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `fund_id` varchar(36) NOT NULL COMMENT '所属心愿基金ID，关联 couple_fund.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '存钱人用户名（区分大小写）',
    `amount` bigint(20) NOT NULL COMMENT '存入金额（分）',
    `note` varchar(100) DEFAULT NULL COMMENT '存钱留言，如「这个月省下的奶茶钱」',
    `created` bigint(20) NOT NULL COMMENT '存入时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_fund_dep_fund` (`fund_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣心愿基金存入记录表：谁在什么时候存了多少，攒钱流水双方可见';


-- smart_collections.user_profile definition

CREATE TABLE `user_profile` (
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '用户名，主键，关联 app_user.username（区分大小写）',
    `nickname` varchar(32) DEFAULT NULL COMMENT '昵称',
    `signature` varchar(100) DEFAULT NULL COMMENT '个性签名',
    `avatar` varchar(16) DEFAULT NULL COMMENT '头像标识（内置头像色档编号，如 c0）',
    `presence_status` varchar(16) DEFAULT 'online' COMMENT '在线状态：online 在线 / busy 忙碌 / away 离开',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '资料更新时间（毫秒时间戳）',
    PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户资料表：与 app_user 一一对应（注册时同步创建），存昵称/签名/头像/在线状态';
