-- V16__couple_care_talk.sql
-- 目的：懂我与被接住批次（F60-F66/F68/F69）：求抱抱、矛盾复盘、道歉券、真心话抽签、
--       匿名树洞、心灵感应、情话储蓄罐。F63 陪聊话题卡 / F64 情绪同步率 / F65 深夜陪伴无需新表。
-- 涉及表：couple_comfort / couple_sorry_ticket / couple_peace_review / couple_truth /
--         couple_whisper / couple_telepathy / couple_love_bank（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F60 求抱抱：一键告诉 TA「我需要安慰」，对方送出抱抱和一句话回应
CREATE TABLE IF NOT EXISTS `couple_comfort` (
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
    UNIQUE KEY `uk_comfort_day` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='求抱抱表：一键求助的温柔按钮，让「需要安慰」被大声说出来';

-- F61 矛盾复盘：和好之后各写一句「我当时为什么在意」和「下次我们可以怎么做」，合成和好锦囊
CREATE TABLE IF NOT EXISTS `couple_peace_review` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '复盘日期（yyyy-MM-dd，每人每天一份）',
    `by_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '复盘人用户名（区分大小写）',
    `my_part` varchar(200) NOT NULL COMMENT '我当时为什么在意/我的那部分',
    `next_time` varchar(200) NOT NULL COMMENT '下次我们可以怎么做',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_peace_review` (`space_id`, `day`, `by_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='矛盾复盘表：和好之后的双人复盘，把争吵变成了解';

-- F62 道歉券：主动递一张「对不起券」，对方收下即代表台阶已搭好
CREATE TABLE IF NOT EXISTS `couple_sorry_ticket` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '道歉方用户名（区分大小写）',
    `note` varchar(100) NOT NULL COMMENT '道歉附言（比如 刚才语气不好，对不起）',
    `status` varchar(10) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE 有效 / USED 已收下',
    `used_note` varchar(100) DEFAULT NULL COMMENT '收下时想说的话（可空）',
    `used_at` bigint(20) DEFAULT NULL COMMENT '收下时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_sorry_ticket` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='道歉券表：把「对不起」做成一张可以递出去的券';

-- F66 真心话抽签：每天抽一道真心话，双方都要答，答完互相点亮
CREATE TABLE IF NOT EXISTS `couple_truth` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '日期（yyyy-MM-dd，每人每天可发起一次）',
    `question` varchar(100) NOT NULL COMMENT '真心话题目',
    `answerer` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '回答人用户名（区分大小写）',
    `answer` varchar(200) NOT NULL COMMENT '回答内容',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_truth_day` (`space_id`, `day`, `answerer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='真心话表：每天一题双方必答，把平时不敢问的问出来';

-- F67 匿名树洞：匿名（或实名）向 TA 提一个不敢问的问题，TA 回答后揭晓提问人
CREATE TABLE IF NOT EXISTS `couple_whisper` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提问人用户名（区分大小写，匿名时对 TA 隐藏）',
    `question` varchar(200) NOT NULL COMMENT '想问的问题',
    `anonymous` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否匿名：1 匿名（回答后揭晓）0 实名',
    `answer` varchar(300) DEFAULT NULL COMMENT 'TA 的回答（空 = 未回答）',
    `answered_at` bigint(20) DEFAULT NULL COMMENT '回答时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_whisper_space` (`space_id`, `answered_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='匿名树洞表：给不敢开口的问题一个安全入口';

-- F68 心灵感应：双方在 10 分钟内各自回答同一道题，答案一致即「感应成功」
CREATE TABLE IF NOT EXISTS `couple_telepathy` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '日期（yyyy-MM-dd）',
    `round` int(11) NOT NULL COMMENT '当天第几轮（1-3）',
    `question` varchar(100) NOT NULL COMMENT '感应题目（双方同题）',
    `answer_a` varchar(50) DEFAULT NULL COMMENT '用户 A 的回答（空 = 未答）',
    `answer_b` varchar(50) DEFAULT NULL COMMENT '用户 B 的回答（空 = 未答）',
    `created` bigint(20) NOT NULL COMMENT '发起时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_telepathy_round` (`space_id`, `day`, `round`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心灵感应表：不商量的同题作答，测一测我们有多同频';

-- F69 情话储蓄罐：平时把情话存进罐子，每天晚上随机取一句「利息」推给 TA
CREATE TABLE IF NOT EXISTS `couple_love_bank` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存入人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '情话内容',
    `delivered` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已作为利息送达：0 在罐里 1 已送达',
    `delivered_at` bigint(20) DEFAULT NULL COMMENT '送达时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_love_bank_deliver` (`space_id`, `delivered`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情话储蓄罐表：今天存下的情话，会在某个晚上变成惊喜利息';
