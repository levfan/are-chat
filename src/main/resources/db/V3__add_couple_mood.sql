-- V3__add_couple_mood.sql
-- 目的：情侣空间心情日记建表（每天每人一条心情 + 一句话，双方可见，形成双人心情曲线）
-- 涉及表：couple_mood
-- 日期：2026-02-08
-- 说明：幂等（CREATE TABLE IF NOT EXISTS），可安全重复执行；老库走 Flyway 自动应用，无需手工执行。
--       区分大小写的列（用户名）用列级 CHARACTER SET utf8mb4 COLLATE utf8mb4_bin 声明，不写 DEFAULT（可空为默认，语法对 H2/MySQL/MariaDB 通用）。

CREATE TABLE IF NOT EXISTS `couple_mood` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '记录人用户名（区分大小写）',
    `mood_day` varchar(10) NOT NULL COMMENT '心情日期（yyyy-MM-dd，按自然日去重）',
    `mood` varchar(16) NOT NULL COMMENT '心情键：LOVE 恋爱中 / HAPPY 开心 / CALM 平静 / BUSY 好忙 / TIRED 累了 / SICK 生病 / SAD 难过 / ANGRY 生气',
    `note` varchar(200) DEFAULT NULL COMMENT '一句话心情',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近修改时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_mood` (`space_id`,`username`,`mood_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣心情日记表：每天每人记录一条心情，双方互相可见，用于绘制双人心情曲线';
