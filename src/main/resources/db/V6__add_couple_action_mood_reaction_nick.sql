-- V6__add_couple_action_mood_reaction_nick.sql
-- 目的：贴贴互动模块——亲密小动作流水表、心情回应表、空间双方专属爱称列；2026-02-02
-- 涉及表：couple_action（新建）、couple_mood_reaction（新建）、couple_space（加列）
-- 幂等方式：CREATE TABLE IF NOT EXISTS + ADD COLUMN IF NOT EXISTS

-- 亲密小动作流水：戳一戳/抱抱/亲亲/捏捏脸/蹭蹭/挠痒痒/想念，一键发送给对方
CREATE TABLE IF NOT EXISTS `couple_action` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发送方用户名（区分大小写）',
    `kind` varchar(16) NOT NULL COMMENT '动作类型：POKE 戳一戳 / HUG 抱抱 / KISS 亲亲 / PAT 捏捏脸 / NUZZLE 蹭蹭 / TICKLE 挠痒痒 / MISS 在想你',
    `created` bigint(20) NOT NULL COMMENT '发送时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_action_space` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣贴贴动作流水表：一键发送亲密小动作，对方实时收到推送，累计次数点亮贴贴里程碑';

-- 心情回应：对对方当天记录的心情贴一个回应（抱抱/亲亲/加油/摸摸头）
CREATE TABLE IF NOT EXISTS `couple_mood_reaction` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `mood_day` varchar(10) NOT NULL COMMENT '被回应的心情日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '回应人用户名（区分大小写）',
    `reaction` varchar(16) NOT NULL COMMENT '回应类型：HUG 抱抱 / KISS 亲亲 / CHEER 加油 / PAT 摸摸头',
    `created` bigint(20) NOT NULL COMMENT '回应时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近修改时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_mood_reaction` (`space_id`,`mood_day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣心情回应表：看到 TA 今天心情不好，贴一个抱抱/亲亲/加油，TA 会实时收到推送';

-- 空间双方专属爱称：nick_a = user_a 的专属爱称（由对方起，空间内展示）
ALTER TABLE `couple_space`
    ADD COLUMN `nick_a` varchar(30) DEFAULT NULL CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT 'user_a 的专属爱称（由对方设置，如「宝宝」「猪猪」）' IF NOT EXISTS,
    ADD COLUMN `nick_b` varchar(30) DEFAULT NULL CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT 'user_b 的专属爱称（由对方设置）' IF NOT EXISTS;
