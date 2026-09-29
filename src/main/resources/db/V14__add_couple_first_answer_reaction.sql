-- V14__add_couple_first_answer_reaction.sql
-- 目的：第一次清单（F46）；今日一问互评（F48）。2026-02-02
-- 涉及表：couple_first（新建）、couple_answer_reaction（新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- 我们的一百个第一次：记录恋爱里每个第一次的日期与心情
CREATE TABLE IF NOT EXISTS `couple_first` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(100) NOT NULL COMMENT '第一次做的事（如 第一次一起看海）',
    `first_day` varchar(10) NOT NULL COMMENT '发生的日期（yyyy-MM-dd）',
    `note` varchar(300) DEFAULT NULL COMMENT '当时的心情/补充（可空）',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_first_space` (`space_id`, `first_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='第一次清单表：恋爱里每个第一次的日子与心情，时光轴的重要素材';

-- 今日一问互评：看完 TA 的回答点一个反应，一天每人一个（可改）
CREATE TABLE IF NOT EXISTS `couple_answer_reaction` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `answer_day` varchar(10) NOT NULL COMMENT '一问日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '评价人用户名（区分大小写）',
    `emoji` varchar(16) NOT NULL COMMENT '反应 emoji（如 ❤️😂）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_answer_reaction` (`space_id`, `answer_day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='今日一问互评表：对 TA 回答的表情反应，每人每天一条（upsert）';
