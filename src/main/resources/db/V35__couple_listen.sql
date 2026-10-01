-- 批次二十二 F260-F269 倾听与发声（沟通增强 2.0）
-- 涉及表：couple_listen_slot / couple_proxy_word / couple_misrewind / couple_stuck_q / couple_swap_letter
--         couple_hold_word / couple_three_line / couple_tone_note / couple_truce / couple_name_day
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列必须 NOT NULL 且不写 DEFAULT；可空/默认空的用户名位用普通 varchar

-- F260 想被听时段：OPEN→CONFIRMED→DONE，在途同时仅 1 个，双评被听感
CREATE TABLE IF NOT EXISTS `couple_listen_slot` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F260 想被听时段：申请-确认-聊完-互评';

-- F261 替我说：害羞的话写成 TA 口吻草稿，TA 改写定稿即 adopt
CREATE TABLE IF NOT EXISTS `couple_proxy_word` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F261 替我说：草稿到定稿';

-- F262 误会倒带卡：一次争执各写「我当时以为/我猜你其实想」，双份齐并排回放
CREATE TABLE IF NOT EXISTS `couple_misrewind` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F262 误会倒带：同一天同主题双方各一份';

-- F263 本周答不上来的问题：每人一题，对方作答，双答齐互见
CREATE TABLE IF NOT EXISTS `couple_stuck_q` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F263 卡壳一问：一周一题，答对方题';

-- F264 换位信：「我是你」写信封存，到开放日拆，双封齐才算交换完成
CREATE TABLE IF NOT EXISTS `couple_swap_letter` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F264 换位信：封存到日互拆';

-- F265 早想说：封存队列每 7 天自动放行一句送达（读时惰性结算）
CREATE TABLE IF NOT EXISTS `couple_hold_word` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F265 早想说：7 天一放行';

-- F266 三行打卡：今日印象/谢一件/夸一件，连续 21 天解锁纪念
CREATE TABLE IF NOT EXISTS `couple_three_line` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F266 每天三行：一人一天一条可改写';

-- F267 语气翻译官：消息太冷时自报语气，对方看到翻译条（一人一天一条）
CREATE TABLE IF NOT EXISTS `couple_tone_note` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '语气日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '自报人（区分大小写）',
    `tone` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'TIRED BUSY SAD OKAY',
    `note` varchar(60) NOT NULL DEFAULT '' COMMENT '补一句',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tone_day` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F267 语气自报：给冷淡加字幕';

-- F268 休战旗：举旗冻结（默认 30 分钟），到点双方各选继续/算了；在途仅一面旗
CREATE TABLE IF NOT EXISTS `couple_truce` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F268 休战旗：吵架急停 30 分钟';

-- F269 称呼日：今日爱称 Bank 抽取，双方各「用过一次」完成当日
CREATE TABLE IF NOT EXISTS `couple_name_day` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '称呼日 yyyy-MM-dd',
    `name_text` varchar(40) NOT NULL DEFAULT '' COMMENT '今日称呼（Bank 存档）',
    `used_a` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userA 用过了',
    `used_b` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userB 用过了',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F269 称呼日：日抛爱称双人用完';
