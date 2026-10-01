# 数据库

> 本页回答：全库数据约定、V1→V33 Flyway 迁移时间线、按域分组的表清单。

结构事实源：`src/main/resources/schema.sql`（全量结构文档，170 张表，不由运行时执行）+ `src/main/resources/db/V*.sql`（真正被 Flyway 执行的增量脚本）。运行时建表路径只有 Flyway；生产与测试（H2）统一走同一批 V 脚本。

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

## 迁移时间线（V1 → V33）

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

注：个别 V 脚本尾部含种子数据（如题库类内容存库的场景）；schema.sql 历史遗留的悬空 `CREATE TABLE` 残行（含 V31 同步基线时引入的第 2323 行一处）已全部修复删除，现 `grep -c "CREATE TABLE"` 与 `^CREATE TABLE \`` 一致，均为 170 张。

## 表清单（按域分组，共 170 张，其中 `couple_*` 157 张）

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

### 情侣空间 — 基础与回忆（V1-V14 / V18）

| 表 | 用途 |
|---|---|
| `couple_space` | 聚合根：userA/userB、在一起纪念日、城市、宣言/主题/贴纸、爱称 |
| `couple_invite` | 绑定邀请（PENDING/接受/拒绝） |
| `couple_promise` / `couple_checkin` / `couple_answer` / `couple_item` | 承诺卡 / 早晚安打卡 / 今日一问双答 / 共享清单 |
| `couple_anniversary` | 纪念日（V21 起带 kind 大日子分类） |
| `couple_mood` / `couple_mood_reaction` | 每日心情 / 对方心情回应 |
| `couple_letter` | 悄悄话信箱（慢递 deliver_at） |
| `couple_pact` | 恋爱条约（提出→盖章） |
| `couple_fund` / `couple_fund_deposit` | 心愿基金目标 / 存入流水 |
| `couple_action` | 贴贴小动作流水 |
| `couple_task` / `couple_tacit` | 每日甜蜜任务卡 / 默契大考验对局 |
| `couple_reconcile` / `couple_praise` / `couple_cycle` | 和好卡 / 夸夸墙 / 生理期记录 |
| `couple_capsule` / `couple_countdown` | 时光胶囊（SEALED→到期）/ 倒数日 |
| `couple_expense` / `couple_chore` / `couple_date_plan` | 甜蜜记账 / 家务轮值 / 约会规划 |
| `couple_habit` / `couple_habit_log` / `couple_cipher` | 双人习惯 / 习惯打卡日志 / 暗号小本本 |
| `couple_notify` | 情侣事件通知中心（所有 couple 推送落此表） |
| `couple_first` / `couple_answer_reaction` | 第一次清单 / 一问回答互评 |
| `couple_quote` / `couple_ticket` / `couple_song` | 甜蜜语录册 / 电影票根 / 我们的歌单 |

### 情侣空间 — 惊喜与关怀（V15-V16）

| 表 | 用途 |
|---|---|
| `couple_scratch` / `couple_mystery_box` / `couple_treasure` | 刮刮乐周卡 / 恋爱盲盒 / 藏宝图任务 |
| `couple_sweet_alarm` / `couple_miss_express` | 心动闹钟（24h 定时）/ 思念速递（随机延迟） |
| `couple_garden` / `couple_rose` / `couple_fortune_slip` | 爱情花园养成 / 每日玫瑰 / 幸运签 |
| `couple_confession` | 告白收藏（每年今天重播） |
| `couple_comfort` | 求抱抱（每天 1 条，感受→话术卡→回应） |
| `couple_peace_review` / `couple_sorry_ticket` | 矛盾复盘（双份合成锦囊）/ 道歉券 |
| `couple_truth` / `couple_whisper` / `couple_telepathy` / `couple_love_bank` | 真心话 / 匿名树洞 / 心灵感应 / 情话储蓄罐 |

### 情侣空间 — 养成与沟通（V17 / V19-V21）

| 表 | 用途 |
|---|---|
| `couple_challenge` / `couple_passbook` / `couple_hundred` / `couple_hundred_checkin` | 双人挑战赛 / 恋爱存折 / 百日之约 / 百日打卡 |
| `couple_wish_exchange` / `couple_read_plan` / `couple_read_progress` | 心愿互换 / 共读计划 / 各自进度 |
| `couple_travel_wish` / `couple_watchlist` / `couple_dict_word` / `couple_next_time` | 旅行心愿 / 追剧清单 / 恋爱词典 / 下次一定 |
| `couple_cool_down` / `couple_mood_relay` / `couple_guess_round` / `couple_story_line` | 冷静角 / 情绪接力 / 比划猜一轮 / 故事接龙句子 |
| `couple_apology_card` / `couple_feeling_word` | 道歉三部曲卡 / 每日心情词 |
| `couple_handhold` / `couple_miss_daily` / `couple_routine` | 隔空牵手 / 双城想念 / 作息表 |
| `couple_reunion_letter` / `couple_cloud_date` / `couple_safety_ping` / `couple_reunion_log` | 见面信 / 云约会清单 / 平安卡 / 见面日记 |
| `couple_security_bank` / `couple_decade_pact` / `couple_vision_card` / `couple_oath` | 安全感账户 / 十年之约 / 愿景板 / 承诺博物馆 |
| `couple_trust_coin` / `couple_self_contract` / `couple_pet` | 信任币 / 双人契约打卡 / 守护兽 |

### 情侣空间 — 游戏·陪伴·成长·浪漫·默契（V22-V26）

| 表 | 用途 |
|---|---|
| `couple_survey_answer` / `couple_quiz_duel` / `couple_love_word` | 一百问答题 / 出题考TA 对局 / 收藏情话课 |
| `couple_blind_pick` / `couple_sweet_battle` / `couple_sweet_line` / `couple_art_gallery` | 周末盲选提交 / 情话Battle / 参赛句子 / 抽象画廊 |
| `couple_dream` / `couple_food_note` / `couple_partner_fact` | 梦境手账 / 美食地图（WANT→EATEN）/ TA 使用手册 |
| `couple_sos_ping` / `couple_daily_three` / `couple_custom_badge` | 情绪SOS（SENT→HELD）/ 每日三问 / 自定义成就 |
| `couple_habit_streak` / `couple_thanks_note` / `couple_feel_log` | 21 天习惯搭子 / 感恩便签 / 情绪颗粒度日记 |
| `couple_weekly_star` / `couple_read_minute` / `couple_delay_task` / `couple_praise_bank` | 每周高光互评 / 共读一分钟感想 / 拖延互助 / 优点存折 |
| `couple_poem_chain` / `couple_poem_3line` / `couple_morning_note` | 情诗接龙 / 三行情书 / 醒来第一条 |
| `couple_drift_bottle` / `couple_cipher_note` / `couple_soul_answer` / `couple_journal` | 漂流瓶 / 密码情书 / 灵魂提问作答 / 贴纸手账 |
| `couple_love_lang` / `couple_heart_flash` / `couple_what_if` | 爱语测评结果 / 心动闪光 / 「如果」问答 |
| `couple_secret_signal` / `couple_sync_tap` / `couple_heart_day` | 动作暗语 / 同频共振按键 / 心动日历 |

### 情侣空间 — 经营、博物馆、饭桌、体温、仪式与公司（V27-V33）

| 表 | 用途 |
|---|---|
| `couple_family_meeting` / `couple_week_host` / `couple_skill_swap` | 家庭会议纪要（周一锚）/ 本周主理人 / 技能交换所 |
| `couple_month_review` / `couple_emergency_card` / `couple_month_snapshot` | 月度互评 / 应急卡 / 情侣存档点（每月 upsert） |
| `couple_point_ledger` / `couple_five_year_plan` / `couple_anniv_plan` | 家务积分流水（F244 发薪日亦插 EARN 流水）/ 五年计划双轨（MINE/OURS）/ 纪念日策划案 |
| `couple_doc_scene` / `couple_exhibit` / `couple_hidden_achievement` | 纪录片分镜（三幕）/ 博物馆展品 / 隐藏成就解锁记录 |
| `couple_house_rule` / `couple_dnd_setting` | 家规与修正案（RULE/AMENDMENT）/ 免打扰时段（支持跨零点） |
| `couple_user_pin` | F207 常用收藏（每人一行，pins 逗号分隔 ≤6 键） |
| `couple_dine_ticket` / `couple_dine_rate` / `couple_dine_nogo` | 今晚饭票（uk space+day+user）/ 吃过星评 / 踩雷库（uk space+name） |
| `couple_dine_weekplan` / `couple_dine_homecook` | 本周菜单（week=周一锚）/ 周拿手菜（uk space+week+user） |
| `couple_dine_cart` / `couple_dine_topic` | 搭伙车（OPEN/LOCKED 双锁成行）/ 饭桌话题打卡（uk space+day） |
| `couple_cozy_lightout` / `couple_cozy_sleep` / `couple_cozy_sheep` | 晚安熄灯打卡（uk space+day+user）/ 昨夜睡眠单（1-5 星+梦话）/ 数羊计数（taps+done+用时，60s 窗口锚 updated_at） |
| `couple_cozy_water` / `couple_cozy_weather` / `couple_cozy_latenight` | 每日杯数接力 / 冷暖互报（city+feel+temp_text，advised_by 记录叮嘱人）/ 熬夜陪伴卡（uk space+day+user） |
| `couple_cozy_slow` / `couple_cozy_remedy` / `couple_cozy_hug` | 周慢生活小事（week=周一锚，done_day 打卡）/ 疼痛对策本（uk space+for_user，每人一本）/ 抱抱流水（append 流水表，仅 idx space+day，无 uk） |
| `couple_ceremony_founded` / `couple_ceremony_ritual` / `couple_ceremony_mark` | 自定义小日子（repeat_year 是否每年）/ 过法卡（每日子≤3）/ 庆祝打勾（uk ritual+day 当日唯一） |
| `couple_ceremony_policy` / `couple_ceremony_renew` | 保险柜月保费（uk space+month+user，一人一月一句）/ 续约签字（anchor_day=满百天或周年当日） |
| `couple_ceremony_coupon` / `couple_ceremony_recap` | 愿望券（OPEN/USED，ref 记 policy-N payout 幂等）/ 当日体感（uk space+day+user，含年份天然分届） |
| `couple_board_role` / `couple_board_vote` | 头衔任命（from/to_user，appointed 盖章位）/ 董事会决议（PENDING/PASSED/VETOED，veto_by+decided_at 留痕） |
| `couple_board_report` / `couple_board_salary` | 年度述职（uk space+year+user，review+goal）/ 感谢工资（uk space+month+user 一月一次） |
| `couple_board_idea` / `couple_board_attend` | 金点子（adopted+vote_id 转决议）/ 例会签到（uk space+day+user，convened 记 10s 双签） |

## 查询与访问模式备忘

- 全部经 MyBatis-Plus `BaseMapperCompat` 的 default 方法，无 XML mapper、无手写 SQL 文件；复杂聚合（编年史/报告/仪表盘）为多查 + 内存聚合。
- "按天/空间稳定取内容"不查库，用 `stableHash(space+epochDay)` 在 Bank 静态数组上取模——同日重放结果一致。
- 高频读接口（today/dashboard/overview）普遍做惰性生成：首访自动建行（花园开垦、任务卡生成、周卡懒发）。
