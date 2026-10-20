-- ============================================================================
-- V54：情侣空间新增经期记录表 couple_period
-- ============================================================================
-- 目的：记录每一次经期的开始日（结束日可后补），周期与下次预测全部读时算，不物化缓存列。
-- 口径：
--   1) 一行 = 一次经期，不是一天；space_id + start_day 唯一，同一天不许记两遍。
--   2) 开始日/结束日都存 yyyy-MM-dd 的 varchar(10)，与 couple_streak_day.day 同一约定；
--      按字符串排序即按日期排序，跨年也成立。
--   3) recorder_user 记录是谁记的（双方都能记、都能改），但不做权限闸门——这是两个人共享的记录。
--   4) 幂等 + H2/MariaDB 双兼容：CREATE TABLE IF NOT EXISTS，索引名全库唯一（uk_couple10_ 前缀）。
-- 涉及表：couple_period（新建）
-- ============================================================================

CREATE TABLE IF NOT EXISTS `couple_period` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `start_day` varchar(10) NOT NULL COMMENT '经期开始日期（yyyy-MM-dd，自然日）',
    `end_day` varchar(10) DEFAULT NULL COMMENT '经期结束日期（yyyy-MM-dd；还没结束为 NULL）',
    `recorder_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名',
    `created` bigint(20) NOT NULL COMMENT '记录时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近修改时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple10_period_start` (`space_id`,`start_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='经期记录表：一行=一次经期的开始日（结束日可后补），平均周期与下次预测读时算';
