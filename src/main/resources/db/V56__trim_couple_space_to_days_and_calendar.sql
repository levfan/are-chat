-- ============================================================================
-- V56：情侣空间三轮裁剪——只留「在一起天数 / 纪念日 / 亲密日历记录」
-- ============================================================================
-- 依据：用户指令「保留在一起天数、纪念日、美柚app上的日历记录，其它功能都不要了」。
--       被下线的是每日一问（couple_question_answer）、连续互动打卡与六档解锁（couple_streak_day）、
--       愿望清单（couple_wish）三张功能卡，以及由它们供养的心动值（无表，读时算）。
-- 口径：
--   1) 三张表整表 DROP：一行都不再有人读——打卡的触发源是「双方都答完每日一问」，问没了打卡就永不
--      发生；心动值的五项供数里四项来自本轮被删的功能，留着就是一串恒为 0 的数字。
--   2) couple_space 的 nick_a / nick_b（爱称）、slogan（宣言）、theme（主题）、city_a / city_b（异地恋）
--      六列**保留不动**：与 couple_notify、message_star 同口径的死存储。删列要动聚合与适配器两侧的
--      映射，收益只是六列 NULL；留着也不会有人读到（CoupleSpace 聚合已不再持有它们）。
--   3) 经期（couple_period）与亲密记录（couple_intimacy_log）两张表原样保留：它们从 2026-10-20 起
--      由亲密日历一张卡承担记录入口，表结构不变。
--   4) 幂等 + H2/MariaDB 双兼容：DROP TABLE IF EXISTS，不含 USE 与切库语句。
--   5) 不做数据迁移、不建备份表：产品裁剪，被删功能的数据随之作废；生产库执行前另行手动备份。
-- 统计：drop 3 张表；couple_* 表 8 → 5（couple_space, couple_invite, couple_notify,
--       couple_period, couple_intimacy_log）；全库 21 → 18 张表。
-- ============================================================================

DROP TABLE IF EXISTS `couple_question_answer`;
DROP TABLE IF EXISTS `couple_streak_day`;
DROP TABLE IF EXISTS `couple_wish`;
