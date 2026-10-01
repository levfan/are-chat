-- 批次二十八 F320-F329 修复车间（安全感与修复）
-- 涉及表：couple_repair_freeze / couple_sorry_review / couple_repair_redo / couple_rebuild_plan
--         couple_repair_makeup / couple_bottom_line / couple_admit_log / couple_repair_box / couple_peace_line
-- F325 冲突类型年报为读时聚合无表（读 F61 矛盾复盘与本批数据）
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空/枚举位用普通 varchar；uk/idx 名一律本模块前缀（全库唯一）

-- F320 冷冻解冻规程：吵架挂冷冻（3-24h），解冻需双人签 + 三问走完
CREATE TABLE IF NOT EXISTS `couple_repair_freeze` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `start_day` varchar(10) NOT NULL COMMENT '挂冻结日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提出冷冻的人（区分大小写）',
    `hours` int(11) NOT NULL DEFAULT 3 COMMENT '冷冻时长 3-24 小时',
    `until_at` bigint(20) NOT NULL COMMENT '解冻时刻（毫秒时间戳）',
    `reason` varchar(140) NOT NULL DEFAULT '' COMMENT '为什么要缓一缓',
    `answer1` varchar(140) NOT NULL DEFAULT '' COMMENT '解冻三问之一：我怕的是',
    `answer2` varchar(140) NOT NULL DEFAULT '' COMMENT '解冻三问之二：我其实要的是',
    `answer3` varchar(140) NOT NULL DEFAULT '' COMMENT '解冻三问之三：我能先做的',
    `signed_a` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=userA 已签解冻',
    `signed_b` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=userB 已签解冻',
    `status` varchar(10) NOT NULL DEFAULT 'FROZEN' COMMENT 'FROZEN THAWED（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_repair_freeze` (`space_id`, `start_day`),
    KEY `idx_repair_freeze_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F320 冷冻解冻规程（F268 休战旗是 30 分钟短停）';

-- F321 道歉质检：六要素自评 + 对方验货，退回重写或进陈列室
CREATE TABLE IF NOT EXISTS `couple_sorry_review` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '道歉的人（区分大小写）',
    `letter` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '道歉信正文',
    `points` varchar(120) NOT NULL DEFAULT '' COMMENT '六要素自评编码 CSV：FACT/FEEL/BLAME/SORRY/FIX/ASK',
    `status` varchar(10) NOT NULL DEFAULT 'VERIFY' COMMENT 'VERIFY PASSED BACK（可空位不写 charset）',
    `verdict` varchar(80) NOT NULL DEFAULT '' COMMENT '对方验货批注',
    `verified_by` varchar(50) NOT NULL DEFAULT '' COMMENT '验货人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_sorry_review_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F321 道歉质检（F62 道歉券是凭证，此为质量）';

-- F322 重来卡：每季一张，重放那段对话并记满意度
CREATE TABLE IF NOT EXISTS `couple_repair_redo` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `quarter` varchar(8) NOT NULL COMMENT '季度 2026Q4',
    `scene` varchar(140) NOT NULL DEFAULT '' COMMENT '要重放的那段对话（哪次/在哪/吵什么）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '持卡申请人（区分大小写）',
    `used` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=已重放',
    `replay_note` varchar(200) NOT NULL DEFAULT '' COMMENT '重放时改说了什么',
    `satisfaction` int(11) DEFAULT NULL COMMENT '重放满意度 1-5',
    `rated_by` varchar(50) NOT NULL DEFAULT '' COMMENT '打分的人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_repair_redo` (`space_id`, `quarter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F322 重来卡（每季一张）';

-- F323 信任重建 30 天：任务卡 + 双方签 + 周复盘，断签可续
CREATE TABLE IF NOT EXISTS `couple_rebuild_plan` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '计划名（同空间唯一）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '开计划的人（区分大小写）',
    `cause` varchar(140) NOT NULL DEFAULT '' COMMENT '因为哪件事重建',
    `start_day` varchar(10) NOT NULL COMMENT '起始日 yyyy-MM-dd',
    `target_days` int(11) NOT NULL DEFAULT 30 COMMENT '目标天数 14/30/60',
    `tasks` varchar(600) NOT NULL DEFAULT '' COMMENT '每日任务卡 CSV（≤10 条）',
    `signed_days` varchar(600) NOT NULL DEFAULT '' COMMENT '签到记号 CSV（MMdd:A / MMdd:B）',
    `review` varchar(200) NOT NULL DEFAULT '' COMMENT '周复盘（可改写）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN DONE GIVENUP（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rebuild_plan` (`space_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F323 信任重建 30 天';

-- F324 和好了倒计时：冷战开 10-60 分钟倒计时，对方可暂停/提前，到点递台阶卡
CREATE TABLE IF NOT EXISTS `couple_repair_makeup` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '冷战日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '开倒计时的人（区分大小写）',
    `minutes` int(11) NOT NULL DEFAULT 20 COMMENT '时长 10-60 分钟',
    `start_at` bigint(20) NOT NULL COMMENT '起算时刻（毫秒时间戳）',
    `paused` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=对方暂停了',
    `paused_by` varchar(50) NOT NULL DEFAULT '' COMMENT '按下暂停的人（可空位不写 charset）',
    `step_card` varchar(140) NOT NULL DEFAULT '' COMMENT '到点递出的台阶卡',
    `status` varchar(10) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING OFFERED ENDED（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_repair_makeup` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F324 和好了倒计时';

-- F326 底线声明卡：各写 3 条底线 + 生效日，被踩要补红线记录
CREATE TABLE IF NOT EXISTS `couple_bottom_line` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '声明底线的人（区分大小写）',
    `slot` int(11) NOT NULL DEFAULT 1 COMMENT '第几条 1-3',
    `text` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '底线内容',
    `since_day` varchar(10) NOT NULL DEFAULT '' COMMENT '生效日 yyyy-MM-dd',
    `breach_count` int(11) NOT NULL DEFAULT 0 COMMENT '被踩次数',
    `breach_note` varchar(80) NOT NULL DEFAULT '' COMMENT '最近一次被踩说明',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bottom_line` (`space_id`, `from_user`, `slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F326 底线声明卡（每人 3 条）';

-- F327 我错了榜：认错需一句具体说明，对方可点「最感人」
CREATE TABLE IF NOT EXISTS `couple_admit_log` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '认错日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '认错的人（区分大小写）',
    `about_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '向谁认错（区分大小写）',
    `detail` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '错在哪（要具体）',
    `touched` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=对方标了最感人',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admit_day` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F327 我错了榜';

-- F328 修复礼盒：和好达成掉盒，开出补偿小任务，完成推 both
CREATE TABLE IF NOT EXISTS `couple_repair_box` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '掉盒日 yyyy-MM-dd',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '要做补偿任务的人（区分大小写）',
    `task` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '盒里开出的补偿小任务',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN DONE（可空位不写 charset）',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时刻',
    `from_freeze` varchar(36) NOT NULL DEFAULT '' COMMENT '来源冻结单 id（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_repair_box` (`space_id`, `day`, `owner_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F328 修复礼盒';

-- F329 和平纪念碑：每次大和好存一句「这段吵架最代表性的话」，周年回看
CREATE TABLE IF NOT EXISTS `couple_peace_line` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '和好日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '立碑的人（区分大小写）',
    `line` varchar(140) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '最代表性的那句话',
    `note` varchar(80) NOT NULL DEFAULT '' COMMENT '现在回看想说什么',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_peace_line` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F329 和平纪念碑';
