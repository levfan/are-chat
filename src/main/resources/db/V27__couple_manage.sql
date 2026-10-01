-- V27__couple_manage.sql
-- 目的：把家经营好·生活经营系（F180-F189）：家庭会议纪要、本周主理人、技能交换所、月度互评、
--       家庭应急卡、情侣存档点、家务积分市场、五年计划双轨、纪念日策划案（F189 经营周报为无表聚合）。
-- 涉及表：couple_family_meeting / couple_week_host / couple_skill_swap / couple_month_review /
--         couple_emergency_card / couple_month_snapshot / couple_point_ledger /
--         couple_five_year_plan / couple_anniv_plan（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F180 家庭会议纪要：每周议题+决议+跟进日，可关闭
CREATE TABLE IF NOT EXISTS `couple_family_meeting` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `topic` varchar(100) NOT NULL COMMENT '议题',
    `decision` varchar(300) NOT NULL DEFAULT '' COMMENT '决议（可后补）',
    `follow_day` varchar(10) DEFAULT NULL COMMENT '跟进日（可空；H2 兼容：可空列不带 charset）',
    `raised_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提出人用户名（区分大小写）',
    `closed` int(11) NOT NULL DEFAULT 0 COMMENT '是否关闭 0 进行中 1 已关闭',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    KEY `idx_family_meeting` (`space_id`, `week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='家庭会议纪要表：大事小事摆上桌，关起门来是一家人';

-- F181 本周主理人：按周轮流当主理人排小计划
CREATE TABLE IF NOT EXISTS `couple_week_host` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `plan` varchar(200) NOT NULL DEFAULT '' COMMENT '主理人排的本周小计划（可后补）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_week_host` (`space_id`,`week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='本周主理人表：这周轮到你当家，小计划你来排';

-- F182 技能交换所：我教你做饭你教我修电脑
CREATE TABLE IF NOT EXISTS `couple_skill_swap` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发起人用户名（区分大小写）',
    `teach` varchar(60) NOT NULL COMMENT '我能教你什么',
    `learn` varchar(60) NOT NULL COMMENT '我想跟你学什么',
    `status` varchar(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态 OPEN 挂牌 / TAKEN 成交 / DONE 两清',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_skill_swap` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='技能交换所表：你会的我想学，我会的你正好要';

-- F183 月度互评：每月给这段关系打星+建议（双评互见）
CREATE TABLE IF NOT EXISTS `couple_month_review` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `month` varchar(7) NOT NULL COMMENT '所属月份（yyyy-MM）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '评价人用户名（区分大小写）',
    `stars` int(11) NOT NULL DEFAULT 5 COMMENT '本月关系星级 1-5',
    `advice` varchar(200) NOT NULL DEFAULT '' COMMENT '想对TA说的一条建议',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_month_review_user` (`space_id`,`month`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='月度互评表：一个月打一次分，日子才有回声';

-- F184 家庭应急卡：联系人/钥匙/药品清单文字版（各填一份，双方互见）
CREATE TABLE IF NOT EXISTS `couple_emergency_card` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '填写人用户名（区分大小写）',
    `contacts` varchar(300) NOT NULL DEFAULT '' COMMENT '紧急联系人清单',
    `keys_place` varchar(200) NOT NULL DEFAULT '' COMMENT '备用钥匙/重要物品存放',
    `medicine` varchar(300) NOT NULL DEFAULT '' COMMENT '常备药与过敏信息',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emergency_card_user` (`space_id`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='家庭应急卡表：愿永远用不上，但必须人人看得懂';

-- F185 情侣存档点：每月快照工作/健康/感情温度（双方各存，对照展示）
CREATE TABLE IF NOT EXISTS `couple_month_snapshot` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `month` varchar(7) NOT NULL COMMENT '所属月份（yyyy-MM）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存档人用户名（区分大小写）',
    `work` varchar(100) NOT NULL DEFAULT '' COMMENT '本月工作状态',
    `health` varchar(100) NOT NULL DEFAULT '' COMMENT '本月健康状态',
    `love_temp` int(11) NOT NULL DEFAULT 60 COMMENT '感情温度 0-100',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_month_snapshot_user` (`space_id`,`month`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣存档点表：像游戏存档一样记录每个月的我们';

-- F186 家务积分市场：做家务赚积分，兑换小奖励（奖励清单静态无表）
CREATE TABLE IF NOT EXISTS `couple_point_ledger` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记账人用户名（区分大小写）',
    `type` varchar(10) NOT NULL COMMENT '流水类型 EARN 赚 / SPEND 花',
    `item` varchar(100) NOT NULL COMMENT '事项或兑换的奖励',
    `points` int(11) NOT NULL DEFAULT 1 COMMENT '积分数（正整数）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_point_ledger` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='家务积分流水表：家务不再是白干，爱也要有账本';

-- F187 五年计划双轨：自己的（MINE）+我们的（OURS），各自认领
CREATE TABLE IF NOT EXISTS `couple_five_year_plan` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `track` varchar(10) NOT NULL COMMENT '轨道 MINE 自己的 / OURS 我们的',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写下人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '五年之约内容',
    `owner_user` varchar(50) DEFAULT NULL COMMENT 'OURS 轨道认领人（可空；H2 兼容：可空列不带 charset）',
    `done` int(11) NOT NULL DEFAULT 0 COMMENT '是否达成 0 进行中 1 已达成',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_five_year_plan` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='五年计划双轨表：一条路自己走，一条路牵着手走';

-- F188 纪念日策划案：谁策划+点子+落地状态
CREATE TABLE IF NOT EXISTS `couple_anniv_plan` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '纪念日日期（yyyy-MM-dd）',
    `title` varchar(60) NOT NULL COMMENT '纪念日名称',
    `planner` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '策划人用户名（区分大小写）',
    `idea` varchar(300) NOT NULL DEFAULT '' COMMENT '策划点子',
    `status` varchar(20) NOT NULL DEFAULT 'IDEA' COMMENT '状态 IDEA 点子 / LOCKED 定稿 / DONE 已落地',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_anniv_plan` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='纪念日策划案表：把惊喜写成方案，把浪漫落到实处';
