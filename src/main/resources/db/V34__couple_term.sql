-- 批次二十一 F250-F259 夫妻老黄历（中式岁时仪式）
-- 涉及表：couple_term_check / couple_term_ritual / couple_lucky_day / couple_festival_plan
--         couple_term_note / couple_holiday_wish / couple_normal_day
--         couple_anniversary（加 calendar_type/lunar_md 列，F253 农历生日）
-- F256 生肖年运 / F259 一年日子小结为读时聚合无表
-- 幂等方式：CREATE TABLE IF NOT EXISTS + ALTER ADD COLUMN IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 约定：utf8mb4_bin 用户名列 NOT NULL 且不写 DEFAULT；普通列可 NOT NULL DEFAULT ''

-- F250 节气跟风机：节气当日一键「跟上了」+晒一句话，双报日标齐
CREATE TABLE IF NOT EXISTS `couple_term_check` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `term` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '节气名（两字）',
    `year` varchar(4) NOT NULL COMMENT '年份 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '跟上的人（区分大小写）',
    `note` varchar(140) NOT NULL DEFAULT '' COMMENT '晒的一句话',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_term_check` (`space_id`, `term`, `year`, `from_user`),
    KEY `idx_term_check_space` (`space_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F250 节气跟风：每人每节气每年一记';

-- F251 节气过法：给任一节气定固定过法（每节气≤2），当日打卡推 TA
CREATE TABLE IF NOT EXISTS `couple_term_ritual` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `term` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '节气名（两字）',
    `content` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '过法一句话',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提议人（区分大小写）',
    `last_done_year` varchar(4) NOT NULL DEFAULT '' COMMENT '最近打卡年份，空=没打过',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_term_ritual` (`space_id`, `term`, `content`),
    KEY `idx_term_ritual_space` (`space_id`, `term`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F251 节气过法：每节气最多 2 条，年年可重复打卡';

-- F252 择吉日：给大事挑一个我们的吉日，发起+对方确认双盖章
CREATE TABLE IF NOT EXISTS `couple_lucky_day` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '吉日 yyyy-MM-dd',
    `matter` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '要办的大事（文本）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '择日人（区分大小写）',
    `confirmed` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=对方已双盖章',
    `confirmed_by` varchar(50) NOT NULL DEFAULT '' COMMENT '确认人（可空位不写 charset）',
    `comment` varchar(140) NOT NULL DEFAULT '' COMMENT '黄历点评（Bank 生成存档）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lucky_day` (`space_id`, `day`, `matter`),
    KEY `idx_lucky_space` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F252 择吉日：同一天同一事唯一';

-- F253 农历生日换算：纪念日/生日增加历法类型与农历月日（两位数字文本）
ALTER TABLE `couple_anniversary` ADD COLUMN IF NOT EXISTS `calendar_type` varchar(10) NOT NULL DEFAULT 'SOLAR' COMMENT '历法：SOLAR 公历 / LUNAR 农历';
ALTER TABLE `couple_anniversary` ADD COLUMN IF NOT EXISTS `lunar_md` varchar(5) NOT NULL DEFAULT '' COMMENT '农历月日 MMDD（LUNAR 时有效，闰月按正月计）';

-- F254 节日家档：8 大节日每年怎么过，双提交成对照页
CREATE TABLE IF NOT EXISTS `couple_festival_plan` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `festival` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '节日键（NEWYEAR/CHUXI/VALENTINE/L520/QIXI/MIDAUTUMN/NATIONAL/ANNIV-M）',
    `year` varchar(4) NOT NULL COMMENT '年份 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写方案的人（区分大小写）',
    `plan` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '今年怎么过',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_festival_plan` (`space_id`, `festival`, `year`, `from_user`),
    KEY `idx_festival_space` (`space_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F254 节日家档：一人一年一案，双案对照';

-- F255 一节气一件事：24 节气手账，每节气一句话，年末成一年日历
CREATE TABLE IF NOT EXISTS `couple_term_note` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `term` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '节气名（两字）',
    `year` varchar(4) NOT NULL COMMENT '年份 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '执笔人（区分大小写）',
    `text` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '这个节气的一件小事',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_term_note` (`space_id`, `term`, `year`, `from_user`),
    KEY `idx_term_note_space` (`space_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F255 节气手账：本人可改写';

-- F257 下个长假倒数：内置法定假日倒数，一人写干什么另一人可补
CREATE TABLE IF NOT EXISTS `couple_holiday_wish` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `holiday` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '假期键（如 2026-SPRING）',
    `day` varchar(10) NOT NULL COMMENT '假期首日 yyyy-MM-dd',
    `wish` varchar(200) NOT NULL DEFAULT '' COMMENT '干什么（两人共写一段）',
    `wished_by` varchar(50) NOT NULL DEFAULT '' COMMENT '首写人（可空位不写 charset）',
    `appended_by` varchar(50) NOT NULL DEFAULT '' COMMENT '补写人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_holiday_wish` (`space_id`, `holiday`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F257 长假愿望：首写+补写两段合一';

-- F258 反仪式感日：每年挑 3 天「什么都不做」，系统挡打卡推「偷得浮生」卡
CREATE TABLE IF NOT EXISTS `couple_normal_day` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `year` varchar(4) NOT NULL COMMENT '年份 yyyy',
    `day` varchar(10) NOT NULL COMMENT '放空日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提报人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_normal_day` (`space_id`, `year`, `day`),
    KEY `idx_normal_space` (`space_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F258 反仪式感日：每年最多 3 天，双方提报合并';
