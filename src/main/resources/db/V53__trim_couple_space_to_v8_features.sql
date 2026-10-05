-- ============================================================================
-- V53：情侣空间二轮裁剪——只留「邀请建立空间 / 每日一问 / 愿望清单 / 连续互动打卡+七档解锁」
-- ============================================================================
-- 依据：用户指令「除了以上这几个功能，其余功能全部删除」，取舍、连带改造与被删清单的完整盘点见
--       docs/adr/0010-couple-trim-to-v8-features.md 与 CONTEXT.md 的 couple 一节。
-- 口径：
--   1) 判据是「保留的四项功能 + 它们赖以成立的地基」：空间本体（couple_space）、邀请（couple_invite）、
--      通知中心（couple_notify，每条情侣事件都要落库补看）、打卡日（改名后的 couple_streak_day）、
--      每日一问（couple_question_answer）、愿望清单（couple_wish）。
--      其余 16 张表全部下线，包括 2026-10-04 那轮按「留 10 张卡」保下来的表。
--   2) 打卡日表改名 couple_bond_day → couple_streak_day：`bond` 在统一语言（CONTEXT.md）里就是「贴贴」，
--      贴贴卡本轮删除，继续叫 bond_day 等于让一个词指两件事。打卡的**触发源同时改口径**——
--      原来靠「双方当天都发过贴贴」，现在靠「双方当天都答完每日一问」（空间建立当天仍自动算第 1 天）。
--      用 DROP + CREATE 而不是 RENAME：索引名在 H2 里是库级唯一的，跨库改索引名没有两边都认的写法；
--      而这张表是 2026-10-05 V52 才建的新表，仓库之外的环境尚无数据，不做行迁移。
--   3) couple_space 删掉 stickers 列（F28 贴纸墙）：它是独立于七档解锁的一张卡，
--      与 50 天档「专属贴纸包」语义撞车，两处并存会让人以为同一个功能有两个入口。
--      `theme`（空间主题）与 `slogan`（宣言）、`nick_a`/`nick_b`（爱称）、`anniversary`（在一起的日子）
--      全部保留——7 天档动态背景叠在 theme 上、14 档昵称特效要爱称、百日回顾与在一起天数都读 anniversary。
--   4) 积分台账 couple_point_ledger 一并删除：三个赚分入口（好事簿/家务轮盘/刮刮乐）与一个花分出口
--      （愿望券本）都在被删之列，留着就是一张永远为 0 的空表；补签因此**不再花钱**，
--      稀缺性改由「只能补最近 7 天 + 每自然月最多 3 次」两条守。
--   5) 全部 DROP TABLE IF EXISTS / ADD·DROP COLUMN IF NOT EXISTS / CREATE TABLE IF NOT EXISTS：
--      新库全量重放 V1→V53、老库只补差异，MariaDB 10.11 与 H2 MODE=MySQL 双兼容，脚本幂等。
--   6) 不做数据迁移、不建备份表：产品裁剪，被删功能的数据随之作废；生产库执行前另行手动备份。
--   7) couple_user_pin（F207 常用收藏 + F206 功能搜索的存储）一并删除：两张卡都是「在十几张卡里找路」
--      的导航设施，裁剪后空间只剩四张卡、一屏滚动即是全部，导航本身失去了对象。
-- 统计：drop 16 张表 + drop 1 列；couple_* 表 22 → 6（其中 1 张改名重建）；现役索引新增 uk_couple9_streak_day。
-- 保留的表：couple_space, couple_invite, couple_notify,
--           couple_streak_day(原 couple_bond_day), couple_question_answer, couple_wish
-- ============================================================================

-- 一、下线 16 张被删功能（含卡片收藏/功能搜索）的表
DROP TABLE IF EXISTS `couple_action`;
DROP TABLE IF EXISTS `couple_anniversary`;
DROP TABLE IF EXISTS `couple_catch_safeword`;
DROP TABLE IF EXISTS `couple_catch_safeword_use`;
DROP TABLE IF EXISTS `couple_ceremony_coupon`;
DROP TABLE IF EXISTS `couple_comfort`;
DROP TABLE IF EXISTS `couple_dine_ticket`;
DROP TABLE IF EXISTS `couple_echo_deed`;
DROP TABLE IF EXISTS `couple_mood`;
DROP TABLE IF EXISTS `couple_mood_reaction`;
DROP TABLE IF EXISTS `couple_mystery_box`;
DROP TABLE IF EXISTS `couple_point_ledger`;
DROP TABLE IF EXISTS `couple_quest_overtime`;
DROP TABLE IF EXISTS `couple_scratch`;
DROP TABLE IF EXISTS `couple_spin_task`;
DROP TABLE IF EXISTS `couple_user_pin`;

-- 二、下线 V52 的贴贴打卡日表（同名概念改名到第三节重建）
DROP TABLE IF EXISTS `couple_bond_day`;

-- 三、打卡日：双方共同活跃的一天，一天一行；连续天数与七档解锁由本表 day 集合读时算，不建缓存列
CREATE TABLE IF NOT EXISTS `couple_streak_day` (
    `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键UUID',
    `space_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '所属情侣空间ID，关联 couple_space.id',
    `day` varchar(10) NOT NULL COMMENT '打卡日期（yyyy-MM-dd，自然日）',
    `source` varchar(10) NOT NULL DEFAULT 'AUTO' COMMENT '确认方式：AUTO 当天双方都答完每日一问（或空间建立当天） / MAKEUP 补签',
    `operator_user` varchar(50) DEFAULT NULL COMMENT '补签操作人（AUTO 时为 NULL）',
    `created` bigint(20) NOT NULL COMMENT '确认时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couple9_streak_day` (`space_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='互动打卡日表：一行=那一天双方共同活跃（答完每日一问）或补签成功，连续天数与七档解锁由本表算出';

-- 四、空间个性化：贴纸墙随功能下线（主题/宣言/爱称/在一起的日子都保留）
ALTER TABLE `couple_space` DROP COLUMN IF EXISTS `stickers`;
