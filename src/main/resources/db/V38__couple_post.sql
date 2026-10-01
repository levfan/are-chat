-- 批次二十五 F290-F299 明日邮局（梦想与未来）
-- 涉及表：couple_post_oath / couple_bucket / couple_bucket_step / couple_someday / couple_dream_home
--         couple_retire_plan / couple_well_qa / couple_relay_capsule / couple_dream_case
--         couple_anniv_wish / couple_future_credit
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空用户名/枚举位用普通 varchar

-- F290 五年后新年卡：每人每年除夕写一张，5 年后元旦读时放行
CREATE TABLE IF NOT EXISTS `couple_post_oath` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `year` varchar(4) NOT NULL COMMENT '写下这一年 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写信人（区分大小写）',
    `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '给五年后我们的一句话',
    `deliver_day` varchar(10) NOT NULL COMMENT '投递日 yyyy-MM-dd（次年元旦）',
    `status` varchar(10) NOT NULL DEFAULT 'SEALED' COMMENT 'SEALED SENT（可空位不写 charset）',
    `sent_at` bigint(20) DEFAULT NULL COMMENT '放行时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oath_year` (`space_id`, `year`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F290 五年后新年卡';

-- F291 人生大事进度：大事册
CREATE TABLE IF NOT EXISTS `couple_bucket` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '大事名（同空间唯一）',
    `target_day` varchar(10) NOT NULL DEFAULT '' COMMENT '目标日，空=不设限',
    `note` varchar(200) NOT NULL DEFAULT '' COMMENT '一句话备注',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发起人（区分大小写）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN DONE GONE（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bucket_name` (`space_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F291 人生大事（区别于 F10 愿景板一词共鸣）';

-- F291 大事拆步：步骤 + TA 补进展章
CREATE TABLE IF NOT EXISTS `couple_bucket_step` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `bucket_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属大事ID',
    `seq` int(11) NOT NULL COMMENT '步骤序号（同大事内唯一）',
    `text` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '步骤描述',
    `done` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=完成',
    `done_by` varchar(50) NOT NULL DEFAULT '' COMMENT '完成/补章人（可空位不写 charset）',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_step_seq` (`bucket_id`, `seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F291 大事步骤';

-- F292 总得有一天拍卖：上拍-7 天认领-排期-完成，逾期下架
CREATE TABLE IF NOT EXISTS `couple_someday` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `thing` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '改天一定的事',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '上拍人（区分大小写）',
    `status` varchar(10) NOT NULL DEFAULT 'SHELF' COMMENT 'SHELF TAKEN DONE EXPIRED（可空位不写 charset）',
    `taken_by` varchar(50) NOT NULL DEFAULT '' COMMENT '认领人（可空位不写 charset）',
    `scheduled_day` varchar(10) NOT NULL DEFAULT '' COMMENT '排期日',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_someday_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F292 改天清单排期制（区别于 F79 单件催办）';

-- F293 想象中的家：字段化梦想家，一年一版
CREATE TABLE IF NOT EXISTS `couple_dream_home` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `year` varchar(4) NOT NULL COMMENT '版本年 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '填写人（区分大小写）',
    `rooms` varchar(100) NOT NULL DEFAULT '' COMMENT '要有哪几个房间',
    `window_view` varchar(100) NOT NULL DEFAULT '' COMMENT '窗外该是什么',
    `smell` varchar(100) NOT NULL DEFAULT '' COMMENT '屋子里的味道',
    `corner` varchar(100) NOT NULL DEFAULT '' COMMENT '我们的专属角落',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_home_year` (`space_id`, `year`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F293 想象中的家';

-- F294 退休计划双写：30/40/50 岁各写我们在干嘛
CREATE TABLE IF NOT EXISTS `couple_retire_plan` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `age_band` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '档位 30/40/50',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写的人（区分大小写）',
    `text` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '那时候我们在干嘛',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_retire` (`space_id`, `age_band`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F294 退休计划双写';

-- F295 许愿井周问：每周一题双写
CREATE TABLE IF NOT EXISTS `couple_well_qa` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `week` varchar(10) NOT NULL COMMENT '周锚 yyyy-MM-dd（周一）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '答题人（区分大小写）',
    `question` varchar(140) NOT NULL DEFAULT '' COMMENT '本周井题（Bank 存卷）',
    `answer` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '我的答案',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_well_week` (`space_id`, `week`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F295 许愿井周问';

-- F296 时光胶囊接龙：给 TA 写 1/2/3 年后的一笔，到点对方拆
CREATE TABLE IF NOT EXISTS `couple_relay_capsule` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写信人（区分大小写）',
    `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '留给未来的话',
    `open_day` varchar(10) NOT NULL COMMENT '开启日（封存日+1/2/3 年）',
    `status` varchar(10) NOT NULL DEFAULT 'SEALED' COMMENT 'SEALED OPENED（可空位不写 charset）',
    `opened_at` bigint(20) DEFAULT NULL COMMENT '拆封时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_relay_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F296 胶囊接龙（区别于 F84 自选远期）';

-- F297 解梦局：记梦投稿，对方一本正经胡说点评
CREATE TABLE IF NOT EXISTS `couple_dream_case` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '梦日 yyyy-MM-dd',
    `dreamer_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '做梦人（区分大小写）',
    `dream` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '梦到什么',
    `reading` varchar(300) NOT NULL DEFAULT '' COMMENT '解梦官点评',
    `read_by` varchar(50) NOT NULL DEFAULT '' COMMENT '解梦官（可空位不写 charset）',
    `good` tinyint(4) DEFAULT NULL COMMENT '1=解得灵 / 0=胡说八道',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dream_day` (`space_id`, `day`, `dreamer_user`),
    KEY `idx_dream_read` (`space_id`, `read_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F297 解梦局（区别于 F141 自己记梦）';

-- F298 周年愿望台账：一年一愿，次年盖章圆上/鸽了
CREATE TABLE IF NOT EXISTS `couple_anniv_wish` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `year` varchar(4) NOT NULL COMMENT '周年年份 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '许愿人（区分大小写）',
    `wish` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '这一年的愿望',
    `verdict` varchar(10) NOT NULL DEFAULT '' COMMENT '空=待定 KEPT 圆上了 PIGEON 鸽了（可空位不写 charset）',
    `verdict_at` bigint(20) DEFAULT NULL COMMENT '盖章时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wish_year` (`space_id`, `year`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F298 周年愿望台账';

-- F299 未来信用卡：承诺未来小事，兑现提额逾期降额
CREATE TABLE IF NOT EXISTS `couple_future_credit` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `promise` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '承诺的小事',
    `due_day` varchar(10) NOT NULL COMMENT '兑现期限 yyyy-MM-dd（未来日）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '承诺人（区分大小写）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN KEPT BROKEN（可空位不写 charset）',
    `kept_at` bigint(20) DEFAULT NULL COMMENT '兑现时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_credit_space` (`space_id`, `from_user`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F299 未来信用卡（区别于 F71 记录当下的存折）';
