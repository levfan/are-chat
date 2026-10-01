-- V28__couple_museum.sql
-- 目的：时光博物馆·回忆进阶+系统贴心（F190-F199）：恋爱纪录片分镜、博物馆展品档案、隐藏彩蛋成就、
--       家规宪法、通知免打扰时段（F192 去年今日 / F193 银发情话 / F194 高频词 / F198 问候引擎 / F199 年度目录为无表聚合）。
-- 涉及表：couple_doc_scene / couple_exhibit / couple_hidden_achievement /
--         couple_house_rule / couple_dnd_setting（均新建）
-- 幂等方式：CREATE TABLE IF NOT EXISTS

-- F190 恋爱纪录片分镜：把一段回忆写成三幕剧本
CREATE TABLE IF NOT EXISTS `couple_doc_scene` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `title` varchar(60) NOT NULL COMMENT '这一部纪录片的名字',
    `act_one` varchar(300) NOT NULL COMMENT '第一幕：相识',
    `act_two` varchar(300) NOT NULL COMMENT '第二幕：相知',
    `act_three` varchar(300) NOT NULL COMMENT '第三幕：相伴',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '编剧用户名（区分大小写）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_doc_scene` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='恋爱纪录片分镜表：我们的故事，值得三幕讲完';

-- F191 恋爱博物馆登记：旧票根/小物件的文字展品档案
CREATE TABLE IF NOT EXISTS `couple_exhibit` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `name` varchar(60) NOT NULL COMMENT '展品名（第一场电影票根）',
    `story` varchar(300) NOT NULL DEFAULT '' COMMENT '展品背后的故事',
    `obtained_day` varchar(10) DEFAULT NULL COMMENT '藏品日期（可空；H2 兼容：可空列不带 charset）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '登记人用户名（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_exhibit` (`space_id`, `obtained_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='博物馆展品表：小物件不值钱，值钱的是它记得那天';

-- F195 隐藏彩蛋成就：行为达标自动解锁（定义在 CoupleMuseumBank，无表内容）
CREATE TABLE IF NOT EXISTS `couple_hidden_achievement` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `code` varchar(40) NOT NULL COMMENT '成就编码（静态白名单）',
    `unlocked_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '达成时触发人用户名（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '解锁时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hidden_achievement` (`space_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='隐藏成就表：有些惊喜，是日子替你们藏的';

-- F196 家规宪法：共同制定家规条款+修正案，对方可签字
CREATE TABLE IF NOT EXISTS `couple_house_rule` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `kind` varchar(20) NOT NULL DEFAULT 'RULE' COMMENT '类型 RULE 条款 / AMENDMENT 修正案',
    `ref_id` varchar(36) DEFAULT NULL COMMENT '修正案针对的原条款ID（可空；H2 兼容：可空列不带 charset）',
    `content` varchar(200) NOT NULL COMMENT '条款内容',
    `proposed_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '提出人用户名（区分大小写）',
    `signed` int(11) NOT NULL DEFAULT 0 COMMENT '对方是否已签字 0 未签 1 已签',
    `signed_by` varchar(50) DEFAULT NULL COMMENT '签字人（可空；H2 兼容：可空列不带 charset）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_house_rule` (`space_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='家规宪法表：约法三章不嫌少，商量着来的才算数';

-- F197 通知免打扰时段：每人一份，深夜静音不弹窗，消息都在
CREATE TABLE IF NOT EXISTS `couple_dnd_setting` (
    `id` varchar(36) NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) NOT NULL COMMENT '情侣空间ID（关联 couple_space.id）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '设置人用户名（区分大小写）',
    `start_time` varchar(5) NOT NULL COMMENT '免打扰开始时刻（HH:mm）',
    `end_time` varchar(5) NOT NULL COMMENT '免打扰结束时刻（HH:mm，可跨零点）',
    `enabled` int(11) NOT NULL DEFAULT 1 COMMENT '是否启用 0 停用 1 启用',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dnd_user` (`space_id`,`from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='免打扰设置表：晚安之后的安静，也是陪伴的一部分';
