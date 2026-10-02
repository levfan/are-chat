-- 批次三十四 F380-F389 聆听者（你随口说的都算数）
-- 涉及表：couple_catch_wish / couple_catch_mine / couple_catch_safeword / couple_catch_safeword_use
--         couple_catch_sensitive / couple_catch_thread / couple_catch_say / couple_catch_protocol
--         couple_catch_topic / couple_catch_daily
-- F389 聆听者年报为读时聚合，无新表
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空/枚举位用普通 varchar；uk/idx 名一律带 catch 前缀（全库唯一已查重）
-- 规格偏差记账：F384 原文写 uk(space,from_user,status)，但同一行又要求「在途每人 ≤5」——
--             同一 (space,from_user,status=OPEN) 允许多行，唯一键与限流条数直接矛盾（第 2 条在途就 400 了）。
--             本脚本按 idx_catch_thread 建普通索引，重复内容另由服务层按 (space,from_user,topic) 查重挡。
-- 时间戳统一毫秒 bigint；主键 36 位 UUID 字符串

-- F380 暗中心愿本：偷偷记 TA 随口提过的想要的，兑现登记后才揭晓
CREATE TABLE IF NOT EXISTS `couple_catch_wish` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '心愿的主人=被记的那位（区分大小写，对 TA 保密）',
    `recorder_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记账的人=另一方（区分大小写）',
    `content` varchar(240) NOT NULL COMMENT 'TA 随口说想要的（≤60 字，宽度放宽）',
    `source_day` varchar(10) NOT NULL DEFAULT '' COMMENT '出处日期 yyyy-MM-dd（TA 是什么时候说的，揭晓时引用）',
    `scene` varchar(160) NOT NULL DEFAULT '' COMMENT '在什么场合说的（≤40 字，宽度放宽）',
    `fulfilled` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否已兑现登记（1=已给）',
    `revealed_at` bigint(20) DEFAULT NULL COMMENT '揭晓时间毫秒（NULL=还保密，兑现登记时才写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_catch_wish` (`space_id`, `owner_user`, `content`),
    KEY `idx_catch_wish_owner` (`space_id`, `owner_user`, `revealed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F380 暗中心愿本（兑现才揭晓）';

-- F381 雷区探测器：提前挂出易吵话题+我的雷点+安全说法，对方盖「已知晓」
CREATE TABLE IF NOT EXISTS `couple_catch_mine` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '挂雷的人（区分大小写）',
    `topic` varchar(120) NOT NULL COMMENT '易吵话题（≤30 字，宽度放宽）',
    `trip` varchar(240) NOT NULL DEFAULT '' COMMENT '我的雷点在哪（≤60 字，宽度放宽）',
    `safe_way` varchar(240) NOT NULL DEFAULT '' COMMENT '安全的说法/做法（≤60 字，宽度放宽）',
    `ack_by` varchar(50) DEFAULT NULL COMMENT '知晓盖章的人=对方（NULL=还没盖）',
    `ack_at` bigint(20) DEFAULT NULL COMMENT '盖章时间毫秒',
    `avoided` int(11) NOT NULL DEFAULT 0 COMMENT '成功避雷次数（对方主动记，年报用）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_catch_mine` (`space_id`, `from_user`, `topic`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F381 雷区探测器（对方盖已知晓）';

-- F382 安全词：双方各约一个暂停词
CREATE TABLE IF NOT EXISTS `couple_catch_safeword` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '约定这个词的人（区分大小写，每人一个）',
    `word` varchar(80) NOT NULL COMMENT '安全词（≤20 字，宽度放宽）',
    `note` varchar(240) NOT NULL DEFAULT '' COMMENT '用了之后希望怎样（≤60 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_catch_safeword` (`space_id`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F382 安全词约定（每人一个）';

-- F382 安全词使用记录：哪天用的+事后一句复盘（一天一人一次）
CREATE TABLE IF NOT EXISTS `couple_catch_safeword_use` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '使用日 yyyy-MM-dd',
    `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '喊了暂停的人（区分大小写）',
    `reflect` varchar(240) NOT NULL DEFAULT '' COMMENT '事后一句复盘（≤60 字，宽度放宽，可后补）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_catch_use` (`space_id`, `day`, `user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F382 安全词使用记录（一天一人一次）';

-- F383 敏感日历：给 TA 的敏感日提前标注+当天想被怎样对待，前 1 天提醒我
CREATE TABLE IF NOT EXISTS `couple_catch_sensitive` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '这个日子属于谁（区分大小写，由对方代为标注）',
    `day` varchar(10) NOT NULL COMMENT '敏感日 yyyy-MM-dd',
    `kind` varchar(16) NOT NULL DEFAULT 'OTHER' COMMENT '类型 PERIOD/CHECK/MEMORY/OTHER（服务层白名单）',
    `care` varchar(240) NOT NULL DEFAULT '' COMMENT '当天想被怎样对待（≤60 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_catch_sensitive` (`space_id`, `owner_user`, `day`, `kind`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F383 敏感日历（提前标注）';

-- F384 「说到哪了」：被打断的话题存档，续完销档，在途每人 ≤5
CREATE TABLE IF NOT EXISTS `couple_catch_thread` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存档的人（区分大小写）',
    `topic` varchar(160) NOT NULL COMMENT '话题一句话（≤40 字，宽度放宽）',
    `progress` varchar(240) NOT NULL DEFAULT '' COMMENT '说到哪了（≤60 字，宽度放宽）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN 在途 / DONE 已续完',
    `done_at` bigint(20) DEFAULT NULL COMMENT '销档时间毫秒',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_catch_thread` (`space_id`, `from_user`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F384 话题断点存档（在途每人 ≤5，故 status 不进唯一键）';

-- F385 真话翻译机：本人申报口是心非词条，对方只见结果不可改
CREATE TABLE IF NOT EXISTS `couple_catch_say` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '申报的人（区分大小写）',
    `say` varchar(80) NOT NULL COMMENT '我嘴上说的（≤20 字，宽度放宽）',
    `means` varchar(240) NOT NULL DEFAULT '' COMMENT '实际意思（≤60 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_catch_say` (`space_id`, `from_user`, `say`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F385 真话翻译词条（每人 ≤10 条，服务层限）';

-- F386 聆听方式协议：各写「我难过时要的是」五选一+补充说明
CREATE TABLE IF NOT EXISTS `couple_catch_protocol` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写协议的人（区分大小写，每人一行可改写）',
    `mode` varchar(16) NOT NULL COMMENT 'REASON 讲道理/RANT 陪骂/HUG 抱抱不说话/FOOD 递吃的/SPACE 别理我（服务层白名单）',
    `note` varchar(240) NOT NULL DEFAULT '' COMMENT '补充说明（≤60 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_catch_protocol` (`space_id`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F386 聆听方式协议（五选一）';

-- F387 话题许愿池：希望我们多聊 XX，对方接单，一周内聊完+一句感想
CREATE TABLE IF NOT EXISTS `couple_catch_topic` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '许愿的人（区分大小写）',
    `title` varchar(120) NOT NULL COMMENT '希望多聊的话题（≤30 字，宽度放宽）',
    `status` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待接 / TAKEN 已接单 / TALKED 聊完了',
    `taken_by` varchar(50) DEFAULT NULL COMMENT '接单的人=对方（NULL=没人接）',
    `taken_at` bigint(20) DEFAULT NULL COMMENT '接单时间毫秒（一周期限从此起算）',
    `talk_day` varchar(10) NOT NULL DEFAULT '' COMMENT '聊完的日期 yyyy-MM-dd',
    `reflect` varchar(240) NOT NULL DEFAULT '' COMMENT '一句感想（≤60 字，宽度放宽）',
    `overdue` tinyint(4) NOT NULL DEFAULT 0 COMMENT '接单后超一周才聊完记一笔（1=超时）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_catch_topic` (`space_id`, `from_user`, `title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F387 话题许愿池（对方接单一周内聊完）';

-- F388 今日一句话：每天给对方留一句想说的话（≤40 字）
CREATE TABLE IF NOT EXISTS `couple_catch_daily` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪一天 yyyy-MM-dd',
    `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '说话的人（区分大小写）',
    `content` varchar(160) NOT NULL DEFAULT '' COMMENT '今日一句话（≤40 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_catch_daily` (`space_id`, `day`, `user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F388 今日一句话（每人每天一句）';
