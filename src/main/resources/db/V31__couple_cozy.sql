-- V31__couple_cozy.sql
-- 目的：批次十八 F220-F229 体温同步·作息与健康——熄灯/睡眠单/数羊/喝水接力/冷暖/熬夜守护/慢生活/疼痛对策/抱抱
-- 涉及表：couple_cozy_lightout / couple_cozy_sleep / couple_cozy_sheep / couple_cozy_water
--         / couple_cozy_weather / couple_cozy_latenight / couple_cozy_slow / couple_cozy_remedy / couple_cozy_hug（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

CREATE TABLE IF NOT EXISTS `couple_cozy_lightout` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '熄灯日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发晚安的人（区分大小写）',
    `at_time` varchar(5) NOT NULL DEFAULT '' COMMENT '按点时刻 HH:mm',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lightout` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F220 晚安同熄灯：双方都发晚安=当日熄灯';

CREATE TABLE IF NOT EXISTS `couple_cozy_sleep` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '报告的是哪一晚 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '报告人',
    `stars` int NOT NULL COMMENT '睡眠质量自评 1-5',
    `dream` varchar(140) NOT NULL DEFAULT '' COMMENT '一句梦话',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sleep` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F221 睡眠报告单：晨间各报昨夜自评';

CREATE TABLE IF NOT EXISTS `couple_cozy_sheep` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪晚数羊 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '数羊人',
    `taps` int NOT NULL DEFAULT 0 COMMENT '已点的羊数（满 10 算数完）',
    `done` int NOT NULL DEFAULT 0 COMMENT '0 进行中 / 1 数完一群',
    `elapsed_ms` int NOT NULL DEFAULT 0 COMMENT '本人数完用时毫秒',
    `created` bigint(20) NOT NULL COMMENT '首次按键时间（毫秒）',
    `updated_at` bigint(20) NOT NULL COMMENT '最近按键时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sheep` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F222 数羊房：双方各点满 10 下一起数完一群羊';

CREATE TABLE IF NOT EXISTS `couple_cozy_water` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪天 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '喝水的人',
    `cups` int NOT NULL DEFAULT 0 COMMENT '今日杯数',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '最近一杯时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_water` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F223 喝水接力：我喝一杯给 TA 的杯子加一格';

CREATE TABLE IF NOT EXISTS `couple_cozy_weather` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪天的体感 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '互报的人',
    `city` varchar(60) NOT NULL COMMENT '所在城市（纯文字）',
    `feel` varchar(20) NOT NULL COMMENT '体感：冷/暖/刚好',
    `temp_text` varchar(20) NOT NULL DEFAULT '' COMMENT '气温文字',
    `advised_by` varchar(50) NOT NULL DEFAULT '' COMMENT '谁叮嘱过添衣（可空列不带 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_weather` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F224 冷暖互报：自报体感+对方一键叮嘱';

CREATE TABLE IF NOT EXISTS `couple_cozy_latenight` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪天递卡 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '守护者（递卡人）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_latenight` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F225 熬夜守护：深夜递早点睡陪伴卡，一天一张';

CREATE TABLE IF NOT EXISTS `couple_cozy_slow` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提议人',
    `thing` varchar(140) NOT NULL COMMENT '一件什么都不赶的小事',
    `done_day` varchar(10) NOT NULL DEFAULT '' COMMENT '打卡日（空=未打卡）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slow` (`space_id`, `week`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F226 周末慢生活：周五各提一件小事周日打卡回放';

CREATE TABLE IF NOT EXISTS `couple_cozy_remedy` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `for_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '这份对策是给谁的',
    `body` varchar(500) NOT NULL COMMENT '正确做法清单文字',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_remedy` (`space_id`, `for_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F227 疼痛对策本：我难受时的正确做法';

CREATE TABLE IF NOT EXISTS `couple_cozy_hug` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '哪天抱的 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '自报的人',
    `cnt` int NOT NULL COMMENT '这一次抱了几下',
    `note` varchar(140) NOT NULL DEFAULT '' COMMENT '一句备注',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_hug_space` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F228 抱抱计量器：见面拥抱自报计数攒里程碑';
