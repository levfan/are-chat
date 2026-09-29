-- V12__add_private_message_heart.sql
-- 目的：聊天联动——私聊消息支持标记「心动时刻」（F36）；2026-02-02
-- 涉及表：private_message（加列 heart_at）
-- 幂等方式：ADD COLUMN ... IF NOT EXISTS

-- 心动时刻标记时间（毫秒时间戳，null = 未标记）；消息双方均可标记/取消，
-- 标记后在情侣空间「心动时刻」回顾区可见
ALTER TABLE `private_message` ADD COLUMN `heart_at` bigint(20) DEFAULT NULL COMMENT '心动时刻标记时间（毫秒时间戳，null = 未标记）' AFTER `edited`;
