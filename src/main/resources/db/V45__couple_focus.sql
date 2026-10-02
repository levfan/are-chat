-- 批次三十二 F360-F369 注意力保护区（把不被手机抢走的注意力还给彼此）
-- 涉及表：couple_focus_night / couple_focus_slot / couple_focus_queue / couple_focus_meal
--         couple_focus_gaze / couple_focus_unplug / couple_focus_nudge / couple_focus_detox
-- F367 专注周报、F369 注意力年报为读时聚合，无新表
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空/枚举位用普通 varchar；uk/idx 名一律带 focus 前缀（全库唯一已查重）
-- 双人列口径：沿用 couple_space 的 A/B 口径（A=字典序小者），minutes_a/count_a/tick_a 属 userA，_b 属 userB，
--             服务层按 mine = (me == userA) 读自己那一列；「双方都报」= 两列都非空。
-- 时间戳统一毫秒 bigint；主键 36 位 UUID 字符串

-- F360 专注打卡：每晚自报「今晚放下手机陪了 TA 多少分钟」（0-180 钳制），双方都报当夜才点亮
CREATE TABLE IF NOT EXISTS `couple_focus_night` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '打卡日 yyyy-MM-dd',
    `minutes_a` int(11) DEFAULT NULL COMMENT 'userA 报的专注分钟 0-180（服务层钳制；NULL=还没报）',
    `minutes_b` int(11) DEFAULT NULL COMMENT 'userB 报的专注分钟 0-180（服务层钳制；NULL=还没报）',
    `note_a` varchar(80) NOT NULL DEFAULT '' COMMENT 'userA 的一句话（≤40 字，宽度放宽）',
    `note_b` varchar(80) NOT NULL DEFAULT '' COMMENT 'userB 的一句话（≤40 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_focus_night` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F360 专注打卡（双方都报当夜点亮）';

-- F361 专属时段：每周预约一段「只属于我们」（对方确认才生效，一天一次）
CREATE TABLE IF NOT EXISTS `couple_focus_slot` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '周锚=该周周一 yyyy-MM-dd（uk 占位，每周一格）',
    `day` varchar(10) NOT NULL COMMENT '时段落在周内哪一天 yyyy-MM-dd（服务层校验必须在本周内）',
    `title` varchar(180) NOT NULL DEFAULT '' COMMENT '这段时间想做什么（≤60 字，宽度放宽）',
    `hours` int(11) NOT NULL DEFAULT 2 COMMENT '时长 1-6 小时（服务层钳制，默认 2）',
    `proposed_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提议人（区分大小写）',
    `confirmer` varchar(50) DEFAULT NULL COMMENT '确认人=对方（不能自确认；NULL=还没确认）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_focus_slot` (`space_id`, `week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F361 专属时段（每周一格，对方确认生效）';

-- F362 攒一句话：对方专注/勿扰时留言排队（在途每人 ≤5），读时批量置已读
CREATE TABLE IF NOT EXISTS `couple_focus_queue` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '留言的人（区分大小写）',
    `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收留言的人=对方（区分大小写）',
    `content` varchar(240) NOT NULL DEFAULT '' COMMENT '攒下来的那句话（≤80 字，宽度放宽）',
    `read_at` bigint(20) DEFAULT NULL COMMENT '已读时间毫秒（NULL=还在排队）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_focus_queue` (`space_id`, `to_user`, `read_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F362 攒一句话（在途每人 ≤5）';

-- F363 饭桌不低头：吃饭手机倒扣 20 分钟打卡，双方各点各的，双点=同桌成功
CREATE TABLE IF NOT EXISTS `couple_focus_meal` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '打卡日 yyyy-MM-dd',
    `tick_a` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userA 是否点过（1=点了，各点各的）',
    `tick_b` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userB 是否点过（1=点了，各点各的）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_focus_meal` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F363 饭桌不低头（双点=同桌成功）';

-- F364 对视十秒：每天一次对视打卡，双方各点各的，双点点亮
CREATE TABLE IF NOT EXISTS `couple_focus_gaze` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '打卡日 yyyy-MM-dd',
    `tick_a` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userA 是否点过（1=点了）',
    `tick_b` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userB 是否点过（1=点了）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_focus_gaze` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F364 对视十秒（双点点亮）';

-- F365 不插电半小时：睡前 30 分钟无手机共同打卡，周连击读时算
CREATE TABLE IF NOT EXISTS `couple_focus_unplug` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '打卡日 yyyy-MM-dd',
    `tick_a` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userA 是否点过（1=点了）',
    `tick_b` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userB 是否点过（1=点了）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_focus_unplug` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F365 不插电半小时（双点成功，周连击读时算）';

-- F366 走神温柔哨：每人每天 ≤2 张「回来啦」卡（超出 400），note 可空
CREATE TABLE IF NOT EXISTS `couple_focus_nudge` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '递卡日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '递卡的人（区分大小写，每人每天 ≤2 张）',
    `note` varchar(80) NOT NULL DEFAULT '' COMMENT '一句话，可空（≤40 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_focus_nudge` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F366 走神温柔哨（每人每天 ≤2 张）';

-- F368 数字排毒半天：AM/PM 二选一发起半日无手机挑战，双方各报各的，双报=达成
CREATE TABLE IF NOT EXISTS `couple_focus_detox` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '挑战日 yyyy-MM-dd',
    `kind` varchar(2) NOT NULL COMMENT 'AM=上半天 / PM=下半天（服务层校验，只此两种）',
    `tick_a` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userA 是否应战（1=应了）',
    `tick_b` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userB 是否应战（1=应了）',
    `confirmed_by` varchar(50) DEFAULT NULL COMMENT '最先应战的人（区分大小写，NULL=还没人应战）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_focus_detox` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F368 数字排毒半天（双方都报=达成）';
