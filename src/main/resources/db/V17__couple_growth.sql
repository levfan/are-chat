-- V17__couple_growth.sql
-- 目的：共同养成批次（F70-F79）：双人挑战赛、恋爱存折、百日之约、心愿互换、
--       共读计划、旅行心愿地图、追剧清单、恋爱词典、下次一定清单。
--       F77 星座配对为纯静态计算，无需新表。
-- 涉及表：couple_challenge / couple_passbook / couple_hundred / couple_hundred_checkin /
--         couple_wish_exchange / couple_read_plan / couple_read_progress /
--         couple_travel_wish / couple_watchlist / couple_dict_word / couple_next_time（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F70 双人挑战赛：每天系统出同一道小挑战，双方各自打卡，双完成即达成
CREATE TABLE IF NOT EXISTS `couple_challenge` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '挑战日期（yyyy-MM-dd，每天一题）',
    `task_text` varchar(100) NOT NULL COMMENT '今日挑战题目（按空间+天稳定抽取）',
    `done_a` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 A 是否完成：0 未完成 1 已完成',
    `done_b` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 B 是否完成：0 未完成 1 已完成',
    `done_at_a` bigint(20) DEFAULT NULL COMMENT '用户 A 完成时间（毫秒）',
    `done_at_b` bigint(20) DEFAULT NULL COMMENT '用户 B 完成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_challenge_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='双人挑战赛表：每天同一道小挑战，一起完成才算赢';

-- F71 恋爱存折：每天各自存一笔「今天为这段感情做的一件小事」，攒连续存款天数
CREATE TABLE IF NOT EXISTS `couple_passbook` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存款人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '存款日期（yyyy-MM-dd，每人每天一笔，重复=修改）',
    `content` varchar(200) NOT NULL COMMENT '今天为这段感情做的一件小事',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_passbook_day` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='恋爱存折表：感情靠每天存一点，攒的是连续和心意';

-- F72 百日之约：设一个 100 天的共同目标，每天双方打卡，第 100 天达成
CREATE TABLE IF NOT EXISTS `couple_hundred` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `goal` varchar(200) NOT NULL COMMENT '百日目标（比如 一起早睡 100 天）',
    `start_day` varchar(10) NOT NULL COMMENT '开始日期（yyyy-MM-dd）',
    `status` varchar(10) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE 进行中 / DONE 达成 / BROKEN 中止',
    `done_at` bigint(20) DEFAULT NULL COMMENT '达成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_hundred_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='百日之约表：把「想坚持的事」变成 100 天的双人打卡';

-- F72 百日之约打卡明细：每个约定每天每人一条打卡
CREATE TABLE IF NOT EXISTS `couple_hundred_checkin` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `pact_id` varchar(36) NOT NULL COMMENT '百日之约ID（关联 couple_hundred.id）',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `by_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '打卡人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '打卡日期（yyyy-MM-dd，每人每天一条，重复=补卡）',
    `note` varchar(100) DEFAULT NULL COMMENT '今日打卡心得（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hundred_checkin` (`pact_id`, `day`, `by_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='百日之约打卡明细表：每天一签，断签不断约';

-- F73 心愿互换：写一个「想让 TA 帮你实现的心愿」，TA 接单并完成
CREATE TABLE IF NOT EXISTS `couple_wish_exchange` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '许愿人用户名（区分大小写）',
    `wish` varchar(200) NOT NULL COMMENT '心愿内容',
    `status` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 待接单 / ACCEPTED 已接单 / DONE 已实现',
    `accepted_at` bigint(20) DEFAULT NULL COMMENT '接单时间（毫秒）',
    `done_note` varchar(100) DEFAULT NULL COMMENT '实现时想说的话（可空）',
    `done_at` bigint(20) DEFAULT NULL COMMENT '实现时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_wish_exchange` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心愿互换表：我的小心愿交给 TA，TA 的心愿我来实现';

-- F74 共读计划：一起读一本书/追一部剧，各自上报进度，看谁先到终点
CREATE TABLE IF NOT EXISTS `couple_read_plan` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(100) NOT NULL COMMENT '书名/剧名',
    `total_units` int(11) NOT NULL COMMENT '总章节数/总集数',
    `unit_label` varchar(10) NOT NULL DEFAULT '章' COMMENT '进度单位（章/集/课）',
    `status` varchar(10) NOT NULL DEFAULT 'READING' COMMENT '状态：READING 共读中 / FINISHED 已读完',
    `finished_at` bigint(20) DEFAULT NULL COMMENT '读完时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_read_plan` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='共读计划表：同一本书，各自翻页，一起到达结局';

-- F74 共读进度明细：各自多次上报最新进度（取每人最新一条）
CREATE TABLE IF NOT EXISTS `couple_read_progress` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `plan_id` varchar(36) NOT NULL COMMENT '共读计划ID（关联 couple_read_plan.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '上报人用户名（区分大小写）',
    `unit` int(11) NOT NULL COMMENT '当前进度（读到第几章/集）',
    `note` varchar(100) DEFAULT NULL COMMENT '一句话感想（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_read_progress` (`plan_id`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='共读进度明细表：进度条各自爬，感想随进度附一句';

-- F75 旅行心愿地图：想一起去的地方 + 想做的事，去过打勾
CREATE TABLE IF NOT EXISTS `couple_travel_wish` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '许愿人用户名（区分大小写）',
    `place` varchar(100) NOT NULL COMMENT '目的地（城市/店名/地标）',
    `want_todo` varchar(200) DEFAULT NULL COMMENT '到了想做的事（可空）',
    `visited` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已打卡：0 心愿中 1 去过啦',
    `visited_at` bigint(20) DEFAULT NULL COMMENT '打卡时间（毫秒）',
    `visited_note` varchar(100) DEFAULT NULL COMMENT '打卡感想（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_travel_wish` (`space_id`, `visited`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='旅行心愿地图表：把「想去」钉在地图上，一个个走成「去过」';

-- F76 追剧清单：一起在追的剧，更新到第几集，看完标记完结
CREATE TABLE IF NOT EXISTS `couple_watchlist` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(100) NOT NULL COMMENT '剧名/片名',
    `current_unit` int(11) NOT NULL DEFAULT 0 COMMENT '共同看到第几集',
    `total_unit` int(11) DEFAULT NULL COMMENT '总集数（未知道路剧可空）',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '最后更新人用户名',
    `status` varchar(10) NOT NULL DEFAULT 'WATCHING' COMMENT '状态：WATCHING 追剧中 / DONE 已完结',
    `finished_at` bigint(20) DEFAULT NULL COMMENT '看完时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_watchlist` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='追剧清单表：遥控器是两个人的，进度也是';

-- F78 恋爱词典：收录我们的专属词汇（外号/梗/只有我们懂的话）+ 释义
CREATE TABLE IF NOT EXISTS `couple_dict_word` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收录人用户名（区分大小写）',
    `word` varchar(50) NOT NULL COMMENT '专属词汇',
    `meaning` varchar(200) NOT NULL COMMENT '释义（只有我们懂的那层意思）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_dict_word` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='恋爱词典表：我们才懂的语言，值得一本词典';

-- F79 下次一定清单：吵架/忙碌时随口说的「下次一定」，落成清单等兑现
CREATE TABLE IF NOT EXISTS `couple_next_time` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '承诺人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '「下次一定」的内容',
    `status` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 待兑现 / DONE 已兑现',
    `done_at` bigint(20) DEFAULT NULL COMMENT '兑现时间（毫秒）',
    `nudged_at` bigint(20) DEFAULT NULL COMMENT '最近一次被催时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_next_time` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='下次一定清单表：随口的承诺不散场，落单可催办';
