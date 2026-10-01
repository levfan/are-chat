-- 批次二十七 F310-F319 身体通知系统（健康关照 2.0）
-- 涉及表：couple_body_metric / couple_body_snore / couple_body_cycle / couple_body_quit / couple_body_fit
--         couple_body_sos / couple_body_redline / couple_body_checkup / couple_body_med / couple_body_oath
-- 联动：F319 早睡军令状读 couple_cozy_lightout；F316 忌口红线读 couple_dining_ticket（均为只读拼标，不改他模块）
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空/枚举位用普通 varchar

-- F310 体征互报：体温/体重/睡眠时长数值打卡，超自设阈值自动推 TA
CREATE TABLE IF NOT EXISTS `couple_body_metric` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '记录日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '报本人（区分大小写）',
    `temp` varchar(10) NOT NULL DEFAULT '' COMMENT '体温文本（如 37.2）',
    `weight` varchar(10) NOT NULL DEFAULT '' COMMENT '体重文本（kg）',
    `sleep_hours` varchar(10) NOT NULL DEFAULT '' COMMENT '睡眠时长文本（小时）',
    `temp_limit` varchar(10) NOT NULL DEFAULT '' COMMENT '自设体温线，空=不设',
    `sleep_limit` varchar(10) NOT NULL DEFAULT '' COMMENT '自设睡眠下限，空=不设',
    `note` varchar(80) NOT NULL DEFAULT '' COMMENT '一句状态',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_body_metric` (`space_id`, `day`, `from_user`),
    KEY `idx_body_metric_space` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F310 体征互报（F221 睡眠单是感受星级，此为数值）';

-- F311 呼噜自报：晨起各报档位，对方可补「震感」点评
CREATE TABLE IF NOT EXISTS `couple_body_snore` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '晨起日 yyyy-MM-dd',
    `level_a` varchar(10) NOT NULL DEFAULT '' COMMENT 'userA 档位 NONE/TINY/MID/HEAVY，空=没报',
    `level_b` varchar(10) NOT NULL DEFAULT '' COMMENT 'userB 档位',
    `shake_a` varchar(60) NOT NULL DEFAULT '' COMMENT 'B 给 A 的震感点评',
    `shake_b` varchar(60) NOT NULL DEFAULT '' COMMENT 'A 给 B 的震感点评',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_body_snore` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F311 呼噜自报';

-- F312 周期共览：本人标当天阶段与不适，TA 递照顾卡
CREATE TABLE IF NOT EXISTS `couple_body_cycle` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '标记日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '标记人（区分大小写）',
    `phase` varchar(12) NOT NULL DEFAULT '' COMMENT 'MENSTRUATING/BEFORE/AFTER/OWULARE，空=没标',
    `discomfort` varchar(60) NOT NULL DEFAULT '' COMMENT '当天不适',
    `care_card` varchar(100) NOT NULL DEFAULT '' COMMENT 'TA 递的照顾卡内容',
    `care_by` varchar(50) NOT NULL DEFAULT '' COMMENT '递卡人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_body_cycle` (`space_id`, `day`, `from_user`),
    KEY `idx_body_cycle_space` (`space_id`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F312 周期共览（F8x 生理期关怀是单方关怀，此为双人共览）';

-- F313 戒烟戒糖互助营：双设目标，破戒留痕 + 陪绑方安慰词
CREATE TABLE IF NOT EXISTS `couple_body_quit` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '目标名（同空间同 OWNER 唯一）',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '戒的人（区分大小写）',
    `target_days` int(11) NOT NULL DEFAULT 21 COMMENT '营期天数 7/21/60/100',
    `start_day` varchar(10) NOT NULL COMMENT '开营日 yyyy-MM-dd',
    `broke_days` varchar(160) NOT NULL DEFAULT '' COMMENT '破戒日期逗号分隔',
    `cheer` varchar(140) NOT NULL DEFAULT '' COMMENT '陪绑方最近一句安慰词',
    `cheer_by` varchar(50) NOT NULL DEFAULT '' COMMENT '安慰词作者（可空位不写 charset）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN DONE GONE（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_body_quit` (`space_id`, `owner_user`, `name`),
    KEY `idx_body_quit_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F313 戒烟戒糖互助营';

-- F314 运动链：同日同项目两人各报计数，30 分钟内双报算接上链
CREATE TABLE IF NOT EXISTS `couple_body_fit` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '运动日 yyyy-MM-dd',
    `kind` varchar(12) NOT NULL DEFAULT 'PUSHUP' COMMENT 'PUSHUP SQUAT PLANK RUN STRETCH（可空位不写 charset）',
    `count_a` int(11) NOT NULL DEFAULT 0 COMMENT 'userA 计数',
    `count_b` int(11) NOT NULL DEFAULT 0 COMMENT 'userB 计数',
    `at_a` bigint(20) NOT NULL DEFAULT 0 COMMENT 'A 报数时刻',
    `at_b` bigint(20) NOT NULL DEFAULT 0 COMMENT 'B 报数时刻',
    `linked` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=两人 30 分钟窗口内都报过',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_body_fit` (`space_id`, `day`, `kind`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F314 运动链（F223 喝水接力是喝水，此为运动 PK）';

-- F315 身体不适 SOS：一键不舒服 + 症状 + 从何时，TA 收「能做什么」话术卡
CREATE TABLE IF NOT EXISTS `couple_body_sos` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '不舒服的人（区分大小写）',
    `symptom` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '症状一句话',
    `since` varchar(30) NOT NULL DEFAULT '' COMMENT '从何时起（口语文本）',
    `status` varchar(10) NOT NULL DEFAULT 'SENT' COMMENT 'SENT HELD（可空位不写 charset）',
    `comfort` varchar(100) NOT NULL DEFAULT '' COMMENT 'TA 选的「我能做」',
    `hold_by` varchar(50) NOT NULL DEFAULT '' COMMENT '接住的人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_body_sos_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F315 身体不适 SOS（F144 情绪 SOS 是情绪，此为躯体）';

-- F316 忌口红线本：过敏/忌口清单，饭桌饭票池读同表拼标
CREATE TABLE IF NOT EXISTS `couple_body_redline` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `item` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '忌口项（同空间唯一）',
    `kind` varchar(10) NOT NULL DEFAULT 'AVOID' COMMENT 'ALLERGY AVOID（可空位不写 charset）',
    `note` varchar(60) NOT NULL DEFAULT '' COMMENT '为什么不行',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '登记人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_body_redline` (`space_id`, `item`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F316 忌口红线本';

-- F317 体检陪同：约体检 + TA 虚拟陪同到场打卡 + 检后一句话互见
CREATE TABLE IF NOT EXISTS `couple_body_checkup` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '体检日 yyyy-MM-dd',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '体检的人（区分大小写）',
    `item` varchar(60) NOT NULL DEFAULT '' COMMENT '查什么',
    `status` varchar(12) NOT NULL DEFAULT 'PLAN' COMMENT 'PLAN ATTENDED REPORTED（可空位不写 charset）',
    `companion` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=TA 已虚拟陪同到场',
    `report` varchar(140) NOT NULL DEFAULT '' COMMENT '检后一句话报告',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_body_checkup` (`space_id`, `day`, `owner_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F317 体检陪同';

-- F318 情绪药友：周记式「最近药怎么样」互助（非医嘱，陪伴话术）
CREATE TABLE IF NOT EXISTS `couple_body_med` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '周锚 yyyy-MM-dd（周一）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '登记人（区分大小写）',
    `how` varchar(12) NOT NULL DEFAULT '' COMMENT 'STEADY/HARD/NONE 三档，空=没记',
    `note` varchar(140) NOT NULL DEFAULT '' COMMENT '自愿写两句',
    `reply` varchar(140) NOT NULL DEFAULT '' COMMENT 'TA 的陪伴话术（Bank 兜底）',
    `reply_by` varchar(50) NOT NULL DEFAULT '' COMMENT '话术作者（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_body_med` (`space_id`, `week`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F318 情绪药友（陪伴非医嘱）';

-- F319 早睡军令状：双签本周熄灯线，违约率接 F220 熄灯数据
CREATE TABLE IF NOT EXISTS `couple_body_oath` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `week` varchar(10) NOT NULL COMMENT '周锚 yyyy-MM-dd（周一）',
    `line_a` varchar(5) NOT NULL DEFAULT '' COMMENT 'userA 熄灯线 HH:mm',
    `line_b` varchar(5) NOT NULL DEFAULT '' COMMENT 'userB 熄灯线 HH:mm',
    `signed_a` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=A 已签',
    `signed_b` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=B 已签',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_body_oath` (`space_id`, `week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F319 早睡军令状';
