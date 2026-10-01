-- are-chat 全量结构文档（MySQL / H2 MODE=MySQL 双兼容写法，所有表与字段均带 COMMENT 注释）
-- 表结构统一由 Flyway 自动执行 db/ 下 V 脚本创建与管理：V1__add_couple_space.sql（couple 系列表）
-- + V2__legacy_tables_baseline.sql（存量业务表基线）+ V3__add_couple_mood.sql（心情日记表）
-- + V4__add_couple_letter.sql（悄悄话信箱表）+ V5__add_couple_pact_city_fund.sql（恋爱条约/城市列/心愿基金）
-- + V6__add_couple_action_mood_reaction_nick.sql（贴贴动作/心情回应/专属爱称）
-- + V7__add_couple_task_tacit.sql（甜蜜任务卡/默契大考验）
-- + V8__add_couple_care_tables.sql（和好卡/夸夸墙/生理期记录）
-- + V9__add_couple_capsule_countdown.sql（时光胶囊/倒数日期待清单）
-- + V10__add_couple_life_tables.sql（记账本/家务轮值/约会规划/双人习惯/暗号本）
-- + V11__add_couple_space_personalization.sql（空间个性化：宣言/主题/贴纸墙）
-- + V12__add_private_message_heart.sql（私聊消息心动时刻标记列）
-- + V13__add_couple_notify_birthday.sql（情侣通知中心表/用户生日列）
-- + V14__add_couple_first_answer_reaction.sql（第一次清单表/一问互评表），
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
    `heart_at` bigint(20) DEFAULT NULL COMMENT '心动时刻标记时间（毫秒时间戳，null = 未标记）',
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
    `nick_a` varchar(30) DEFAULT NULL COMMENT 'user_a 的专属爱称（由对方设置，如「宝宝」「猪猪」）',
    `nick_b` varchar(30) DEFAULT NULL COMMENT 'user_b 的专属爱称（由对方设置）',
    `slogan` varchar(60) DEFAULT NULL COMMENT '我们的宣言：只有彼此懂的一句话（60 字内）',
    `theme` varchar(20) NOT NULL DEFAULT 'classic' COMMENT '空间主题：classic 经典粉 / cherry 樱花 / ocean 海盐 / forest 森绿 / night 星夜',
    `stickers` varchar(200) DEFAULT NULL COMMENT '贴纸墙佩戴的贴纸 key（逗号分隔，最多 6 枚）',
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
    `kind` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '日子类型：NORMAL 普通 / LOVE 恋爱 / FAMILY 家人 / FRIEND 朋友 / WORK 工作',
    `calendar_type` varchar(10) NOT NULL DEFAULT 'SOLAR' COMMENT '历法：SOLAR 公历 / LUNAR 农历',
    `lunar_md` varchar(5) NOT NULL DEFAULT '' COMMENT '农历月日 MMDD（LUNAR 时有效，闰月按正月计）',
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


-- smart_collections.couple_action definition

CREATE TABLE `couple_action` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发送方用户名（区分大小写）',
    `kind` varchar(16) NOT NULL COMMENT '动作类型：POKE 戳一戳 / HUG 抱抱 / KISS 亲亲 / PAT 捏捏脸 / NUZZLE 蹭蹭 / TICKLE 挠痒痒 / MISS 在想你',
    `created` bigint(20) NOT NULL COMMENT '发送时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_action_space` (`space_id`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣贴贴动作流水表：一键发送亲密小动作，对方实时收到推送，累计次数点亮贴贴里程碑';


-- smart_collections.couple_mood_reaction definition

CREATE TABLE `couple_mood_reaction` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `mood_day` varchar(10) NOT NULL COMMENT '被回应的心情日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '回应人用户名（区分大小写）',
    `reaction` varchar(16) NOT NULL COMMENT '回应类型：HUG 抱抱 / KISS 亲亲 / CHEER 加油 / PAT 摸摸头',
    `created` bigint(20) NOT NULL COMMENT '回应时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近修改时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_mood_reaction` (`space_id`,`mood_day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣心情回应表：看到 TA 今天心情不好，贴一个抱抱/亲亲/加油，TA 会实时收到推送';


-- smart_collections.couple_task definition

CREATE TABLE `couple_task` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `task_day` varchar(10) NOT NULL COMMENT '任务日期（yyyy-MM-dd，每人每天一题）',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '任务归属人用户名（区分大小写）',
    `content` varchar(100) NOT NULL COMMENT '任务内容，如「今天夸对方 3 次」',
    `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING 待完成 / DONE 已完成',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '生成时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_task` (`space_id`,`task_day`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣甜蜜任务卡表：每天一个小任务给彼此撒糖，完成打卡积累心动值';


-- smart_collections.couple_tacit definition

CREATE TABLE `couple_tacit` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `question` varchar(100) NOT NULL COMMENT '题目（从默契题库随机）',
    `answer_a` varchar(60) DEFAULT NULL COMMENT '用户 A 的答案（null = 还没答）',
    `answer_b` varchar(60) DEFAULT NULL COMMENT '用户 B 的答案（null = 还没答）',
    `match` tinyint(4) DEFAULT NULL COMMENT '是否默契一致：1 一致 / 0 不一致 / null 待结算',
    `created` bigint(20) NOT NULL COMMENT '发起时间（毫秒时间戳）',
    `settled_at` bigint(20) DEFAULT NULL COMMENT '结算时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_tacit_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣默契大考验表：背对背回答同一道题，答案一致即为心有灵犀';


-- smart_collections.couple_reconcile definition

CREATE TABLE `couple_reconcile` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '递卡人用户名（区分大小写）',
    `message` varchar(200) NOT NULL COMMENT '和好留言，如「是我不好，抱一下就和好」',
    `start_at` bigint(20) DEFAULT NULL COMMENT '这次别扭开始时间（毫秒时间戳，可空，用于计算和好耗时）',
    `status` varchar(16) NOT NULL DEFAULT 'SENT' COMMENT '状态：SENT 已递出待接受 / ACCEPTED 已接受',
    `accepted_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '接受人用户名',
    `accepted_at` bigint(20) DEFAULT NULL COMMENT '接受时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '递卡时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_reconcile_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣和好卡表：小别扭的温柔收尾，接受后记录和好时刻与耗时';


-- smart_collections.couple_praise definition

CREATE TABLE `couple_praise` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '夸人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '夸夸内容，要具体哦',
    `status` varchar(16) NOT NULL DEFAULT 'POSTED' COMMENT '状态：POSTED 已张贴 / RECEIVED 已收到',
    `received_at` bigint(20) DEFAULT NULL COMMENT '收到时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '张贴时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_praise_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣夸夸墙表：把欣赏说出口，具体地夸，认真地收';


-- smart_collections.couple_cycle definition

CREATE TABLE `couple_cycle` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写，记自己的）',
    `period_day` varchar(10) NOT NULL COMMENT '最近一次生理期开始日期（yyyy-MM-dd）',
    `cycle_days` int(11) NOT NULL DEFAULT 28 COMMENT '周期长度（天，默认 28）',
    `period_days` int(11) NOT NULL DEFAULT 5 COMMENT '经期持续天数（默认 5）',
    `note` varchar(100) DEFAULT NULL COMMENT '小备注，如「这几天想喝热的」',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近修改时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_cycle` (`space_id`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣生理期记录表：自动预告下次日期，对方端展示温柔模式提醒';


-- smart_collections.couple_capsule definition

CREATE TABLE `couple_capsule` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `sender` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发件人用户名（区分大小写）',
    `recipient` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收件人用户名（区分大小写）',
    `content` varchar(500) NOT NULL COMMENT '胶囊内容（1-500 字）',
    `open_day` varchar(10) NOT NULL COMMENT '可开启日期（yyyy-MM-dd，30-365 天后）',
    `status` varchar(16) NOT NULL DEFAULT 'SEALED' COMMENT '状态：SEALED 封存中 / OPENED 已开启',
    `opened_at` bigint(20) DEFAULT NULL COMMENT '开启时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '封存时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_capsule_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣时光胶囊表：把现在的约定封进胶囊，到点才能开启，给未来的彼此一个惊喜';


-- smart_collections.couple_countdown definition

CREATE TABLE `couple_countdown` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `title` varchar(60) NOT NULL COMMENT '期待的事情，如「去看海」「TA 的生日惊喜」',
    `target_day` varchar(10) NOT NULL COMMENT '目标日期（yyyy-MM-dd）',
    `note` varchar(200) DEFAULT NULL COMMENT '小备注',
    `done` tinyint(4) DEFAULT 0 COMMENT '是否已实现：1 实现（归档）/ 0 期待中',
    `done_at` bigint(20) DEFAULT NULL COMMENT '实现时间（毫秒时间戳）',
    `last_remind_day` varchar(10) DEFAULT NULL COMMENT '最近一次倒数提醒日期（防重复打扰）',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '创建人用户名',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_countdown_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣倒数日表：把期待的事写下来倒数，临近自动提醒双方';


-- smart_collections.couple_expense definition

CREATE TABLE `couple_expense` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '付款人用户名（区分大小写）',
    `amount` bigint(20) NOT NULL COMMENT '金额（分）',
    `category` varchar(16) NOT NULL DEFAULT 'OTHER' COMMENT '分类：FOOD 餐饮 / TRANSPORT 交通 / FUN 娱乐 / HOME 日用 / GIFT 礼物 / OTHER 其他',
    `note` varchar(100) DEFAULT NULL COMMENT '花在什么上',
    `spent_day` varchar(10) NOT NULL COMMENT '花销日期（yyyy-MM-dd）',
    `created` bigint(20) NOT NULL COMMENT '记录时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_expense_space` (`space_id`,`spent_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣甜蜜记账本表：一起花的钱记清楚，月底看看谁付得多，AA 差额一目了然';


-- smart_collections.couple_chore definition

CREATE TABLE `couple_chore` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `title` varchar(60) NOT NULL COMMENT '家务名，如「洗碗」「倒垃圾」',
    `rotate` varchar(16) NOT NULL DEFAULT 'ALTERNATE' COMMENT '轮值方式：SINGLE 固定一人 / ALTERNATE 每次轮换',
    `turn` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '当前值日生用户名（区分大小写）',
    `done_count` int(11) NOT NULL DEFAULT 0 COMMENT '累计完成次数',
    `last_done_day` varchar(10) DEFAULT NULL COMMENT '最近一次完成日期（yyyy-MM-dd）',
    `last_done_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '最近一次完成人',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_chore_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣家务轮值表：家务分工不再靠嘴说，完成打卡自动轮换值日生';


-- smart_collections.couple_date_plan definition

CREATE TABLE `couple_date_plan` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `title` varchar(60) NOT NULL COMMENT '约会主题，如「周五去看展」',
    `plan_day` varchar(10) NOT NULL COMMENT '约会日期（yyyy-MM-dd）',
    `place` varchar(100) DEFAULT NULL COMMENT '地点',
    `items` varchar(500) DEFAULT NULL COMMENT '想做的事，换行分隔（最多 500 字）',
    `status` varchar(16) NOT NULL DEFAULT 'PLANNED' COMMENT '状态：PLANNED 计划中 / DONE 已完成',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间（毫秒时间戳）',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '创建人用户名',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_date_plan_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣约会规划表：把「下次一起」变成有日期的约定，完成后自动归档进时光轴';


-- smart_collections.couple_habit definition

CREATE TABLE `couple_habit` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `title` varchar(60) NOT NULL COMMENT '习惯名，如「23:30 前睡」「每天喝够 8 杯水」',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '创建人用户名',
    `active` tinyint(4) DEFAULT 1 COMMENT '是否进行中：1 进行中 / 0 已结束',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_habit_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣双人习惯表：一起坚持一件小事，断签互相提醒';


-- smart_collections.couple_habit_log definition

CREATE TABLE `couple_habit_log` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `habit_id` varchar(36) NOT NULL COMMENT '所属习惯ID，关联 couple_habit.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '打卡人用户名（区分大小写）',
    `log_day` varchar(10) NOT NULL COMMENT '打卡日期（yyyy-MM-dd）',
    `created` bigint(20) NOT NULL COMMENT '打卡时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_habit_log` (`habit_id`,`username`,`log_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣双人习惯打卡日志表：双方都打卡才算这一天共同坚持';


-- smart_collections.couple_cipher definition

CREATE TABLE `couple_cipher` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `keyword` varchar(40) NOT NULL COMMENT '暗号词，如「菠萝」',
    `meaning` varchar(200) NOT NULL COMMENT '它的意思，如「想你了，快来找我」',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '创建人用户名',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_cipher_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣暗号本表：把只有彼此懂的梗和暗号记下来，随时对上频率';


-- smart_collections.user_profile definition

CREATE TABLE `user_profile` (
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '用户名，主键，关联 app_user.username（区分大小写）',
    `nickname` varchar(32) DEFAULT NULL COMMENT '昵称',
    `signature` varchar(100) DEFAULT NULL COMMENT '个性签名',
    `avatar` varchar(16) DEFAULT NULL COMMENT '头像标识（内置头像色档编号，如 c0）',
    `presence_status` varchar(16) DEFAULT 'online' COMMENT '在线状态：online 在线 / busy 忙碌 / away 离开',
    `birthday` varchar(10) DEFAULT NULL COMMENT '生日（yyyy-MM-dd，允许只填 MM-dd 表达不在意年份）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '资料更新时间（毫秒时间戳）',
    PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户资料表：与 app_user 一一对应（注册时同步创建），存昵称/签名/头像/在线状态';


-- smart_collections.couple_notify definition

CREATE TABLE `couple_notify` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收件人用户名（区分大小写）',
    `event` varchar(40) NOT NULL COMMENT '事件名（如 letter-created / countdown-reminder）',
    `actor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '触发人用户名（system = 定时任务）',
    `detail` varchar(300) NOT NULL COMMENT '通知文案',
    `read_flag` tinyint(4) DEFAULT 0 COMMENT '是否已读：1 已读 / 0 未读',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_notify_user` (`username`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣空间通知中心表：每次事件推送都给收件人存档一条，登录后可补看与标记已读';


-- smart_collections.couple_first definition

CREATE TABLE `couple_first` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(100) NOT NULL COMMENT '第一次做的事（如 第一次一起看海）',
    `first_day` varchar(10) NOT NULL COMMENT '发生的日期（yyyy-MM-dd）',
    `note` varchar(300) DEFAULT NULL COMMENT '当时的心情/补充（可空）',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_first_space` (`space_id`,`first_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='第一次清单表：恋爱里每个第一次的日子与心情，时光轴的重要素材';


-- smart_collections.couple_answer_reaction definition

CREATE TABLE `couple_answer_reaction` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `answer_day` varchar(10) NOT NULL COMMENT '一问日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '评价人用户名（区分大小写）',
    `emoji` varchar(16) NOT NULL COMMENT '反应 emoji（如 ❤️😂）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_answer_reaction` (`space_id`,`answer_day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='今日一问互评表：对 TA 回答的表情反应，每人每天一条（upsert）';


-- smart_collections.couple_scratch definition

CREATE TABLE `couple_scratch` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week_key` varchar(10) NOT NULL COMMENT '周标识（如 2026-W40），每周一张',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '送券人用户名（区分大小写）',
    `owner` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收券人用户名（刮券的人）',
    `prize_kind` varchar(20) NOT NULL COMMENT '券类型（hug/breakfast/movie 等）',
    `prize_text` varchar(100) NOT NULL COMMENT '券面内容（如 一个按规定动作的拥抱）',
    `scratched` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已刮开：0 未刮 1 已刮',
    `scratched_at` bigint(20) DEFAULT NULL COMMENT '刮开时间（毫秒）',
    `redeemed_at` bigint(20) DEFAULT NULL COMMENT '核销时间（空 = 券还没用）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scratch_week` (`space_id`,`week_key`,`owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='爱情刮刮乐表：每周一张来自 TA 的奖励券，刮开核销';


-- smart_collections.couple_mystery_box definition

CREATE TABLE `couple_mystery_box` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '装盒人用户名（区分大小写）',
    `kind` varchar(10) NOT NULL COMMENT '盒子类型：whisper 悄悄话 / task 小任务',
    `content` varchar(300) NOT NULL COMMENT '盒子里的话或任务',
    `open_day` varchar(10) NOT NULL COMMENT '可拆日期（yyyy-MM-dd，最早明天）',
    `opened` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已拆开：0 未拆 1 已拆',
    `opened_at` bigint(20) DEFAULT NULL COMMENT '拆开时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_mystery_box_space` (`space_id`,`open_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='恋爱盲盒表：延迟拆开的悄悄话/小任务，制造一天的小期待';


-- smart_collections.couple_sweet_alarm definition

CREATE TABLE `couple_sweet_alarm` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '设钟人用户名（区分大小写）',
    `message` varchar(200) NOT NULL COMMENT '到点送达的那句话',
    `fire_at` bigint(20) NOT NULL COMMENT '触发时间（毫秒，限未来 24 小时内）',
    `fired` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已送达：0 未触发 1 已送达',
    `fired_at` bigint(20) DEFAULT NULL COMMENT '实际送达时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_sweet_alarm_fire` (`space_id`,`fired`,`fire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心动闹钟表：定时送达的一句话，让 TA 在你指定的时间被撩到';


-- smart_collections.couple_miss_express definition

CREATE TABLE `couple_miss_express` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '想 TA 的人用户名（区分大小写）',
    `deliver_at` bigint(20) NOT NULL COMMENT '计划送达时间（毫秒，下单时间 + 5~30 分钟随机）',
    `delivered` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已送达：0 在途 1 已送达',
    `delivered_at` bigint(20) DEFAULT NULL COMMENT '实际送达时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_miss_express_deliver` (`space_id`,`delivered`,`deliver_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='思念速递表：延迟几分钟送达的想念，制造「被惦记」的惊喜时刻';


-- smart_collections.couple_garden definition

CREATE TABLE `couple_garden` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（唯一，一个空间一座花园）',
    `stage` int(11) NOT NULL DEFAULT 0 COMMENT '成长阶段 0-6（种子→发芽→幼苗→枝干→含苞→盛开→繁茂）',
    `total_water` int(11) NOT NULL DEFAULT 0 COMMENT '累计浇水次数',
    `last_water_day_a` varchar(10) DEFAULT NULL COMMENT '用户 A 最近浇水日（yyyy-MM-dd，每天限一次）',
    `last_water_day_b` varchar(10) DEFAULT NULL COMMENT '用户 B 最近浇水日',
    `withered` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已蔫：连续 3 天没人浇水置 1',
    `revived_count` int(11) NOT NULL DEFAULT 0 COMMENT '被救活次数',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近变动时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_garden_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='爱情花园表：双方共同养成的小树，浇水长大、缺水会蔫';


-- smart_collections.couple_rose definition

CREATE TABLE `couple_rose` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '送花人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '送花日期（yyyy-MM-dd）',
    `flower_key` varchar(20) NOT NULL COMMENT '花的种类 key（rose/tulip/sunflower 等）',
    `word` varchar(60) NOT NULL COMMENT '随花附上的花语',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_rose_space_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日玫瑰表：每天限量 3 朵的鲜花与花语，日常的小浪漫';


-- smart_collections.couple_fortune_slip definition

CREATE TABLE `couple_fortune_slip` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '抽签人用户名（区分大小写，签是抽给对方的）',
    `day` varchar(10) NOT NULL COMMENT '抽签日期（yyyy-MM-dd，每人每天一支）',
    `slip_key` varchar(20) NOT NULL COMMENT '签文 key（对应内容库）',
    `content` varchar(120) NOT NULL COMMENT '签文内容',
    `level` varchar(10) NOT NULL COMMENT '签的等级（大吉/中吉/小吉/锦鲤）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fortune_slip` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='幸运签表：每天替 TA 抽一支签，把好运送过去';


-- smart_collections.couple_confession definition

CREATE TABLE `couple_confession` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `content` varchar(500) NOT NULL COMMENT '当年的告白词',
    `confess_day` varchar(10) NOT NULL COMMENT '告白发生的日期（yyyy-MM-dd，每年这天重现）',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '录入人用户名（区分大小写）',
    `replay_years` varchar(60) DEFAULT NULL COMMENT '已重现过的年份（逗号分隔，防重复推送）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_confession_space` (`space_id`,`confess_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告白重现表：存下当年的告白，每年的今天自动重播';


-- smart_collections.couple_treasure definition

CREATE TABLE `couple_treasure` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '埋宝人用户名（区分大小写）',
    `task_text` varchar(100) NOT NULL COMMENT '给 TA 的任务（如 去阳台看看）',
    `prize_text` varchar(100) NOT NULL COMMENT '宝藏内容（完成后才揭晓，如 一个大拥抱+今晚电影你选）',
    `status` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 待完成 / DONE 已揭晓',
    `done_at` bigint(20) DEFAULT NULL COMMENT '揭晓时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_treasure_space` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='藏宝图任务表：现实小任务 + 完成后揭晓的宝藏，把惊喜藏进生活';


-- smart_collections.couple_comfort definition

CREATE TABLE `couple_comfort` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '求抱抱的人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '日期（yyyy-MM-dd，每人每天一条，重复=修改）',
    `feeling` varchar(20) NOT NULL COMMENT '此刻的感受（SAD 难过/WRONGED 委屈/TIRED 累/ANXIOUS 焦虑/EMO emo）',
    `handled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '对方是否已回应：0 等待 1 已回应',
    `handled_note` varchar(100) DEFAULT NULL COMMENT 'TA 的回应（从话术卡选择或手写）',
    `handled_at` bigint(20) DEFAULT NULL COMMENT '回应时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comfort_day` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='求抱抱表：一键求助的温柔按钮，让「需要安慰」被大声说出来';


-- smart_collections.couple_peace_review definition

CREATE TABLE `couple_peace_review` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '复盘日期（yyyy-MM-dd，每人每天一份）',
    `by_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '复盘人用户名（区分大小写）',
    `my_part` varchar(200) NOT NULL COMMENT '我当时为什么在意/我的那部分',
    `next_time` varchar(200) NOT NULL COMMENT '下次我们可以怎么做',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_peace_review` (`space_id`,`day`,`by_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='矛盾复盘表：和好之后的双人复盘，把争吵变成了解';


-- smart_collections.couple_sorry_ticket definition

CREATE TABLE `couple_sorry_ticket` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '道歉方用户名（区分大小写）',
    `note` varchar(100) NOT NULL COMMENT '道歉附言（比如 刚才语气不好，对不起）',
    `status` varchar(10) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE 有效 / USED 已收下',
    `used_note` varchar(100) DEFAULT NULL COMMENT '收下时想说的话（可空）',
    `used_at` bigint(20) DEFAULT NULL COMMENT '收下时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_sorry_ticket` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='道歉券表：把「对不起」做成一张可以递出去的券';


-- smart_collections.couple_truth definition

CREATE TABLE `couple_truth` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '日期（yyyy-MM-dd，每人每天可发起一次）',
    `question` varchar(100) NOT NULL COMMENT '真心话题目',
    `answerer` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '回答人用户名（区分大小写）',
    `answer` varchar(200) NOT NULL COMMENT '回答内容',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_truth_day` (`space_id`,`day`,`answerer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='真心话表：每天一题双方必答，把平时不敢问的问出来';


-- smart_collections.couple_whisper definition

CREATE TABLE `couple_whisper` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提问人用户名（区分大小写，匿名时对 TA 隐藏）',
    `question` varchar(200) NOT NULL COMMENT '想问的问题',
    `anonymous` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否匿名：1 匿名（回答后揭晓）0 实名',
    `answer` varchar(300) DEFAULT NULL COMMENT 'TA 的回答（空 = 未回答）',
    `answered_at` bigint(20) DEFAULT NULL COMMENT '回答时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_whisper_space` (`space_id`,`answered_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='匿名树洞表：给不敢开口的问题一个安全入口';


-- smart_collections.couple_telepathy definition

CREATE TABLE `couple_telepathy` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '日期（yyyy-MM-dd）',
    `round` int(11) NOT NULL COMMENT '当天第几轮（1-3）',
    `question` varchar(100) NOT NULL COMMENT '感应题目（双方同题）',
    `answer_a` varchar(50) DEFAULT NULL COMMENT '用户 A 的回答（空 = 未答）',
    `answer_b` varchar(50) DEFAULT NULL COMMENT '用户 B 的回答（空 = 未答）',
    `created` bigint(20) NOT NULL COMMENT '发起时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_telepathy_round` (`space_id`,`day`,`round`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心灵感应表：不商量的同题作答，测一测我们有多同频';


-- smart_collections.couple_love_bank definition

CREATE TABLE `couple_love_bank` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存入人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '情话内容',
    `delivered` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已作为利息送达：0 在罐里 1 已送达',
    `delivered_at` bigint(20) DEFAULT NULL COMMENT '送达时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_love_bank_deliver` (`space_id`,`delivered`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情话储蓄罐表：今天存下的情话，会在某个晚上变成惊喜利息';


-- smart_collections.couple_challenge definition

CREATE TABLE `couple_challenge` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '挑战日期（yyyy-MM-dd，每天一题）',
    `task_text` varchar(100) NOT NULL COMMENT '今日挑战题目（按空间+天稳定抽取）',
    `done_a` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 A 是否完成：0 未完成 1 已完成',
    `done_b` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 B 是否完成：0 未完成 1 已完成',
    `done_at_a` bigint(20) DEFAULT NULL COMMENT '用户 A 完成时间（毫秒）',
    `done_at_b` bigint(20) DEFAULT NULL COMMENT '用户 B 完成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_challenge_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='双人挑战赛表：每天同一道小挑战，一起完成才算赢';


-- smart_collections.couple_passbook definition

CREATE TABLE `couple_passbook` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存款人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '存款日期（yyyy-MM-dd，每人每天一笔，重复=修改）',
    `content` varchar(200) NOT NULL COMMENT '今天为这段感情做的一件小事',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_passbook_day` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='恋爱存折表：感情靠每天存一点，攒的是连续和心意';


-- smart_collections.couple_hundred definition

CREATE TABLE `couple_hundred` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `goal` varchar(200) NOT NULL COMMENT '百日目标（比如 一起早睡 100 天）',
    `start_day` varchar(10) NOT NULL COMMENT '开始日期（yyyy-MM-dd）',
    `status` varchar(10) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE 进行中 / DONE 达成 / BROKEN 中止',
    `done_at` bigint(20) DEFAULT NULL COMMENT '达成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_hundred_space` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='百日之约表：把「想坚持的事」变成 100 天的双人打卡';


-- smart_collections.couple_hundred_checkin definition

CREATE TABLE `couple_hundred_checkin` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `pact_id` varchar(36) NOT NULL COMMENT '百日之约ID（关联 couple_hundred.id）',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `by_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '打卡人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '打卡日期（yyyy-MM-dd，每人每天一条，重复=补卡）',
    `note` varchar(100) DEFAULT NULL COMMENT '今日打卡心得（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hundred_checkin` (`pact_id`,`day`,`by_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='百日之约打卡明细表：每天一签，断签不断约';


-- smart_collections.couple_wish_exchange definition

CREATE TABLE `couple_wish_exchange` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '许愿人用户名（区分大小写）',
    `wish` varchar(200) NOT NULL COMMENT '心愿内容',
    `status` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 待接单 / ACCEPTED 已接单 / DONE 已实现',
    `accepted_at` bigint(20) DEFAULT NULL COMMENT '接单时间（毫秒）',
    `done_note` varchar(100) DEFAULT NULL COMMENT '实现时想说的话（可空）',
    `done_at` bigint(20) DEFAULT NULL COMMENT '实现时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_wish_exchange` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心愿互换表：我的小心愿交给 TA，TA 的心愿我来实现';


-- smart_collections.couple_read_plan definition

CREATE TABLE `couple_read_plan` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(100) NOT NULL COMMENT '书名/剧名',
    `total_units` int(11) NOT NULL COMMENT '总章节数/总集数',
    `unit_label` varchar(10) NOT NULL DEFAULT '章' COMMENT '进度单位（章/集/课）',
    `status` varchar(10) NOT NULL DEFAULT 'READING' COMMENT '状态：READING 共读中 / FINISHED 已读完',
    `finished_at` bigint(20) DEFAULT NULL COMMENT '读完时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_read_plan` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='共读计划表：同一本书，各自翻页，一起到达结局';


-- smart_collections.couple_read_progress definition

CREATE TABLE `couple_read_progress` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `plan_id` varchar(36) NOT NULL COMMENT '共读计划ID（关联 couple_read_plan.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '上报人用户名（区分大小写）',
    `unit` int(11) NOT NULL COMMENT '当前进度（读到第几章/集）',
    `note` varchar(100) DEFAULT NULL COMMENT '一句话感想（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_read_progress` (`plan_id`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='共读进度明细表：进度条各自爬，感想随进度附一句';


-- smart_collections.couple_travel_wish definition

CREATE TABLE `couple_travel_wish` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '许愿人用户名（区分大小写）',
    `place` varchar(100) NOT NULL COMMENT '目的地（城市/店名/地标）',
    `want_todo` varchar(200) DEFAULT NULL COMMENT '到了想做的事（可空）',
    `visited` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已打卡：0 心愿中 1 去过啦',
    `visited_at` bigint(20) DEFAULT NULL COMMENT '打卡时间（毫秒）',
    `visited_note` varchar(100) DEFAULT NULL COMMENT '打卡感想（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_travel_wish` (`space_id`,`visited`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='旅行心愿地图表：把「想去」钉在地图上，一个个走成「去过」';


-- smart_collections.couple_watchlist definition

CREATE TABLE `couple_watchlist` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(100) NOT NULL COMMENT '剧名/片名',
    `current_unit` int(11) NOT NULL DEFAULT 0 COMMENT '共同看到第几集',
    `total_unit` int(11) DEFAULT NULL COMMENT '总集数（未知道路剧可空）',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '最后更新人用户名',
    `status` varchar(10) NOT NULL DEFAULT 'WATCHING' COMMENT '状态：WATCHING 追剧中 / DONE 已完结',
    `finished_at` bigint(20) DEFAULT NULL COMMENT '看完时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_watchlist` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='追剧清单表：遥控器是两个人的，进度也是';


-- smart_collections.couple_dict_word definition

CREATE TABLE `couple_dict_word` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收录人用户名（区分大小写）',
    `word` varchar(50) NOT NULL COMMENT '专属词汇',
    `meaning` varchar(200) NOT NULL COMMENT '释义（只有我们懂的那层意思）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_dict_word` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='恋爱词典表：我们才懂的语言，值得一本词典';


-- smart_collections.couple_next_time definition

CREATE TABLE `couple_next_time` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '承诺人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '「下次一定」的内容',
    `status` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 待兑现 / DONE 已兑现',
    `done_at` bigint(20) DEFAULT NULL COMMENT '兑现时间（毫秒）',
    `nudged_at` bigint(20) DEFAULT NULL COMMENT '最近一次被催时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_next_time` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='下次一定清单表：随口的承诺不散场，落单可催办';


-- smart_collections.couple_quote definition

CREATE TABLE `couple_quote` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收藏人用户名（区分大小写）',
    `content` varchar(300) NOT NULL COMMENT '语录内容（TA 说的话或你们的对话）',
    `context` varchar(100) DEFAULT NULL COMMENT '当时的场景（可空，比如 某个加班的深夜）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_quote_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='甜蜜语录收藏册表：甜话会过期，收藏不会';


-- smart_collections.couple_ticket definition

CREATE TABLE `couple_ticket` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `title` varchar(100) NOT NULL COMMENT '片名',
    `watch_day` varchar(10) NOT NULL COMMENT '观看日期（yyyy-MM-dd）',
    `rating` int(11) NOT NULL DEFAULT 5 COMMENT '两人的评分（1-5 星，默认满分）',
    `comment` varchar(200) DEFAULT NULL COMMENT '一句观影感想（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_space` (`space_id`,`watch_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='恋爱电影票根表：散场不散，票根为证';


-- smart_collections.couple_song definition

CREATE TABLE `couple_song` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收藏人用户名（区分大小写）',
    `title` varchar(100) NOT NULL COMMENT '歌名',
    `artist` varchar(50) DEFAULT NULL COMMENT '歌手（可空）',
    `reason` varchar(200) DEFAULT NULL COMMENT '为什么是我们的歌（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_song_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='我们的歌单表：每首歌都藏着一段我们的故事';


-- smart_collections.couple_cool_down definition

CREATE TABLE `couple_cool_down` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发起人用户名（区分大小写）',
    `reason` varchar(100) DEFAULT NULL COMMENT '想冷静一下的原因（可空）',
    `status` varchar(10) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE 冷静中 / HEALED 已和好',
    `end_at` bigint(20) NOT NULL COMMENT '冷静期结束时间（毫秒）',
    `soft_a` varchar(100) DEFAULT NULL COMMENT '用户 A 的软话（可空）',
    `soft_b` varchar(100) DEFAULT NULL COMMENT '用户 B 的软话（可空）',
    `soft_at_a` bigint(20) DEFAULT NULL COMMENT '用户 A 留软话时间（毫秒）',
    `soft_at_b` bigint(20) DEFAULT NULL COMMENT '用户 B 留软话时间（毫秒）',
    `healed_at` bigint(20) DEFAULT NULL COMMENT '和好时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_cool_down` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='冷静角表：吵架不隔夜，先冷静 30 分钟再好好说';


-- smart_collections.couple_mood_relay definition

CREATE TABLE `couple_mood_relay` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '抛棒人用户名（区分大小写）',
    `mood_word` varchar(20) NOT NULL COMMENT '心情词（比如 开心/委屈/累）',
    `mood_emoji` varchar(10) DEFAULT NULL COMMENT '心情 emoji（可空）',
    `note` varchar(100) DEFAULT NULL COMMENT '想多说的一句（可空）',
    `status` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 在路上 / CAUGHT 已被接住',
    `catch_note` varchar(100) DEFAULT NULL COMMENT '接棒人的回应（可空）',
    `caught_at` bigint(20) DEFAULT NULL COMMENT '被接住时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_mood_relay` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情绪接力棒表：我的心情抛给你，你接住了就换你抛回来';


-- smart_collections.couple_guess_round definition

CREATE TABLE `couple_guess_round` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '对局日期（yyyy-MM-dd，每天限 5 轮）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '比划人用户名（区分大小写）',
    `word` varchar(30) NOT NULL COMMENT '要猜的词（只给比划人看）',
    `clue` varchar(100) DEFAULT NULL COMMENT '比划人的提示（不能包含原词）',
    `guess` varchar(30) DEFAULT NULL COMMENT '最近一次猜的词（可空）',
    `attempts` int(11) NOT NULL DEFAULT 0 COMMENT '已猜次数',
    `status` varchar(10) NOT NULL DEFAULT 'DRAWN' COMMENT '状态：DRAWN 已抽词 / CLUED 已出提示 / HIT 猜中 / MISSED 没猜中',
    `settled_at` bigint(20) DEFAULT NULL COMMENT '结算时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_guess_round` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='你比划我猜表：比划不能说，猜中才过瘾';


-- smart_collections.couple_story_line definition

CREATE TABLE `couple_story_line` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `chain_id` varchar(36) NOT NULL COMMENT '故事链ID（= 首句的 id）',
    `seq` int(11) NOT NULL COMMENT '句序（从 1 开始）',
    `by_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写这句的人用户名（区分大小写）',
    `content` varchar(100) NOT NULL COMMENT '接龙的一句话',
    `is_final` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为完结句：0 连载 1 完结',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_story_line` (`space_id`,`chain_id`,`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='故事接龙表：你一句我一句，我们的故事自己写';


-- smart_collections.couple_apology_card definition

CREATE TABLE `couple_apology_card` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '道歉人用户名（区分大小写）',
    `what_wrong` varchar(100) NOT NULL COMMENT '我错了：错在什么事',
    `why_wrong` varchar(100) NOT NULL COMMENT '错在哪：让 TA 难受的点',
    `will_do` varchar(100) NOT NULL COMMENT '以后我会：具体改变',
    `status` varchar(10) NOT NULL DEFAULT 'SENT' COMMENT '状态：SENT 已送出 / ACCEPTED 已收下',
    `accepted_at` bigint(20) DEFAULT NULL COMMENT '收下时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_apology_card` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='道歉三部曲表：好好道歉，是有勇气的浪漫';


-- smart_collections.couple_feeling_word definition

CREATE TABLE `couple_feeling_word` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '记录日期（yyyy-MM-dd，每人每天一词，重复=修改）',
    `word` varchar(20) NOT NULL COMMENT '今天的心情词',
    `note` varchar(100) DEFAULT NULL COMMENT '想说的一句话（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_feeling_day` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情绪词汇足迹表：一天一个词，攒成我们的情绪星图';


-- smart_collections.couple_handhold definition

CREATE TABLE `couple_handhold` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '牵手日期（yyyy-MM-dd，每天一签）',
    `hold_a` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 A 是否点亮：0 否 1 已点亮',
    `hold_b` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 B 是否点亮：0 否 1 已点亮',
    `hold_at_a` bigint(20) DEFAULT NULL COMMENT '用户 A 点亮时间（毫秒）',
    `hold_at_b` bigint(20) DEFAULT NULL COMMENT '用户 B 点亮时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_handhold_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='隔空牵手表：隔着屏幕也牵手，一天一握，握紧不放';


-- smart_collections.couple_miss_daily definition

CREATE TABLE `couple_miss_daily` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '想念日期（yyyy-MM-dd，每天一签）',
    `miss_a` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 A 是否点亮：0 否 1 已点亮',
    `miss_b` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 B 是否点亮：0 否 1 已点亮',
    `miss_at_a` bigint(20) DEFAULT NULL COMMENT '用户 A 点亮时间（毫秒）',
    `miss_at_b` bigint(20) DEFAULT NULL COMMENT '用户 B 点亮时间（毫秒）',
    `both_at` bigint(20) DEFAULT NULL COMMENT '双向奔赴达成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_miss_daily` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='想念计量所表：今天想你了，想被同样想我的人看见';


-- smart_collections.couple_routine definition

CREATE TABLE `couple_routine` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作息主人用户名（区分大小写）',
    `wake_time` varchar(5) NOT NULL COMMENT '起床时间（HH:mm）',
    `work_start` varchar(5) NOT NULL COMMENT '上班/上课开始（HH:mm）',
    `work_end` varchar(5) NOT NULL COMMENT '下班/下课（HH:mm）',
    `sleep_time` varchar(5) NOT NULL COMMENT '睡觉时间（HH:mm）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_routine_owner` (`space_id`,`owner_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='我们的作息表：把两个人的时间摆在一起，重叠的就是我们的时间';


-- smart_collections.couple_reunion_letter definition

CREATE TABLE `couple_reunion_letter` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写信人用户名（区分大小写）',
    `content` varchar(300) NOT NULL COMMENT '信的内容（见面时再拆）',
    `status` varchar(10) NOT NULL DEFAULT 'SEELED' COMMENT '状态：SEELED 已封存 / OPENED 已拆开',
    `opened_at` bigint(20) DEFAULT NULL COMMENT '拆信时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_reunion_letter` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='下次见面信表：把想说的存进信封，见面那天再拆';


-- smart_collections.couple_cloud_date definition

CREATE TABLE `couple_cloud_date` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发起人用户名（区分大小写）',
    `item` varchar(100) NOT NULL COMMENT '云约会内容（静态库可点选/自定义）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 待完成 / DONE 已完成',
    `done_note` varchar(100) DEFAULT NULL COMMENT '完成时想说的一句（可空）',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_cloud_date` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='云约会清单表：距离隔开的是城市，隔不开的是一起做事';


-- smart_collections.couple_safety_ping definition

CREATE TABLE `couple_safety_ping` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '报平安人用户名（区分大小写）',
    `kind` varchar(10) NOT NULL COMMENT '类型：GO_OUT 出发了 / ARRIVE 到家啦',
    `note` varchar(100) DEFAULT NULL COMMENT '附带一句话（可空，比如 车上人多慢慢开）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_safety_ping` (`space_id`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平安卡表：每一次出发与到达，都有人惦记';


-- smart_collections.couple_reunion_log definition

CREATE TABLE `couple_reunion_log` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `by_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `meet_day` varchar(10) NOT NULL COMMENT '见面日期（yyyy-MM-dd，同天只记一条）',
    `note` varchar(200) DEFAULT NULL COMMENT '这次见面做了什么/最难忘的瞬间（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reunion_day` (`space_id`,`meet_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='见面日记表：每一次见面都值得记账，间隔的天数都在攒想念';


-- smart_collections.couple_security_bank definition

CREATE TABLE `couple_security_bank` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存入安心话的用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '安心话内容',
    `status` varchar(10) NOT NULL DEFAULT 'DEPOSITED' COMMENT '状态：DEPOSITED 已存入 / ACCEPTED 已收下',
    `accepted_at` bigint(20) DEFAULT NULL COMMENT '收下时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_security_bank` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='安全感账户表：安心的话存起来，需要勇气的时候来取';


-- smart_collections.couple_decade_pact definition

CREATE TABLE `couple_decade_pact` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '立约人用户名（区分大小写，一人一条）',
    `content` varchar(200) NOT NULL COMMENT '十年后我们……',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_decade_user` (`space_id`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='十年之约表：十年很长，但说好了就是十年';


-- smart_collections.couple_vision_card definition

CREATE TABLE `couple_vision_card` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '许愿人用户名（区分大小写）',
    `word` varchar(20) NOT NULL COMMENT '愿景关键词（一个词，同词即共鸣）',
    `note` varchar(100) DEFAULT NULL COMMENT '想多说的一句（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_vision_word` (`space_id`,`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='愿景板表：把想要的未来贴上墙，重合的部分一起实现';


-- smart_collections.couple_oath definition

CREATE TABLE `couple_oath` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '承诺人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '郑重承诺内容',
    `stamp_a` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 A 是否盖章：0 否 1 已盖',
    `stamp_b` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 B 是否盖章：0 否 1 已盖',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_oath_stamp` (`space_id`,`stamp_a`,`stamp_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='承诺博物馆表：盖了章的话，是要进博物馆的';


-- smart_collections.couple_trust_coin definition

CREATE TABLE `couple_trust_coin` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存入人用户名（区分大小写）',
    `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收币人用户名（区分大小写）',
    `reason` varchar(100) DEFAULT NULL COMMENT '为什么值得信（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_trust_to` (`space_id`,`to_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='信任存折表：一枚一枚攒，攒成无条件的信任';


-- smart_collections.couple_self_contract definition

CREATE TABLE `couple_self_contract` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(50) NOT NULL COMMENT '契约名称（如 每天说晚安）',
    `content` varchar(200) DEFAULT NULL COMMENT '契约内容补充（可空）',
    `count_a` int(11) NOT NULL DEFAULT 0 COMMENT '用户 A 完成次数',
    `count_b` int(11) NOT NULL DEFAULT 0 COMMENT '用户 B 完成次数',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_self_contract` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='双人契约表：说好一起做的事，一次一次攒默契';


-- smart_collections.couple_pet definition

CREATE TABLE `couple_pet` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `name` varchar(20) NOT NULL COMMENT '守护兽名字',
    `kind` varchar(10) NOT NULL DEFAULT 'FOX' COMMENT '守护兽种类：FOX 狐狸 / CAT 猫 / BEAR 熊 / BUNNY 兔',
    `care_count` int(11) NOT NULL DEFAULT 0 COMMENT '累计照料次数',
    `last_care_at` bigint(20) DEFAULT NULL COMMENT '最近照料时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pet_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='守护兽表：你们的关系有了一只小兽看着，别忘喂它';


-- smart_collections.couple_survey_answer definition

CREATE TABLE `couple_survey_answer` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '答题人用户名（区分大小写）',
    `q_no` int(11) NOT NULL COMMENT '题号（1-100）',
    `answer` varchar(200) NOT NULL COMMENT '我的答案',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_survey_user_q` (`space_id`,`from_user`,`q_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='一百问答题表：一百个问题，慢慢把彼此读成一本好书';


-- smart_collections.couple_quiz_duel definition

CREATE TABLE `couple_quiz_duel` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '出题人用户名（区分大小写）',
    `question` varchar(200) NOT NULL COMMENT '题目',
    `answer_text` varchar(200) DEFAULT NULL COMMENT '对方的回答（可空）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 待作答 / ANSWERED 待判分 / JUDGED 已判分',
    `verdict` varchar(10) DEFAULT NULL COMMENT '判分结果：RIGHT 答对 / WRONG 答错（出题人判定）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_quiz_duel` (`space_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出题考TA表：看看 TA 有多懂你，出题人说了算';


-- smart_collections.couple_love_word definition

CREATE TABLE `couple_love_word` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收藏人用户名（区分大小写）',
    `word` varchar(100) NOT NULL COMMENT '情话原文',
    `meaning` varchar(200) DEFAULT NULL COMMENT '含义/翻译（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_love_word` (`space_id`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='世界情话课表：把全世界的情话都学会，只对你说';


-- smart_collections.couple_blind_pick definition

CREATE TABLE `couple_blind_pick` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提交人用户名（区分大小写）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd，每周一期）',
    `picks` varchar(300) NOT NULL COMMENT '三个周末愿望（逗号分隔）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blind_week_user` (`space_id`,`week`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='周末盲选表：把周末交给盲盒，惊喜由两个人一起出';


-- smart_collections.couple_sweet_battle definition

CREATE TABLE `couple_sweet_battle` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '擂台日期（yyyy-MM-dd，每天一场）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 等发话 / FULL 等投票 / DONE 已结算',
    `vote_a` varchar(50) DEFAULT NULL COMMENT '用户 A 投给的句子作者',
    `vote_b` varchar(50) DEFAULT NULL COMMENT '用户 B 投给的句子作者',
    `winner` varchar(50) DEFAULT NULL COMMENT '赢家（双方投同一人时）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_battle_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情话Battle表：今天也为了「谁更会撩」吵了一架';


-- smart_collections.couple_sweet_line definition

CREATE TABLE `couple_sweet_line` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `battle_id` varchar(36) NOT NULL COMMENT '所属擂台（关联 couple_sweet_battle.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发话人用户名（区分大小写）',
    `content` varchar(100) NOT NULL COMMENT '情话内容',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_sweet_line` (`space_id`,`battle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情话Battle句子表：一句顶一万句';


-- smart_collections.couple_art_gallery definition

CREATE TABLE `couple_art_gallery` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作画人用户名（区分大小写）',
    `title` varchar(50) NOT NULL COMMENT '画作标题',
    `seed` int(11) NOT NULL COMMENT '画作种子（前端按种子生成抽象画）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_art_gallery` (`space_id`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='抽象画画廊表：我看不懂，但我大受震撼，而且很喜欢';


-- smart_collections.couple_dream definition

CREATE TABLE `couple_dream` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记梦人用户名（区分大小写）',
    `content` varchar(500) NOT NULL COMMENT '梦境内容',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_dream` (`space_id`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='梦境手账表：昨晚又梦到 TA，醒来第一时间写下来';


-- smart_collections.couple_food_note definition

CREATE TABLE `couple_food_note` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `shop` varchar(60) NOT NULL COMMENT '店名',
    `dish` varchar(60) NOT NULL COMMENT '招牌菜',
    `status` varchar(10) NOT NULL DEFAULT 'WANT' COMMENT '状态：WANT 想吃 / EATEN 已打卡',
    `rating` int(11) DEFAULT NULL COMMENT '评分 0-5 星（打卡时填写）',
    `comment` varchar(200) DEFAULT NULL COMMENT '吃后感（打卡时填写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_food_note` (`space_id`,`status`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='美食地图表：把「改天一起吃」变成一张张打卡票根';


-- smart_collections.couple_partner_fact definition

CREATE TABLE `couple_partner_fact` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '撰写人用户名（区分大小写）',
    `kind` varchar(10) NOT NULL DEFAULT 'TASTE' COMMENT '条目类型：TASTE 口味 / NOGO 雷区 / FAV 心头好 / QUIRK 小怪癖',
    `content` varchar(200) NOT NULL COMMENT '条目内容',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_partner_fact` (`space_id`,`kind`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='TA 使用手册表：把彼此的说明书一页页补全';


-- smart_collections.couple_sos_ping definition

CREATE TABLE `couple_sos_ping` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '求抱抱人用户名（区分大小写）',
    `message` varchar(100) DEFAULT NULL COMMENT '想多说的一句（可空）',
    `status` varchar(10) NOT NULL DEFAULT 'SENT' COMMENT '状态：SENT 等接住 / HELD 已抱住',
    `held_at` bigint(20) DEFAULT NULL COMMENT '被抱住时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_sos_ping` (`space_id`,`status`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情绪SOS表：成年人的崩溃需要快捷键，抱抱是最好的响应';


-- smart_collections.couple_daily_three definition

CREATE TABLE `couple_daily_three` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '答题人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `joy` varchar(200) DEFAULT NULL COMMENT '今天最开心的事',
    `touched` varchar(200) DEFAULT NULL COMMENT '今天最被感动的瞬间',
    `want_to_say` varchar(200) DEFAULT NULL COMMENT '最想对 TA 说的一句话',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_three_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日三问表：睡前三分钟，把一天过成值得纪念的样子';


-- smart_collections.couple_custom_badge definition

CREATE TABLE `couple_custom_badge` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '立成就人用户名（区分大小写）',
    `title` varchar(50) NOT NULL COMMENT '成就名（连吃七天早餐）',
    `condition` varchar(200) DEFAULT NULL COMMENT '达成条件说明（可空）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 挑战中 / ISSUED 已颁发',
    `issued_at` bigint(20) DEFAULT NULL COMMENT '颁发时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_custom_badge` (`space_id`,`status`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义成就表：我们自己定义什么值得庆祝';


-- smart_collections.couple_habit_streak definition

CREATE TABLE `couple_habit_streak` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '立习惯人用户名（区分大小写）',
    `title` varchar(60) NOT NULL COMMENT '习惯名（每天读书30分钟）',
    `target_days` int(11) NOT NULL DEFAULT 21 COMMENT '目标天数',
    `done_days` int(11) NOT NULL DEFAULT 0 COMMENT '已打卡天数',
    `last_done_day` varchar(10) DEFAULT NULL COMMENT '最近打卡日期（yyyy-MM-dd）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 挑战中 / DONE 已达成',
    `done_at` bigint(20) DEFAULT NULL COMMENT '达成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_habit_streak` (`space_id`,`status`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='21天习惯搭子表：TA 盯着的日子，总不好意思偷懒';


-- smart_collections.couple_thanks_note definition

CREATE TABLE `couple_thanks_note` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '致谢人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '感谢内容',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_thanks_note` (`space_id`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='感恩便签墙：把「理所当然」写回「谢谢」';


-- smart_collections.couple_feel_log definition

CREATE TABLE `couple_feel_log` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `word` varchar(20) NOT NULL COMMENT '细名情绪词（委屈/雀跃/怅然…）',
    `intensity` int(11) NOT NULL DEFAULT 3 COMMENT '强度 1-5',
    `note` varchar(200) DEFAULT NULL COMMENT '一句注脚（可空）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_feel_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情绪颗粒度日记表：情绪词汇量越大，越不需要用吵架说话';


-- smart_collections.couple_weekly_star definition

CREATE TABLE `couple_weekly_star` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提名人工用户名（区分大小写）',
    `highlight` varchar(200) NOT NULL COMMENT '对方本周的高光瞬间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_week_star` (`space_id`,`week`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每周高光互评表：被看见，是关系里最好的营养';


-- smart_collections.couple_read_minute definition

CREATE TABLE `couple_read_minute` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '感想人用户名（区分大小写）',
    `thought` varchar(200) NOT NULL COMMENT '今日感想',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_read_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='共读一分钟表：读同一段文字，是在精神里散步';


-- smart_collections.couple_delay_task definition

CREATE TABLE `couple_delay_task` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '立事人用户名（区分大小写）',
    `title` varchar(100) NOT NULL COMMENT '拖延的事（去体检/交年报）',
    `deadline_day` varchar(10) DEFAULT NULL COMMENT '截止日期（可空）',
    `nag_count` int(11) NOT NULL DEFAULT 0 COMMENT '被催次数',
    `last_nag_at` bigint(20) DEFAULT NULL COMMENT '最近催办时间（毫秒）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 拖着 / DONE 完成',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_delay_task` (`space_id`,`status`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='拖延互助所表：成年人的自律，需要一个盯着你的爱人';


-- smart_collections.couple_praise_bank definition

CREATE TABLE `couple_praise_bank` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存款人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '对方的一个优点',
    `scene` varchar(60) DEFAULT NULL COMMENT '存入场景（可空：吵架时读）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_praise_bank` (`space_id`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优点存折表：生气时先取三条利息，再决定要不要吵架';


-- smart_collections.couple_poem_chain definition

CREATE TABLE `couple_poem_chain` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '执笔人用户名（区分大小写）',
    `line` varchar(100) NOT NULL COMMENT '今天这一句诗',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_poem_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情诗接龙表：一天一句，把日子连成一首写不完的诗';


-- smart_collections.couple_poem_3line definition

CREATE TABLE `couple_poem_3line` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作者用户名（区分大小写）',
    `line1` varchar(60) NOT NULL COMMENT '第一行',
    `line2` varchar(60) NOT NULL COMMENT '第二行',
    `line3` varchar(60) NOT NULL COMMENT '第三行',
    `liked_by` varchar(50) DEFAULT NULL COMMENT '点赞人用户名（对方，可空；H2 兼容：可空列不带 charset）',
    `liked_at` bigint(20) DEFAULT NULL COMMENT '点赞时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_poem_3line` (`space_id`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='三行情书表：最深的话，用最短的诗说';


-- smart_collections.couple_morning_note definition

CREATE TABLE `couple_morning_note` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写信人用户名（区分大小写）',
    `content` varchar(300) NOT NULL COMMENT '想说的那句话',
    `deliver_day` varchar(10) NOT NULL COMMENT '送达日期（写信次日 yyyy-MM-dd）',
    `read_at` bigint(20) DEFAULT NULL COMMENT '对方已读时间（毫秒，可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_morning_note` (`space_id`,`deliver_day`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='醒来第一条表：让 TA 的一天，从你的话开始';


-- smart_collections.couple_drift_bottle definition

CREATE TABLE `couple_drift_bottle` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '扔瓶人用户名（区分大小写）',
    `mood` varchar(20) NOT NULL COMMENT '瓶中心情（委屈/疲惫/烦躁…）',
    `content` varchar(300) NOT NULL COMMENT '瓶中信内容',
    `reply` varchar(300) DEFAULT NULL COMMENT '对方的回信（可空）',
    `replied_at` bigint(20) DEFAULT NULL COMMENT '回信时间（毫秒，可空）',
    `status` varchar(10) NOT NULL DEFAULT 'FLOATING' COMMENT '状态：FLOATING 漂着 / REPLIED 已回信',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_drift_bottle` (`space_id`,`status`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心情漂流瓶表：把坏情绪交给海，把回应留给爱人';


-- smart_collections.couple_cipher_note definition

CREATE TABLE `couple_cipher_note` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '加密人用户名（区分大小写）',
    `cipher` varchar(500) NOT NULL COMMENT '数字密码串',
    `hint` varchar(100) DEFAULT NULL COMMENT '解码提示（可空）',
    `decoded_by` varchar(50) DEFAULT NULL COMMENT '解码人用户名（可空；H2 兼容：可空列不带 charset）',
    `decoded_at` bigint(20) DEFAULT NULL COMMENT '解码时间（毫秒，可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_cipher_note` (`space_id`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数字密码情书表：看懂那串数字的人，是全世界最幸运的';


-- smart_collections.couple_soul_answer definition

CREATE TABLE `couple_soul_answer` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作答人用户名（区分大小写）',
    `answer` varchar(300) NOT NULL COMMENT '答案',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_soul_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='灵魂提问盲盒表：深聊一次，胜过闲聊一百次';


-- smart_collections.couple_journal definition

CREATE TABLE `couple_journal` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '手账人用户名（区分大小写）',
    `sticker` varchar(20) NOT NULL DEFAULT '✨' COMMENT '贴纸 emoji',
    `text` varchar(200) NOT NULL COMMENT '手账正文',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_journal_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='贴纸手账表：一天一页，把平凡日子贴成册';


-- smart_collections.couple_love_lang definition

CREATE TABLE `couple_love_lang` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作答人用户名（区分大小写）',
    `words_score` int(11) NOT NULL DEFAULT 0 COMMENT '肯定的言语得分',
    `time_score` int(11) NOT NULL DEFAULT 0 COMMENT '用心陪伴得分',
    `gifts_score` int(11) NOT NULL DEFAULT 0 COMMENT '接受礼物得分',
    `service_score` int(11) NOT NULL DEFAULT 0 COMMENT '服务的行动得分',
    `touch_score` int(11) NOT NULL DEFAULT 0 COMMENT '身体的接触得分',
    `primary_lang` varchar(20) NOT NULL COMMENT '主爱语（WORDS/TIME/GIFTS/SERVICE/TOUCH）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_love_lang_user` (`space_id`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='爱语测评表：爱要用对方的语言说，才不算白说';


-- smart_collections.couple_heart_flash definition

CREATE TABLE `couple_heart_flash` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `moment` varchar(200) NOT NULL COMMENT '心动的一刻',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_heart_flash` (`space_id`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心动闪光表：把突然软下来的瞬间存住，想念时取用';


-- smart_collections.couple_what_if definition

CREATE TABLE `couple_what_if` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作答人用户名（区分大小写）',
    `answer` varchar(200) NOT NULL COMMENT '答案',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_whatif_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='「如果」问答表：聊点不着边际的，反而最懂彼此';


-- smart_collections.couple_secret_signal definition

CREATE TABLE `couple_secret_signal` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '约定人用户名（区分大小写）',
    `signal` varchar(50) NOT NULL COMMENT '动作（捏三下手心）',
    `meaning` varchar(100) NOT NULL COMMENT '含义（我爱你，别怕）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_secret_signal` (`space_id`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='动作暗语本：人前不能说的话，都写在动作里';


-- smart_collections.couple_sync_tap definition

CREATE TABLE `couple_sync_tap` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `attempts` int(11) NOT NULL DEFAULT 0 COMMENT '尝试次数',
    `best_ms` bigint(20) DEFAULT NULL COMMENT '最小时间差（毫秒）',
    `last_tap_user` varchar(50) DEFAULT NULL COMMENT '最近按键人（可空）',
    `last_tap_at` bigint(20) DEFAULT NULL COMMENT '最近按键时间（毫秒）',
    `hits` int(11) NOT NULL DEFAULT 0 COMMENT '同频成功次数（差值<=500ms）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sync_tap_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='同频共振表：不用言语也能对上拍子的两个人';


-- smart_collections.couple_heart_day definition

CREATE TABLE `couple_heart_day` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '标记人用户名（区分大小写）',
    `level` int(11) NOT NULL DEFAULT 1 COMMENT '心动等级 1-3（1 平淡 2 甜甜 3 心动爆棚）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_heart_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心动日历表：给每天的心情盖一个心动邮戳';



-- smart_collections.couple_family_meeting definition

-- F180 家庭会议纪要：每周议题+决议+跟进日，可关闭
CREATE TABLE `couple_family_meeting` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `topic` varchar(100) NOT NULL COMMENT '议题',
    `decision` varchar(300) NOT NULL DEFAULT '' COMMENT '决议（可后补）',
    `follow_day` varchar(10) DEFAULT NULL COMMENT '跟进日（可空；H2 兼容：可空列不带 charset）',
    `raised_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提出人用户名（区分大小写）',
    `closed` int(11) NOT NULL DEFAULT 0 COMMENT '是否关闭 0 进行中 1 已关闭',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    KEY `idx_family_meeting` (`space_id`, `week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='家庭会议纪要表：大事小事摆上桌，关起门来是一家人';


-- smart_collections.couple_week_host definition

CREATE TABLE `couple_week_host` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `plan` varchar(200) NOT NULL DEFAULT '' COMMENT '主理人排的本周小计划（可后补）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_week_host` (`space_id`,`week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='本周主理人表：这周轮到你当家，小计划你来排';


-- smart_collections.couple_skill_swap definition

CREATE TABLE `couple_skill_swap` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发起人用户名（区分大小写）',
    `teach` varchar(60) NOT NULL COMMENT '我能教你什么',
    `learn` varchar(60) NOT NULL COMMENT '我想跟你学什么',
    `status` varchar(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态 OPEN 挂牌 / TAKEN 成交 / DONE 两清',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_skill_swap` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='技能交换所表：你会的我想学，我会的你正好要';


-- smart_collections.couple_month_review definition

CREATE TABLE `couple_month_review` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `month` varchar(7) NOT NULL COMMENT '所属月份（yyyy-MM）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '评价人用户名（区分大小写）',
    `stars` int(11) NOT NULL DEFAULT 5 COMMENT '本月关系星级 1-5',
    `advice` varchar(200) NOT NULL DEFAULT '' COMMENT '想对TA说的一条建议',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_month_review_user` (`space_id`,`month`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='月度互评表：一个月打一次分，日子才有回声';


-- smart_collections.couple_emergency_card definition

CREATE TABLE `couple_emergency_card` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '填写人用户名（区分大小写）',
    `contacts` varchar(300) NOT NULL DEFAULT '' COMMENT '紧急联系人清单',
    `keys_place` varchar(200) NOT NULL DEFAULT '' COMMENT '备用钥匙/重要物品存放',
    `medicine` varchar(300) NOT NULL DEFAULT '' COMMENT '常备药与过敏信息',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emergency_card_user` (`space_id`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='家庭应急卡表：愿永远用不上，但必须人人看得懂';


-- smart_collections.couple_month_snapshot definition

CREATE TABLE `couple_month_snapshot` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `month` varchar(7) NOT NULL COMMENT '所属月份（yyyy-MM）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存档人用户名（区分大小写）',
    `work` varchar(100) NOT NULL DEFAULT '' COMMENT '本月工作状态',
    `health` varchar(100) NOT NULL DEFAULT '' COMMENT '本月健康状态',
    `love_temp` int(11) NOT NULL DEFAULT 60 COMMENT '感情温度 0-100',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_month_snapshot_user` (`space_id`,`month`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣存档点表：像游戏存档一样记录每个月的我们';


-- smart_collections.couple_point_ledger definition

CREATE TABLE `couple_point_ledger` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记账人用户名（区分大小写）',
    `type` varchar(10) NOT NULL COMMENT '流水类型 EARN 赚 / SPEND 花',
    `item` varchar(100) NOT NULL COMMENT '事项或兑换的奖励',
    `points` int(11) NOT NULL DEFAULT 1 COMMENT '积分数（正整数）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_point_ledger` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='家务积分流水表：家务不再是白干，爱也要有账本';


-- smart_collections.couple_five_year_plan definition

CREATE TABLE `couple_five_year_plan` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `track` varchar(10) NOT NULL COMMENT '轨道 MINE 自己的 / OURS 我们的',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写下人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '五年之约内容',
    `owner_user` varchar(50) DEFAULT NULL COMMENT 'OURS 轨道认领人（可空；H2 兼容：可空列不带 charset）',
    `done` int(11) NOT NULL DEFAULT 0 COMMENT '是否达成 0 进行中 1 已达成',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_five_year_plan` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='五年计划双轨表：一条路自己走，一条路牵着手走';


-- smart_collections.couple_anniv_plan definition

CREATE TABLE `couple_anniv_plan` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '纪念日日期（yyyy-MM-dd）',
    `title` varchar(60) NOT NULL COMMENT '纪念日名称',
    `planner` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '策划人用户名（区分大小写）',
    `idea` varchar(300) NOT NULL DEFAULT '' COMMENT '策划点子',
    `status` varchar(20) NOT NULL DEFAULT 'IDEA' COMMENT '状态 IDEA 点子 / LOCKED 定稿 / DONE 已落地',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_anniv_plan` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='纪念日策划案表：把惊喜写成方案，把浪漫落到实处';



-- smart_collections.couple_doc_scene definition

-- F190 恋爱纪录片分镜：把一段回忆写成三幕剧本
CREATE TABLE `couple_doc_scene` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(60) NOT NULL COMMENT '这一部纪录片的名字',
    `act_one` varchar(300) NOT NULL COMMENT '第一幕：相识',
    `act_two` varchar(300) NOT NULL COMMENT '第二幕：相知',
    `act_three` varchar(300) NOT NULL COMMENT '第三幕：相伴',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '编剧用户名（区分大小写）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_doc_scene` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='恋爱纪录片分镜表：我们的故事，值得三幕讲完';


-- smart_collections.couple_exhibit definition

CREATE TABLE `couple_exhibit` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `name` varchar(60) NOT NULL COMMENT '展品名（第一场电影票根）',
    `story` varchar(300) NOT NULL DEFAULT '' COMMENT '展品背后的故事',
    `obtained_day` varchar(10) DEFAULT NULL COMMENT '藏品日期（可空；H2 兼容：可空列不带 charset）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '登记人用户名（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_exhibit` (`space_id`, `obtained_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='博物馆展品表：小物件不值钱，值钱的是它记得那天';


-- smart_collections.couple_hidden_achievement definition

CREATE TABLE `couple_hidden_achievement` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `code` varchar(40) NOT NULL COMMENT '成就编码（静态白名单）',
    `unlocked_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '达成时触发人用户名（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '解锁时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hidden_achievement` (`space_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='隐藏成就表：有些惊喜，是日子替你们藏的';


-- smart_collections.couple_house_rule definition

CREATE TABLE `couple_house_rule` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `kind` varchar(20) NOT NULL DEFAULT 'RULE' COMMENT '类型 RULE 条款 / AMENDMENT 修正案',
    `ref_id` varchar(36) DEFAULT NULL COMMENT '修正案针对的原条款ID（可空；H2 兼容：可空列不带 charset）',
    `content` varchar(200) NOT NULL COMMENT '条款内容',
    `proposed_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提出人用户名（区分大小写）',
    `signed` int(11) NOT NULL DEFAULT 0 COMMENT '对方是否已签字 0 未签 1 已签',
    `signed_by` varchar(50) DEFAULT NULL COMMENT '签字人（可空；H2 兼容：可空列不带 charset）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_house_rule` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='家规宪法表：约法三章不嫌少，商量着来的才算数';


-- smart_collections.couple_dnd_setting definition

CREATE TABLE `couple_dnd_setting` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '设置人用户名（区分大小写）',
    `start_time` varchar(5) NOT NULL COMMENT '免打扰开始时刻（HH:mm）',
    `end_time` varchar(5) NOT NULL COMMENT '免打扰结束时刻（HH:mm，可跨零点）',
    `enabled` int(11) NOT NULL DEFAULT 1 COMMENT '是否启用 0 停用 1 启用',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dnd_user` (`space_id`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='免打扰设置表：晚安之后的安静，也是陪伴的一部分';

-- smart_collections.couple_user_pin definition
CREATE TABLE `couple_user_pin` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '归属用户名（区分大小写）',
    `pins` varchar(500) NOT NULL DEFAULT '' COMMENT '功能卡键，逗号分隔，最多 6 个',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_space_user` (`space_id`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F207 常用收藏表：把最常翻的卡片钉在手边';


-- smart_collections.couple_dine_ticket definition
CREATE TABLE `couple_dine_ticket` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '提名日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提名用户名（区分大小写）',
    `dish` varchar(60) NOT NULL COMMENT '菜名',
    `reason` varchar(140) NOT NULL DEFAULT '' COMMENT '一句理由',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F210 今晚饭票：每人每天提名一道菜';

-- smart_collections.couple_dine_rate definition
CREATE TABLE `couple_dine_rate` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '吃的那天 yyyy-MM-dd',
    `dish` varchar(60) NOT NULL COMMENT '吃了啥（菜或店）',
    `stars` int NOT NULL COMMENT '1-5 星',
    `comment` varchar(140) NOT NULL DEFAULT '' COMMENT '一句点评',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_rate_space` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F212 吃过星评：我们的餐厅档案';

-- smart_collections.couple_dine_nogo definition
CREATE TABLE `couple_dine_nogo` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `name` varchar(60) NOT NULL COMMENT '小店/外卖名',
    `reason` varchar(140) NOT NULL COMMENT '避雷理由',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提议人（只有 TA 能划掉）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_nogo` (`space_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F213 踩雷库：一起拉黑的小店';

-- smart_collections.couple_dine_weekplan definition
CREATE TABLE `couple_dine_weekplan` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `day` varchar(10) NOT NULL COMMENT '周内某天 yyyy-MM-dd',
    `dish` varchar(60) NOT NULL COMMENT '这顿吃什么',
    `updated_by` varchar(50) NOT NULL DEFAULT '' COMMENT '最后编辑人（可空列不带 charset）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F214 本周菜单：7 个格子各排一顿正餐';

-- smart_collections.couple_dine_homecook definition
CREATE TABLE `couple_dine_homecook` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '下厨人',
    `dish` varchar(60) NOT NULL COMMENT '拿手菜',
    `score` int NOT NULL COMMENT '自封配饭指数 1-5',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_homecook` (`space_id`, `week`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F215 家常菜搭档：周末各报一道拿手菜';

-- smart_collections.couple_dine_cart definition
CREATE TABLE `couple_dine_cart` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd，随周清空靠查询过滤）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '加菜人',
    `item` varchar(60) NOT NULL COMMENT '菜品名',
    `qty` int NOT NULL DEFAULT 1 COMMENT '份数 1-20',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/LOCKED，双方各锁一次才成行',
    `locked_by` varchar(120) NOT NULL DEFAULT '' COMMENT '已按锁的人（逗号分隔，可空列不带 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_cart_week` (`space_id`, `week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F217 外卖搭伙车：同辆车各加菜，凑齐喊锁车';

-- smart_collections.couple_dine_topic definition
CREATE TABLE `couple_dine_topic` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪天的饭桌话题 yyyy-MM-dd',
    `marked_by` varchar(50) NOT NULL DEFAULT '' COMMENT '谁标记聊过了（可空列不带 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F218 饭桌话题卡：吃完打个卡';


-- smart_collections.couple_cozy_lightout definition
CREATE TABLE `couple_cozy_lightout` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '熄灯日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发晚安的人（区分大小写）',
    `at_time` varchar(5) NOT NULL DEFAULT '' COMMENT '按点时刻 HH:mm',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lightout` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F220 晚安同熄灯：双方都发晚安=当日熄灯';

-- smart_collections.couple_cozy_sleep definition
CREATE TABLE `couple_cozy_sleep` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '报告的是哪一晚 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '报告人',
    `stars` int NOT NULL COMMENT '睡眠质量自评 1-5',
    `dream` varchar(140) NOT NULL DEFAULT '' COMMENT '一句梦话',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sleep` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F221 睡眠报告单：晨间各报昨夜自评';

-- smart_collections.couple_cozy_sheep definition
CREATE TABLE `couple_cozy_sheep` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪晚数羊 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '数羊人',
    `taps` int NOT NULL DEFAULT 0 COMMENT '已点的羊数（满 10 算数完）',
    `done` int NOT NULL DEFAULT 0 COMMENT '0 进行中 / 1 数完一群',
    `elapsed_ms` int NOT NULL DEFAULT 0 COMMENT '本人数完用时毫秒',
    `created` bigint(20) NOT NULL COMMENT '首次按键时间（毫秒）',
    `updated_at` bigint(20) NOT NULL COMMENT '最近按键时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sheep` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F222 数羊房：双方各点满 10 下一起数完一群羊';

-- smart_collections.couple_cozy_water definition
CREATE TABLE `couple_cozy_water` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪天 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '喝水的人',
    `cups` int NOT NULL DEFAULT 0 COMMENT '今日杯数',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '最近一杯时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_water` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F223 喝水接力：我喝一杯给 TA 的杯子加一格';

-- smart_collections.couple_cozy_weather definition
CREATE TABLE `couple_cozy_weather` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪天的体感 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '互报的人',
    `city` varchar(60) NOT NULL COMMENT '所在城市（纯文字）',
    `feel` varchar(20) NOT NULL COMMENT '体感：冷/暖/刚好',
    `temp_text` varchar(20) NOT NULL DEFAULT '' COMMENT '气温文字',
    `advised_by` varchar(50) NOT NULL DEFAULT '' COMMENT '谁叮嘱过添衣（可空列不带 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_weather` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F224 冷暖互报：自报体感+对方一键叮嘱';

-- smart_collections.couple_cozy_latenight definition
CREATE TABLE `couple_cozy_latenight` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪天递卡 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '守护者（递卡人）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_latenight` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F225 熬夜守护：深夜递早点睡陪伴卡，一天一张';

-- smart_collections.couple_cozy_slow definition
CREATE TABLE `couple_cozy_slow` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提议人',
    `thing` varchar(140) NOT NULL COMMENT '一件什么都不赶的小事',
    `done_day` varchar(10) NOT NULL DEFAULT '' COMMENT '打卡日（空=未打卡）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slow` (`space_id`, `week`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F226 周末慢生活：周五各提一件小事周日打卡回放';

-- smart_collections.couple_cozy_remedy definition
CREATE TABLE `couple_cozy_remedy` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `for_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '这份对策是给谁的',
    `body` varchar(500) NOT NULL COMMENT '正确做法清单文字',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_remedy` (`space_id`, `for_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F227 疼痛对策本：我难受时的正确做法';

-- smart_collections.couple_cozy_hug definition
CREATE TABLE `couple_cozy_hug` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪天抱的 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '自报的人',
    `cnt` int NOT NULL COMMENT '这一次抱了几下',
    `note` varchar(140) NOT NULL DEFAULT '' COMMENT '一句备注',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_hug_space` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F228 抱抱计量器：见面拥抱自报计数攒里程碑';


-- smart_collections.couple_ceremony_founded definition
CREATE TABLE `couple_ceremony_founded` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `name` varchar(60) NOT NULL COMMENT '小日子名称（如「我们的建国纪念日」）',
    `start_day` varchar(10) NOT NULL COMMENT '起始日 yyyy-MM-dd',
    `repeat_year` int NOT NULL DEFAULT 1 COMMENT '1 每年重复 / 0 一次性',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    KEY `idx_founded` (`space_id`, `start_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F230 建国纪念日：自定义我们的小日子（可与官方纪念日区分）';

-- smart_collections.couple_ceremony_ritual definition
CREATE TABLE `couple_ceremony_ritual` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `founded_id` varchar(36) NOT NULL COMMENT '所属小日子（关联 couple_ceremony_founded.id）',
    `content` varchar(140) NOT NULL COMMENT '庆祝方式文字动作（如「一起吃火锅」）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_ritual` (`founded_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F232 过法任务卡：每个小日子写死 1-3 条庆祝方式';

-- smart_collections.couple_ceremony_mark definition
CREATE TABLE `couple_ceremony_mark` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `ritual_id` varchar(36) NOT NULL COMMENT '被打勾的过法卡（关联 couple_ceremony_ritual.id）',
    `day` varchar(10) NOT NULL COMMENT '打卡日 yyyy-MM-dd',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mark` (`ritual_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F233 庆祝打卡：当日逐条打勾，隔日未齐补催';

-- smart_collections.couple_ceremony_policy definition
CREATE TABLE `couple_ceremony_policy` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `month` varchar(7) NOT NULL COMMENT '保费月 yyyy-MM',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '交保费的人（互夸作者）',
    `quote` varchar(140) NOT NULL COMMENT '夸 TA 的一句（本月保费）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_policy` (`space_id`, `month`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F234 爱情保险柜：每月互夸各 1 句=交齐当月保费';

-- smart_collections.couple_ceremony_renew definition
CREATE TABLE `couple_ceremony_renew` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `anchor_day` varchar(10) NOT NULL COMMENT '续约锚点日（满 100 天/周年的当日）yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '签字人',
    `line` varchar(140) NOT NULL COMMENT '重签的一句「我还是选你」',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_renew` (`space_id`, `anchor_day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F235 续约仪式：每满 100 天/周年双方各签一句';

-- smart_collections.couple_ceremony_coupon definition
CREATE TABLE `couple_ceremony_coupon` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(80) NOT NULL COMMENT '券面文字（自拟）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN 待兑 / USED 已核销',
    `ref` varchar(60) NOT NULL DEFAULT '' COMMENT '保险柜 payout 来源标记（policy-3 等；手动发为空）',
    `issuer` varchar(50) NOT NULL COMMENT '发券人',
    `used_by` varchar(50) NOT NULL DEFAULT '' COMMENT '核销人（未核销为空）',
    `used_at` bigint(20) NOT NULL DEFAULT 0 COMMENT '核销时间（毫秒，未核销为 0）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_coupon` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F236 愿望券本：手动发/核销，保险柜满 3/6/12 月自动 payout';

-- smart_collections.couple_ceremony_recap definition
CREATE TABLE `couple_ceremony_recap` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '仪式发生日 yyyy-MM-dd（含年份，天然区分届次）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '留言人',
    `feeling` varchar(140) NOT NULL COMMENT '此刻感觉一句话',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recap` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F239 当日体感：仪式当天各留一句，次年今日对比';

-- smart_collections.couple_board_role definition
CREATE TABLE `couple_board_role` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提名人（谁封的官）',
    `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '被任命的人',
    `title` varchar(60) NOT NULL COMMENT '头衔（财政部长/首席大厨…）',
    `appointed` int NOT NULL DEFAULT 0 COMMENT '0 待任命 / 1 已盖章生效',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    KEY `idx_role` (`space_id`, `to_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F240 头衔任命：各报两个在家职位，对方点任命生效';

-- smart_collections.couple_board_vote definition
CREATE TABLE `couple_board_vote` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(140) NOT NULL COMMENT '决议事项',
    `proposer` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提案人',
    `status` varchar(12) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待表决 / PASSED 通过 / VETOED 一票否决',
    `veto_by` varchar(50) NOT NULL DEFAULT '' COMMENT '否决人（仅 VETOED 时有值）',
    `decided_at` bigint(20) NOT NULL DEFAULT 0 COMMENT '表决时刻（毫秒，未决为 0）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_vote` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F241 董事会决议：提案→附议→通过/否决（一票否决）全程留痕';

-- smart_collections.couple_board_report definition
CREATE TABLE `couple_board_report` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `year` varchar(4) NOT NULL COMMENT '述职年度 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '述职人',
    `review` varchar(500) NOT NULL COMMENT '本年述职',
    `goal` varchar(200) NOT NULL COMMENT '明年一个小目标',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report` (`space_id`, `year`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F242 年度股东大会：述职+小目标，双提交互见';

-- smart_collections.couple_board_salary definition
CREATE TABLE `couple_board_salary` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `month` varchar(7) NOT NULL COMMENT '发薪月 yyyy-MM',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发工资的人',
    `thanks` varchar(200) NOT NULL COMMENT '本月感谢工资（一句感谢）',
    `created` bigint(20) NOT NULL COMMENT '发薪时刻（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_salary` (`space_id`, `month`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F244 发薪日：每月各发一句感谢工资并+5积分入账台账';

-- smart_collections.couple_board_idea definition
CREATE TABLE `couple_board_idea` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '点子提出人',
    `content` varchar(140) NOT NULL COMMENT '一句话改进提案',
    `adopted` int NOT NULL DEFAULT 0 COMMENT '0 待看 / 1 已采纳转决议',
    `vote_id` varchar(36) NOT NULL DEFAULT '' COMMENT '采纳后生成的决议ID（关联 couple_board_vote.id）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_idea` (`space_id`, `adopted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F245 金点子箱：被采纳即转董事会决议';

-- smart_collections.couple_board_attend definition
CREATE TABLE `couple_board_attend` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '签到日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '签到人',
    `attended` int NOT NULL DEFAULT 1 COMMENT '当日是否已签到',
    `convened` int NOT NULL DEFAULT 0 COMMENT '0/1 当日已散会级双签到（10s 窗口内双签）',
    `created` bigint(20) NOT NULL COMMENT '首次签到时间（毫秒）',
    `updated_at` bigint(20) NOT NULL COMMENT '最近签到时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_attend` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F247 会议签到：10s 窗口内双签到才算开了会';

-- smart_collections.couple_term_check definition
CREATE TABLE `couple_term_check` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `term` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '节气名（两字）',
    `year` varchar(4) NOT NULL COMMENT '年份 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '跟上的人（区分大小写）',
    `note` varchar(140) NOT NULL DEFAULT '' COMMENT '晒的一句话',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_term_check` (`space_id`, `term`, `year`, `from_user`),
    KEY `idx_term_check_space` (`space_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F250 节气跟风：每人每节气每年一记';

-- smart_collections.couple_term_ritual definition
CREATE TABLE `couple_term_ritual` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `term` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '节气名（两字）',
    `content` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '过法一句话',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提议人（区分大小写）',
    `last_done_year` varchar(4) NOT NULL DEFAULT '' COMMENT '最近打卡年份，空=没打过',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_term_ritual` (`space_id`, `term`, `content`),
    KEY `idx_term_ritual_space` (`space_id`, `term`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F251 节气过法：每节气最多 2 条，年年可重复打卡';

-- smart_collections.couple_lucky_day definition
CREATE TABLE `couple_lucky_day` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '吉日 yyyy-MM-dd',
    `matter` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '要办的大事（文本）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '择日人（区分大小写）',
    `confirmed` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=对方已双盖章',
    `confirmed_by` varchar(50) NOT NULL DEFAULT '' COMMENT '确认人（可空位不写 charset）',
    `comment` varchar(140) NOT NULL DEFAULT '' COMMENT '黄历点评（Bank 生成存档）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lucky_day` (`space_id`, `day`, `matter`),
    KEY `idx_lucky_space` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F252 择吉日：同一天同一事唯一';

-- smart_collections.couple_festival_plan definition
CREATE TABLE `couple_festival_plan` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `festival` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '节日键（NEWYEAR/CHUXI/VALENTINE/L520/QIXI/MIDAUTUMN/NATIONAL/ANNIV-M）',
    `year` varchar(4) NOT NULL COMMENT '年份 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写方案的人（区分大小写）',
    `plan` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '今年怎么过',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_festival_plan` (`space_id`, `festival`, `year`, `from_user`),
    KEY `idx_festival_space` (`space_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F254 节日家档：一人一年一案，双案对照';

-- smart_collections.couple_term_note definition
CREATE TABLE `couple_term_note` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `term` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '节气名（两字）',
    `year` varchar(4) NOT NULL COMMENT '年份 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '执笔人（区分大小写）',
    `text` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '这个节气的一件小事',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_term_note` (`space_id`, `term`, `year`, `from_user`),
    KEY `idx_term_note_space` (`space_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F255 节气手账：本人可改写';

-- smart_collections.couple_holiday_wish definition
CREATE TABLE `couple_holiday_wish` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `holiday` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '假期键（如 2026-SPRING）',
    `day` varchar(10) NOT NULL COMMENT '假期首日 yyyy-MM-dd',
    `wish` varchar(200) NOT NULL DEFAULT '' COMMENT '干什么（两人共写一段）',
    `wished_by` varchar(50) NOT NULL DEFAULT '' COMMENT '首写人（可空位不写 charset）',
    `appended_by` varchar(50) NOT NULL DEFAULT '' COMMENT '补写人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_holiday_wish` (`space_id`, `holiday`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F257 长假愿望：首写+补写两段合一';

-- smart_collections.couple_normal_day definition
CREATE TABLE `couple_normal_day` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `year` varchar(4) NOT NULL COMMENT '年份 yyyy',
    `day` varchar(10) NOT NULL COMMENT '放空日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提报人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_normal_day` (`space_id`, `year`, `day`),
    KEY `idx_normal_space` (`space_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F258 反仪式感日：每年最多 3 天，双方提报合并';

-- smart_collections.couple_listen_slot definition
CREATE TABLE `couple_listen_slot` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '申请日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '要说的人（区分大小写）',
    `topic` varchar(140) NOT NULL DEFAULT '' COMMENT '想聊的主题',
    `status` varchar(12) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN CONFIRMED DONE CANCELLED',
    `confirmed_at` bigint(20) DEFAULT NULL COMMENT '倾听人确认时间',
    `done_at` bigint(20) DEFAULT NULL COMMENT '聊完时间',
    `rate_mine` int(11) DEFAULT NULL COMMENT '我给自己的表达感 1-5',
    `rate_partner` int(11) DEFAULT NULL COMMENT '倾听人给的被听感 1-5',
    `note` varchar(140) NOT NULL DEFAULT '' COMMENT '一句话收尾',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_slot_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F260 想被听时段：申请-确认-聊完-互评';

-- smart_collections.couple_proxy_word definition
CREATE TABLE `couple_proxy_word` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `content` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '替 TA 写的话',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '代笔人（区分大小写）',
    `status` varchar(10) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT ADOPTED',
    `final_text` varchar(200) NOT NULL DEFAULT '' COMMENT '定稿（TA 可改写）',
    `adopted_by` varchar(50) NOT NULL DEFAULT '' COMMENT '定稿人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_proxy_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F261 替我说：草稿到定稿';

-- smart_collections.couple_misrewind definition
CREATE TABLE `couple_misrewind` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `topic` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '争执主题（同空间当日唯一）',
    `day` varchar(10) NOT NULL COMMENT '记录日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '填写人（区分大小写）',
    `mine` varchar(200) NOT NULL DEFAULT '' COMMENT '我当时以为',
    `theirs` varchar(200) NOT NULL DEFAULT '' COMMENT '我猜你其实想',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_misrewind` (`space_id`, `day`, `topic`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F262 误会倒带：同一天同主题双方各一份';

-- smart_collections.couple_stuck_q definition
CREATE TABLE `couple_stuck_q` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `week` varchar(10) NOT NULL COMMENT '周锚 yyyy-MM-dd（周一）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '出题人（区分大小写）',
    `question` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '难住我的问题',
    `answer` varchar(200) NOT NULL DEFAULT '' COMMENT 'TA 的作答',
    `answered_at` bigint(20) DEFAULT NULL COMMENT '作答时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stuck_q` (`space_id`, `week`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F263 卡壳一问：一周一题，答对方题';

-- smart_collections.couple_swap_letter definition
CREATE TABLE `couple_swap_letter` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '写信日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写信人（区分大小写）',
    `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '以你的口吻写的信',
    `open_day` varchar(10) NOT NULL COMMENT '开放日 yyyy-MM-dd（须晚于写信日）',
    `status` varchar(10) NOT NULL DEFAULT 'SEALED' COMMENT 'SEALED OPENED',
    `opened_at` bigint(20) DEFAULT NULL COMMENT '拆信时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_swap_letter` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F264 换位信：封存到日互拆';

-- smart_collections.couple_hold_word definition
CREATE TABLE `couple_hold_word` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `content` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '早就想说的一句话',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '封存人（区分大小写）',
    `open_day` varchar(10) NOT NULL COMMENT '放行日 yyyy-MM-dd',
    `status` varchar(10) NOT NULL DEFAULT 'HELD' COMMENT 'HELD SENT',
    `sent_at` bigint(20) DEFAULT NULL COMMENT '送达时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_hold_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F265 早想说：7 天一放行';

-- smart_collections.couple_three_line definition
CREATE TABLE `couple_three_line` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '打卡日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '打卡人（区分大小写）',
    `morning` varchar(80) NOT NULL DEFAULT '' COMMENT '今日印象',
    `thanks` varchar(80) NOT NULL DEFAULT '' COMMENT '谢的一件',
    `praise` varchar(80) NOT NULL DEFAULT '' COMMENT '夸的一件',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_three_line` (`space_id`, `day`, `from_user`),
    KEY `idx_three_space` (`space_id`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F266 每天三行：一人一天一条可改写';

-- smart_collections.couple_tone_note definition
CREATE TABLE `couple_tone_note` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '语气日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '自报人（区分大小写）',
    `tone` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'TIRED BUSY SAD OKAY',
    `note` varchar(60) NOT NULL DEFAULT '' COMMENT '补一句',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tone_day` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F267 语气自报：给冷淡加字幕';

-- smart_collections.couple_truce definition
CREATE TABLE `couple_truce` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `raiser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '举旗人（区分大小写）',
    `until_at` bigint(20) NOT NULL COMMENT '解冻时刻（毫秒）',
    `decide_a` int(11) DEFAULT NULL COMMENT 'userA 决定：1 继续 0 算了',
    `decide_b` int(11) DEFAULT NULL COMMENT 'userB 决定：1 继续 0 算了',
    `status` varchar(10) NOT NULL DEFAULT 'ON' COMMENT 'ON ENDED',
    `ended_at` bigint(20) DEFAULT NULL COMMENT '结束时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_truce_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F268 休战旗：吵架急停 30 分钟';

-- smart_collections.couple_name_day definition
CREATE TABLE `couple_name_day` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '称呼日 yyyy-MM-dd',
    `name_text` varchar(40) NOT NULL DEFAULT '' COMMENT '今日称呼（Bank 存档）',
    `used_a` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userA 用过了',
    `used_b` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userB 用过了',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F269 称呼日：日抛爱称双人用完';

-- smart_collections.couple_spin_task definition
CREATE TABLE `couple_spin_task` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `week` varchar(10) NOT NULL COMMENT '周锚 yyyy-MM-dd（周一）',
    `item` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '家务事项',
    `assigned_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '轮到天选之人（区分大小写）',
    `confirmed` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=双方认账（对方点认）',
    `done` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=干完了',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spin_item` (`space_id`, `week`, `item`),
    KEY `idx_spin_space` (`space_id`, `week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F270 家务轮盘：一周一转，items 逐条落行';

-- smart_collections.couple_shop_item definition
CREATE TABLE `couple_shop_item` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '要买的东西',
    `qty` varchar(30) NOT NULL DEFAULT '' COMMENT '数量文本',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '登记人（区分大小写）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN DONE',
    `done_by` varchar(50) NOT NULL DEFAULT '' COMMENT '谁买回来的（可空位不写 charset）',
    `done_at` bigint(20) DEFAULT NULL COMMENT '买回时间（月榜统计用）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_shop_space` (`space_id`, `status`),
    KEY `idx_shop_done` (`space_id`, `done_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F271 采买清单';

-- smart_collections.couple_stock definition
CREATE TABLE `couple_stock` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `item` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '食材名（同空间同名唯一）',
    `qty` varchar(30) NOT NULL DEFAULT '' COMMENT '数量文本（两把/3个）',
    `put_day` varchar(10) NOT NULL DEFAULT '' COMMENT '入库日 yyyy-MM-dd',
    `expire_day` varchar(10) NOT NULL DEFAULT '' COMMENT '赏味期至，空=没写',
    `status` varchar(10) NOT NULL DEFAULT 'IN' COMMENT 'IN OUT',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '登记人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_item` (`space_id`, `item`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F272 冰箱值守：同物同名互相覆盖提醒';

-- smart_collections.couple_parcel definition
CREATE TABLE `couple_parcel` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `note` varchar(60) NOT NULL DEFAULT '' COMMENT '快递描述（几号柜/多大件）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '求代拿的人（区分大小写）',
    `status` varchar(10) NOT NULL DEFAULT 'SENT' COMMENT 'SENT GRABBED DONE',
    `grabber` varchar(50) NOT NULL DEFAULT '' COMMENT '接单侠（可空位不写 charset）',
    `done_at` bigint(20) DEFAULT NULL COMMENT '送达时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_parcel_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F273 代拿快递';

-- smart_collections.couple_wake_word definition
CREATE TABLE `couple_wake_word` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `week` varchar(10) NOT NULL COMMENT '生效周 yyyy-MM-dd（周一）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '定制人（区分大小写）',
    `content` varchar(60) NOT NULL DEFAULT '' COMMENT '叫醒词',
    `given_day` varchar(10) NOT NULL DEFAULT '' COMMENT '最近递卡日（可空位不写 charset）',
    `given_by` varchar(50) NOT NULL DEFAULT '' COMMENT '最近递卡人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wake_week` (`space_id`, `week`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F274 叫醒服务：周词+日卡';

-- smart_collections.couple_medicine definition
CREATE TABLE `couple_medicine` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '药名（同人同名唯一）',
    `times` varchar(60) NOT NULL DEFAULT '' COMMENT '每日时段文本（早饭后/睡前）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '服药本人（区分大小写）',
    `status` varchar(10) NOT NULL DEFAULT 'ONGOING' COMMENT 'ONGOING STOPPED',
    `last_remind_day` varchar(10) NOT NULL DEFAULT '' COMMENT 'TA 最近提醒日',
    `last_taken_day` varchar(10) NOT NULL DEFAULT '' COMMENT '本人最近服下日',
    `streak` int(11) NOT NULL DEFAULT 0 COMMENT '连续链（断一天清零）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_med_name` (`space_id`, `name`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F275 服药提醒链';

-- smart_collections.couple_standup definition
CREATE TABLE `couple_standup` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '拍日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '拍的人（区分大小写）',
    `tapped_at` bigint(20) NOT NULL COMMENT '拍的时刻（毫秒）',
    `paired` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=当日同起已判定并推送',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stand_day` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F276 久坐互拍';

-- smart_collections.couple_advance definition
CREATE TABLE `couple_advance` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `item` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '大项支出名目',
    `payer_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '垫付人（区分大小写）',
    `amount_cents` int(11) NOT NULL COMMENT '金额（分，正整数）',
    `note` varchar(140) NOT NULL DEFAULT '' COMMENT '备注',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN SETTLED',
    `settled_at` bigint(20) DEFAULT NULL COMMENT '清账时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_advance_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F277 垫付本（区别于 F5 日常记账）';

-- smart_collections.couple_grocery definition
CREATE TABLE `couple_grocery` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `week` varchar(10) NOT NULL COMMENT '周锚 yyyy-MM-dd（周一）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '采购方（区分大小写）',
    `items` varchar(300) NOT NULL DEFAULT '' COMMENT '买了啥（逗号分隔，≤5 件）',
    `guess` varchar(300) NOT NULL DEFAULT '' COMMENT 'TA 的猜测文本',
    `guess_by` varchar(50) NOT NULL DEFAULT '' COMMENT '猜测人（可空位不写 charset）',
    `score` int(11) DEFAULT NULL COMMENT '采购方打分 0-5',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_grocery_week` (`space_id`, `week`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F278 战利品互猜';

-- smart_collections.couple_home_check definition
CREATE TABLE `couple_home_check` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `month` varchar(7) NOT NULL COMMENT '检月 yyyy-MM',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '检人（区分大小写）',
    `items` varchar(120) NOT NULL DEFAULT '' COMMENT '勾选项编码逗号分隔（GAS/WATER/ELEC/WINDOW/LOCK/FIRSTAID）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_check_month` (`space_id`, `month`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='F279 家安月检：缺月由总览读出提醒';
