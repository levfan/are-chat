-- V32__couple_ceremony.sql
-- 目的：批次十九 F230-F239 小日子·仪式感——建国纪念日/过法任务卡/庆祝打卡/爱情保险柜/续约仪式/愿望券/当日体感
-- 涉及表：couple_ceremony_founded / couple_ceremony_ritual / couple_ceremony_mark
--         / couple_ceremony_policy / couple_ceremony_renew / couple_ceremony_coupon / couple_ceremony_recap（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

CREATE TABLE IF NOT EXISTS `couple_ceremony_founded` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `name` varchar(60) NOT NULL COMMENT '小日子名称（如「我们的建国纪念日」）',
    `start_day` varchar(10) NOT NULL COMMENT '起始日 yyyy-MM-dd',
    `repeat_year` int NOT NULL DEFAULT 1 COMMENT '1 每年重复 / 0 一次性',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    KEY `idx_founded` (`space_id`, `start_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F230 建国纪念日：自定义我们的小日子（可与官方纪念日区分）';

CREATE TABLE IF NOT EXISTS `couple_ceremony_ritual` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `founded_id` varchar(36) NOT NULL COMMENT '所属小日子（关联 couple_ceremony_founded.id）',
    `content` varchar(140) NOT NULL COMMENT '庆祝方式文字动作（如「一起吃火锅」）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_ritual` (`founded_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F232 过法任务卡：每个小日子写死 1-3 条庆祝方式';

CREATE TABLE IF NOT EXISTS `couple_ceremony_mark` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `ritual_id` varchar(36) NOT NULL COMMENT '被打勾的过法卡（关联 couple_ceremony_ritual.id）',
    `day` varchar(10) NOT NULL COMMENT '打卡日 yyyy-MM-dd',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mark` (`ritual_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F233 庆祝打卡：当日逐条打勾，隔日未齐补催';

CREATE TABLE IF NOT EXISTS `couple_ceremony_policy` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `month` varchar(7) NOT NULL COMMENT '保费月 yyyy-MM',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '交保费的人（互夸作者）',
    `quote` varchar(140) NOT NULL COMMENT '夸 TA 的一句（本月保费）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_policy` (`space_id`, `month`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F234 爱情保险柜：每月互夸各 1 句=交齐当月保费';

CREATE TABLE IF NOT EXISTS `couple_ceremony_renew` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `anchor_day` varchar(10) NOT NULL COMMENT '续约锚点日（满 100 天/周年的当日）yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '签字人',
    `line` varchar(140) NOT NULL COMMENT '重签的一句「我还是选你」',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_renew` (`space_id`, `anchor_day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F235 续约仪式：每满 100 天/周年双方各签一句';

CREATE TABLE IF NOT EXISTS `couple_ceremony_coupon` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(80) NOT NULL COMMENT '券面文字（自拟）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN 待兑 / USED 已核销',
    `ref` varchar(60) NOT NULL DEFAULT '' COMMENT '保险柜 payout 来源标记（policy-3 等；手动发为空）',
    `issuer` varchar(50) NOT NULL COMMENT '发券人',
    `used_by` varchar(50) NOT NULL DEFAULT '' COMMENT '核销人（未核销为空）',
    `used_at` bigint(20) NOT NULL DEFAULT 0 COMMENT '核销时间（毫秒，未核销为 0）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_coupon` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F236 愿望券本：手动发/核销，保险柜满 3/6/12 月自动 payout';

CREATE TABLE IF NOT EXISTS `couple_ceremony_recap` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '仪式发生日 yyyy-MM-dd（含年份，天然区分届次）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '留言人',
    `feeling` varchar(140) NOT NULL COMMENT '此刻感觉一句话',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recap` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F239 当日体感：仪式当天各留一句，次年今日对比';
