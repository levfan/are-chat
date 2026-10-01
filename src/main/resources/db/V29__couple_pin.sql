-- V29__couple_pin.sql
-- 目的：批次十六 F207 常用收藏——每人可 pin ≤6 个情侣空间功能卡键，各页签顶部「我的常用」用
-- 涉及表：couple_user_pin（新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

CREATE TABLE IF NOT EXISTS `couple_user_pin` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '归属用户名（区分大小写）',
    `pins` varchar(500) NOT NULL DEFAULT '' COMMENT '功能卡键，逗号分隔，最多 6 个',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_space_user` (`space_id`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F207 常用收藏';
