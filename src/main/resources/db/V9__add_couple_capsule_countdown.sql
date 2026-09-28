-- V9__add_couple_capsule_countdown.sql
-- 目的：纪念与回忆模块——时光胶囊表、倒数日期待清单表；2026-02-02
-- 涉及表：couple_capsule（新建）、couple_countdown（新建）
-- 说明：里程碑徽章/成就系统/那年今天均由既有数据实时推导，不需要建表
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- 时光胶囊：写给未来的 TA，30~365 天后才能拆开（区别于 7 天慢递悄悄话）
CREATE TABLE IF NOT EXISTS `couple_capsule` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `sender` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发件人用户名（区分大小写）',
    `recipient` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收件人用户名（区分大小写）',
    `content` varchar(500) NOT NULL COMMENT '胶囊内容（1-500 字）',
    `open_day` varchar(10) NOT NULL COMMENT '可开启日期（yyyy-MM-dd，30-365 天后）',
    `status` varchar(16) NOT NULL DEFAULT 'SEALED' COMMENT '状态：SEALED 封存中 / OPENED 已开启',
    `opened_at` bigint(20) DEFAULT NULL COMMENT '开启时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '封存时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_capsule_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣时光胶囊表：把现在的约定封进胶囊，到点才能开启，给未来的彼此一个惊喜';

-- 倒数日期待清单：期待中的日子（约会/旅行/演唱会），带倒数提醒
CREATE TABLE IF NOT EXISTS `couple_countdown` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `title` varchar(60) NOT NULL COMMENT '期待的事情，如「去看海」「TA 的生日惊喜」',
    `target_day` varchar(10) NOT NULL COMMENT '目标日期（yyyy-MM-dd）',
    `note` varchar(200) DEFAULT NULL COMMENT '小备注',
    `done` tinyint(4) DEFAULT 0 COMMENT '是否已实现：1 实现（归档）/ 0 期待中',
    `done_at` bigint(20) DEFAULT NULL COMMENT '实现时间（毫秒时间戳）',
    `last_remind_day` varchar(10) DEFAULT NULL COMMENT '最近一次倒数提醒日期（防重复打扰）',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '创建人用户名',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_countdown_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣倒数日表：把期待的事写下来倒数，临近自动提醒双方';
