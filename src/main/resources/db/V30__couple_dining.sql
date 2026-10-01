-- V30__couple_dining.sql
-- 目的：批次十七 F210-F219 两个人的饭桌——饭票/星评/踩雷/菜单/拿手菜/搭伙车/话题卡标记
-- 涉及表：couple_dine_ticket / couple_dine_rate / couple_dine_nogo / couple_dine_weekplan
--         / couple_dine_homecook / couple_dine_cart / couple_dine_topic（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

CREATE TABLE IF NOT EXISTS `couple_dine_ticket` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '提名日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提名用户名（区分大小写）',
    `dish` varchar(60) NOT NULL COMMENT '菜名',
    `reason` varchar(140) NOT NULL DEFAULT '' COMMENT '一句理由',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F210 今晚饭票：每人每天提名一道菜';

CREATE TABLE IF NOT EXISTS `couple_dine_rate` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '吃的那天 yyyy-MM-dd',
    `dish` varchar(60) NOT NULL COMMENT '吃了啥（菜或店）',
    `stars` int NOT NULL COMMENT '1-5 星',
    `comment` varchar(140) NOT NULL DEFAULT '' COMMENT '一句点评',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_rate_space` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F212 吃过星评：我们的餐厅档案';

CREATE TABLE IF NOT EXISTS `couple_dine_nogo` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `name` varchar(60) NOT NULL COMMENT '小店/外卖名',
    `reason` varchar(140) NOT NULL COMMENT '避雷理由',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提议人（只有 TA 能划掉）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_nogo` (`space_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F213 踩雷库：一起拉黑的小店';

CREATE TABLE IF NOT EXISTS `couple_dine_weekplan` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `day` varchar(10) NOT NULL COMMENT '周内某天 yyyy-MM-dd',
    `dish` varchar(60) NOT NULL COMMENT '这顿吃什么',
    `updated_by` varchar(50) NOT NULL DEFAULT '' COMMENT '最后编辑人（可空列不带 charset）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F214 本周菜单：7 个格子各排一顿正餐';

CREATE TABLE IF NOT EXISTS `couple_dine_homecook` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '下厨人',
    `dish` varchar(60) NOT NULL COMMENT '拿手菜',
    `score` int NOT NULL COMMENT '自封配饭指数 1-5',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_homecook` (`space_id`, `week`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F215 家常菜搭档：周末各报一道拿手菜';

CREATE TABLE IF NOT EXISTS `couple_dine_cart` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd，随周清空靠查询过滤）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '加菜人',
    `item` varchar(60) NOT NULL COMMENT '菜品名',
    `qty` int NOT NULL DEFAULT 1 COMMENT '份数 1-20',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/LOCKED，双方各锁一次才成行',
    `locked_by` varchar(120) NOT NULL DEFAULT '' COMMENT '已按锁的人（逗号分隔，可空列不带 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_cart_week` (`space_id`, `week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F217 外卖搭伙车：同辆车各加菜，凑齐喊锁车';

CREATE TABLE IF NOT EXISTS `couple_dine_topic` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪天的饭桌话题 yyyy-MM-dd',
    `marked_by` varchar(50) NOT NULL DEFAULT '' COMMENT '谁标记聊过了（可空列不带 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F218 饭桌话题卡：吃完打个卡';
