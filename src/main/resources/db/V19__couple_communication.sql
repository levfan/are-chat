-- V19__couple_communication.sql
-- 目的：沟通增强批次（F100-F109）：冷静角、情绪接力棒、你比划我猜、故事接龙、
--       道歉三部曲、情绪词汇足迹。
--       F100 恋爱翻译器 / F105 词典小考 / F106 情话合成器 / F109 晚安电台
--       为静态库或聚合计算，无新表。
-- 涉及表：couple_cool_down / couple_mood_relay / couple_guess_round /
--         couple_story_line / couple_apology_card / couple_feeling_word（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F101 冷静角：吵架停战协议，任一方发起 30 分钟冷静期，结束后双方各留一句软话和好
CREATE TABLE IF NOT EXISTS `couple_cool_down` (
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
    KEY `idx_cool_down` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='冷静角表：吵架不隔夜，先冷静 30 分钟再好好说';

-- F102 情绪接力棒：把心情抛给 TA，TA 接住回应并抛回新的心情
CREATE TABLE IF NOT EXISTS `couple_mood_relay` (
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
    KEY `idx_mood_relay` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情绪接力棒表：我的心情抛给你，你接住了就换你抛回来';

-- F103 你比划我猜：一方描述（不能带原词），另一方猜，每天最多 5 轮
CREATE TABLE IF NOT EXISTS `couple_guess_round` (
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
    KEY `idx_guess_round` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='你比划我猜表：比划不能说，猜中才过瘾';

-- F104 故事接龙：轮流写一句，同一条链 chain_id = 首句 id，可完结开新篇
CREATE TABLE IF NOT EXISTS `couple_story_line` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `chain_id` varchar(36) NOT NULL COMMENT '故事链ID（= 首句的 id）',
    `seq` int(11) NOT NULL COMMENT '句序（从 1 开始）',
    `by_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写这句的人用户名（区分大小写）',
    `content` varchar(100) NOT NULL COMMENT '接龙的一句话',
    `is_final` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为完结句：0 连载 1 完结',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_story_line` (`space_id`, `chain_id`, `seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='故事接龙表：你一句我一句，我们的故事自己写';

-- F107 道歉三部曲：引导式道歉卡（我错了→错在哪→以后我会），对方收下即结
CREATE TABLE IF NOT EXISTS `couple_apology_card` (
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
    KEY `idx_apology_card` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='道歉三部曲表：好好道歉，是有勇气的浪漫';

-- F108 情绪词汇足迹：每天每人一个词形容今天的心情，攒情绪词云
CREATE TABLE IF NOT EXISTS `couple_feeling_word` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '记录日期（yyyy-MM-dd，每人每天一词，重复=修改）',
    `word` varchar(20) NOT NULL COMMENT '今天的心情词',
    `note` varchar(100) DEFAULT NULL COMMENT '想说的一句话（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_feeling_day` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情绪词汇足迹表：一天一个词，攒成我们的情绪星图';
