-- 批次二十三 F270-F279 二人制造厂（生活协作 2.0）
-- 涉及表：couple_spin_task / couple_shop_item / couple_stock / couple_parcel / couple_wake_word
--         couple_medicine / couple_standup / couple_advance / couple_grocery / couple_home_check
-- 幂等方式：CREATE TABLE IF NOT EXISTS；H2(MODE=MySQL) 与 MariaDB 双兼容
-- 红线：utf8mb4_bin 列 NOT NULL 且不写 DEFAULT；可空/默认空用户名位用普通 varchar

-- F270 家务轮盘：周锚分派，结果双签生效，完成打勾，赖账进欠账栏
CREATE TABLE IF NOT EXISTS `couple_spin_task` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `week` varchar(10) NOT NULL COMMENT '周锚 yyyy-MM-dd（周一）',
    `item` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '家务事项',
    `assigned_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '轮到天选之人（区分大小写）',
    `confirmed` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=双方认账（对方点认）',
    `done` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=干完了',
    `done_at` bigint(20) DEFAULT NULL COMMENT '完成时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spin_item` (`space_id`, `week`, `item`),
    KEY `idx_spin_space` (`space_id`, `week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F270 家务轮盘：一周一转，items 逐条落行';

-- F271 采买清单：超市清单共编，买了打勾推 TA，月终点名最多者=生活委员
CREATE TABLE IF NOT EXISTS `couple_shop_item` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '要买的东西',
    `qty` varchar(30) NOT NULL DEFAULT '' COMMENT '数量文本',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '登记人（区分大小写）',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN DONE',
    `done_by` varchar(50) NOT NULL DEFAULT '' COMMENT '谁买回来的（可空位不写 charset）',
    `done_at` bigint(20) DEFAULT NULL COMMENT '买回时间（月榜统计用）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_shop_space` (`space_id`, `status`),
    KEY `idx_shop_done` (`space_id`, `done_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F271 采买清单';

-- F272 冰箱库存：食材文字库存，临期读时提示「今晚吃掉它」
CREATE TABLE IF NOT EXISTS `couple_stock` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `item` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '食材名（同空间同名唯一）',
    `qty` varchar(30) NOT NULL DEFAULT '' COMMENT '数量文本（两把/3个）',
    `put_day` varchar(10) NOT NULL DEFAULT '' COMMENT '入库日 yyyy-MM-dd',
    `expire_day` varchar(10) NOT NULL DEFAULT '' COMMENT '赏味期至，空=没写',
    `status` varchar(10) NOT NULL DEFAULT 'IN' COMMENT 'IN OUT',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '登记人（区分大小写）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_item` (`space_id`, `item`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F272 冰箱值守：同物同名互相覆盖提醒';

-- F273 代拿快递：下单-接单-送达，送达向台账插小额 EARN 感谢章
CREATE TABLE IF NOT EXISTS `couple_parcel` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `note` varchar(60) NOT NULL DEFAULT '' COMMENT '快递描述（几号柜/多大件）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '求代拿的人（区分大小写）',
    `status` varchar(10) NOT NULL DEFAULT 'SENT' COMMENT 'SENT GRABBED DONE',
    `grabber` varchar(50) NOT NULL DEFAULT '' COMMENT '接单侠（可空位不写 charset）',
    `done_at` bigint(20) DEFAULT NULL COMMENT '送达时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_parcel_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F273 代拿快递';

-- F274 叫醒服务：一周定一句叫醒词，另一人每天可递一张叫醒卡（每日一张）
CREATE TABLE IF NOT EXISTS `couple_wake_word` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `week` varchar(10) NOT NULL COMMENT '生效周 yyyy-MM-dd（周一）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '定制人（区分大小写）',
    `content` varchar(60) NOT NULL DEFAULT '' COMMENT '叫醒词',
    `given_day` varchar(10) NOT NULL DEFAULT '' COMMENT '最近递卡日（可空位不写 charset）',
    `given_by` varchar(50) NOT NULL DEFAULT '' COMMENT '最近递卡人（可空位不写 charset）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wake_week` (`space_id`, `week`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F274 叫醒服务：周词+日卡';

-- F275 服药提醒链：TA 点提醒了+本人点吃了成链，连续链读时聚合
CREATE TABLE IF NOT EXISTS `couple_medicine` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '药名（同人同名唯一）',
    `times` varchar(60) NOT NULL DEFAULT '' COMMENT '每日时段文本（早饭后/睡前）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '服药本人（区分大小写）',
    `status` varchar(10) NOT NULL DEFAULT 'ONGOING' COMMENT 'ONGOING STOPPED',
    `last_remind_day` varchar(10) NOT NULL DEFAULT '' COMMENT 'TA 最近提醒日',
    `last_taken_day` varchar(10) NOT NULL DEFAULT '' COMMENT '本人最近服下日',
    `streak` int(11) NOT NULL DEFAULT 0 COMMENT '连续链（断一天清零）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_med_name` (`space_id`, `name`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F275 服药提醒链';

-- F276 久坐互拍：一人一天一拍，双方间隔≤1h 记「同起」一日
CREATE TABLE IF NOT EXISTS `couple_standup` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `day` varchar(10) NOT NULL COMMENT '拍日 yyyy-MM-dd',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '拍的人（区分大小写）',
    `tapped_at` bigint(20) NOT NULL COMMENT '拍的时刻（毫秒）',
    `paired` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1=当日同起已判定并推送',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stand_day` (`space_id`, `day`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F276 久坐互拍';

-- F277 垫付本：互欠结算台账，清账推 both「无债一身轻」
CREATE TABLE IF NOT EXISTS `couple_advance` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `item` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '大项支出名目',
    `payer_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '垫付人（区分大小写）',
    `amount_cents` int(11) NOT NULL COMMENT '金额（分，正整数）',
    `note` varchar(140) NOT NULL DEFAULT '' COMMENT '备注',
    `status` varchar(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN SETTLED',
    `settled_at` bigint(20) DEFAULT NULL COMMENT '清账时间',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    KEY `idx_advance_space` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F277 垫付本（区别于 F5 日常记账）';

-- F278 逛超市战利品：一周一报采购清单，对方猜为什么买，报的人打分
CREATE TABLE IF NOT EXISTS `couple_grocery` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `week` varchar(10) NOT NULL COMMENT '周锚 yyyy-MM-dd（周一）',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '采购方（区分大小写）',
    `items` varchar(300) NOT NULL DEFAULT '' COMMENT '买了啥（逗号分隔，≤5 件）',
    `guess` varchar(300) NOT NULL DEFAULT '' COMMENT 'TA 的猜测文本',
    `guess_by` varchar(50) NOT NULL DEFAULT '' COMMENT '猜测人（可空位不写 charset）',
    `score` int(11) DEFAULT NULL COMMENT '采购方打分 0-5',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_grocery_week` (`space_id`, `week`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F278 战利品互猜';

-- F279 家安月检：每月各交一份六项勾选，双人才算检完
CREATE TABLE IF NOT EXISTS `couple_home_check` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '情侣空间ID',
    `month` varchar(7) NOT NULL COMMENT '检月 yyyy-MM',
    `from_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '检人（区分大小写）',
    `items` varchar(120) NOT NULL DEFAULT '' COMMENT '勾选项编码逗号分隔（GAS/WATER/ELEC/WINDOW/LOCK/FIRSTAID）',
    `created` bigint(20) NOT NULL COMMENT '时间（毫秒时间戳）',
    `updated_at` bigint(20) NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_check_month` (`space_id`, `month`, `from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='F279 家安月检：缺月由总览读出提醒';
