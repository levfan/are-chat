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
    PRIMARY KEY (`id`),
    KEY `idx_fr_to_user_status` (`to_user`, `status`),
    KEY `idx_fr_from_to` (`from_user`, `to_user`)
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
    PRIMARY KEY (`id`),
    KEY `idx_pm_from_to_created` (`from_user`, `to_user`, `created`),
    KEY `idx_pm_to_from_created` (`to_user`, `from_user`, `created`)
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


-- smart_collections.couple_checkin definition


-- smart_collections.couple_answer definition


-- smart_collections.couple_item definition


-- smart_collections.couple_anniversary definition



-- smart_collections.couple_mood definition



-- smart_collections.couple_letter definition


-- smart_collections.couple_pact definition


-- smart_collections.couple_fund definition


-- smart_collections.couple_fund_deposit definition


-- smart_collections.couple_action definition



-- smart_collections.couple_mood_reaction definition



-- smart_collections.couple_task definition


-- smart_collections.couple_tacit definition


-- smart_collections.couple_reconcile definition


-- smart_collections.couple_praise definition


-- smart_collections.couple_cycle definition


-- smart_collections.couple_capsule definition


-- smart_collections.couple_countdown definition


-- smart_collections.couple_expense definition


-- smart_collections.couple_chore definition


-- smart_collections.couple_date_plan definition


-- smart_collections.couple_habit definition


-- smart_collections.couple_habit_log definition


-- smart_collections.couple_cipher definition


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


-- smart_collections.couple_answer_reaction definition


-- smart_collections.couple_scratch definition



-- smart_collections.couple_mystery_box definition



-- smart_collections.couple_sweet_alarm definition


-- smart_collections.couple_miss_express definition


-- smart_collections.couple_garden definition


-- smart_collections.couple_rose definition


-- smart_collections.couple_fortune_slip definition


-- smart_collections.couple_confession definition


-- smart_collections.couple_treasure definition


-- smart_collections.couple_comfort definition



-- smart_collections.couple_peace_review definition


-- smart_collections.couple_sorry_ticket definition


-- smart_collections.couple_truth definition


-- smart_collections.couple_whisper definition


-- smart_collections.couple_telepathy definition


-- smart_collections.couple_love_bank definition


-- smart_collections.couple_challenge definition


-- smart_collections.couple_passbook definition


-- smart_collections.couple_hundred definition


-- smart_collections.couple_hundred_checkin definition


-- smart_collections.couple_wish_exchange definition


-- smart_collections.couple_read_plan definition


-- smart_collections.couple_read_progress definition


-- smart_collections.couple_travel_wish definition


-- smart_collections.couple_watchlist definition


-- smart_collections.couple_dict_word definition


-- smart_collections.couple_next_time definition


-- smart_collections.couple_quote definition


-- smart_collections.couple_ticket definition


-- smart_collections.couple_song definition


-- smart_collections.couple_cool_down definition


-- smart_collections.couple_mood_relay definition


-- smart_collections.couple_guess_round definition


-- smart_collections.couple_story_line definition


-- smart_collections.couple_apology_card definition


-- smart_collections.couple_feeling_word definition


-- smart_collections.couple_handhold definition


-- smart_collections.couple_miss_daily definition


-- smart_collections.couple_routine definition


-- smart_collections.couple_reunion_letter definition


-- smart_collections.couple_cloud_date definition


-- smart_collections.couple_safety_ping definition


-- smart_collections.couple_reunion_log definition


-- smart_collections.couple_security_bank definition


-- smart_collections.couple_decade_pact definition


-- smart_collections.couple_vision_card definition


-- smart_collections.couple_oath definition


-- smart_collections.couple_trust_coin definition


-- smart_collections.couple_self_contract definition


-- smart_collections.couple_pet definition


-- smart_collections.couple_survey_answer definition


-- smart_collections.couple_quiz_duel definition


-- smart_collections.couple_love_word definition


-- smart_collections.couple_blind_pick definition


-- smart_collections.couple_sweet_battle definition


-- smart_collections.couple_sweet_line definition


-- smart_collections.couple_art_gallery definition


-- smart_collections.couple_dream definition


-- smart_collections.couple_food_note definition


-- smart_collections.couple_partner_fact definition


-- smart_collections.couple_sos_ping definition


-- smart_collections.couple_daily_three definition


-- smart_collections.couple_custom_badge definition


-- smart_collections.couple_habit_streak definition


-- smart_collections.couple_thanks_note definition


-- smart_collections.couple_feel_log definition


-- smart_collections.couple_weekly_star definition


-- smart_collections.couple_read_minute definition


-- smart_collections.couple_delay_task definition


-- smart_collections.couple_praise_bank definition


-- smart_collections.couple_poem_chain definition


-- smart_collections.couple_poem_3line definition


-- smart_collections.couple_morning_note definition


-- smart_collections.couple_drift_bottle definition


-- smart_collections.couple_cipher_note definition


-- smart_collections.couple_soul_answer definition


-- smart_collections.couple_journal definition


-- smart_collections.couple_love_lang definition


-- smart_collections.couple_heart_flash definition


-- smart_collections.couple_what_if definition


-- smart_collections.couple_secret_signal definition


-- smart_collections.couple_sync_tap definition


-- smart_collections.couple_heart_day definition



-- smart_collections.couple_family_meeting definition

-- F180 家庭会议纪要：每周议题+决议+跟进日，可关闭

-- smart_collections.couple_week_host definition


-- smart_collections.couple_skill_swap definition


-- smart_collections.couple_month_review definition


-- smart_collections.couple_emergency_card definition


-- smart_collections.couple_month_snapshot definition


-- smart_collections.couple_point_ledger definition



-- smart_collections.couple_five_year_plan definition


-- smart_collections.couple_anniv_plan definition



-- smart_collections.couple_doc_scene definition

-- F190 恋爱纪录片分镜：把一段回忆写成三幕剧本

-- smart_collections.couple_exhibit definition


-- smart_collections.couple_hidden_achievement definition


-- smart_collections.couple_house_rule definition


-- smart_collections.couple_dnd_setting definition

-- smart_collections.couple_user_pin definition


-- smart_collections.couple_dine_ticket definition

-- smart_collections.couple_dine_rate definition
-- smart_collections.couple_dine_nogo definition
-- smart_collections.couple_dine_weekplan definition
-- smart_collections.couple_dine_homecook definition
-- smart_collections.couple_dine_cart definition
-- smart_collections.couple_dine_topic definition

-- smart_collections.couple_cozy_lightout definition
-- smart_collections.couple_cozy_sleep definition
-- smart_collections.couple_cozy_sheep definition
-- smart_collections.couple_cozy_water definition
-- smart_collections.couple_cozy_weather definition
-- smart_collections.couple_cozy_latenight definition
-- smart_collections.couple_cozy_slow definition
-- smart_collections.couple_cozy_remedy definition
-- smart_collections.couple_cozy_hug definition

-- smart_collections.couple_ceremony_founded definition
-- smart_collections.couple_ceremony_ritual definition
-- smart_collections.couple_ceremony_mark definition
-- smart_collections.couple_ceremony_policy definition
-- smart_collections.couple_ceremony_renew definition
-- smart_collections.couple_ceremony_coupon definition

-- smart_collections.couple_ceremony_recap definition
-- smart_collections.couple_board_role definition
-- smart_collections.couple_board_vote definition
-- smart_collections.couple_board_report definition
-- smart_collections.couple_board_salary definition
-- smart_collections.couple_board_idea definition
-- smart_collections.couple_board_attend definition
-- smart_collections.couple_term_check definition
-- smart_collections.couple_term_ritual definition
-- smart_collections.couple_lucky_day definition
-- smart_collections.couple_festival_plan definition
-- smart_collections.couple_term_note definition
-- smart_collections.couple_holiday_wish definition
-- smart_collections.couple_normal_day definition
-- smart_collections.couple_listen_slot definition
-- smart_collections.couple_proxy_word definition
-- smart_collections.couple_misrewind definition
-- smart_collections.couple_stuck_q definition
-- smart_collections.couple_swap_letter definition
-- smart_collections.couple_hold_word definition
-- smart_collections.couple_three_line definition
-- smart_collections.couple_tone_note definition
-- smart_collections.couple_truce definition
-- smart_collections.couple_name_day definition
-- smart_collections.couple_spin_task definition

-- smart_collections.couple_shop_item definition
-- smart_collections.couple_stock definition
-- smart_collections.couple_parcel definition
-- smart_collections.couple_wake_word definition
-- smart_collections.couple_medicine definition
-- smart_collections.couple_standup definition
-- smart_collections.couple_advance definition
-- smart_collections.couple_grocery definition
-- smart_collections.couple_home_check definition
-- smart_collections.couple_codex_entry definition
-- smart_collections.couple_quiz_show definition
-- smart_collections.couple_top_list definition
-- smart_collections.couple_top_guess definition
-- smart_collections.couple_petname_story definition
-- smart_collections.couple_exam definition
-- smart_collections.couple_place definition
-- smart_collections.couple_first_look definition
-- smart_collections.couple_habit_map definition
-- smart_collections.couple_taste_shift definition
-- smart_collections.couple_type_report definition
-- smart_collections.couple_post_oath definition
-- smart_collections.couple_bucket definition
-- smart_collections.couple_bucket_step definition
-- smart_collections.couple_someday definition
-- smart_collections.couple_dream_home definition
-- smart_collections.couple_retire_plan definition
-- smart_collections.couple_well_qa definition
-- smart_collections.couple_relay_capsule definition
-- smart_collections.couple_dream_case definition
-- smart_collections.couple_anniv_wish definition
-- smart_collections.couple_future_credit definition
-- smart_collections.couple_role_day definition
-- smart_collections.couple_swap_diary definition
-- smart_collections.couple_master_day definition
-- smart_collections.couple_booth_note definition
-- smart_collections.couple_private_ref definition
-- smart_collections.couple_act_award definition
-- smart_collections.couple_if_family definition
-- smart_collections.couple_role_movie definition
-- smart_collections.couple_service_ticket definition
-- smart_collections.couple_body_metric definition
-- smart_collections.couple_body_snore definition
-- smart_collections.couple_body_cycle definition
-- smart_collections.couple_body_quit definition
-- smart_collections.couple_body_fit definition
-- smart_collections.couple_body_sos definition
-- smart_collections.couple_body_redline definition
-- smart_collections.couple_body_checkup definition
-- smart_collections.couple_body_med definition
-- smart_collections.couple_body_oath definition
-- smart_collections.couple_repair_freeze definition
-- smart_collections.couple_sorry_review definition
-- smart_collections.couple_repair_redo definition
-- smart_collections.couple_rebuild_plan definition
-- smart_collections.couple_repair_makeup definition
-- smart_collections.couple_bottom_line definition
-- smart_collections.couple_admit_log definition
-- smart_collections.couple_repair_box definition
-- smart_collections.couple_peace_line definition
-- smart_collections.couple_world_visit definition
-- smart_collections.couple_world_gift definition
-- smart_collections.couple_world_friend_view definition
-- smart_collections.couple_world_declare definition
-- smart_collections.couple_world_caption definition
-- smart_collections.couple_world_city_plan definition
-- smart_collections.couple_world_relatives_q definition
-- smart_collections.couple_world_vow definition
-- smart_collections.couple_world_group_report definition
-- smart_collections.couple_world_apology definition
-- smart_collections.couple_legacy_ten definition
-- smart_collections.couple_legacy_audit definition
-- smart_collections.couple_legacy_speech definition
-- smart_collections.couple_legacy_fx definition
-- smart_collections.couple_legacy_brand definition
-- smart_collections.couple_legacy_review definition
-- smart_collections.couple_legacy_item definition
-- smart_collections.couple_legacy_draw definition
-- smart_collections.couple_echo_deed definition

-- smart_collections.couple_echo_juice definition
-- smart_collections.couple_echo_refill_log definition
-- smart_collections.couple_echo_slow definition
-- smart_collections.couple_echo_highlight definition
-- smart_collections.couple_echo_receipt definition
-- smart_collections.couple_echo_battery definition
-- smart_collections.couple_echo_self_letter definition
-- smart_collections.couple_focus_night definition
-- smart_collections.couple_focus_slot definition
-- smart_collections.couple_focus_queue definition
-- smart_collections.couple_focus_meal definition
-- smart_collections.couple_focus_gaze definition
-- smart_collections.couple_focus_unplug definition
-- smart_collections.couple_focus_nudge definition
-- smart_collections.couple_focus_detox definition
-- smart_collections.couple_quest_battle definition
-- F371 出关战报：一战一报 WIN/LOSE/SURVIVE，对方按结果盖章

-- smart_collections.couple_quest_report definition
-- F372 加班预报：今晚加班到几点，TA 可留一张「到家灯给你留着」卡

-- smart_collections.couple_quest_overtime definition

-- F373 生病陪护单：TA 生病开单，喝水/吃药由陪护人代记，痊愈日关单庆典

-- smart_collections.couple_quest_nurse definition
-- F373 陪护打卡：喝水/吃药由陪护人代记，一天每种只记一次

-- smart_collections.couple_quest_care_mark definition
-- F374 考试周静音舱：TA 入舱至某日，我只发加油卡（每日 ≤1），出舱提醒补长信

-- smart_collections.couple_quest_pod definition
-- F375 搬家互助：8 个打包区块分工认领 + 纸箱计数

-- smart_collections.couple_quest_move definition
-- F375 新家第一晚：双人才算庆祝打卡

-- smart_collections.couple_quest_move_night definition
-- F376 低谷通行证：TA 宣布最近状态不好（7-30 天），对方每日一张「不说话也行」卡

-- smart_collections.couple_quest_valley definition
-- F377 小胜利账本：每天记一件做成的小事，周日互颁「小赢奖」

-- smart_collections.couple_quest_win definition
-- F379 下次关卡预约：未来 60 天已知关口挂双人时间轴，对方点「我会到场」

-- smart_collections.couple_quest_upcoming definition
-- smart_collections.couple_catch_wish definition
-- F381 雷区探测器：提前挂出易吵话题+我的雷点+安全说法，对方盖「已知晓」

-- smart_collections.couple_catch_mine definition
-- F382 安全词：双方各约一个暂停词

-- smart_collections.couple_catch_safeword definition

-- F382 安全词使用记录：哪天用的+事后一句复盘（一天一人一次）

-- smart_collections.couple_catch_safeword_use definition

-- F383 敏感日历：给 TA 的敏感日提前标注+当天想被怎样对待，前 1 天提醒我

-- smart_collections.couple_catch_sensitive definition
-- F384 「说到哪了」：被打断的话题存档，续完销档，在途每人 ≤5

-- smart_collections.couple_catch_thread definition
-- F385 真话翻译机：本人申报口是心非词条，对方只见结果不可改

-- smart_collections.couple_catch_say definition
-- F386 聆听方式协议：各写「我难过时要的是」五选一+补充说明

-- smart_collections.couple_catch_protocol definition
-- F387 话题许愿池：希望我们多聊 XX，对方接单，一周内聊完+一句感想

-- smart_collections.couple_catch_topic definition
-- F388 今日一句话：每天给对方留一句想说的话（≤40 字）

-- smart_collections.couple_catch_daily definition
-- smart_collections.couple_laugh_moment definition
-- F391 每日一逗：每天一方负责逗笑（按周轮换），对方判笑/没笑/强撑

-- smart_collections.couple_laugh_daily definition
-- F392 冷笑话结冰榜：互发冷笑话，对方判「结冰」，年度结冰最多者封冷场之王

-- smart_collections.couple_laugh_joke definition
-- F393 尴尬回收站：社死时刻提交，对方盖「抱抱你」章，365 天后读时转成好笑的事

-- smart_collections.couple_laugh_cringe definition
-- F394 快乐突袭：突发一串夸奖/一个梗/一段回忆杀，对方「中弹」盖章，一天一突袭

-- smart_collections.couple_laugh_attack definition
-- F395 笑点默契考：同一个梗，两人各自预判对方笑不笑，预判一致算默契

-- smart_collections.couple_laugh_guess definition
-- F396 大笑处方：对方低落时开处方指定翻某条笑点/尴尬/突袭，对方「已服用」回执

-- smart_collections.couple_laugh_rx definition
-- F397 幽默风格图鉴：自评+互评幽默类型，差异出相处建议

-- smart_collections.couple_laugh_style definition


-- smart_collections.couple_streak_day definition（V52 建为 couple_bond_day，V53 改名）

CREATE TABLE `couple_streak_day` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `day` varchar(10) NOT NULL COMMENT '打卡日期（yyyy-MM-dd，自然日）',
    `source` varchar(10) NOT NULL DEFAULT 'AUTO' COMMENT '确认方式：AUTO 双方当天都答完每日一问（或空间建立当天） / MAKEUP 补签',
    `operator_user` varchar(50) DEFAULT NULL COMMENT '补签操作人（AUTO 时为 NULL）',
    `created` bigint(20) NOT NULL COMMENT '确认时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple9_streak_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='互动打卡日表：一行=那一天双方共同活跃（都答完每日一问）或补签成功，连续天数与七档解锁由本表的 day 集合算出';


-- smart_collections.couple_question_answer definition（V52）

CREATE TABLE `couple_question_answer` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `day` varchar(10) NOT NULL COMMENT '问题日期（yyyy-MM-dd，同空间同天同题）',
    `question_index` int(11) NOT NULL COMMENT '题库下标（按空间+天稳定哈希得到，落库留快照）',
    `question` varchar(200) NOT NULL COMMENT '题目原文（当日快照，题库后续扩充不影响历史）',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '回答人用户名（区分大小写）',
    `answer` varchar(300) NOT NULL COMMENT '回答内容（当天可改写）',
    `created` bigint(20) NOT NULL COMMENT '首次回答时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近改写时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple8_question_day_user` (`space_id`,`day`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日一问回答表：每人每天一行，双方都答过之后才互相可见';


-- smart_collections.couple_wish definition（V52）

CREATE TABLE `couple_wish` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '许愿人用户名（想要这个东西的人）',
    `creator_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（可以不是许愿人本人）',
    `title` varchar(80) NOT NULL COMMENT '想要的东西（一句话，80 字内）',
    `note` varchar(200) DEFAULT NULL COMMENT '补充说明（款式/尺码/什么时候想要）',
    `status` varchar(16) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 待实现 / PREPARED 对方已偷偷准备（仅标记人可见） / FULFILLED 已实现',
    `prepared_by` varchar(50) DEFAULT NULL COMMENT '谁标记的「已准备」（只有对方能标自己许的愿）',
    `prepared_at` bigint(20) DEFAULT NULL COMMENT '标记已准备的时间（毫秒时间戳）',
    `fulfilled_at` bigint(20) DEFAULT NULL COMMENT '愿望实现的时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近修改时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple8_wish_title` (`space_id`,`owner_user`,`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='愿望清单表：双方互相添加想要的东西，PREPARED 状态对被许愿人保密直到本人确认实现';