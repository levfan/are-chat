-- 批次二十六 F300-F309 今日我是别人（扮演剧场）
-- 涉及表：couple_role_day / couple_swap_diary / couple_master_day / couple_booth_note / couple_private_ref
--         couple_act_award / couple_if_family / couple_role_movie / couple_service_ticket
-- F309 冷知识颁奖礼为读时聚合无表
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空/枚举位用普通 varchar

-- F300 今日身份签：双方同日同身份，日终互评
CREATE TABLE IF NOT EXISTS `couple_role_day` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '扮演日 yyyy-MM-dd',
    `role_name` varchar(30) NOT NULL DEFAULT '' COMMENT '今日身份（Bank 存档）',
    `guide` varchar(200) NOT NULL DEFAULT '' COMMENT '与该身份相处指南',
    `rate_a` int(11) DEFAULT NULL COMMENT 'userA 日终评分 1-5',
    `rate_b` int(11) DEFAULT NULL COMMENT 'userB 日终评分',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F300 今日身份签';

-- F301 一日互换日记：各写一页「作为 TA」，次日双齐互见
CREATE TABLE IF NOT EXISTS `couple_swap_diary` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '日记日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '执笔人（区分大小写）',
    `text` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作为 TA 的一天',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_diary_day` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F301 一日互换日记（区别于 F264 换位信）';

-- F302 师徒日：一周一卦，徒每日侍奉打卡、师期满评语定级
CREATE TABLE IF NOT EXISTS `couple_master_day` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `week` varchar(10) NOT NULL COMMENT '周锚 yyyy-MM-dd（周一）',
    `master_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '师父（区分大小写）',
    `apprentice_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '徒弟（区分大小写）',
    `serves` varchar(40) NOT NULL DEFAULT '' COMMENT '徒侍奉日期逗号分隔（每日一次）',
    `review` varchar(100) NOT NULL DEFAULT '' COMMENT '师父期满评语',
    `grade` varchar(10) NOT NULL DEFAULT '' COMMENT '出师 GRADUATED / 留级 REPEAT / 空=在途',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_master_week` (`space_id`, `week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F302 师徒日';

-- F303 时空电话亭：给一年前/后的 TA 留言，周年回放（读时惰性送达）
CREATE TABLE IF NOT EXISTS `couple_booth_note` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '拨打人（区分大小写）',
    `kind` varchar(6) NOT NULL DEFAULT 'FUTURE' COMMENT 'FUTURE 给一年后 / PAST 给一年前的我们（可空位不写 charset）',
    `text` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '电话内容',
    `open_day` varchar(10) NOT NULL COMMENT '回放日 yyyy-MM-dd',
    `status` varchar(10) NOT NULL DEFAULT 'SEALED' COMMENT 'SEALED SENT（可空位不写 charset）',
    `sent_at` bigint(20) DEFAULT NULL COMMENT '接通时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_booth_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F303 时空电话亭';

-- F304 黑话大全：只有一俩人懂的梗 + 抽查谁先忘
CREATE TABLE IF NOT EXISTS `couple_private_ref` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `term` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '黑话（同空间唯一）',
    `meaning` varchar(200) NOT NULL DEFAULT '' COMMENT '只有我们懂的意思',
    `origin` varchar(200) NOT NULL DEFAULT '' COMMENT '出处案发现场',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收录人（区分大小写）',
    `quiz_answer` varchar(200) NOT NULL DEFAULT '' COMMENT '对方最近一次抽查作答',
    `quiz_by` varchar(50) NOT NULL DEFAULT '' COMMENT '被抽查的人（可空位不写 charset）',
    `judged` varchar(10) NOT NULL DEFAULT '' COMMENT '抽查判定 RIGHT WRONG / 空=没抽查（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ref_term` (`space_id`, `term`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F304 黑话大全';

-- F305 奥斯卡：互提今日最佳演技一句话证据
CREATE TABLE IF NOT EXISTS `couple_act_award` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '提名日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '颁奖群众（区分大小写）',
    `about_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '被提名演员（区分大小写）',
    `evidence` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '一句话演技证据',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_act_day` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F305 每日奥斯卡提名';

-- F306 如果我是你爸妈：每日一题双答互见
CREATE TABLE IF NOT EXISTS `couple_if_family` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '拷问日 yyyy-MM-dd',
    `question` varchar(140) NOT NULL DEFAULT '' COMMENT '今日家长题（Bank 存卷）',
    `answer_a` varchar(200) NOT NULL DEFAULT '' COMMENT 'userA 作答',
    `answer_b` varchar(200) NOT NULL DEFAULT '' COMMENT 'userB 作答',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_family_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F306 如果我是你爸妈';

-- F307 双角色追剧：同一部剧各认领一角写角色日记，双完合剧本
CREATE TABLE IF NOT EXISTS `couple_role_movie` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `work` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '剧目名',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '认领人（区分大小写）',
    `role_name` varchar(20) NOT NULL DEFAULT '' COMMENT '认领的角色',
    `diary` varchar(600) NOT NULL DEFAULT '' COMMENT '角色日记（追更累积）',
    `status` varchar(10) NOT NULL DEFAULT 'ONGOING' COMMENT 'ONGOING FINISHED（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_movie_work` (`space_id`, `work`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F307 双角色追剧（区别于 F76 进度清单）';

-- F308 今日客服：下单-30 分钟响应-评分-差评申诉
CREATE TABLE IF NOT EXISTS `couple_service_ticket` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `note` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '服务工单（合理小事）',
    `customer_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '下单顾客（区分大小写）',
    `status` varchar(12) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN ANSWERED RATED APPEALED（可空位不写 charset）',
    `answered_at` bigint(20) DEFAULT NULL COMMENT '响应时间',
    `on_time` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=30 分钟内响应',
    `score` int(11) DEFAULT NULL COMMENT '顾客评分 1-5',
    `appeal` varchar(80) NOT NULL DEFAULT '' COMMENT '客服差评申诉',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_svc_ticket_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F308 今日客服';
