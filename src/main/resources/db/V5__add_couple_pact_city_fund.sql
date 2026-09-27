-- V5__add_couple_pact_city_fund.sql
-- 目的：情侣空间 P2 三件套——①恋爱条约表（一方提出、对方盖章生效）②couple_space 增加双方城市列（异地恋助手：
--       手填城市匹配内置城市库算时差/距离）③心愿基金两表（共同存钱目标 + 存入记录）
-- 涉及表：couple_pact（新建）、couple_space（加列 city_a/city_b）、couple_fund（新建）、couple_fund_deposit（新建）
-- 日期：2026-09-27
-- 说明：建表幂等（CREATE TABLE IF NOT EXISTS）。加列语句不用 IF NOT EXISTS——H2 MODE=MySQL 不认 MariaDB 方言的
--       「ADD COLUMN ... IF NOT EXISTS」（实测 42000 语法错），与规范中「不支持的变更靠 Flyway history 幂等」同策略：
--       两个库的首跑时该列必然不存在，重复执行由 flyway_schema_history 挡住；手工重复执行会报 duplicate column。
--       区分大小写的列（用户名）用列级 CHARACTER SET utf8mb4 COLLATE utf8mb4_bin 声明，不写 DEFAULT（语法对 H2/MySQL/MariaDB 通用）。

CREATE TABLE IF NOT EXISTS `couple_pact` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `content` varchar(100) NOT NULL COMMENT '条约内容，如「吵架不过夜」「每周一次约会日」',
    `proposed_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '提出人用户名（区分大小写）',
    `accepted_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '盖章人用户名（空=待对方盖章）',
    `accepted_at` bigint(20) DEFAULT NULL COMMENT '盖章时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '提出时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_pact_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣恋爱条约表：双方共同签署的甜蜜公约，一方提出、另一方盖章后生效';

ALTER TABLE `couple_space`
    ADD COLUMN `city_a` varchar(50) DEFAULT NULL COMMENT '用户 A 所在城市（手填，匹配内置城市库计算时差/距离）';

ALTER TABLE `couple_space`
    ADD COLUMN `city_b` varchar(50) DEFAULT NULL COMMENT '用户 B 所在城市（手填，匹配内置城市库计算时差/距离）';

CREATE TABLE IF NOT EXISTS `couple_fund` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `title` varchar(60) NOT NULL COMMENT '心愿名称，如「一起去北海道旅行」「换一台大电视」',
    `target_amount` bigint(20) NOT NULL COMMENT '目标金额（分，1 元 = 100 分）',
    `saved_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT '已存金额（分）',
    `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '基金状态：ACTIVE 攒钱中 / REACHED 已达成',
    `done_at` bigint(20) DEFAULT NULL COMMENT '达成时间（毫秒时间戳）',
    `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '创建人用户名',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_fund_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣心愿基金表：共同存钱目标，双方都能往里存钱，攒够自动庆祝';

CREATE TABLE IF NOT EXISTS `couple_fund_deposit` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `fund_id` varchar(36) NOT NULL COMMENT '所属心愿基金ID，关联 couple_fund.id',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '存钱人用户名（区分大小写）',
    `amount` bigint(20) NOT NULL COMMENT '存入金额（分）',
    `note` varchar(100) DEFAULT NULL COMMENT '存钱留言，如「这个月省下的奶茶钱」',
    `created` bigint(20) NOT NULL COMMENT '存入时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_couple_fund_dep_fund` (`fund_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='情侣心愿基金存入记录表：谁在什么时候存了多少，攒钱流水双方可见';
