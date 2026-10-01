-- V33__couple_board.sql
-- 目的：批次二十 F240-F249 我们公司——头衔任命/董事会决议/年度述职/发薪日/金点子/请假式签到（晋升公示/名片/周报无表）
-- 涉及表：couple_board_role / couple_board_vote / couple_board_report / couple_board_salary
--         / couple_board_idea / couple_board_attend（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

CREATE TABLE IF NOT EXISTS `couple_board_role` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提名人（谁封的官）',
    `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '被任命的人',
    `title` varchar(60) NOT NULL COMMENT '头衔（财政部长/首席大厨…）',
    `appointed` int NOT NULL DEFAULT 0 COMMENT '0 待任命 / 1 已盖章生效',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    KEY `idx_role` (`space_id`, `to_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F240 头衔任命：各报两个在家职位，对方点任命生效';

CREATE TABLE IF NOT EXISTS `couple_board_vote` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(140) NOT NULL COMMENT '决议事项',
    `proposer` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提案人',
    `status` varchar(12) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待表决 / PASSED 通过 / VETOED 一票否决',
    `veto_by` varchar(50) NOT NULL DEFAULT '' COMMENT '否决人（仅 VETOED 时有值）',
    `decided_at` bigint(20) NOT NULL DEFAULT 0 COMMENT '表决时刻（毫秒，未决为 0）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_vote` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F241 董事会决议：提案→附议→通过/否决（一票否决）全程留痕';

CREATE TABLE IF NOT EXISTS `couple_board_report` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `year` varchar(4) NOT NULL COMMENT '述职年度 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '述职人',
    `review` varchar(500) NOT NULL COMMENT '本年述职',
    `goal` varchar(200) NOT NULL COMMENT '明年一个小目标',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report` (`space_id`, `year`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F242 年度股东大会：述职+小目标，双提交互见';

CREATE TABLE IF NOT EXISTS `couple_board_salary` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `month` varchar(7) NOT NULL COMMENT '发薪月 yyyy-MM',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发工资的人',
    `thanks` varchar(200) NOT NULL COMMENT '本月感谢工资（一句感谢）',
    `created` bigint(20) NOT NULL COMMENT '发薪时刻（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_salary` (`space_id`, `month`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F244 发薪日：每月各发一句感谢工资并+5积分入账台账';

CREATE TABLE IF NOT EXISTS `couple_board_idea` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '点子提出人',
    `content` varchar(140) NOT NULL COMMENT '一句话改进提案',
    `adopted` int NOT NULL DEFAULT 0 COMMENT '0 待看 / 1 已采纳转决议',
    `vote_id` varchar(36) NOT NULL DEFAULT '' COMMENT '采纳后生成的决议ID（关联 couple_board_vote.id）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_idea` (`space_id`, `adopted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F245 金点子箱：被采纳即转董事会决议';

CREATE TABLE IF NOT EXISTS `couple_board_attend` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '签到日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '签到人',
    `attended` int NOT NULL DEFAULT 1 COMMENT '当日是否已签到',
    `convened` int NOT NULL DEFAULT 0 COMMENT '0/1 当日已散会级双签到（10s 窗口内双签）',
    `created` bigint(20) NOT NULL COMMENT '首次签到时间（毫秒）',
    `updated_at` bigint(20) NOT NULL COMMENT '最近签到时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_attend` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F247 会议签到：10s 窗口内双签到才算开了会';
