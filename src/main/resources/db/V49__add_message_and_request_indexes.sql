-- V49__add_message_and_request_indexes.sql
-- 目的：`private_message` 建表时只有 PRIMARY KEY(id)，此后 47 个迁移也没给它补过任何二级索引——
--       所有会话查询（分页、最后一条、未读计数、全局搜索、标已读的 UPDATE）都在全表扫；
--       `friend_request` 同样只有主键，而收件箱按 to_user 查、查重按 (from_user,to_user) 查，都是全表扫。
--       联系人列表每条要跑 2 个这种查询（未读数 + 最后一条），消息越多越慢得越明显。
-- 涉及表：private_message、friend_request（仅加索引，不动列与数据）
-- 兼容：只用语义中性的 `CREATE INDEX ... ON ...`，MariaDB 与 H2(MODE=MySQL) 都支持；
--       不用 MariaDB 才有的 `CREATE OR REPLACE INDEX`，也不用 H2 才有的 `CREATE INDEX IF NOT EXISTS`。
--       索引名全库唯一（沿用 v6 红线，带表名缩写前缀，避免与 couple_* 的 idx_/uk_ 撞名）。
-- 日期：2026-10-03
-- 说明：Flyway 按版本号执行，本脚本每个库只跑一次；若中途失败需重跑，请先人工确认索引是否已建。

-- 会话双向各一条：
--   (from_user, to_user, created)  覆盖「我发出去的」那一半与按时间取最近
--   (to_user, from_user, created)  覆盖「我收到的」那一半，也是未读计数
--   （to_user=我 AND from_user=对方 AND status='SENT' AND created>last_read_at）与标已读的 UPDATE 的主路径
CREATE INDEX `idx_pm_from_to_created` ON `private_message` (`from_user`, `to_user`, `created`);
CREATE INDEX `idx_pm_to_from_created` ON `private_message` (`to_user`, `from_user`, `created`);

-- 好友申请：收件箱（to_user + status）与查重/撤回（from_user + to_user）
CREATE INDEX `idx_fr_to_user_status` ON `friend_request` (`to_user`, `status`);
CREATE INDEX `idx_fr_from_to` ON `friend_request` (`from_user`, `to_user`);
