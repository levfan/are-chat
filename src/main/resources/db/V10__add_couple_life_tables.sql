-- V10__add_couple_life_tables.sql
-- 目的：共同生活模块——记账本、家务轮值、约会规划、双人习惯（+打卡日志）、暗号小本本；2026-02-02
-- 涉及表：couple_expense（新建）、couple_chore（新建）、couple_date_plan（新建）、
--         couple_habit（新建）、couple_habit_log（新建）、couple_cipher（新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- 甜蜜记账本：日常开销谁花的记一笔，月度对比 + AA 差额提示
CREATE TABLE IF NOT EXISTS `couple_expense` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '付款人用户名（区分大小写）',
    `amount` bigint(20) NOT NULL COMMENT '金额（分）',
    `category` varchar(16) NOT NULL DEFAULT 'OTHER' COMMENT '分类：FOOD 餐饮 / TRANSPORT 交通 / FUN 娱乐 / HOME 日用 / GIFT 礼物 / OTHER 其他',
    `note` varchar(100) DEFAULT NULL COMMENT '花在什么上',
    `spent_day` varchar(10) NOT NULL COMMENT '花销日期（yyyy-MM-dd）',
    `created` bigint(20) NOT NULL COMMENT '记录时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_expense_space` (`space_id`, `spent_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣甜蜜记账本表：一起花的钱记清楚，月底看看谁付得多，AA 差额一目了然';

-- 家务轮值表：定义家务 + 轮值方式，今天该谁做一目了然，打卡翻转
CREATE TABLE IF NOT EXISTS `couple_chore` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `title` varchar(60) NOT NULL COMMENT '家务名，如「洗碗」「倒垃圾」',
    `rotate` varchar(16) NOT NULL DEFAULT 'ALTERNATE' COMMENT '轮值方式：SINGLE 固定一人 / ALTERNATE 每次轮换',
    `turn` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '当前值日生用户名（区分大小写）',
    `done_count` int(11) NOT NULL DEFAULT 0 COMMENT '累计完成次数',
    `last_done_day` varchar(10) DEFAULT NULL COMMENT '最近一次完成日期（yyyy-MM-dd）',
    `last_done_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '最近一次完成人',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_chore_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣家务轮值表：家务分工不再靠嘴说，完成打卡自动轮换值日生';

-- 约会规划卡：计划一场约会（时间/地点/想做的事），完成后归档成回忆
CREATE TABLE IF NOT EXISTS `couple_date_plan` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `title` varchar(60) NOT NULL COMMENT '约会主题，如「周五去看展」',
    `plan_day` varchar(10) NOT NULL COMMENT '约会日期（yyyy-MM-dd）',
    `place` varchar(100) DEFAULT NULL COMMENT '地点',
    `items` varchar(500) DEFAULT NULL COMMENT '想做的事，换行分隔（最多 500 字）',
    `status` varchar(16) NOT NULL DEFAULT 'PLANNED' COMMENT '状态：PLANNED 计划中 / DONE 已完成',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间（毫秒时间戳）',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '创建人用户名',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_date_plan_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣约会规划表：把「下次一起」变成有日期的约定，完成后自动归档进时光轴';

-- 双人习惯养成：一起坚持一个习惯，每日双方打卡
CREATE TABLE IF NOT EXISTS `couple_habit` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `title` varchar(60) NOT NULL COMMENT '习惯名，如「23:30 前睡」「每天喝够 8 杯水」',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '创建人用户名',
    `active` tinyint(4) DEFAULT 1 COMMENT '是否进行中：1 进行中 / 0 已结束',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_habit_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣双人习惯表：一起坚持一件小事，断签互相提醒';

-- 双人习惯打卡日志：每人每天一条（空间+习惯+人+日唯一）
CREATE TABLE IF NOT EXISTS `couple_habit_log` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `habit_id` varchar(36) NOT NULL COMMENT '所属习惯ID，关联 couple_habit.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '打卡人用户名（区分大小写）',
    `log_day` varchar(10) NOT NULL COMMENT '打卡日期（yyyy-MM-dd）',
    `created` bigint(20) NOT NULL COMMENT '打卡时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_couple_habit_log` (`habit_id`,`username`,`log_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣双人习惯打卡日志表：双方都打卡才算这一天共同坚持';

-- 暗号小本本：只有彼此懂的暗号/梗/悄悄约定
CREATE TABLE IF NOT EXISTS `couple_cipher` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `keyword` varchar(40) NOT NULL COMMENT '暗号词，如「菠萝」',
    `meaning` varchar(200) NOT NULL COMMENT '它的意思，如「想你了，快来找我」',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '创建人用户名',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_cipher_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣暗号本表：把只有彼此懂的梗和暗号记下来，随时对上频率';
