-- V8__add_couple_care_tables.sql
-- 目的：情绪关怀模块——和好卡表、夸夸墙表、生理期记录表；2026-02-02
-- 涉及表：couple_reconcile（新建）、couple_praise（新建）、couple_cycle（新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- 和好卡：吵架后主动递一张卡，对方接受即和好，可记录这次别扭从什么时候开始
CREATE TABLE IF NOT EXISTS `couple_reconcile` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '递卡人用户名（区分大小写）',
    `message` varchar(200) NOT NULL COMMENT '和好留言，如「是我不好，抱一下就和好」',
    `start_at` bigint(20) DEFAULT NULL COMMENT '这次别扭开始时间（毫秒时间戳，可空，用于计算和好耗时）',
    `status` varchar(16) NOT NULL DEFAULT 'SENT' COMMENT '状态：SENT 已递出待接受 / ACCEPTED 已接受',
    `accepted_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '接受人用户名',
    `accepted_at` bigint(20) DEFAULT NULL COMMENT '接受时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '递卡时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_reconcile_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣和好卡表：小别扭的温柔收尾，接受后记录和好时刻与耗时';

-- 夸夸墙：互相写夸夸小卡片贴上墙，对方点「收到啦」
CREATE TABLE IF NOT EXISTS `couple_praise` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '夸人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '夸夸内容，要具体哦',
    `status` varchar(16) NOT NULL DEFAULT 'POSTED' COMMENT '状态：POSTED 已张贴 / RECEIVED 已收到',
    `received_at` bigint(20) DEFAULT NULL COMMENT '收到时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '张贴时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_praise_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣夸夸墙表：把欣赏说出口，具体地夸，认真地收';

-- 生理期记录：每人记自己的（一般女生记），对方可见预告与「温柔模式」提醒
CREATE TABLE IF NOT EXISTS `couple_cycle` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写，记自己的）',
    `period_day` varchar(10) NOT NULL COMMENT '最近一次生理期开始日期（yyyy-MM-dd）',
    `cycle_days` int(11) NOT NULL DEFAULT 28 COMMENT '周期长度（天，默认 28）',
    `period_days` int(11) NOT NULL DEFAULT 5 COMMENT '经期持续天数（默认 5）',
    `note` varchar(100) DEFAULT NULL COMMENT '小备注，如「这几天想喝热的」',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近修改时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_cycle` (`space_id`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣生理期记录表：自动预告下次日期，对方端展示温柔模式提醒';
