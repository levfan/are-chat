-- 批次三十三 F370-F379 人生关卡（TA 的大日子，我不缺席）
-- 涉及表：couple_quest_battle / couple_quest_report / couple_quest_overtime / couple_quest_nurse
--         couple_quest_care_mark / couple_quest_pod / couple_quest_move / couple_quest_move_night
--         couple_quest_valley / couple_quest_win / couple_quest_upcoming
-- F378 关卡成就墙为读时聚合，无新表
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空/枚举位用普通 varchar；uk/idx 名一律带 quest 前缀（全库唯一已查重）
-- 双人列口径：沿用 couple_space 的 A/B 口径（A=字典序小者），tick_a/tick_b 属 userA/userB，
--             服务层按 mine = (me == userA) 读自己那一列；日期一律 varchar(10) yyyy-MM-dd，
--             CSV 位（打卡日/加油日）用 MMdd（见 are-chat-map 第四节「日期进 CSV 列」红线）。
-- 时间戳统一毫秒 bigint；主键 36 位 UUID 字符串

-- F370 关卡预告：宣布自己的 Boss 战（面试/汇报/答辩/谈判/体检/其它），在途每人 ≤3
CREATE TABLE IF NOT EXISTS `couple_quest_battle` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '打这一关的人（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '关卡日 yyyy-MM-dd（必须是今天或以后）',
    `kind` varchar(16) NOT NULL DEFAULT 'OTHER' COMMENT '关卡类型 INTERVIEW/REPORT/DEFEND/TALK/CHECKUP/OTHER（服务层校验）',
    `name` varchar(90) NOT NULL COMMENT '关卡名（≤30 字，宽度放宽）',
    `fear` varchar(180) NOT NULL DEFAULT '' COMMENT '一句怯场话（≤60 字，宽度放宽）',
    `status` varchar(10) NOT NULL DEFAULT 'PREP' COMMENT 'PREP 在途 / DONE 已报战报',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quest_battle` (`space_id`, `from_user`, `day`, `name`),
    KEY `idx_quest_battle_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F370 关卡预告（在途每人 ≤3）';

-- F371 出关战报：一战一报 WIN/LOSE/SURVIVE，对方按结果盖章
CREATE TABLE IF NOT EXISTS `couple_quest_report` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `battle_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属关卡（关联 couple_quest_battle.id，一战一报）',
    `result` varchar(10) NOT NULL COMMENT 'WIN 通关 / LOSE 没扛住 / SURVIVE 活着回来了（服务层校验）',
    `feeling` varchar(180) NOT NULL DEFAULT '' COMMENT '一句感受（≤60 字，宽度放宽）',
    `sealed_by` varchar(50) DEFAULT NULL COMMENT '盖章的人=对方（NULL=还没盖）',
    `sealed_at` bigint(20) DEFAULT NULL COMMENT '盖章时间毫秒',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quest_report_battle` (`battle_id`),
    KEY `idx_quest_report_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F371 出关战报（一战一报，对方盖章）';

-- F372 加班预报：今晚加班到几点，TA 可留一张「到家灯给你留着」卡
CREATE TABLE IF NOT EXISTS `couple_quest_overtime` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '预报日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '加班的人（区分大小写，每人每天一行）',
    `until_hour` int(11) NOT NULL DEFAULT 20 COMMENT '预计忙到几点 13-23（服务层钳制）',
    `note` varchar(120) NOT NULL DEFAULT '' COMMENT '一句说明（≤40 字，宽度放宽）',
    `lamp` varchar(180) NOT NULL DEFAULT '' COMMENT '对方留的灯卡（≤60 字，宽度放宽，空=还没留）',
    `lamp_by` varchar(50) DEFAULT NULL COMMENT '留灯的人（NULL=没留）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quest_overtime` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F372 加班预报与留灯卡';

-- F373 生病陪护单：TA 生病开单，喝水/吃药由陪护人代记，痊愈日关单庆典
CREATE TABLE IF NOT EXISTS `couple_quest_nurse` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `patient_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '生病的人（区分大小写，在途每人 ≤1）',
    `carer_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '陪护的人=对方',
    `open_day` varchar(10) NOT NULL COMMENT '开单日 yyyy-MM-dd',
    `close_day` varchar(10) NOT NULL DEFAULT '' COMMENT '痊愈关单日 yyyy-MM-dd（空=还在陪护）',
    `symptom` varchar(180) NOT NULL DEFAULT '' COMMENT '症状一句话（≤60 字，宽度放宽）',
    `message` varchar(240) NOT NULL DEFAULT '' COMMENT '陪护人的病中留言（≤80 字，宽度放宽）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN 陪护中 / CLOSED 已关单',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quest_nurse` (`space_id`, `patient_user`, `open_day`),
    KEY `idx_quest_nurse_status` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F373 生病陪护单（在途每人 ≤1）';

-- F373 陪护打卡：喝水/吃药由陪护人代记，一天每种只记一次
CREATE TABLE IF NOT EXISTS `couple_quest_care_mark` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `nurse_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属陪护单（关联 couple_quest_nurse.id）',
    `day` varchar(10) NOT NULL COMMENT '打卡日 yyyy-MM-dd',
    `kind` varchar(10) NOT NULL COMMENT 'WATER 喝水 / MED 吃药（服务层校验）',
    `by_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '代记的人=陪护人',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quest_care_mark` (`nurse_id`, `day`, `kind`, `by_user`),
    KEY `idx_quest_care_space` (`space_id`, `nurse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F373 陪护代记打卡';

-- F374 考试周静音舱：TA 入舱至某日，我只发加油卡（每日 ≤1），出舱提醒补长信
CREATE TABLE IF NOT EXISTS `couple_quest_pod` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '入舱的人（区分大小写）',
    `start_day` varchar(10) NOT NULL COMMENT '入舱日 yyyy-MM-dd',
    `until_day` varchar(10) NOT NULL COMMENT '出舱日 yyyy-MM-dd（必须晚于入舱日）',
    `status` varchar(10) NOT NULL DEFAULT 'IN' COMMENT 'IN 在舱 / OUT 已出舱',
    `cheers` varchar(320) NOT NULL DEFAULT '' COMMENT '已发加油卡的日期 CSV，MMdd 逗号分隔（每日一张，60 条 × 5 字符 = 300，留 320 余量）',
    `letter_done` tinyint(4) NOT NULL DEFAULT 0 COMMENT '出舱后的长信是否已补（1=已补）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quest_pod` (`space_id`, `from_user`, `start_day`),
    KEY `idx_quest_pod_status` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F374 考试周静音舱';

-- F375 搬家互助：8 个打包区块分工认领 + 纸箱计数
CREATE TABLE IF NOT EXISTS `couple_quest_move` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `slot` int(11) NOT NULL COMMENT '区块位 1-8（固定八格，服务层校验）',
    `name` varchar(60) NOT NULL DEFAULT '' COMMENT '区块名（厨房/书房…，≤20 字，宽度放宽）',
    `owner` varchar(50) DEFAULT NULL COMMENT '认领的人（NULL=没人认领）',
    `boxes` int(11) NOT NULL DEFAULT 0 COMMENT '打包纸箱数 0-99（服务层钳制）',
    `done` tinyint(4) NOT NULL DEFAULT 0 COMMENT '该区块是否打包完（1=完）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quest_move_slot` (`space_id`, `slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F375 搬家八区块分工';

-- F375 新家第一晚：双人才算庆祝打卡
CREATE TABLE IF NOT EXISTS `couple_quest_move_night` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '第一晚是哪天 yyyy-MM-dd',
    `tick_a` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userA 是否点过（1=点了）',
    `tick_b` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'userB 是否点过（1=点了）',
    `note` varchar(180) NOT NULL DEFAULT '' COMMENT '那一晚的一句话（≤60 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quest_move_night` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F375 新家第一晚双打卡';

-- F376 低谷通行证：TA 宣布最近状态不好（7-30 天），对方每日一张「不说话也行」卡
CREATE TABLE IF NOT EXISTS `couple_quest_valley` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '宣布进低谷的人（区分大小写，在途每人 ≤1）',
    `open_day` varchar(10) NOT NULL COMMENT '宣布日 yyyy-MM-dd',
    `until_day` varchar(10) NOT NULL COMMENT '预计回升日 yyyy-MM-dd（7-30 天内，服务层校验）',
    `status` varchar(10) NOT NULL DEFAULT 'LOW' COMMENT 'LOW 在谷底 / UP 已回升（本人主动定）',
    `care_days` varchar(240) NOT NULL DEFAULT '' COMMENT '已递卡的日期 CSV，MMdd 逗号分隔（一天一张）',
    `revive_day` varchar(10) NOT NULL DEFAULT '' COMMENT '实际宣布回升的日子 yyyy-MM-dd',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quest_valley` (`space_id`, `from_user`, `open_day`),
    KEY `idx_quest_valley_status` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F376 低谷通行证';

-- F377 小胜利账本：每天记一件做成的小事，周日互颁「小赢奖」
CREATE TABLE IF NOT EXISTS `couple_quest_win` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '做成小事的那天 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记账的人（区分大小写，每人每天一条）',
    `content` varchar(120) NOT NULL COMMENT '做成的小事（≤40 字，宽度放宽）',
    `award_day` varchar(10) NOT NULL DEFAULT '' COMMENT '被对方评为本周最佳的日子 yyyy-MM-dd（空=没获奖）',
    `awarded_by` varchar(50) NOT NULL DEFAULT '' COMMENT '颁奖的人（只能对方颁）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quest_win` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F377 小胜利账本与互颁小赢奖';

-- F379 下次关卡预约：未来 60 天已知关口挂双人时间轴，对方点「我会到场」
CREATE TABLE IF NOT EXISTS `couple_quest_upcoming` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '关口日子 yyyy-MM-dd（今天起 60 天内，服务层校验）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '挂上来的人（区分大小写）',
    `title` varchar(90) NOT NULL COMMENT '关口名（≤30 字，宽度放宽）',
    `attend_by` varchar(50) NOT NULL DEFAULT '' COMMENT '说「我会到场」的人=对方（空=还没应援）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quest_upcoming` (`space_id`, `day`, `from_user`, `title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F379 下次关卡预约（60 天内）';
