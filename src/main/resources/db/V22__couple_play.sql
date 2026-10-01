-- V22__couple_play.sql
-- 目的：趣味游戏批次（F130-F139）：一百问、出题考TA、世界情话课、周末盲选、
--       情话Battle、抽象画（F132 心动概率 / F133 塔罗 / F137 恋爱天气 / F139 骰子为无表或纯前端）。
-- 涉及表：couple_survey_answer / couple_quiz_duel / couple_love_word /
--         couple_blind_pick / couple_sweet_battle / couple_sweet_line / couple_art_gallery（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F130 一百问：两人各答 100 道小问题，答完互相可见
CREATE TABLE IF NOT EXISTS `couple_survey_answer` (
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

-- F131 出题考TA：出题人出问答题，对方作答，出题人人工判分
CREATE TABLE IF NOT EXISTS `couple_quiz_duel` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '出题人用户名（区分大小写）',
    `question` varchar(200) NOT NULL COMMENT '题目',
    `answer_text` varchar(200) DEFAULT NULL COMMENT '对方的回答（可空）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 待作答 / ANSWERED 待判分 / JUDGED 已判分',
    `verdict` varchar(10) DEFAULT NULL COMMENT '判分结果：RIGHT 答对 / WRONG 答错（出题人判定）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_quiz_duel` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出题考TA表：看看 TA 有多懂你，出题人说了算';

-- F134 世界情话课：收藏全世界的情话（今日一课由静态库按日推送）
CREATE TABLE IF NOT EXISTS `couple_love_word` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收藏人用户名（区分大小写）',
    `word` varchar(100) NOT NULL COMMENT '情话原文',
    `meaning` varchar(200) DEFAULT NULL COMMENT '含义/翻译（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_love_word` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='世界情话课表：把全世界的情话都学会，只对你说';

-- F135 周末盲选：双方各写 3 个周末愿望，双方都提交后配对结算
CREATE TABLE IF NOT EXISTS `couple_blind_pick` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提交人用户名（区分大小写）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd，每周一期）',
    `picks` varchar(300) NOT NULL COMMENT '三个周末愿望（逗号分隔）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blind_week_user` (`space_id`,`week`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='周末盲选表：把周末交给盲盒，惊喜由两个人一起出';

-- F136 情话Battle：今日擂台，双方各发一句情话，互相投票定胜负
CREATE TABLE IF NOT EXISTS `couple_sweet_battle` (
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

-- F136 情话Battle 参赛句子
CREATE TABLE IF NOT EXISTS `couple_sweet_line` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `battle_id` varchar(36) NOT NULL COMMENT '所属擂台（关联 couple_sweet_battle.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发话人用户名（区分大小写）',
    `content` varchar(100) NOT NULL COMMENT '情话内容',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_sweet_line` (`space_id`, `battle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情话Battle句子表：一句顶一万句';

-- F138 抽象画：前端按种子生成抽象画，双方互赠入馆
CREATE TABLE IF NOT EXISTS `couple_art_gallery` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作画人用户名（区分大小写）',
    `title` varchar(50) NOT NULL COMMENT '画作标题',
    `seed` int(11) NOT NULL COMMENT '画作种子（前端按种子生成抽象画）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_art_gallery` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='抽象画画廊表：我看不懂，但我大受震撼，而且很喜欢';
