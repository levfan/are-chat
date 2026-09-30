-- V18__couple_keepsake.sql
-- 目的：回忆资产批次（F83/F88/F89）：甜蜜语录收藏册、恋爱电影票根、我们的歌单。
--       F80 编年史 / F81 考古卡 / F82 问答机 / F85 周年报告 / F86 生日回顾为现有数据聚合，无需新表；
--       F84 远期胶囊仅放宽时间上限常量；F87 胶囊到期提醒为定时任务。
-- 涉及表：couple_quote / couple_ticket / couple_song（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F83 甜蜜语录收藏册：把 TA 说过的（或你们之间的）甜话收藏成册
CREATE TABLE IF NOT EXISTS `couple_quote` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收藏人用户名（区分大小写）',
    `content` varchar(300) NOT NULL COMMENT '语录内容（TA 说的话或你们的对话）',
    `context` varchar(100) DEFAULT NULL COMMENT '当时的场景（可空，比如 某个加班的深夜）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_quote_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='甜蜜语录收藏册表：甜话会过期，收藏不会';

-- F88 恋爱电影票根：一起看过的每一部，都是一张票根
CREATE TABLE IF NOT EXISTS `couple_ticket` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `title` varchar(100) NOT NULL COMMENT '片名',
    `watch_day` varchar(10) NOT NULL COMMENT '观看日期（yyyy-MM-dd）',
    `rating` int(11) NOT NULL DEFAULT 5 COMMENT '两人的评分（1-5 星，默认满分）',
    `comment` varchar(200) DEFAULT NULL COMMENT '一句观影感想（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_space` (`space_id`, `watch_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='恋爱电影票根表：散场不散，票根为证';

-- F89 我们的歌单：收藏「我们的歌」，每首都有为什么
CREATE TABLE IF NOT EXISTS `couple_song` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收藏人用户名（区分大小写）',
    `title` varchar(100) NOT NULL COMMENT '歌名',
    `artist` varchar(50) DEFAULT NULL COMMENT '歌手（可空）',
    `reason` varchar(200) DEFAULT NULL COMMENT '为什么是我们的歌（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_song_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='我们的歌单表：每首歌都藏着一段我们的故事';
