-- V7__add_couple_task_tacit.sql
-- 目的：每日仪式升级——甜蜜任务卡表、默契大考验对局表；2026-02-02
-- 涉及表：couple_task（新建）、couple_tacit（新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- 甜蜜任务卡：每天为双方各随机生成一个小任务（夸夸对方/分享小事…），完成打卡
CREATE TABLE IF NOT EXISTS `couple_task` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `task_day` varchar(10) NOT NULL COMMENT '任务日期（yyyy-MM-dd，每人每天一题）',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '任务归属人用户名（区分大小写）',
    `content` varchar(100) NOT NULL COMMENT '任务内容，如「今天夸对方 3 次」',
    `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING 待完成 / DONE 已完成',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '生成时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_task` (`space_id`,`task_day`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣甜蜜任务卡表：每天一个小任务给彼此撒糖，完成打卡积累心动值';

-- 默契大考验：同一道趣味题双方背对背作答，答案一致即默契达成
CREATE TABLE IF NOT EXISTS `couple_tacit` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `question` varchar(100) NOT NULL COMMENT '题目（从默契题库随机）',
    `answer_a` varchar(60) DEFAULT NULL COMMENT '用户 A 的答案（null = 还没答）',
    `answer_b` varchar(60) DEFAULT NULL COMMENT '用户 B 的答案（null = 还没答）',
    `match` tinyint(4) DEFAULT NULL COMMENT '是否默契一致：1 一致 / 0 不一致 / null 待结算',
    `created` bigint(20) NOT NULL COMMENT '发起时间（毫秒时间戳）',
    `settled_at` bigint(20) DEFAULT NULL COMMENT '结算时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_tacit_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣默契大考验表：背对背回答同一道题，答案一致即为心有灵犀';
