-- 批次二十四 F280-F289 我们百科（默契资产化）
-- 涉及表：couple_codex_entry / couple_quiz_show / couple_top_list / couple_top_guess / couple_petname_story
--         couple_exam / couple_place / couple_first_look / couple_habit_map / couple_taste_shift / couple_type_report
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空用户名/枚举位用普通 varchar

-- F280 词条共建：「我们的词」考据，双人共编
CREATE TABLE IF NOT EXISTS `couple_codex_entry` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `term` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '词条（同空间唯一）',
    `definition` varchar(200) NOT NULL DEFAULT '' COMMENT '释义',
    `origin` varchar(200) NOT NULL DEFAULT '' COMMENT '出处（何时何地诞生）',
    `usage_note` varchar(200) NOT NULL DEFAULT '' COMMENT '现行用法',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '首建人（区分大小写）',
    `updated_by` varchar(50) NOT NULL DEFAULT '' COMMENT '最近编辑人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_codex_term` (`space_id`, `term`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F280 词条共建';

-- F281 默契综艺：每日一期，从词条出 5 题填空，双答算默契率
CREATE TABLE IF NOT EXISTS `couple_quiz_show` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '期日 yyyy-MM-dd',
    `terms` varchar(600) NOT NULL DEFAULT '' COMMENT '五个填空词条逗号分隔（按 stableHash 选）',
    `answer_a` varchar(240) NOT NULL DEFAULT '' COMMENT 'userA 五答逗号分隔',
    `answer_b` varchar(240) NOT NULL DEFAULT '' COMMENT 'userB 五答逗号分隔',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quiz_day` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F281 默契综艺：一天一期，同答算默契';

-- F282 喜好 TOP10：各类目本人榜
CREATE TABLE IF NOT EXISTS `couple_top_list` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '类目键',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '榜单主人（区分大小写）',
    `items` varchar(600) NOT NULL DEFAULT '' COMMENT '十条逗号分隔',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_top_list` (`space_id`, `category`, `owner_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F282 喜好 TOP10 本人榜';

-- F282 喜好互猜：猜对方榜单，揭榜后差异进「重新认识清单」
CREATE TABLE IF NOT EXISTS `couple_top_guess` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '类目键',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '被猜的榜主（区分大小写）',
    `guesser_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '猜测人（区分大小写）',
    `items` varchar(600) NOT NULL DEFAULT '' COMMENT '猜测十条逗号分隔',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_top_guess` (`space_id`, `category`, `guesser_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F282 喜好互猜';

-- F283 外号考据：每个爱称的诞生故事
CREATE TABLE IF NOT EXISTS `couple_petname_story` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `nickname` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '外号（同空间唯一）',
    `given_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '起名的人（区分大小写）',
    `occasion` varchar(200) NOT NULL DEFAULT '' COMMENT '诞生场合',
    `story` varchar(300) NOT NULL DEFAULT '' COMMENT '考据故事',
    `first_used_day` varchar(10) NOT NULL DEFAULT '' COMMENT '首次使用日',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '录入人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_petname` (`space_id`, `nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F283 外号考据';

-- F284 友情测验：给对方出「你记得吗」题，错则 7 天后补考
CREATE TABLE IF NOT EXISTS `couple_exam` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `question` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '题面',
    `answer` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '标准答案（出题人存）',
    `quizzed_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '被考的人（区分大小写）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '出题人（区分大小写）',
    `verdict` varchar(10) NOT NULL DEFAULT '' COMMENT '空=待考 RIGHT WRONG（可空位不写 charset）',
    `last_try_day` varchar(10) NOT NULL DEFAULT '' COMMENT '最近作答日（ WRONG 后 7 天冷却）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_q` (`space_id`, `question`),
    KEY `idx_exam_space` (`space_id`, `quizzed_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F284 友情测验';

-- F285 去过的地方：足迹档案
CREATE TABLE IF NOT EXISTS `couple_place` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '地名（同空间唯一）',
    `year` varchar(4) NOT NULL DEFAULT '' COMMENT '去的年份 yyyy',
    `happened` varchar(200) NOT NULL DEFAULT '' COMMENT '发生了什么',
    `rating` int(11) NOT NULL DEFAULT 5 COMMENT '回味指数 1-5 钳制',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '录入人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_place_name` (`space_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F285 去过的地方（区别于 F75 未来心愿）';

-- F286 第一眼对视：双盲提交「你注意到我是哪一刻」，一致或各满 3 次互见
CREATE TABLE IF NOT EXISTS `couple_first_look` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提交人（区分大小写）',
    `moment` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '那一刻的描述',
    `tries` int(11) NOT NULL DEFAULT 1 COMMENT '已提交次数（≤3）',
    `revealed` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=已互见',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_firstlook_user` (`space_id`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F286 第一眼对视双盲';

-- F287 习惯图鉴：观察 TA 的小习惯，TA 可标确实/冤枉
CREATE TABLE IF NOT EXISTS `couple_habit_map` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `habit` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '习惯描述',
    `tag` varchar(20) NOT NULL DEFAULT '' COMMENT '频率标签（每天/偶尔/突发）',
    `observer_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '观察员（区分大小写）',
    `target_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '被观察的人（区分大小写）',
    `verdict` varchar(10) NOT NULL DEFAULT '' COMMENT '空=待判 REAL WRONG（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_habit_map` (`space_id`, `habit`, `observer_user`),
    KEY `idx_habit_target` (`space_id`, `target_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F287 习惯图鉴';

-- F288 口味变迁：以前不爱现在爱
CREATE TABLE IF NOT EXISTS `couple_taste_shift` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `thing` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '口味对象（香菜/悬疑片…）',
    `before_text` varchar(100) NOT NULL DEFAULT '' COMMENT '以前',
    `now_text` varchar(100) NOT NULL DEFAULT '' COMMENT '现在',
    `shifted_day` varchar(10) NOT NULL DEFAULT '' COMMENT '转折日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '本人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_taste_thing` (`space_id`, `thing`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F288 口味变迁';

-- F289 人格双报：8 题速测四维类型，一年一报
CREATE TABLE IF NOT EXISTS `couple_type_report` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `year` varchar(4) NOT NULL COMMENT '测年 yyyy',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '受测人（区分大小写）',
    `answers` varchar(30) NOT NULL DEFAULT '' COMMENT '八题选项 1/2 逗号分隔',
    `type_key` varchar(6) NOT NULL DEFAULT '' COMMENT '四维类型码（如 ETPJ）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_year` (`space_id`, `year`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F289 人格双报（区别于 F172 爱语测评）';
