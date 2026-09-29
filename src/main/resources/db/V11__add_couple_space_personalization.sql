-- V11__add_couple_space_personalization.sql
-- 目的：空间个性化——一句话宣言、空间主题、贴纸墙；2026-02-02
-- 涉及表：couple_space（加列 slogan / theme / stickers）
-- 幂等方式：ADD COLUMN ... IF NOT EXISTS

-- F26 一句话宣言：只有彼此懂的一句话，挂在空间头部
ALTER TABLE `couple_space` ADD COLUMN `slogan` varchar(60) DEFAULT NULL COMMENT '我们的宣言：只有彼此懂的一句话（60 字内）' AFTER `nick_b`;

-- F27 空间主题：classic/cherry/ocean/forest/night 五款主题（空间级，双方共享）
ALTER TABLE `couple_space` ADD COLUMN `theme` varchar(20) NOT NULL DEFAULT 'classic' COMMENT '空间主题：classic 经典粉 / cherry 樱花 / ocean 海盐 / forest 森绿 / night 星夜' AFTER `slogan`;

-- F28 贴纸墙：佩戴展示的纪念贴纸 key 列表（逗号分隔，最多 6 枚；自动发放 + 手动选择）
ALTER TABLE `couple_space` ADD COLUMN `stickers` varchar(200) DEFAULT NULL COMMENT '贴纸墙佩戴的贴纸 key（逗号分隔，最多 6 枚）' AFTER `theme`;
