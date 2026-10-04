-- ============================================================================
-- V52：v8 第一批——连续互动打卡（贴贴日）、每日一问、愿望清单
-- ============================================================================
-- 依据：用户需求「向好友发起邀请建立情侣空间 / 每日一问 / 愿望清单 / 连续互动打卡解锁」。
--       其中邀请流程与 couple_invite 早在 V1 就在线，本轮不动它的表；
--       打卡与问答所需的表在 V51 裁剪时已随功能下线被 drop，本轮重建为语义更窄的新表。
-- 口径：
--   1) 表名刻意避开被裁掉功能的旧名（couple_checkin 旧语义是「早安/晚安各自打卡」、
--      couple_answer 旧语义是「一问互评」），CONTEXT.md 要求一个概念只有一个名字，
--      新表承载的是新概念，不复用旧词：couple_bond_day（双方共同贴贴日）、
--      couple_question_answer（每日一问的回答）。
--   2) 连续天数与七档解锁**不建表**：与心动值一样是读时算，唯一事实源是 couple_bond_day
--      的 day 集合（含补签行），避免物化列与真源漂移。
--   3) 不给 couple_space 加「一用户只有一个有效空间」的数据库唯一约束：
--      可空 active_flag + UK 的写法在存量库若已存在同一用户多行 ACTIVE 会让本脚本
--      直接失败、导致服务起不来，这个失败模式在私有化部署环境不可接受；
--      该不变式仍由 accept() 的事前检查兜底，残留风险与理由登记在 docs/adr/0007。
--   4) 全部 CREATE TABLE IF NOT EXISTS：新库全量重放 V1→V52、老库只补差异，脚本幂等；
--      H2 MODE=MySQL 与 MariaDB 10.11 双兼容（charset/collate 列一律 NOT NULL，
--      可空用户名列用普通 varchar DEFAULT NULL，见 schema.sql 头部注释）。
-- 统计：新建 3 张表（couple_bond_day / couple_question_answer / couple_wish），
--       couple_* 表由 19 张增至 22 张。
-- ============================================================================

-- 贴贴打卡日：一个空间一行代表「这一天双方都贴贴过」，是连续天数的唯一事实源。
CREATE TABLE IF NOT EXISTS `couple_bond_day` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `day` varchar(10) NOT NULL COMMENT '打卡日期（yyyy-MM-dd，自然日）',
    `source` varchar(10) NOT NULL DEFAULT 'AUTO' COMMENT '确认方式：AUTO 双方当天都发过贴贴 / MAKEUP 补签',
    `operator_user` varchar(50) DEFAULT NULL COMMENT '补签操作人（AUTO 时为 NULL）',
    `created` bigint(20) NOT NULL COMMENT '确认时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple8_bond_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='贴贴打卡日表：一行=一天双方共同贴贴过（或补签），连续天数与解锁档位由本表的 day 集合算出';

-- 每日一问：一个空间一天一题，每人每天一行回答；双方都答完才互看。
CREATE TABLE IF NOT EXISTS `couple_question_answer` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `day` varchar(10) NOT NULL COMMENT '问题日期（yyyy-MM-dd，同空间同天同题）',
    `question_index` int(11) NOT NULL COMMENT '题库下标（按空间+天稳定哈希得到，落库留快照）',
    `question` varchar(200) NOT NULL COMMENT '题目原文（当日快照，题库后续扩充不影响历史）',
    `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '回答人用户名（区分大小写）',
    `answer` varchar(300) NOT NULL COMMENT '回答内容（当天可改写）',
    `created` bigint(20) NOT NULL COMMENT '首次回答时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近改写时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple8_question_day_user` (`space_id`,`day`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日一问回答表：每人每天一行，双方都答过之后才互相可见';

-- 愿望清单：我想要什么东西；对方可以偷偷标记「已准备」，该状态对被许愿人不可见。
CREATE TABLE IF NOT EXISTS `couple_wish` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `owner_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '许愿人用户名（想要这个东西的人）',
    `creator_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '记录人用户名（可以不是许愿人本人）',
    `title` varchar(80) NOT NULL COMMENT '想要的东西（一句话，80 字内）',
    `note` varchar(200) DEFAULT NULL COMMENT '补充说明（款式/尺码/什么时候想要）',
    `status` varchar(16) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN 待实现 / PREPARED 对方已偷偷准备（仅标记人可见） / FULFILLED 已实现',
    `prepared_by` varchar(50) DEFAULT NULL COMMENT '谁标记的「已准备」（只有对方能标自己许的愿）',
    `prepared_at` bigint(20) DEFAULT NULL COMMENT '标记已准备的时间（毫秒时间戳）',
    `fulfilled_at` bigint(20) DEFAULT NULL COMMENT '愿望实现的时间（毫秒时间戳）',
    `created` bigint(20) NOT NULL COMMENT '创建时间（毫秒时间戳）',
    `updated_at` bigint(20) DEFAULT NULL COMMENT '最近修改时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple8_wish_title` (`space_id`,`owner_user`,`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='愿望清单表：双方互相添加想要的东西，PREPARED 状态对被许愿人保密直到本人确认实现';
