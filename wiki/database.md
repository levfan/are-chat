# 数据库

> 本页回答：全库数据约定、V1→V53 Flyway 迁移时间线、现役 19 张表的逐表清单、随 ADR-0010 下线的 16 张 `couple_*` 表。

结构事实源：真正被运行时执行的是 `src/main/resources/db/V*.sql`（Flyway 增量脚本，**V1→V53 共 53 个**）；`src/main/resources/schema.sql` 是**全量结构文档、不再被执行**（`spring.sql.init.mode=never`）。生产 MariaDB 10.11 与测试 H2（MODE=MySQL）跑同一批 V 脚本。

**当前规模（实测 `grep -c "^CREATE TABLE" src/main/resources/schema.sql` = 19）**：全库 **19 张表 = 6 张 `couple_*` + 13 张非情侣表**。其中 couple 侧在 2026-10-04 裁剪（V50/V51）后为 19 张、2026-10-05 v8 第一批（V52）加到 22 张、**同日二轮裁剪（V53）删到 6 张**（`docs/adr/0010-couple-trim-to-v8-features.md` 第 1/9 条）。13 张非情侣表自 V2 基线以来未受这两轮情侣裁剪影响。

## 数据约定（硬性）

| 约定 | 说明 |
|---|---|
| 主键 | 一律 `varchar(36)` UUID 字符串，应用层生成（实体 `@TableId(IdType.INPUT)`），列注释「主键UUID」 |
| 时间 | 毫秒 bigint 时间戳，字段名 `created` / `updated_at` 等；无 datetime 列 |
| 大小写敏感列 | 用户名/手机号列用 `CHARACTER SET utf8mb4 COLLATE utf8mb4_bin` 声明，**不写 DEFAULT**（H2/MySQL/MariaDB 三兼容） |
| utf8mb4_bin 坑 | 该声明的列**必须 NOT NULL**；需要「可空用户名列」时改用普通 `varchar(50) DEFAULT NULL`（V17/V22 踩过坑） |
| 空间关联 | couple 系功能表都带 `space_id`（关联 `couple_space.id`）；双方落在 `user_a`/`user_b` 或 `from_user`/`to_user` 列 |
| 状态列 | varchar 常量（`STATUS_*`），状态机只前进（见 [couple-space.md](couple-space.md)） |
| 建表风格 | 全表 `CREATE TABLE IF NOT EXISTS` + 每列 COMMENT；`couple_space` 列的 `slogan`/`theme`/`nick_a`/`nick_b`/`anniversary` 现役，`stickers` 列已由 V53 删除 |
| 字符集/引擎 | utf8mb4 + InnoDB；表与列注释齐全 |

改表流程与幂等细则：`.agents/skills/db-migration/SKILL.md`；「新增 V 脚本 + 同步 schema.sql + 独立 db commit」为硬性要求（见 [dev-guide.md](dev-guide.md)）。

## 迁移时间线（V1 → V53）

> 下表记录每个脚本**当时**建过/删过什么，是历史事实；其中大量 `couple_*` 表已在 V50/V51/V53 被 drop，不代表现役（现役只有 19 张）。

| 版本 | 主题 | 变更摘要 |
|---|---|---|
| V1 | 情侣空间基础 | couple_space, couple_invite, couple_promise, couple_checkin, couple_answer, couple_item, couple_anniversary |
| V2 | 存量业务表基线（空库一次性建全；老库重放自动跳过） | admin_audit, announcement, announcement_read, app_user, conversation_pin, friend, friend_request, message_reaction, message_star, private_message, registration_application, uploaded_file, user_profile |
| V3–V14 | 早期各功能域 + 基础设施 | 新建 `couple_notify`（V13，现役）以及信箱/条约/城市/心愿基金/任务卡/默契/和好/夸夸/生理期/胶囊/倒数/共同生活/第一次清单/一问互评等一批早期表，另含心情日记、贴贴动作、心情回应等功能域表（**除 couple_notify 外均已下线**）；`couple_space` 加列 slogan/theme/stickers（stickers 由 V53 删）、`private_message` 加列 heart_at（现役）、`user_profile` 加列 birthday（现役） |
| V15–V48 | 各批功能域（惊喜/接住/养成/回忆/沟通/异地/安全感/游戏/陪伴/成长/文字/默契/经营/博物馆/收藏/饭桌/作息/仪式/公司/关卡/回音/注意力/欢笑…） | 每批 7–11 张 `couple_*` 表；其中收藏卡、饭票、愿望券、家务轮盘、好事簿、加班留灯、安全词等功能域表曾在 2026-10-04 进入保留名单，**但已于 V50/V51/V53 全部 drop**（无一进入现役） |
| V49 | 私聊与好友申请补二级索引 | `private_message` 双向会话复合索引 `idx_pm_from_to_created`/`idx_pm_to_from_created` + `friend_request` 收件/查重索引（非 couple 域，保留） |
| V50 | 系统裁剪第一批（2026-10-04） | drop 193 张已下线功能的表 |
| V51 | 系统裁剪第十六/十七轮（2026-10-04） | 再 drop 85 张，情侣空间收口到 19 张；**不改 V50** |
| V52 | v8 第一批（2026-10-05） | 新建 3 张：`couple_bond_day`（贴贴打卡日）、`couple_question_answer`、`couple_wish`；索引统一 `uk_couple8_/idx_couple8_` 前缀；连续天数与解锁读时算不建表；`couple_*` 增至 22 张 |
| **V53** | **二轮裁剪（2026-10-05）** | **drop 16 张**被删功能表；`couple_bond_day` 改名重建为 `couple_streak_day`（索引 `uk_couple8_bond_day`→`uk_couple9_streak_day`，用 DROP+CREATE 因 H2 索引名全库唯一、跨库改名无双兼容写法，且该表 V52 才建、库外无数据）；`couple_space` DROP COLUMN `stickers`。现役 `couple_*` 收口到 **6 张**、全库 **19 张**。不做数据迁移、不建备份表（被删功能数据作废，生产库执行前手动备份）。 |

> **索引/唯一键名全库唯一**（H2 索引不随表隔离，重名报 42S11「Index already exists」）：V52 用 `uk_couple8_` 前缀、V53 新增 `uk_couple9_streak_day`，写入前均对全部 `db/V*.sql` 查过重。
> **已入库脚本一律不得再修改**：每轮裁剪都新增脚本（V51 不改 V50、V53 不改 V52），因为回改会让打过该版本的库在 Flyway 校验和上失败。

## 现役表清单（19 张，实测自 schema.sql 的 CREATE TABLE）

### identity — 账号与准入（3 张）

| 表 | 用途 | 关键字段 | 唯一键 / 索引 |
|---|---|---|---|
| `app_user` | 系统合法用户（注册审批通过后创建） | `username`/`phone`（`utf8mb4_bin` 区分大小写）、`password_hash`(PBKDF2)、`role` USER/ADMIN、`status` ACTIVE/DISABLED/CLOSED、`created` | `uq_app_user_username`、`uq_app_user_phone` |
| `registration_application` | 入会申请单（通过后才建账号） | `phone`/`username`/`password_hash`、`status` PENDING/APPROVED/REJECTED、`reject_reason`、`reviewed_by` | PK `id`；`idx_reg_app_status(status)` |
| `admin_audit` | 管理员操作审计流水 | `actor`/`action`(APPROVE/REJECT/ENABLE/DISABLE/RESET_PASSWORD)/`target`/`detail`/`created` | PK `id` |

### messaging — 好友 / 私聊 / 在线（7 张）

| 表 | 用途 | 关键字段 | 唯一键 / 索引 |
|---|---|---|---|
| `friend` | 双向好友关系（双向各存一行，owner 视角） | `owner_username`/`friend_username`、`remark`/`tag`、`pinned`/`muted`/`blocked`、`last_read_at` | `uq_friend_pair(owner_username,friend_username)` |
| `friend_request` | 好友申请 | `from_user`/`to_user`、`message`、`status` PENDING/ACCEPTED/REJECTED | `idx_fr_to_user_status`、`idx_fr_from_to` |
| `private_message` | 点对点私信（text/image/poke/system/card/location/file） | `from_user`/`to_user`、`msg_type`、`status` SENT/RECALLED、`reply_to_id`、`read_flag`、`heart_at` | PK `id`；`idx_pm_from_to_created`、`idx_pm_to_from_created` |
| `message_reaction` | 消息表情回应（toggle） | `msg_id`/`username`/`emoji` | `uq_reaction(msg_id,username,emoji)` |
| `message_star` | 消息收藏（个人视角） | `username`/`msg_id` | `uq_star(username,msg_id)` |
| `conversation_pin` | 会话置顶消息（每双人会话一条，双方共享） | `user_a`/`user_b`（字典序）、`msg_id`、`created_by` | `uq_conv_pin(user_a,user_b)` |
| `user_profile` | 用户资料（与 `app_user` 一一对应） | PK 为 `username`；`nickname`/`signature`/`avatar`/`presence_status`/`birthday` | PK `username` |

### platform — 公告（2 张）

| 表 | 用途 | 关键字段 | 唯一键 / 索引 |
|---|---|---|---|
| `announcement` | 全站公告横幅 | `content`/`created_by`/`enabled`/`created` | PK `id` |
| `announcement_read` | 公告按用户已读记录 | `username`/`announcement_id`/`read_at` | `uq_ann_read(username,announcement_id)` |

### filestorage — 文件（1 张）

| 表 | 用途 | 关键字段 | 唯一键 / 索引 |
|---|---|---|---|
| `uploaded_file` | 上传文件档案（内容寻址去重） | `original_name`/`stored_path`/`content_type`/`size`/`sha256` | `uq_uploaded_file_sha256(sha256)` |

### couple — 情侣空间（6 张，详写）

| 表 | 用途 | 关键字段 | 唯一键 / 索引 |
|---|---|---|---|
| `couple_space` | 空间本体（唯一容器） | `user_a`/`user_b`（`utf8mb4_bin`，字典序小者为 A）、`status` ACTIVE/DISSOLVED、`anniversary`(yyyy-MM-dd)、`slogan`(≤60)、`theme`(classic/cherry/ocean/forest/night)、`nick_a`/`nick_b`(≤30)、`created`/`dissolved_at`；`city_a`/`city_b` 列为历史遗留（异地功能已下线，列仍在） | PK `id`；`idx_couple_space_a(user_a)`、`idx_couple_space_b(user_b)`。**有意不加**「一用户一有效空间」唯一约束（ADR-0007 第 12 条 / ADR-0010 第 11 条） |
| `couple_invite` | 建立空间的邀请单 | `from_user`/`to_user`、`message`(≤100)、`status` PENDING/ACCEPTED/REJECTED/CANCELED、`updated_at` | PK `id`；`idx_couple_invite_to(to_user,status)` |
| `couple_notify` | 通知中心（每条情侣 WS 事件的落库副本，离线补看） | `username`、`event`（现役 15 个事件名）、`actor`（`system`=定时任务）、`detail`、`read_flag` | PK `id`；`idx_couple_notify_user(username,created)`。`event` 字段不做白名单 |
| `couple_streak_day` | 连续互动打卡日（原 `couple_bond_day`，V53 改名） | `space_id`、`day`(yyyy-MM-dd)、`source` AUTO/MAKEUP、`operator_user`、`created`；连续天数与七档解锁由本表 day 集合**读时算**，无缓存列 | `uk_couple9_streak_day(space_id,day)` |
| `couple_question_answer` | 每日一问回答（同时是打卡触发源） | `space_id`、`day`、`question_index`（按 space+day 稳定哈希，落快照）、`question`(≤200)、`username`、`answer`(≤300)、`created`/`updated_at`；「双方都答过才互看」是读时判定，库里不存可见位 | `uk_couple8_question_day_user(space_id,day,username)` |
| `couple_wish` | 愿望清单 | `space_id`、`owner_user`（想要的人）、`creator_user`（记录的人，可不同）、`title`(≤80)、`note`(≤200)、`status` OPEN/PREPARED/FULFILLED、`prepared_by`/`prepared_at`（对被许愿人保密）、`fulfilled_at` | `uk_couple8_wish_title(space_id,owner_user,title)` |

> **随 ADR-0010 下线的 16 张 `couple_*` 表**（不再存在，确切表名的 DROP 列表见 `db/V53__trim_couple_space_to_v8_features.sql` 与 `docs/adr/0010` 第 1 条）：
> 贴贴动作、共同日历（倒数日清单）、安全词约定、安全词使用记录、愿望券本、求抱抱、今晚饭票、好事簿、心情日记、心情回应、恋爱盲盒、
> 积分台账、加班预报与留灯、刮刮乐、家务轮盘、常用收藏卡。（另 `couple_bond_day` 是**改名重建**为 `couple_streak_day`，不计入删除。）

## Flyway 现状

- 运行时建表**只走 Flyway**：`spring.flyway.locations=classpath:db`，脚本命名 `V{n}__{描述}.sql`，**最新到 V53**（53 个迁移在 H2 文件库从 V1 全量重放通过，见 ADR-0010 验证一节）。
- `spring.sql.init.mode=never`：`src/main/resources/schema.sql` 已退役为**全量结构文档**（供人读与逐表核对），**不再被任何环境执行**；改表时必须「新增 V 脚本 + 同步 schema.sql」双写。
- `baseline-on-migrate=true` + `baseline-version=0`：存量老库首次启动自动打 0 基线后**从 V1 全量重放**——所以**每个脚本都必须幂等**（`CREATE TABLE IF NOT EXISTS` / `ADD·DROP COLUMN ... IF NOT EXISTS` / `DROP TABLE IF EXISTS`）。`clean-disabled=true`。
- 已入库脚本不可修改；新版本号 = `db` 目录最大版本 + 1。完整规范见 `.agents/skills/db-migration/SKILL.md`。

## 查询与访问模式备忘

- 全部经 MyBatis-Plus `BaseMapperCompat` 的 default 方法，无 XML mapper、无手写 SQL 文件；复杂聚合为多查 + 内存算。
- 「按天/空间稳定取内容」不查库，用 `CoupleQuestionBank.stableHash(space + salt + day)` 在静态数组上取模——同日重放一致。
- **双向会话谓词 `(... AND from=?) OR (... AND to=?)` 吃不到索引**：取「最后一条」「未读数」一律走 `findLatestCreatedPerPeer` / `selectUnreadCountsByPeer`，不许退回 per-peer 循环（V49 补索引后实测仍慢）。
