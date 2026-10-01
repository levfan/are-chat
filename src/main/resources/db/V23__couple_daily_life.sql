-- V23__couple_daily_life.sql
-- 目的：深度陪伴·生活分享批次（F140-F149）：梦境手账、美食地图、TA 使用手册、
--       情绪 SOS、每日三问、自定义成就（F140 主题曲 / F146 夸夸 / F147 接头暗号为
--       静态库按日抽取，无表；F149 恋爱仪表盘为聚合改版，无新表）。
-- 涉及表：couple_dream / couple_food_note / couple_partner_fact /
--         couple_sos_ping / couple_daily_three / couple_custom_badge（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F141 梦境手账：记下梦到 TA 的梦，对方醒来读
CREATE TABLE IF NOT EXISTS `couple_dream` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记梦人用户名（区分大小写）',
    `content` varchar(500) NOT NULL COMMENT '梦境内容',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_dream` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='梦境手账表：昨晚又梦到 TA，醒来第一时间写下来';

-- F142 美食地图：一起吃过/想吃的店与菜，打卡留评
CREATE TABLE IF NOT EXISTS `couple_food_note` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `shop` varchar(60) NOT NULL COMMENT '店名',
    `dish` varchar(60) NOT NULL COMMENT '招牌菜',
    `status` varchar(10) NOT NULL DEFAULT 'WANT' COMMENT '状态：WANT 想吃 / EATEN 已打卡',
    `rating` int(11) DEFAULT NULL COMMENT '评分 0-5 星（打卡时填写）',
    `comment` varchar(200) DEFAULT NULL COMMENT '吃后感（打卡时填写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_food_note` (`space_id`, `status`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='美食地图表：把「改天一起吃」变成一张张打卡票根';

-- F143 TA 使用手册：口味/雷区/心头好/小怪癖档案，双方互补
CREATE TABLE IF NOT EXISTS `couple_partner_fact` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '撰写人用户名（区分大小写）',
    `kind` varchar(10) NOT NULL DEFAULT 'TASTE' COMMENT '条目类型：TASTE 口味 / NOGO 雷区 / FAV 心头好 / QUIRK 小怪癖',
    `content` varchar(200) NOT NULL COMMENT '条目内容',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_partner_fact` (`space_id`, `kind`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='TA 使用手册表：把彼此的说明书一页页补全';

-- F144 情绪 SOS：一键发出「现在就要抱抱」，对方抱住即接住
CREATE TABLE IF NOT EXISTS `couple_sos_ping` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '求抱抱人用户名（区分大小写）',
    `message` varchar(100) DEFAULT NULL COMMENT '想多说的一句（可空）',
    `status` varchar(10) NOT NULL DEFAULT 'SENT' COMMENT '状态：SENT 等接住 / HELD 已抱住',
    `held_at` bigint(20) DEFAULT NULL COMMENT '被抱住时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_sos_ping` (`space_id`, `status`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情绪SOS表：成年人的崩溃需要快捷键，抱抱是最好的响应';

-- F145 每日三问：今天最开心/最被感动/最想对 TA 说
CREATE TABLE IF NOT EXISTS `couple_daily_three` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '答题人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `joy` varchar(200) DEFAULT NULL COMMENT '今天最开心的事',
    `touched` varchar(200) DEFAULT NULL COMMENT '今天最被感动的瞬间',
    `want_to_say` varchar(200) DEFAULT NULL COMMENT '最想对 TA 说的一句话',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_three_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日三问表：睡前三分钟，把一天过成值得纪念的样子';

-- F148 自定义成就：自设成就+达成条件，达成颁发双人证书
CREATE TABLE IF NOT EXISTS `couple_custom_badge` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '立成就人用户名（区分大小写）',
    `title` varchar(50) NOT NULL COMMENT '成就名（连吃七天早餐）',
    `condition` varchar(200) DEFAULT NULL COMMENT '达成条件说明（可空）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 挑战中 / ISSUED 已颁发',
    `issued_at` bigint(20) DEFAULT NULL COMMENT '颁发时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_custom_badge` (`space_id`, `status`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义成就表：我们自己定义什么值得庆祝';
