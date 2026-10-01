-- 批次三十 F340-F349 传世系统（年度与资产化收官）
-- 涉及表：couple_legacy_ten / couple_legacy_audit / couple_legacy_speech / couple_legacy_fx
--         couple_legacy_brand / couple_legacy_review / couple_legacy_item / couple_legacy_draw
-- F343 里程碑倒推、F349 空间等级为读时计算无表
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空/枚举位用普通 varchar；uk/idx 名一律带本模块前缀

-- F340 年度十问：每年固定十问双答，跨年看变化
CREATE TABLE IF NOT EXISTS `couple_legacy_ten` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `year` varchar(4) NOT NULL COMMENT '年份 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '答题的人（区分大小写）',
    `answers` varchar(1500) NOT NULL DEFAULT '' COMMENT '十答 CSV（每条≤140 字）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_legacy_ten` (`space_id`, `year`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F340 年度十问';

-- F341 记忆库年审：最想删/最想留各限 3 条
CREATE TABLE IF NOT EXISTS `couple_legacy_audit` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `year` varchar(4) NOT NULL COMMENT '年审年份 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提交意见的人（区分大小写）',
    `keep_three` varchar(300) NOT NULL DEFAULT '' COMMENT '最想留 CSV ≤3 条',
    `delete_three` varchar(300) NOT NULL DEFAULT '' COMMENT '最想删 CSV ≤3 条',
    `note` varchar(140) NOT NULL DEFAULT '' COMMENT '给这段记忆的一句话',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_legacy_audit` (`space_id`, `year`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F341 记忆库年审';

-- F342 续约发布会：年度发言稿 + 对方按评分卡打分
CREATE TABLE IF NOT EXISTS `couple_legacy_speech` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `year` varchar(4) NOT NULL COMMENT '发布年份 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发言的人（区分大小写）',
    `text` varchar(600) NOT NULL DEFAULT '' COMMENT '发言稿',
    `score` int(11) DEFAULT NULL COMMENT '对方给的发布分 1-5',
    `score_note` varchar(140) NOT NULL DEFAULT '' COMMENT '评分卡的评语',
    `rated_by` varchar(50) NOT NULL DEFAULT '' COMMENT '打分的人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_legacy_speech` (`space_id`, `year`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F342 续约发布会（F235 续约是 100 天短句）';

-- F344 恋爱汇率：三种心意的兑换比，年末趣味结算
CREATE TABLE IF NOT EXISTS `couple_legacy_fx` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '报汇率的人（区分大小写）',
    `kiss_to_hug` int(11) NOT NULL DEFAULT 5 COMMENT '1 个亲亲 = 几个抱抱',
    `hug_to_word` int(11) NOT NULL DEFAULT 3 COMMENT '1 个抱抱 = 句夸夸',
    `settled_year` varchar(4) NOT NULL DEFAULT '' COMMENT '已结算年份，空=没结',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_legacy_fx` (`space_id`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F344 恋爱汇率';

-- F345 情侣品牌：关系命名 + slogan + 产品简介
CREATE TABLE IF NOT EXISTS `couple_legacy_brand` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '关系名',
    `slogan` varchar(60) NOT NULL DEFAULT '' COMMENT 'slogan',
    `intro` varchar(300) NOT NULL DEFAULT '' COMMENT '产品简介',
    `published` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=已发布到空间头部',
    `by_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '编辑的人（区分大小写）',
    `confirmed_by` varchar(50) NOT NULL DEFAULT '' COMMENT '对方确认的人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_legacy_brand` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F345 情侣品牌（F11 个性化是主题皮肤）';

-- F346 我们的一年：一键年度盘点（真数字，非套话）
CREATE TABLE IF NOT EXISTS `couple_legacy_review` (`id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `year` varchar(4) NOT NULL COMMENT '盘点年份 yyyy',
    `content` varchar(1500) NOT NULL DEFAULT '' COMMENT '年度盘点正文',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '生成的人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_legacy_review` (`space_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F346 我们的一年（F85/F99 是单域年报）';

-- F347 传世清单：想留给 TA 的东西（地点/口令类），双签封存
CREATE TABLE IF NOT EXISTS `couple_legacy_item` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `item` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '条目名（同空间唯一）',
    `kind` varchar(10) NOT NULL DEFAULT 'THING' COMMENT 'PLACE PASSWORD THING WORD（可空位不写 charset）',
    `detail` varchar(200) NOT NULL DEFAULT '' COMMENT '在哪/怎么用',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '交给谁之前由谁保管（区分大小写）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN SEALED（可空位不写 charset）',
    `signed_by` varchar(50) NOT NULL DEFAULT '' COMMENT '第二签字的人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_legacy_item` (`space_id`, `item`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F347 传世清单（F84 胶囊是信，此为清单）';

-- F348 周年抽奖箱：奖池来自当年迷你愿望，周年当天双方各抽一次
CREATE TABLE IF NOT EXISTS `couple_legacy_draw` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `year` varchar(4) NOT NULL COMMENT '抽奖年份 yyyy',
    `prize_a` varchar(80) NOT NULL DEFAULT '' COMMENT 'userA 的奖',
    `prize_b` varchar(80) NOT NULL DEFAULT '' COMMENT 'userB 的奖',
    `drawn_a` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=A 已抽',
    `drawn_b` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=B 已抽',
    `notified` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=周年提醒已发（读时惰性结算，不建 Job）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_legacy_draw` (`space_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F348 周年抽奖箱';
