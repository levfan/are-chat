-- V20__couple_distance.sql
-- 目的：异地恋·时空同步批次（F110-F119）：隔空牵手、想念计量所、我们的作息表、
--       下次见面信、云约会清单、平安卡、见面日记。
--       F111 双城时刻卡 / F113 见面能量瓶 / F119 异地恋报告为聚合计算，无新表。
-- 涉及表：couple_handhold / couple_miss_daily / couple_routine /
--         couple_reunion_letter / couple_cloud_date / couple_safety_ping /
--         couple_reunion_log（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F110 隔空牵手：同一天双方都点亮「牵手」即成功，累计牵手日子
CREATE TABLE IF NOT EXISTS `couple_handhold` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '牵手日期（yyyy-MM-dd，每天一签）',
    `hold_a` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 A 是否点亮：0 否 1 已点亮',
    `hold_b` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 B 是否点亮：0 否 1 已点亮',
    `hold_at_a` bigint(20) DEFAULT NULL COMMENT '用户 A 点亮时间（毫秒）',
    `hold_at_b` bigint(20) DEFAULT NULL COMMENT '用户 B 点亮时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_handhold_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='隔空牵手表：隔着屏幕也牵手，一天一握，握紧不放';

-- F112 想念计量所：每天点亮「今天想你了」，同天互想 = 双向奔赴
CREATE TABLE IF NOT EXISTS `couple_miss_daily` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '想念日期（yyyy-MM-dd，每天一签）',
    `miss_a` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 A 是否点亮：0 否 1 已点亮',
    `miss_b` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 B 是否点亮：0 否 1 已点亮',
    `miss_at_a` bigint(20) DEFAULT NULL COMMENT '用户 A 点亮时间（毫秒）',
    `miss_at_b` bigint(20) DEFAULT NULL COMMENT '用户 B 点亮时间（毫秒）',
    `both_at` bigint(20) DEFAULT NULL COMMENT '双向奔赴达成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_miss_daily` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='想念计量所表：今天想你了，想被同样想我的人看见';

-- F114 我们的作息表：双方各自一份作息时间线，重叠时段高亮
CREATE TABLE IF NOT EXISTS `couple_routine` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作息主人用户名（区分大小写）',
    `wake_time` varchar(5) NOT NULL COMMENT '起床时间（HH:mm）',
    `work_start` varchar(5) NOT NULL COMMENT '上班/上课开始（HH:mm）',
    `work_end` varchar(5) NOT NULL COMMENT '下班/下课（HH:mm）',
    `sleep_time` varchar(5) NOT NULL COMMENT '睡觉时间（HH:mm）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_routine_owner` (`space_id`, `owner_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='我们的作息表：把两个人的时间摆在一起，重叠的就是我们的时间';

-- F115 下次见面信：写给「见面时」的信，见面打卡后才可拆
CREATE TABLE IF NOT EXISTS `couple_reunion_letter` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写信人用户名（区分大小写）',
    `content` varchar(300) NOT NULL COMMENT '信的内容（见面时再拆）',
    `status` varchar(10) NOT NULL DEFAULT 'SEELED' COMMENT '状态：SEELED 已封存 / OPENED 已拆开',
    `opened_at` bigint(20) DEFAULT NULL COMMENT '拆信时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_reunion_letter` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='下次见面信表：把想说的存进信封，见面那天再拆';

-- F116 云约会清单：异地也能一起做的事（连麦观影/同步吃饭…），完成打卡
CREATE TABLE IF NOT EXISTS `couple_cloud_date` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发起人用户名（区分大小写）',
    `item` varchar(100) NOT NULL COMMENT '云约会内容（静态库可点选/自定义）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 待完成 / DONE 已完成',
    `done_note` varchar(100) DEFAULT NULL COMMENT '完成时想说的一句（可空）',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_cloud_date` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='云约会清单表：距离隔开的是城市，隔不开的是一起做事';

-- F117 平安卡：出发/到家一键报平安，对方收到强提醒
CREATE TABLE IF NOT EXISTS `couple_safety_ping` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '报平安人用户名（区分大小写）',
    `kind` varchar(10) NOT NULL COMMENT '类型：GO_OUT 出发了 / ARRIVE 到家啦',
    `note` varchar(100) DEFAULT NULL COMMENT '附带一句话（可空，比如 车上人多慢慢开）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_safety_ping` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平安卡表：每一次出发与到达，都有人惦记';

-- F118 见面日记：每次真实见面记一笔，能量瓶与异地恋报告的数据源
CREATE TABLE IF NOT EXISTS `couple_reunion_log` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `by_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `meet_day` varchar(10) NOT NULL COMMENT '见面日期（yyyy-MM-dd，同天只记一条）',
    `note` varchar(200) DEFAULT NULL COMMENT '这次见面做了什么/最难忘的瞬间（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reunion_day` (`space_id`, `meet_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='见面日记表：每一次见面都值得记账，间隔的天数都在攒想念';
