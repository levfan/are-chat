-- 批次三十五 F390-F399 欢笑银行（幽默是关系的复利）
-- 涉及表：couple_laugh_moment / couple_laugh_daily / couple_laugh_joke / couple_laugh_cringe
--         couple_laugh_attack / couple_laugh_guess / couple_laugh_rx / couple_laugh_style
-- F398 欢乐周报、F399 年度欢笑榜为读时聚合，无新表
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空/枚举位用普通 varchar；uk/idx 名一律带 laugh 前缀（全库唯一已查重）
-- 双人列口径：沿用 couple_space 的 A/B 口径（A=字典序小者），tick_a/tick_b 属 userA/userB；
--             「笑没笑/结没结冰/中没中弹」这类判定一律记 judged_by/healed_by/hit_by 用户名，不拆双列，
--             因为一逗一日一格、冷笑话一人一条，判定人只可能是对方。
-- 时间戳统一毫秒 bigint；主键 36 位 UUID 字符串

-- F390 笑点存档：笑到肚子疼的时刻，对方可补现场证词
CREATE TABLE IF NOT EXISTS `couple_laugh_moment` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '发生的日子 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记账的人（区分大小写）',
    `title` varchar(120) NOT NULL COMMENT '这条笑点叫什么（≤30 字，宽度放宽）',
    `culprit` varchar(50) NOT NULL DEFAULT '' COMMENT '谁干的（写用户名或外号，≤20 字，宽度放宽）',
    `scene` varchar(400) NOT NULL DEFAULT '' COMMENT '现场还原（≤100 字，宽度放宽）',
    `fun_level` int(11) NOT NULL DEFAULT 3 COMMENT '好笑度 1-5（服务层钳制）',
    `witness` varchar(400) NOT NULL DEFAULT '' COMMENT '对方的现场证词（≤100 字，宽度放宽，空=还没补）',
    `witness_by` varchar(50) DEFAULT NULL COMMENT '补证词的人=对方（NULL=没补）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_laugh_moment` (`space_id`, `day`, `from_user`, `title`),
    KEY `idx_laugh_moment_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F390 笑点存档（对方可补现场证词）';

-- F391 每日一逗：每天一方负责逗笑（按周轮换），对方判笑/没笑/强撑
CREATE TABLE IF NOT EXISTS `couple_laugh_daily` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪一天 yyyy-MM-dd（一天一格）',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '今天负责逗的人（服务层按周轮换算出，抢班 400）',
    `content` varchar(400) NOT NULL DEFAULT '' COMMENT '逗的内容（≤100 字，宽度放宽）',
    `verdict` varchar(10) NOT NULL DEFAULT '' COMMENT '对方判分 HAPPY 笑了/FLAT 没笑/FAKE 强撑（空=还没判）',
    `judged_by` varchar(50) DEFAULT NULL COMMENT '判分的人=对方（NULL=还没判）',
    `judged_at` bigint(20) DEFAULT NULL COMMENT '判分时间毫秒',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_laugh_daily` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F391 每日一逗（周轮换，对方判分）';

-- F392 冷笑话结冰榜：互发冷笑话，对方判「结冰」，年度结冰最多者封冷场之王
CREATE TABLE IF NOT EXISTS `couple_laugh_joke` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '发出日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '讲冷笑话的人（区分大小写）',
    `content` varchar(320) NOT NULL COMMENT '冷笑话正文（≤80 字，宽度放宽）',
    `frozen` int(11) NOT NULL DEFAULT 0 COMMENT '是否判为结冰（1=结冰，0=没冰，NULL 语义用 0；未判也存 0 靠 judged_by 区分）',
    `judged_by` varchar(50) DEFAULT NULL COMMENT '判的人=对方（NULL=还没判，一条只判一次）',
    `judged_at` bigint(20) DEFAULT NULL COMMENT '判分时间毫秒',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_laugh_joke` (`space_id`, `from_user`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F392 冷笑话结冰榜（对方判结冰，一条只判一次）';

-- F393 尴尬回收站：社死时刻提交，对方盖「抱抱你」章，365 天后读时转成好笑的事
CREATE TABLE IF NOT EXISTS `couple_laugh_cringe` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '社死日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '经历社死的人（区分大小写）',
    `content` varchar(400) NOT NULL COMMENT '社死现场（≤100 字，宽度放宽）',
    `healed_by` varchar(50) DEFAULT NULL COMMENT '盖「抱抱你」章的人=对方（NULL=还没盖）',
    `healed_at` bigint(20) DEFAULT NULL COMMENT '盖章时间毫秒',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_laugh_cringe` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F393 尴尬回收站（365 天后读时结算）';

-- F394 快乐突袭：突发一串夸奖/一个梗/一段回忆杀，对方「中弹」盖章，一天一突袭
CREATE TABLE IF NOT EXISTS `couple_laugh_attack` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '突袭日 yyyy-MM-dd（每人每天一次）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发动突袭的人（区分大小写）',
    `kind` varchar(12) NOT NULL DEFAULT 'PRAISE' COMMENT '类型 PRAISE 一串夸奖/MEME 一个梗/MEMORY 一段回忆杀（服务层白名单）',
    `content` varchar(400) NOT NULL DEFAULT '' COMMENT '突袭内容（≤100 字，宽度放宽）',
    `hit_by` varchar(50) DEFAULT NULL COMMENT '中弹盖章的人=对方（NULL=还没中弹）',
    `hit_at` bigint(20) DEFAULT NULL COMMENT '中弹时间毫秒',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_laugh_attack` (`space_id`, `from_user`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F394 快乐突袭（一天一突袭，对方中弹盖章）';

-- F395 笑点默契考：同一个梗，两人各自预判对方笑不笑，预判一致算默契
CREATE TABLE IF NOT EXISTS `couple_laugh_guess` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `joke_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '考的是哪条冷笑话（关联 couple_laugh_joke.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '做预判的人（区分大小写）',
    `predict` int(11) NOT NULL DEFAULT 0 COMMENT '预判对方会不会笑（1=会笑，0=不会笑）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_laugh_guess` (`space_id`, `joke_id`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F395 笑点默契考（一条梗每人一票）';

-- F396 大笑处方：对方低落时开处方指定翻某条笑点/尴尬/突袭，对方「已服用」回执
CREATE TABLE IF NOT EXISTS `couple_laugh_rx` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '开处方日 yyyy-MM-dd（每人每天一张）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '开处方的人（区分大小写）',
    `target_kind` varchar(12) NOT NULL COMMENT '处方指向 MOMENT 笑点/CRINGE 尴尬/ATTACK 突袭（服务层白名单）',
    `target_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '指向的条目 id（必须是本空间的行）',
    `note` varchar(240) NOT NULL DEFAULT '' COMMENT '医嘱一句话（≤60 字，宽度放宽）',
    `taken_by` varchar(50) DEFAULT NULL COMMENT '已服用回执的人=收方（NULL=还没服）',
    `taken_at` bigint(20) DEFAULT NULL COMMENT '服用时间毫秒',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_laugh_rx` (`space_id`, `from_user`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F396 大笑处方（对方已服用回执）';

-- F397 幽默风格图鉴：自评+互评幽默类型，差异出相处建议
CREATE TABLE IF NOT EXISTS `couple_laugh_style` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `about_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '评的是谁（区分大小写）',
    `rater` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '谁来评：等于 about_user 就是自评，否则互评（区分大小写）',
    `style` varchar(12) NOT NULL DEFAULT 'PUN' COMMENT '类型 PUN 谐音梗/COLD 冷幽默/SELF 自嘲/ACTION 动作派/MIME 模仿派（服务层白名单）',
    `note` varchar(240) NOT NULL DEFAULT '' COMMENT '补一句（≤60 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_laugh_style` (`space_id`, `about_user`, `rater`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F397 幽默风格图鉴（自评+互评各一行）';
