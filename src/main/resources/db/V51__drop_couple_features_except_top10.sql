-- ============================================================================
-- V51：系统裁剪第十六/十七轮——情侣空间只保留 10 张功能卡，其余功能的表全部下线
-- ============================================================================
-- 依据：docs/couple-trim-ranking.md（174 张卡排序后留 10 删 164）第四、五节。
-- 口径：
--   1) 判据是「db/V*.sql 建过的 couple_* 表」减去「现存实体 @TableName 集合」再减去
--      「V50 已经 drop 的 193 张」，不手写清单——实体↔Mapper↔表在本项目是精确 1:1，
--      且 Mapper 无一条手写 SQL 联外表，所以没有实体就等于没有读写方。
--   2) 不修改 V50：已入库脚本内容不得再修改（改了会让已经打过 V50 的库在 Flyway
--      校验和上直接失败），本轮增量只新增 V51。
--   3) 用 DROP TABLE IF EXISTS：MariaDB 10.11 与 H2 MODE=MySQL 都认，且新库存量走
--      baseline-on-migrate 会全量重放 V1→V51，脚本必须幂等。
--   4) 不做数据迁移、不建备份表：产品裁剪，被删功能的数据随之作废；
--      生产库执行前另行手动备份，脚本本身不带备份逻辑。
--   5) 明确保留 couple_point_ledger：积分台账的三个赚分入口（好事簿/家务轮盘/刮刮乐）
--      与唯一花分出口（愿望券本）都还在，心动值也靠它供数。
-- 统计：db 建过的 couple_* 表 297 张，现存 couple 实体 19 个，
--       V50 已 drop 193 张，本脚本再 drop 85 张（合计 278 张）。
-- 保留的表：couple_action, couple_anniversary, couple_catch_safeword, couple_catch_safeword_use, couple_ceremony_coupon, couple_comfort, couple_dine_ticket, couple_echo_deed, couple_invite, couple_mood, couple_mood_reaction, couple_mystery_box, couple_notify, couple_point_ledger, couple_quest_overtime, couple_scratch, couple_space, couple_spin_task, couple_user_pin
-- ============================================================================

DROP TABLE IF EXISTS `couple_answer`;
DROP TABLE IF EXISTS `couple_answer_reaction`;
DROP TABLE IF EXISTS `couple_art_gallery`;
DROP TABLE IF EXISTS `couple_blind_pick`;
DROP TABLE IF EXISTS `couple_capsule`;
DROP TABLE IF EXISTS `couple_catch_daily`;
DROP TABLE IF EXISTS `couple_catch_mine`;
DROP TABLE IF EXISTS `couple_catch_wish`;
DROP TABLE IF EXISTS `couple_ceremony_founded`;
DROP TABLE IF EXISTS `couple_ceremony_mark`;
DROP TABLE IF EXISTS `couple_ceremony_policy`;
DROP TABLE IF EXISTS `couple_ceremony_renew`;
DROP TABLE IF EXISTS `couple_ceremony_ritual`;
DROP TABLE IF EXISTS `couple_challenge`;
DROP TABLE IF EXISTS `couple_checkin`;
DROP TABLE IF EXISTS `couple_confession`;
DROP TABLE IF EXISTS `couple_countdown`;
DROP TABLE IF EXISTS `couple_cozy_hug`;
DROP TABLE IF EXISTS `couple_cozy_latenight`;
DROP TABLE IF EXISTS `couple_cozy_lightout`;
DROP TABLE IF EXISTS `couple_cozy_remedy`;
DROP TABLE IF EXISTS `couple_cozy_sheep`;
DROP TABLE IF EXISTS `couple_cozy_sleep`;
DROP TABLE IF EXISTS `couple_cozy_slow`;
DROP TABLE IF EXISTS `couple_cozy_water`;
DROP TABLE IF EXISTS `couple_cozy_weather`;
DROP TABLE IF EXISTS `couple_cycle`;
DROP TABLE IF EXISTS `couple_dine_nogo`;
DROP TABLE IF EXISTS `couple_dine_rate`;
DROP TABLE IF EXISTS `couple_echo_battery`;
DROP TABLE IF EXISTS `couple_echo_juice`;
DROP TABLE IF EXISTS `couple_echo_refill_log`;
DROP TABLE IF EXISTS `couple_first`;
DROP TABLE IF EXISTS `couple_focus_detox`;
DROP TABLE IF EXISTS `couple_focus_gaze`;
DROP TABLE IF EXISTS `couple_focus_meal`;
DROP TABLE IF EXISTS `couple_focus_nudge`;
DROP TABLE IF EXISTS `couple_fortune_slip`;
DROP TABLE IF EXISTS `couple_fund`;
DROP TABLE IF EXISTS `couple_fund_deposit`;
DROP TABLE IF EXISTS `couple_garden`;
DROP TABLE IF EXISTS `couple_guess_round`;
DROP TABLE IF EXISTS `couple_hundred`;
DROP TABLE IF EXISTS `couple_hundred_checkin`;
DROP TABLE IF EXISTS `couple_item`;
DROP TABLE IF EXISTS `couple_laugh_cringe`;
DROP TABLE IF EXISTS `couple_laugh_joke`;
DROP TABLE IF EXISTS `couple_laugh_moment`;
DROP TABLE IF EXISTS `couple_legacy_draw`;
DROP TABLE IF EXISTS `couple_letter`;
DROP TABLE IF EXISTS `couple_love_bank`;
DROP TABLE IF EXISTS `couple_love_word`;
DROP TABLE IF EXISTS `couple_misrewind`;
DROP TABLE IF EXISTS `couple_miss_express`;
DROP TABLE IF EXISTS `couple_next_time`;
DROP TABLE IF EXISTS `couple_pact`;
DROP TABLE IF EXISTS `couple_passbook`;
DROP TABLE IF EXISTS `couple_peace_review`;
DROP TABLE IF EXISTS `couple_praise`;
DROP TABLE IF EXISTS `couple_promise`;
DROP TABLE IF EXISTS `couple_quest_battle`;
DROP TABLE IF EXISTS `couple_quest_care_mark`;
DROP TABLE IF EXISTS `couple_quest_nurse`;
DROP TABLE IF EXISTS `couple_quest_report`;
DROP TABLE IF EXISTS `couple_quiz_duel`;
DROP TABLE IF EXISTS `couple_quote`;
DROP TABLE IF EXISTS `couple_reconcile`;
DROP TABLE IF EXISTS `couple_rose`;
DROP TABLE IF EXISTS `couple_song`;
DROP TABLE IF EXISTS `couple_sorry_ticket`;
DROP TABLE IF EXISTS `couple_survey_answer`;
DROP TABLE IF EXISTS `couple_sweet_alarm`;
DROP TABLE IF EXISTS `couple_sweet_battle`;
DROP TABLE IF EXISTS `couple_sweet_line`;
DROP TABLE IF EXISTS `couple_tacit`;
DROP TABLE IF EXISTS `couple_task`;
DROP TABLE IF EXISTS `couple_telepathy`;
DROP TABLE IF EXISTS `couple_ticket`;
DROP TABLE IF EXISTS `couple_top_guess`;
DROP TABLE IF EXISTS `couple_top_list`;
DROP TABLE IF EXISTS `couple_travel_wish`;
DROP TABLE IF EXISTS `couple_treasure`;
DROP TABLE IF EXISTS `couple_truth`;
DROP TABLE IF EXISTS `couple_whisper`;
DROP TABLE IF EXISTS `couple_wish_exchange`;
