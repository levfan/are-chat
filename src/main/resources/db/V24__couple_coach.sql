-- V24__couple_coach.sql
-- 目的：更好的我们·成长系（F150-F159）：21天习惯搭子、感恩便签墙、情绪颗粒度日记、
--       每周高光互评、共读一分钟、拖延互助所、优点存折
--       （F156 早安能量站 / F157 自习室番茄钟 / F159 成长年度关键词为无表或纯前端）。
-- 涉及表：couple_habit_streak / couple_thanks_note / couple_feel_log /
--         couple_weekly_star / couple_read_minute / couple_delay_task / couple_praise_bank（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F150 21天习惯搭子：各自立习惯每日打卡，满 target 天自动达成
-- （表名避开 V10 既有 couple_habit 双人习惯）
CREATE TABLE IF NOT EXISTS `couple_habit_streak` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '立习惯人用户名（区分大小写）',
    `title` varchar(60) NOT NULL COMMENT '习惯名（每天读书30分钟）',
    `target_days` int(11) NOT NULL DEFAULT 21 COMMENT '目标天数',
    `done_days` int(11) NOT NULL DEFAULT 0 COMMENT '已打卡天数',
    `last_done_day` varchar(10) DEFAULT NULL COMMENT '最近打卡日期（yyyy-MM-dd）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 挑战中 / DONE 已达成',
    `done_at` bigint(20) DEFAULT NULL COMMENT '达成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_habit_streak` (`space_id`, `status`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='21天习惯搭子表：TA 盯着的日子，总不好意思偷懒';

-- F151 感恩便签墙：每天一句感谢对方的小事
CREATE TABLE IF NOT EXISTS `couple_thanks_note` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '致谢人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '感谢内容',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_thanks_note` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='感恩便签墙：把「理所当然」写回「谢谢」';

-- F152 情绪颗粒度日记：每日一个细名情绪+强度（说得出情绪，才管得住情绪）
CREATE TABLE IF NOT EXISTS `couple_feel_log` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `word` varchar(20) NOT NULL COMMENT '细名情绪词（委屈/雀跃/怅然…）',
    `intensity` int(11) NOT NULL DEFAULT 3 COMMENT '强度 1-5',
    `note` varchar(200) DEFAULT NULL COMMENT '一句注脚（可空）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_feel_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情绪颗粒度日记表：情绪词汇量越大，越不需要用吵架说话';

-- F153 每周高光互评：每周提名对方的一个高光瞬间
CREATE TABLE IF NOT EXISTS `couple_weekly_star` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '所属周（周一日期 yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提名人工用户名（区分大小写）',
    `highlight` varchar(200) NOT NULL COMMENT '对方本周的高光瞬间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_week_star` (`space_id`,`week`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每周高光互评表：被看见，是关系里最好的营养';

-- F154 共读一分钟：每日一段小文共读+各自一句感想
CREATE TABLE IF NOT EXISTS `couple_read_minute` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '所属日期（yyyy-MM-dd）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '感想人用户名（区分大小写）',
    `thought` varchar(200) NOT NULL COMMENT '今日感想',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_read_day_user` (`space_id`,`day`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='共读一分钟表：读同一段文字，是在精神里散步';

-- F155 拖延互助所：登记拖延的事，对方催办（1h 冷却），完成庆祝
CREATE TABLE IF NOT EXISTS `couple_delay_task` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '立事人用户名（区分大小写）',
    `title` varchar(100) NOT NULL COMMENT '拖延的事（去体检/交年报）',
    `deadline_day` varchar(10) DEFAULT NULL COMMENT '截止日期（可空）',
    `nag_count` int(11) NOT NULL DEFAULT 0 COMMENT '被催次数',
    `last_nag_at` bigint(20) DEFAULT NULL COMMENT '最近催办时间（毫秒）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 拖着 / DONE 完成',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_delay_task` (`space_id`, `status`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='拖延互助所表：成年人的自律，需要一个盯着你的爱人';

-- F158 优点存折：平时存对方优点，吵架时读三条
CREATE TABLE IF NOT EXISTS `couple_praise_bank` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存款人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '对方的一个优点',
    `scene` varchar(60) DEFAULT NULL COMMENT '存入场景（可空：吵架时读）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_praise_bank` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优点存折表：生气时先取三条利息，再决定要不要吵架';
