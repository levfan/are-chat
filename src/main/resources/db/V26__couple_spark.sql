-- V26__couple_spark.sql
-- 目的：懂你更多·默契亲密系（F170-F179）：爱语测评、心动闪光、如果问答、动作暗语、
--       同频共振、心动日历（F171 爱语对照 / F176 默契仪表盘 / F178 同频排行 / F179 周报为无表聚合）。
-- 涉及表：couple_love_lang / couple_heart_flash / couple_what_if /
--         couple_secret_signal / couple_sync_tap / couple_heart_day（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F170 爱语测评：五爱语 12 题静态卷，重测覆盖
CREATE TABLE IF NOT EXISTS `couple_love_lang` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作答人用户名（区分大小写）',
    `words_score` int(11) NOT NULL DEFAULT 0 COMMENT '肯定的言语得分',
    `time_score` int(11) NOT NULL DEFAULT 0 COMMENT '用心陪伴得分',
    `gifts_score` int(11) NOT NULL DEFAULT 0 COMMENT '接受礼物得分',
    `service_score` int(11) NOT NULL DEFAULT 0 COMMENT '服务的行动得分',
    `touch_score` int(11) NOT NULL DEFAULT 0 COMMENT '身体的接触得分',
    `primary_lang` varchar(20) NOT NULL COMMENT '主爱语（WORDS/TIME/GIFTS/SERVICE/TOUCH）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_love_lang_user` (`space_id`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='爱语测评表：爱要用对方的语言说，才不算白说';

-- F172 心动闪光捕捉：突然心动的一刻速记
CREATE TABLE IF NOT EXISTS `couple_heart_flash` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `moment` varchar(200) NOT NULL COMMENT '心动的一刻',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_heart_flash` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心动闪光表：把突然软下来的瞬间存住，想念时取用';

-- F173 「如果」问答：每天一道脑洞题，双答互见，先答者今日默契之星
CREATE TABLE IF NOT EXISTS `couple_what_if` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '作答人用户名（区分大小写）',
    `answer` varchar(200) NOT NULL COMMENT '答案',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_whatif_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='「如果」问答表：聊点不着边际的，反而最懂彼此';

-- F174 动作暗语本：约定一个动作=一句话
CREATE TABLE IF NOT EXISTS `couple_secret_signal` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '约定人用户名（区分大小写）',
    `signal` varchar(50) NOT NULL COMMENT '动作（捏三下手心）',
    `meaning` varchar(100) NOT NULL COMMENT '含义（我爱你，别怕）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_secret_signal` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='动作暗语本：人前不能说的话，都写在动作里';

-- F175 同频共振：双方 10 秒内先后按键，差值毫秒定默契
CREATE TABLE IF NOT EXISTS `couple_sync_tap` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `attempts` int(11) NOT NULL DEFAULT 0 COMMENT '尝试次数',
    `best_ms` bigint(20) DEFAULT NULL COMMENT '最小时间差（毫秒）',
    `last_tap_user` varchar(50) DEFAULT NULL COMMENT '最近按键人（可空；H2 兼容：可空列不带 charset）',
    `last_tap_at` bigint(20) DEFAULT NULL COMMENT '最近按键时间（毫秒）',
    `hits` int(11) NOT NULL DEFAULT 0 COMMENT '同频成功次数（差值<=500ms）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sync_tap_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='同频共振表：不用言语也能对上拍子的两个人';

-- F177 心动日历：每日标记心动等级 1-3
CREATE TABLE IF NOT EXISTS `couple_heart_day` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '标记人用户名（区分大小写）',
    `level` int(11) NOT NULL DEFAULT 1 COMMENT '心动等级 1-3（1 平淡 2 甜甜 3 心动爆棚）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_heart_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='心动日历表：给每天的心情盖一个心动邮戳';
