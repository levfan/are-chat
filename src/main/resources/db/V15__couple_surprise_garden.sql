-- V15__couple_surprise_garden.sql
-- 目的：惊喜与期待批次（F50-F59）：爱情刮刮乐、恋爱盲盒、心动闹钟、思念速递、
--       爱情花园、每日玫瑰、幸运签、告白重现、藏宝图任务。生日彩蛋（F59）无需新表。
-- 涉及表：couple_scratch / couple_mystery_box / couple_sweet_alarm / couple_miss_express /
--         couple_garden / couple_rose / couple_fortune_slip / couple_confession / couple_treasure（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F50 爱情刮刮乐：每周系统给双方各发一张来自对方的奖励券，刮开才知道是什么，用完可请对方核销
CREATE TABLE IF NOT EXISTS `couple_scratch` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week_key` varchar(10) NOT NULL COMMENT '周标识（如 2026-W40），每周一张',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '送券人用户名（区分大小写）',
    `owner` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收券人用户名（刮券的人）',
    `prize_kind` varchar(20) NOT NULL COMMENT '券类型（hug/breakfast/movie 等）',
    `prize_text` varchar(100) NOT NULL COMMENT '券面内容（如 一个按规定动作的拥抱）',
    `scratched` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已刮开：0 未刮 1 已刮',
    `scratched_at` bigint(20) DEFAULT NULL COMMENT '刮开时间（毫秒）',
    `redeemed_at` bigint(20) DEFAULT NULL COMMENT '核销时间（空 = 券还没用）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scratch_week` (`space_id`, `week_key`, `owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='爱情刮刮乐表：每周一张来自 TA 的奖励券，刮开核销';

-- F51 恋爱盲盒：把一句话或一个小任务装进盒子，对方要到指定日子才能拆
CREATE TABLE IF NOT EXISTS `couple_mystery_box` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '装盒人用户名（区分大小写）',
    `kind` varchar(10) NOT NULL COMMENT '盒子类型：whisper 悄悄话 / task 小任务',
    `content` varchar(300) NOT NULL COMMENT '盒子里的话或任务',
    `open_day` varchar(10) NOT NULL COMMENT '可拆日期（yyyy-MM-dd，最早明天）',
    `opened` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已拆开：0 未拆 1 已拆',
    `opened_at` bigint(20) DEFAULT NULL COMMENT '拆开时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_mystery_box_space` (`space_id`, `open_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='恋爱盲盒表：延迟拆开的悄悄话/小任务，制造一天的小期待';

-- F52 心动闹钟：把一句想说的话设成未来的闹钟，到点由小助手替你送达
CREATE TABLE IF NOT EXISTS `couple_sweet_alarm` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '设钟人用户名（区分大小写）',
    `message` varchar(200) NOT NULL COMMENT '到点送达的那句话',
    `fire_at` bigint(20) NOT NULL COMMENT '触发时间（毫秒，限未来 24 小时内）',
    `fired` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已送达：0 未触发 1 已送达',
    `fired_at` bigint(20) DEFAULT NULL COMMENT '实际送达时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_sweet_alarm_fire` (`space_id`, `fired`, `fire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心动闹钟表：定时送达的一句话，让 TA 在你指定的时间被撩到';

-- F53 思念速递：点一下「想你了」，小助手在 5~30 分钟后的随机时刻替你说出口
CREATE TABLE IF NOT EXISTS `couple_miss_express` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '想 TA 的人用户名（区分大小写）',
    `deliver_at` bigint(20) NOT NULL COMMENT '计划送达时间（毫秒，下单时间 + 5~30 分钟随机）',
    `delivered` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已送达：0 在途 1 已送达',
    `delivered_at` bigint(20) DEFAULT NULL COMMENT '实际送达时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_miss_express_deliver` (`space_id`, `delivered`, `deliver_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='思念速递表：延迟几分钟送达的想念，制造「被惦记」的惊喜时刻';

-- F54 爱情花园：两个人共同浇灌的一棵小树，天天浇水慢慢长大，三天没人管会蔫
CREATE TABLE IF NOT EXISTS `couple_garden` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（唯一，一个空间一座花园）',
    `stage` int(11) NOT NULL DEFAULT 0 COMMENT '成长阶段 0-6（种子→发芽→幼苗→枝干→含苞→盛开→繁茂）',
    `total_water` int(11) NOT NULL DEFAULT 0 COMMENT '累计浇水次数',
    `last_water_day_a` varchar(10) DEFAULT NULL COMMENT '用户 A 最近浇水日（yyyy-MM-dd，每天限一次）',
    `last_water_day_b` varchar(10) DEFAULT NULL COMMENT '用户 B 最近浇水日',
    `withered` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已蔫：连续 3 天没人浇水置 1',
    `revived_count` int(11) NOT NULL DEFAULT 0 COMMENT '被救活次数',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近变动时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_garden_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='爱情花园表：双方共同养成的小树，浇水长大、缺水会蔫';

-- F55 每日玫瑰：每天 3 朵玫瑰配花语送给对方
CREATE TABLE IF NOT EXISTS `couple_rose` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '送花人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '送花日期（yyyy-MM-dd）',
    `flower_key` varchar(20) NOT NULL COMMENT '花的种类 key（rose/tulip/sunflower 等）',
    `word` varchar(60) NOT NULL COMMENT '随花附上的花语',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_rose_space_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日玫瑰表：每天限量 3 朵的鲜花与花语，日常的小浪漫';

-- F56 幸运签：每天可以为 TA 抽一支签，把好运寄给对方
CREATE TABLE IF NOT EXISTS `couple_fortune_slip` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '抽签人用户名（区分大小写，签是抽给对方的）',
    `day` varchar(10) NOT NULL COMMENT '抽签日期（yyyy-MM-dd，每人每天一支）',
    `slip_key` varchar(20) NOT NULL COMMENT '签文 key（对应内容库）',
    `content` varchar(120) NOT NULL COMMENT '签文内容',
    `level` varchar(10) NOT NULL COMMENT '签的等级（大吉/中吉/小吉/锦鲤）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fortune_slip` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='幸运签表：每天替 TA 抽一支签，把好运送过去';

-- F57 告白重现：把当年的告白词存下来，每年的今天由小助手重播一遍
CREATE TABLE IF NOT EXISTS `couple_confession` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `content` varchar(500) NOT NULL COMMENT '当年的告白词',
    `confess_day` varchar(10) NOT NULL COMMENT '告白发生的日期（yyyy-MM-dd，每年这天重现）',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '录入人用户名（区分大小写）',
    `replay_years` varchar(60) DEFAULT NULL COMMENT '已重现过的年份（逗号分隔，防重复推送）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_confession_space` (`space_id`, `confess_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告白重现表：存下当年的告白，每年的今天自动重播';

-- F58 藏宝图任务：给 TA 布置一个现实里的小任务，完成后才能揭晓藏起来的「宝藏」
CREATE TABLE IF NOT EXISTS `couple_treasure` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '埋宝人用户名（区分大小写）',
    `task_text` varchar(100) NOT NULL COMMENT '给 TA 的任务（如 去阳台看看）',
    `prize_text` varchar(100) NOT NULL COMMENT '宝藏内容（完成后才揭晓，如 一个大拥抱+今晚电影你选）',
    `status` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 待完成 / DONE 已揭晓',
    `done_at` bigint(20) DEFAULT NULL COMMENT '揭晓时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_treasure_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='藏宝图任务表：现实小任务 + 完成后揭晓的宝藏，把惊喜藏进生活';
