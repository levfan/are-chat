-- ============================================================================
-- V55：情侣空间新增亲密记录表 couple_intimacy_log
-- ============================================================================
-- 目的：记录两个人之间的边缘亲昵与性生活，一行 = 一次；同一天可以有多条。
-- 口径：
--   1) day 存 yyyy-MM-dd 的 varchar(10)，与 couple_streak_day.day 同一约定，不能晚于今天。
--   2) kind 只有 EDGE（边缘亲昵）/ SEX（性生活）两档，由领域层收口，表上不加约束。
--   3) recorder_user 只做留痕，双方都能记、都能删；这是两个人共享的私密账本。
--   4) 刻意不建唯一键：同一天记两次是真实情况，去重交给前端防连点。
--   5) 幂等 + H2/MariaDB 双兼容：CREATE TABLE IF NOT EXISTS，索引名全库唯一（idx_couple11_ 前缀）。
-- 涉及表：couple_intimacy_log（新建）
-- ============================================================================

CREATE TABLE IF NOT EXISTS `couple_intimacy_log` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `day` varchar(10) NOT NULL COMMENT '发生日期（yyyy-MM-dd，自然日）',
    `kind` varchar(10) NOT NULL COMMENT '类型：EDGE 边缘亲昵 / SEX 性生活',
    `note` varchar(60) DEFAULT NULL COMMENT '备注（选填，最多 60 字）',
    `recorder_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名',
    `created` bigint(20) NOT NULL COMMENT '记录时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple11_intimacy_space_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='亲密记录表：一行=一次边缘亲昵或性生活，同一天可多条，双方共享';
