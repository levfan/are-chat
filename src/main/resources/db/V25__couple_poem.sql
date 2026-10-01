-- V25__couple_poem.sql
-- 目的：把日子写成诗·文字浪漫系（F160-F169）：情诗接龙、三行情书、醒来第一条、
--       心情漂流瓶、数字密码情书、灵魂提问盲盒、贴纸手账
--       （F167 恋爱语录机 / F168 情书模板库 / F169 手账贴纸库为无表静态或聚合）。
-- 涉及表：couple_poem_chain / couple_poem_3line / couple_morning_note /
--         couple_drift_bottle / couple_cipher_note / couple_soul_answer / couple_journal（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F160 情诗接龙：每天每人一句，连成我们的诗（今天执笔人按 space+day 稳定）
CREATE TABLE IF NOT EXISTS `couple_poem_chain` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '执笔人用户名（区分大小写）',
    `line` varchar(100) NOT NULL COMMENT '今天这一句诗',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_poem_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情诗接龙表：一天一句，把日子连成一首写不完的诗';

-- F161 三行情书：三行小诗，对方可点赞
CREATE TABLE IF NOT EXISTS `couple_poem_3line` (
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
    KEY `idx_poem_3line` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='三行情书表：最深的话，用最短的诗说';

-- F162 醒来第一条：睡前写留言，次日晨才送达对方，读后标记
CREATE TABLE IF NOT EXISTS `couple_morning_note` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写信人用户名（区分大小写）',
    `content` varchar(300) NOT NULL COMMENT '想说的那句话',
    `deliver_day` varchar(10) NOT NULL COMMENT '送达日期（写信次日 yyyy-MM-dd）',
    `read_at` bigint(20) DEFAULT NULL COMMENT '对方已读时间（毫秒，可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_morning_note` (`space_id`, `deliver_day`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='醒来第一条表：让 TA 的一天，从你的话开始';

-- F163 心情漂流瓶：坏心情写下来扔出去，对方捡到回信
CREATE TABLE IF NOT EXISTS `couple_drift_bottle` (
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
    KEY `idx_drift_bottle` (`space_id`, `status`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心情漂流瓶表：把坏情绪交给海，把回应留给爱人';

-- F164 数字密码情书：把情话藏进数字密码，对方解码
CREATE TABLE IF NOT EXISTS `couple_cipher_note` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '加密人用户名（区分大小写）',
    `cipher` varchar(500) NOT NULL COMMENT '数字密码串',
    `hint` varchar(100) DEFAULT NULL COMMENT '解码提示（可空）',
    `decoded_by` varchar(50) DEFAULT NULL COMMENT '解码人用户名（可空；H2 兼容：可空列不带 charset）',
    `decoded_at` bigint(20) DEFAULT NULL COMMENT '解码时间（毫秒，可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_cipher_note` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数字密码情书表：看懂那串数字的人，是全世界最幸运的';

-- F165 灵魂提问盲盒：每天一个深刻问题，双答才互见
CREATE TABLE IF NOT EXISTS `couple_soul_answer` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作答人用户名（区分大小写）',
    `answer` varchar(300) NOT NULL COMMENT '答案',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_soul_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='灵魂提问盲盒表：深聊一次，胜过闲聊一百次';

-- F166 贴纸手账：emoji 贴纸+一句话，每天一页可改
CREATE TABLE IF NOT EXISTS `couple_journal` (
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
