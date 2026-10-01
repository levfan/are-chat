-- 批次二十九 F330-F339 两家与朋友（情侣看世界）
-- 涉及表：couple_world_visit / couple_world_gift / couple_world_friend_view / couple_world_declare
--         couple_world_caption / couple_world_city_plan / couple_world_relatives_q / couple_world_vow
--         couple_world_group_report / couple_world_apology
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空/枚举位用普通 varchar；uk/idx 名一律带本模块前缀（全库唯一）

-- F330 拜访攻略：回谁家前置任务卡双确认，回访后写战报
CREATE TABLE IF NOT EXISTS `couple_world_visit` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '拜访日 yyyy-MM-dd',
    `host_side` varchar(10) NOT NULL DEFAULT 'MINE' COMMENT 'MINE 我家 / YOURS 你家（可空位不写 charset）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写攻略的人（区分大小写）',
    `preps` varchar(600) NOT NULL DEFAULT '' COMMENT '前置任务卡 CSV（带什么/聊什么/雷区，≤8 条）',
    `confirmed` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=对方已确认这份攻略',
    `report` varchar(200) NOT NULL DEFAULT '' COMMENT '回访战报',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN DONE（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_world_visit` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F330 拜访攻略';

-- F331 送礼互助池：收 TA 亲友可能喜欢 + 接单代买 + 节前排雷
CREATE TABLE IF NOT EXISTS `couple_world_gift` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `person` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '送谁（如：我妈/你弟）',
    `idea` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '可能喜欢什么（同空间唯一）',
    `budget` varchar(20) NOT NULL DEFAULT '' COMMENT '预算区间文本',
    `avoid` varchar(60) NOT NULL DEFAULT '' COMMENT '雷点，节前排雷用',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '登记人（区分大小写）',
    `taker_user` varchar(50) NOT NULL DEFAULT '' COMMENT '接单代买的人（可空位不写 charset）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN TAKEN BOUGHT（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_world_gift_idea` (`space_id`, `idea`),
    KEY `idx_world_gift_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F331 送礼互助池（F73 心愿互换是给彼此）';

-- F332 朋友视角问卷：三题「外人怎么看我们」，线下问友回填出他观卡
CREATE TABLE IF NOT EXISTS `couple_world_friend_view` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `slot` int(11) NOT NULL DEFAULT 1 COMMENT '第几题 1-3',
    `question` varchar(140) NOT NULL DEFAULT '' COMMENT '题面（Bank 存卷）',
    `asked_to` varchar(20) NOT NULL DEFAULT '' COMMENT '问了谁（朋友称谓）',
    `answer` varchar(200) NOT NULL DEFAULT '' COMMENT '朋友的原话',
    `by_user` varchar(50) NOT NULL DEFAULT '' COMMENT '回填的人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_world_friend_slot` (`space_id`, `slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F332 朋友视角问卷';

-- F333 官宣日：每月一张纯文字官宣卡，成官宣编年
CREATE TABLE IF NOT EXISTS `couple_world_declare` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `month` varchar(7) NOT NULL COMMENT '官宣月 yyyy-MM',
    `text` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '官宣文案',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发卡的人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_world_declare` (`space_id`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F333 官宣日（每月一张）';

-- F334 文案代写：各交三候选互评选稿，定稿进百科
CREATE TABLE IF NOT EXISTS `couple_world_caption` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '求稿日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '交稿的人（区分大小写）',
    `slot` int(11) NOT NULL DEFAULT 1 COMMENT '第几候选 1-3',
    `text` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '候选文案',
    `won` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=被定为稿',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_world_caption` (`space_id`, `day`, `from_user`, `slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F334 文案代写（每人每日三候选）';

-- F335 进城接待方案：TA 来访城市的行程/交通/陪同小包共建手册
CREATE TABLE IF NOT EXISTS `couple_world_city_plan` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `city` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '城市（同空间唯一手册）',
    `arrive_day` varchar(10) NOT NULL DEFAULT '' COMMENT '预计到访日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '做东的人（区分大小写）',
    `itinerary` varchar(600) NOT NULL DEFAULT '' COMMENT '行程 CSV（≤8 条）',
    `transport` varchar(140) NOT NULL DEFAULT '' COMMENT '交通怎么接',
    `pack_list` varchar(300) NOT NULL DEFAULT '' COMMENT '陪同小包清单 CSV',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_world_city` (`space_id`, `city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F335 进城接待方案（F112 见面倒数是情绪，此为攻略）';

-- F336 亲戚称呼册：称谓关系测验，错题进考前强化
CREATE TABLE IF NOT EXISTS `couple_world_relatives_q` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `term` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '称谓（同空间唯一）',
    `question` varchar(140) NOT NULL DEFAULT '' COMMENT '怎么叫/什么关系',
    `answer` varchar(60) NOT NULL DEFAULT '' COMMENT '标准答案',
    `wrong_count` int(11) NOT NULL DEFAULT 0 COMMENT '累计答错次数',
    `last_wrong_day` varchar(10) NOT NULL DEFAULT '' COMMENT '最近答错日',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '出题的人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_world_relatives` (`space_id`, `term`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F336 亲戚称呼册';

-- F337 社会信用：公开声明「我保证不做…」+ TA 见证 + 到期解除或塌房
CREATE TABLE IF NOT EXISTS `couple_world_vow` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `content` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '保证不做的事',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '立保证的人（区分大小写）',
    `due_day` varchar(10) NOT NULL COMMENT '到期日 yyyy-MM-dd',
    `witnessed` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=TA 已见证',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN KEPT BROKEN（可空位不写 charset）',
    `broken_note` varchar(80) NOT NULL DEFAULT '' COMMENT '塌房记录',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_world_vow` (`space_id`, `owner_user`, `content`),
    KEY `idx_world_vow_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F337 社会信用（F125 信任存折是积累，此为承诺）';

-- F338 群聊记者：每日一条「今天群里最好笑的是我们…」互递素材
CREATE TABLE IF NOT EXISTS `couple_world_group_report` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '素材日 yyyy-MM-dd',
    `line_a` varchar(200) NOT NULL DEFAULT '' COMMENT 'userA 的素材',
    `line_b` varchar(200) NOT NULL DEFAULT '' COMMENT 'userB 的素材',
    `laugh_a` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=A 笑了 B 那条',
    `laugh_b` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=B 笑了 A 那条',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_world_group_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F338 群聊记者';

-- F339 代 TA 赔礼：与 TA 亲友有误会，代写赔礼信经 TA 审阅才算送达
CREATE TABLE IF NOT EXISTS `couple_world_apology` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `to_person` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '赔礼对象（TA 的亲友称谓）',
    `reason` varchar(200) NOT NULL DEFAULT '' COMMENT '误会是怎么来的',
    `draft` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '信正文',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '求代写的人（区分大小写）',
    `review_note` varchar(140) NOT NULL DEFAULT '' COMMENT 'TA 的审阅意见',
    `reviewed_by` varchar(50) NOT NULL DEFAULT '' COMMENT '审阅人（可空位不写 charset）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN SENT BACK（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_world_apology_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F339 代 TA 赔礼（F261 替我说是表白向）';
