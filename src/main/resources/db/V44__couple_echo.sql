-- 批次三十一 F350-F359 回音壁（把「TA 爱我」的证据存下来，低落时取出来充电）
-- 涉及表：couple_echo_deed / couple_echo_juice / couple_echo_refill_log / couple_echo_slow
--         couple_echo_highlight / couple_echo_receipt / couple_echo_battery / couple_echo_self_letter
-- F353 被爱日历、F359 年报为读时聚合，无新表
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空/枚举位用普通 varchar；uk/idx 名一律带 echo 前缀（全库唯一已查重）
-- 单记录人口径（F350 修订）：couple_echo_deed 只有 from_user=记录人，语义是「记录人写下 TA 为自己做的一件事」

-- F350 好事簿：TA 爱我的证据，由记录人收藏；内容不进唯一键，同日同人同内容重复由服务层查重
CREATE TABLE IF NOT EXISTS `couple_echo_deed` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人：写下 TA 为自己做的一件事（区分大小写）',
    `content` varchar(120) NOT NULL DEFAULT '' COMMENT '事情本身（≤80 字，宽度放宽）',
    `day` varchar(10) NOT NULL COMMENT '发生日 yyyy-MM-dd',
    `starred` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=记录人点过「这条救过我」（仅记录人本人，幂等）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_echo_deed` (`space_id`, `from_user`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F350 好事簿（单记录人模型）';

-- F352 鼓励语罐：每人 5 格，第 6 条装不下；删掉腾出的格子可复用
CREATE TABLE IF NOT EXISTS `couple_echo_juice` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写鼓励语的人（区分大小写）',
    `idx` int(11) NOT NULL DEFAULT 1 COMMENT '罐子槽位 1-5（uk 占位，删除后空槽复用）',
    `content` varchar(90) NOT NULL DEFAULT '' COMMENT '鼓励语（≤60 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_echo_juice` (`space_id`, `from_user`, `idx`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F352 鼓励语罐（每人 ≤5 条）';

-- F351 能量补给领取日志：每人每天一次（重复 400）
CREATE TABLE IF NOT EXISTS `couple_echo_refill_log` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '领补给的人（区分大小写）',
    `day` varchar(10) NOT NULL COMMENT '领取日 yyyy-MM-dd',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_echo_refill` (`space_id`, `from_user`, `day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F351 能量补给领取日志（每人每天一次）';

-- F354 感谢慢递：想谢的话封存 7 天后送达，在途每人 ≤3 封，到日读时惰性结算
CREATE TABLE IF NOT EXISTS `couple_echo_slow` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写信的人（区分大小写）',
    `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收信的人=对方（区分大小写）',
    `content` varchar(150) NOT NULL DEFAULT '' COMMENT '想谢的话（≤100 字，宽度放宽）',
    `open_day` varchar(10) NOT NULL COMMENT '送达日=写信日+7 yyyy-MM-dd',
    `delivered` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=已送达（open_day<=今天 且 delivered=0 读时置 1 并推双方）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_echo_slow` (`space_id`, `from_user`, `delivered`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F354 感谢慢递（在途每人 ≤3 封）';

-- F355 高光重放：moment/did/feel 三行字段，每人 ≤12 条，本人可删
CREATE TABLE IF NOT EXISTS `couple_echo_highlight` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收藏这条高光的人（区分大小写）',
    `moment` varchar(60) NOT NULL DEFAULT '' COMMENT '什么时候（≤40 字，宽度放宽）',
    `did` varchar(120) NOT NULL DEFAULT '' COMMENT '做了什么（≤80 字，宽度放宽）',
    `feel` varchar(120) NOT NULL DEFAULT '' COMMENT '当时什么感觉（≤80 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_echo_highlight` (`space_id`, `from_user`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F355 高光重放（每人 ≤12 条三行卡）';

-- F356 夸夸回执：对 couple_praise（F54 夸夸墙，跨模块只读）某句点「收到」，uk 保证幂等
CREATE TABLE IF NOT EXISTS `couple_echo_receipt` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `quote_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '夸夸墙条目 id（关联 couple_praise.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '回执的人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_echo_receipt` (`space_id`, `quote_id`, `from_user`),
    KEY `idx_echo_receipt_user` (`space_id`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F356 夸夸回执（重复点不重复记）';

-- F357 电量预报：每人每天一格，level 1-5 钳制；对方 ≤2 格读时给「今晚轻轻的」提示
CREATE TABLE IF NOT EXISTS `couple_echo_battery` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `day` varchar(10) NOT NULL COMMENT '预报日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '报电量的人（区分大小写）',
    `level` int(11) NOT NULL DEFAULT 3 COMMENT '电量 1-5（服务层钳制）',
    `want` varchar(60) NOT NULL DEFAULT '' COMMENT '今天想被怎样对待（≤40 字，宽度放宽）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_echo_battery` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F357 电量预报（每人每天一格）';

-- F358 写给低落的自己：一人同时一封在途（SEALED 存在即 400，查询约束不用唯一函数索引）
CREATE TABLE IF NOT EXISTS `couple_echo_self_letter` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '写信给自己的人（区分大小写）',
    `content` varchar(400) NOT NULL DEFAULT '' COMMENT '想对低落的自己说的话（≤300 字，宽度放宽）',
    `status` varchar(10) NOT NULL DEFAULT 'SEALED' COMMENT 'SEALED 在途 / READ 已开读（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_echo_self_letter` (`space_id`, `from_user`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F358 写给低落的自己（一人同时一封在途）';
