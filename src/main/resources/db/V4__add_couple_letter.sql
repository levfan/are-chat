-- V4__add_couple_letter.sql
-- 目的：情侣空间悄悄话信箱建表（写给 TA 的小纸条，支持慢递到点拆封，拆封后双方可见）
-- 涉及表：couple_letter
-- 日期：2026-09-27
-- 说明：幂等（CREATE TABLE IF NOT EXISTS），可安全重复执行；老库走 Flyway 自动应用，无需手工执行。
--       区分大小写的列（用户名）用列级 CHARACTER SET utf8mb4 COLLATE utf8mb4_bin 声明，不写 DEFAULT（可空为默认，语法对 H2/MySQL/MariaDB 通用）。

CREATE TABLE IF NOT EXISTS `couple_letter` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `sender` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '发件人用户名（区分大小写）',
    `recipient` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '收件人用户名（区分大小写）',
    `content` varchar(300) NOT NULL COMMENT '悄悄话内容（1-300 字的小纸条）',
    `deliver_at` bigint(20) DEFAULT NULL COMMENT '可拆封时间（毫秒时间戳，空=立即可拆；慢递最长 7 天）',
    `status` varchar(16) NOT NULL DEFAULT 'SEALED' COMMENT '信件状态：SEALED 未拆封 / OPENED 已拆封',
    `opened_at` bigint(20) DEFAULT NULL COMMENT '拆封时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '发信时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_letter_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣悄悄话信箱表：写给 TA 的小纸条，支持慢递（到点才能拆），拆封后双方可见';
