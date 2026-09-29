-- V13__add_couple_notify_birthday.sql
-- 目的：全局通知——情侣空间事件通知中心（F41）；好友生日——user_profile 加生日列（F42）；2026-02-02
-- 涉及表：couple_notify（新建）、user_profile（加列 birthday）
-- 幂等方式：CREATE TABLE IF NOT EXISTS / ADD COLUMN ... IF NOT EXISTS

-- 情侣空间事件通知中心：所有 pushCoupleEvent 落库一份，离线也能补看
CREATE TABLE IF NOT EXISTS `couple_notify` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收件人用户名（区分大小写）',
    `event` varchar(40) NOT NULL COMMENT '事件名（如 letter-created / countdown-reminder）',
    `actor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '触发人用户名（system = 定时任务）',
    `detail` varchar(300) NOT NULL COMMENT '通知文案',
    `read_flag` tinyint(4) DEFAULT 0 COMMENT '是否已读：1 已读 / 0 未读',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_notify_user` (`username`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣空间通知中心表：每次事件推送都给收件人存档一条，登录后可补看与标记已读';

-- F42 好友生日：本人填写，生日当天好友列表会亮起蛋糕提示
ALTER TABLE `user_profile` ADD COLUMN IF NOT EXISTS `birthday` varchar(10) DEFAULT NULL COMMENT '生日（yyyy-MM-dd，允许只填 MM-dd 表达不在意年份）' AFTER `presence_status`;
