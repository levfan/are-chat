# 数据库

> 本页回答：全库数据约定、V1→V51 Flyway 迁移时间线、按域分组的表清单。

结构事实源：`src/main/resources/schema.sql`（全量结构文档，**当前 32 张表 = 19 张 `couple_*` + 13 张非情侣表**，不由运行时执行） + `src/main/resources/db/V*.sql`（真正被 Flyway 执行的增量脚本，V1→V51 共 51 个）。运行时建表路径只有 Flyway；生产与测试（H2）走同一批 V 脚本。**2026-10-04 情侣空间裁剪**：V50 drop 193 张、V51 再 drop 85 张，V1-V49 建过的 297 张 `couple_*` 表只剩上面这 19 张；集合等式「297 − 278 = 19」由脚本核过（见 docs/couple-trim-ranking.md 第 9.2 节）。

## 数据约定（硬性）

| 约定 | 说明 |
|---|---|
| 主键 | 一律 `varchar(36)` UUID 字符串，应用层生成（实体 `@TableId(IdType.INPUT)`），列注释"主键UUID" |
| 时间 | 毫秒 bigint 时间戳，字段名 `created` / `updated_at` 等；无 datetime 列 |
| 大小写敏感列 | 用户名/手机号列用 `CHARACTER SET utf8mb4 COLLATE utf8mb4_bin` 声明，**不写 DEFAULT**（对 H2/MySQL/MariaDB 三兼容的通用写法） |
| utf8mb4_bin 坑 | 该声明的列**必须 NOT NULL**；需要"可空用户名列"时改用普通 `varchar(50) DEFAULT NULL`——V17/V22 踩过此坑 |
| 金额 | 统一以**分**为整数存（如 couple_fund_deposit、couple_expense） |
| 空间关联 | couple 系表都带 `space_id`（关联 couple_space.id），双人均在 user_a/user_b 或 from/to_user 列上 |
| 状态列 | varchar 常量（STATUS_*），状态机只前进（见 [couple-space.md](couple-space.md) 规则表） |
| 建表风格 | 全表 `CREATE TABLE IF NOT EXISTS` + 每列 COMMENT；加列 `ADD COLUMN ... IF NOT EXISTS`（MariaDB 方言，H2 MODE=MySQL 兼容） |
| 字符集/引擎 | utf8mb4 + InnoDB；表与列注释齐全 |

改表流程与幂等细则：`.agents/skills/db-migration/SKILL.md`；"新增 V 脚本 + 同步 schema.sql + 独立 db commit" 为硬性要求（见 [dev-guide.md](dev-guide.md)）。

## 迁移时间线（V1 → V51）

| 版本 | 主题 | 新增表 / 变更 |
|---|---|---|
| V1 | 情侣空间基础 | couple_space, couple_invite, couple_promise, couple_checkin, couple_answer, couple_item, couple_anniversary |
| V2 | 存量业务表基线（空库一次性建全；老库重放自动跳过） | admin_audit, announcement, announcement_read, app_user, conversation_pin, friend, friend_request, message_reaction, message_star, private_message, registration_application, uploaded_file（+ user_profile） |
| V3 | 心情日记 | couple_mood |
| V4 | 悄悄话信箱（慢递） | couple_letter |
| V5 | 恋爱条约 / 异地城市 / 心愿基金 | couple_pact, couple_fund, couple_fund_deposit；couple_space 加列 city_a/city_b（加列不用 IF NOT EXISTS 的兼容写法见脚本头注释） |
| V6 | 贴贴动作 / 心情回应 / 专属爱称 | couple_action, couple_mood_reaction；couple_space 加列 |
| V7 | 甜蜜任务卡 / 默契大考验 | couple_task, couple_tacit |
| V8 | 和好卡 / 夸夸墙 / 生理期 | couple_reconcile, couple_praise, couple_cycle |
| V9 | 时光胶囊 / 倒数日 | couple_capsule, couple_countdown（徽章墙/成就/那年今天实时推导，无表） |
| V10 | 共同生活 | couple_expense, couple_chore, couple_date_plan, couple_habit, couple_habit_log, couple_cipher |
| V11 | 空间个性化（宣言/主题/贴纸） | couple_space 加列 slogan/theme/stickers |
| V12 | 私聊心动时刻 F36 | private_message 加列 heart_at |
| V13 | 通知中心 F41 / 生日 F42 | couple_notify；user_profile 加列 birthday |
| V14 | 第一次清单 F46 / 一问互评 F48 | couple_first, couple_answer_reaction |
| V15 | 惊喜与期待 F50-F59 | couple_scratch, couple_mystery_box, couple_sweet_alarm, couple_miss_express, couple_garden, couple_rose, couple_fortune_slip, couple_confession, couple_treasure |
| V16 | 懂我与被接住 F60-F69 | couple_comfort, couple_sorry_ticket, couple_peace_review, couple_truth, couple_whisper, couple_telepathy, couple_love_bank |
| V17 | 共同养成 F70-F79 | couple_challenge, couple_passbook, couple_hundred, couple_hundred_checkin, couple_wish_exchange, couple_read_plan, couple_read_progress, couple_travel_wish, couple_watchlist, couple_dict_word, couple_next_time |
| V18 | 回忆资产 F80-F89 | couple_quote, couple_ticket, couple_song（F80-F82/F85-F87 聚合或常量放宽，无新表） |
| V19 | 沟通增强 F100-F109 | couple_cool_down, couple_mood_relay, couple_guess_round, couple_story_line, couple_apology_card, couple_feeling_word |
| V20 | 异地恋 F110-F119 | couple_handhold, couple_miss_daily, couple_routine, couple_reunion_letter, couple_cloud_date, couple_safety_ping, couple_reunion_log |
| V21 | 确定感安全感 F120-F129 | couple_security_bank, couple_decade_pact, couple_vision_card, couple_oath, couple_trust_coin, couple_self_contract, couple_pet；couple_anniversary 加列 kind |
| V22 | 趣味游戏 F130-F139 | couple_survey_answer, couple_quiz_duel, couple_love_word, couple_blind_pick, couple_sweet_battle, couple_sweet_line, couple_art_gallery |
| V23 | 深度陪伴 F140-F149 | couple_dream, couple_food_note, couple_partner_fact, couple_sos_ping, couple_daily_three, couple_custom_badge |
| V24 | 成长系 F150-F159 | couple_habit_streak（刻意避开 V10 couple_habit 命名）, couple_thanks_note, couple_feel_log, couple_weekly_star, couple_read_minute, couple_delay_task, couple_praise_bank |
| V25 | 文字浪漫 F160-F169 | couple_poem_chain, couple_poem_3line, couple_morning_note, couple_drift_bottle, couple_cipher_note, couple_soul_answer, couple_journal |
| V26 | 默契亲密 F170-F179 | couple_love_lang, couple_heart_flash, couple_what_if, couple_secret_signal, couple_sync_tap, couple_heart_day |
| V27 | 生活经营 F180-F189 | couple_family_meeting, couple_week_host, couple_skill_swap, couple_month_review, couple_emergency_card, couple_month_snapshot, couple_point_ledger, couple_five_year_plan, couple_anniv_plan |
| V28 | 时光博物馆 F190-F199 | couple_doc_scene, couple_exhibit, couple_hidden_achievement, couple_house_rule, couple_dnd_setting |
| V29 | 常用收藏 F207 | couple_user_pin |
| V30 | 两个人的饭桌 F210-F219 | couple_dine_ticket, couple_dine_rate, couple_dine_nogo, couple_dine_weekplan, couple_dine_homecook, couple_dine_cart, couple_dine_topic |
| V31 | 体温同步·作息与健康 F220-F229 | couple_cozy_lightout, couple_cozy_sleep, couple_cozy_sheep, couple_cozy_water, couple_cozy_weather, couple_cozy_latenight, couple_cozy_slow, couple_cozy_remedy, couple_cozy_hug（F229 月度小结聚合无表） |
| V32 | 小日子·仪式感 F230-F239 | couple_ceremony_founded, couple_ceremony_ritual, couple_ceremony_mark, couple_ceremony_policy, couple_ceremony_renew, couple_ceremony_coupon, couple_ceremony_recap（F231 黄历/F237 史册/F238 加冕聚合无表） |
| V33 | 我们公司 F240-F249 | couple_board_role, couple_board_vote, couple_board_report, couple_board_salary, couple_board_idea, couple_board_attend（F243 职级/F248 名片/F249 周报聚合无表；发薪复用 V27 couple_point_ledger 插 EARN 流水） |
| V34-V48 | 老黄历 / 倾听与发声 / 二人制造厂 / 我们百科 / 明日邮局 / 扮演剧场 / 身体通知 / 修复车间 / 两家与朋友 / 传世系统 / 回音壁 / 注意力保护区 / 人生关卡 / 聆听者 / 欢笑银行 | 各批 7-11 张表；**除 V36 的 couple_spin_task、V46 的 couple_quest_overtime、V44 的 couple_echo_deed、V47 的 couple_catch_safeword(\_use)、V32 的 couple_ceremony_coupon 外，其余表已在 V50/V51 全部 drop** |
| V49 | 私聊消息与好友申请补二级索引 | private_message 双向会话复合索引 + friend_request 收件/查重索引（非 couple 域，保留） |
| **V50** | **系统裁剪第一批** | drop 193 张已下线功能的表；保留 couple_point_ledger |
| **V51** | **系统裁剪第十六/十七轮** | 再 drop 85 张，情侣空间收口到 10 张卡的 19 张表；**不改 V50**（改已入库脚本会让已打过 V50 的库在 Flyway 校验和上失败） |


注：个别 V 脚本尾部含种子数据（如题库类内容存库的场景）；schema.sql 历史遗留的悬空 `CREATE TABLE` 残行（含 V31 同步基线时引入的第 2323 行一处）已全部修复删除，现 `grep -c "CREATE TABLE"` 与 `^CREATE TABLE \`` 一致，均为 170 张。

## 表清单（按域分组，共 32 张，其中 `couple_*` 19 张）

### 账号与运营（auth / system）

| 表 | 用途 |
|---|---|
| `app_user` | 用户账号（状态含禁用/注销） |
| `registration_application` | 注册审批申请单（PBKDF2 哈希随申请保存） |
| `admin_audit` | 管理员操作审计（审批/启停/重置密码） |
| `user_profile` | 用户资料：昵称/头像/签名/birthday（生日被 couple 侧复用） |
| `announcement` / `announcement_read` | 系统公告横幅与按用户已读记录 |

### IM（im）

| 表 | 用途 |
|---|---|
| `friend` | 双向好友关系（含备注） |
| `friend_request` | 好友申请 |
| `private_message` | 私信（含 heart_at 心动时刻标记列） |
| `message_reaction` | 消息表情回应 |
| `message_star` | 消息收藏 |
| `conversation_pin` | 会话置顶消息 |
| `uploaded_file` | 上传文件元数据（image/file 附件墙） |

### 情侣空间（裁剪后 19 张，全部来自 schema.sql 实测）

| 表 | 用途 |
|---|---|
| `couple_action` | 贴贴动作流（卡 `couple-bond`）：宫格动作逐条落库，心动值按"双方同日都动过"计天数 |
| `couple_anniversary` | 共同日历纪念日（calendar_type/lunar_md 支持农历生日换算） |
| `couple_catch_safeword` | 安全词约定（卡 `couple-catch-safeword`）：每人一格，可改写 |
| `couple_catch_safeword_use` | 一次暂停使用（一天一人一行）+ 事后复盘（只有喊停本人能补） |
| `couple_ceremony_coupon` | 愿望券（卡 `couple-cere-coupon`）：OPEN→USED，发券时向台账写一条 SPEND |
| `couple_comfort` | 求抱抱（卡 `couple-comfort`）：每人每天一条感受 + 对方回应的那句话 |
| `couple_dine_ticket` | 今晚饭票（卡 `couple-dine-today`）：每人每天一票，裁决由票池按空间+日稳定 hash 现算 |
| `couple_echo_deed` | 好事簿（卡 `couple-echo-deed`）：我记的「TA 为我做的事」+ 加星位 |
| `couple_invite` | 建立空间的邀请单（pending/accepted/rejected/cancelled） |
| `couple_mood` | 心情日记（卡 `couple-mood`）：每人每天一条 + 一句随笔 |
| `couple_mood_reaction` | 对 TA 某天心情的回应（抱抱/亲亲/加油/摸摸头），归属贴贴域 |
| `couple_mystery_box` | 恋爱盲盒（卡 `couple-surprise`）：open_day 到日才可拆，装盒人不能自拆 |
| `couple_notify` | 空间动态通知中心（F41 铃铛）：每条 push 都落一行，与 WS 双写 |
| `couple_point_ledger` | 积分台账：三赚（好事簿/家务轮盘/刮刮乐）一花（愿望券本）的物理载体，心动值也读它 |
| `couple_quest_overtime` | 加班预报与留灯（卡 `couple-quest-overtime`）：每人每天一行，灯文本与留灯人在同一行 |
| `couple_scratch` | 刮刮乐（卡 `couple-surprise`）：每周懒生成两张，核销权在送券人，核销才向台账写 EARN |
| `couple_space` | 空间主表：userA/userB、在一起纪念日、状态、双方爱称、宣言/主题/贴纸墙 |
| `couple_spin_task` | 家务轮盘（卡 `couple-fy-spin`）：本周格子，分配人/认账位/干完位 |
| `couple_user_pin` | 常用收藏（F207）：每人一行，功能卡 key 逗号分隔 ≤6 个 |

> 原 157 张 `couple_*` 表里的 142 张已随功能裁剪被 V50（193 张）+ V51（85 张）drop，
> 本页不再列出；每张卡的落点与保留理由见 [../docs/couple-trim-ranking.md](../docs/couple-trim-ranking.md) 第四节。

## 表名与索引的两条硬约束（裁剪后依然有效）

- 索引/唯一键名**全库唯一**（H2 的索引不随表隔离，重名直接报 42S11），新增前先对全部 `db/V*.sql` 查重。
- `utf8mb4_bin` 的用户名列**必须 NOT NULL**，可空的改用普通 `varchar(50) DEFAULT NULL`。
## 查询与访问模式备忘

- 全部经 MyBatis-Plus `BaseMapperCompat` 的 default 方法，无 XML mapper、无手写 SQL 文件；复杂聚合（编年史/报告/仪表盘）为多查 + 内存聚合。
- "按天/空间稳定取内容"不查库，用 `stableHash(space+epochDay)` 在 Bank 静态数组上取模——同日重放结果一致。
- 高频读接口（today/dashboard/overview）普遍做惰性生成：首访自动建行（花园开垦、任务卡生成、周卡懒发）。
