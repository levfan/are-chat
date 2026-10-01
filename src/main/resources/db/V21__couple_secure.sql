-- V21__couple_secure.sql
-- 目的：确定感·安全感批次（F120-F129）：安全感账户、十年之约、愿景板、承诺博物馆、
--       信任存折、双人契约、守护兽（F121 恋爱体检 / F126 恋爱年轮为聚合计算，无新表；
--       F127 大日子分类为 couple_anniversary 增加 kind 列）。
-- 涉及表：couple_security_bank / couple_decade_pact / couple_vision_card /
--         couple_oath / couple_trust_coin / couple_self_contract / couple_pet（均新建）
--         couple_anniversary（加 kind 列）
-- 幂等方式：CREATE TABLE IF NOT EXISTS / ADD COLUMN IF NOT EXISTS

-- F120 安全感账户：TA 说一句让你安心的话，你收进账户，余额就是底气
CREATE TABLE IF NOT EXISTS `couple_security_bank` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存入安心话的用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '安心话内容',
    `status` varchar(10) NOT NULL DEFAULT 'DEPOSITED' COMMENT '状态：DEPOSITED 已存入 / ACCEPTED 已收下',
    `accepted_at` bigint(20) DEFAULT NULL COMMENT '收下时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_security_bank` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='安全感账户表：安心的话存起来，需要勇气的时候来取';

-- F122 十年之约：双方各写一句「十年后的我们」，都写了就并排展示
CREATE TABLE IF NOT EXISTS `couple_decade_pact` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '立约人用户名（区分大小写，一人一条）',
    `content` varchar(200) NOT NULL COMMENT '十年后我们……',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_decade_user` (`space_id`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='十年之约表：十年很长，但说好了就是十年';

-- F123 愿景板：一人一张愿景卡，写了同一个词就是共鸣
CREATE TABLE IF NOT EXISTS `couple_vision_card` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '许愿人用户名（区分大小写）',
    `word` varchar(20) NOT NULL COMMENT '愿景关键词（一个词，同词即共鸣）',
    `note` varchar(100) DEFAULT NULL COMMENT '想多说的一句（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_vision_word` (`space_id`, `word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='愿景板表：把想要的未来贴上墙，重合的部分一起实现';

-- F124 承诺博物馆：郑重的承诺，双方都盖章后永久展出
CREATE TABLE IF NOT EXISTS `couple_oath` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '承诺人用户名（区分大小写）',
    `content` varchar(200) NOT NULL COMMENT '郑重承诺内容',
    `stamp_a` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 A 是否盖章：0 否 1 已盖',
    `stamp_b` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户 B 是否盖章：0 否 1 已盖',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_oath_stamp` (`space_id`, `stamp_a`, `stamp_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='承诺博物馆表：盖了章的话，是要进博物馆的';

-- F125 信任存折：给对方存信任币，攒的是「我信你」
CREATE TABLE IF NOT EXISTS `couple_trust_coin` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '存入人用户名（区分大小写）',
    `to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收币人用户名（区分大小写）',
    `reason` varchar(100) DEFAULT NULL COMMENT '为什么值得信（可空）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_trust_to` (`space_id`, `to_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='信任存折表：一枚一枚攒，攒成无条件的信任';

-- F128 双人契约：一起养成的契约（如「每天说晚安」），双方各自计数
CREATE TABLE IF NOT EXISTS `couple_self_contract` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(50) NOT NULL COMMENT '契约名称（如 每天说晚安）',
    `content` varchar(200) DEFAULT NULL COMMENT '契约内容补充（可空）',
    `count_a` int(11) NOT NULL DEFAULT 0 COMMENT '用户 A 完成次数',
    `count_b` int(11) NOT NULL DEFAULT 0 COMMENT '用户 B 完成次数',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_self_contract` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='双人契约表：说好一起做的事，一次一次攒默契';

-- F129 守护兽：双人共同养的守护兽，心情按最近照料时间惰性衰减
CREATE TABLE IF NOT EXISTS `couple_pet` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `name` varchar(20) NOT NULL COMMENT '守护兽名字',
    `kind` varchar(10) NOT NULL DEFAULT 'FOX' COMMENT '守护兽种类：FOX 狐狸 / CAT 猫 / BEAR 熊 / BUNNY 兔',
    `care_count` int(11) NOT NULL DEFAULT 0 COMMENT '累计照料次数',
    `last_care_at` bigint(20) DEFAULT NULL COMMENT '最近照料时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pet_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='守护兽表：你们的关系有了一只小兽看着，别忘喂它';

-- F127 大日子分类：纪念日/倒数日增加 kind（NORMAL 普通 / LOVE 恋爱 / FAMILY 家人 / FRIEND 朋友 / WORK 工作）
ALTER TABLE `couple_anniversary` ADD COLUMN IF NOT EXISTS `kind` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '日子类型：NORMAL 普通 / LOVE 恋爱 / FAMILY 家人 / FRIEND 朋友 / WORK 工作';
