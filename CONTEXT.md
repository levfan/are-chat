# CONTEXT.md — are-chat 统一语言（Ubiquitous Language）

> 本文件是**词汇的合同**，不是文档装饰。改代码里的类名/字段名/事件名前，先在这里对齐术语；这里写的词，代码里就必须只用这一个词。
> 来历：2026-10-04 情侣空间裁剪（见 `docs/couple-trim-ranking.md`）后启动 DDD 改造时建立。

## 全局规则

- 一个概念**只有一个名字**。历史别名（如 `letter` 曾同时指「情书」与「异地见面信」）已随裁剪消失，不得再引入。
- 中文文案与代码术语一一对应：界面上叫「亲密日历」的概念，代码里是 `CoupleCalendar`，不出现 `calendar-month`/`month-view` 这类同义异名。
- 表映射类一律 `*PO`（ADR-0002/0008）；下表「代码」列写的是**领域类型名**，括号里给出对应的表映射类，两者不是同一个类型，翻译只在仓储适配器里发生。
- 术语属于**它所在的限界上下文**。`user` 在 `identity` 指账号，在 `messaging` 指好友关系的一端，在 `couple` 指空间成员——同名不同义是允许的，跨上下文传递时必须显式转换（见 `docs/ddd/02-layering.md`）。

## identity（账号与准入）

| 术语 | 代码 | 含义 | 不是什么 |
|---|---|---|---|
| 账号 | `Account`（表映射 `AppUserPO`） | 一个可登录的主体，含 `role: USER/ADMIN` | 不是「好友」、不是「空间成员」 |
| 注册申请 | `RegistrationApplication`（表映射 `RegistrationApplicationPO`） | 审批制的入会申请，通过后才产生账号 | 注册本身**不产生登录态** |
| 会话 | `Sessions.requireUser(session)` | 当前登录用户名；HttpSession 是唯一凭据载体 | 不是 token，没有 JWT |

## messaging（私聊与关系）

| 术语 | 代码 | 含义 |
|---|---|---|
| 好友 | `Friend`（表映射 `FriendPO`；双向边的判定在 `FriendshipGate`） | 双向已确认的关系边 |
| 私信 | `PrivateMessage`（表映射 `PrivateMessagePO`） | 一对一消息；「双向会话谓词吃不到索引」是硬口径，取数走 `findLatestCreatedPerPeer` |
| 在线表 | `ChatSessionRegistry` | username → WS session 的在线映射 |
| 推送门面 | `ImPushService` | 所有出站实时帧的唯一出口（含情侣事件） |

## couple（情侣空间 · 核心域）

| 术语 | 代码 | 含义 | 关键约束 |
|---|---|---|---|
| 空间 | `CoupleSpace`（表映射 `CoupleSpacePO`） | 两个人绑定的唯一容器；`userA`/`userB` 按**用户名（区分大小写）字典序**分配 | 一用户只能在一个有效空间 |
| 我 / TA | `me` / `partnerOf(me)` | 服务层的两个视角；`partnerOf` 是唯一的「对方」定义 | 不得出现 `user1/user2` 这类无语言指称的变量 |
| 有效空间 | `requireSpace(me)` | 取当前有效空间，无效抛 404 | 「未建空间」一律 404 中文文案，不由前端兜 |
| 在一起的日子 | `CoupleSpace.anniversary`（列） | 单个日期，在一起天数与纪念日倒数共同的根据 | **不是共同日历**（`couple_anniversary` 倒数日清单已删）；没绑过时天数按建空间那天起算 |
| 在一起的天数 | `CoupleService.daysTogether(space)` → `SpaceVO.days` | 从纪念日（缺省取建立日）数到今天，含当天 | 建立当天 = 第 1 天；最小值钳到 1 |
| 经期 | `PeriodRecord`（表映射 `CouplePeriodPO`；`couple_period`） | 一次经期的一行：开始日必填、结束日可后补 | 结束日只能记「今天」，历史区间改不了；同一天不许记两遍 |
| 覆盖日 | `PeriodRecord.covers(day, today)` | 某一天是不是在这次经期的第一天到最后一日之间 | 记了结束日含首含尾；**没记结束日只到今天**；脏日期静默算没覆盖 |
| 周期 | `PeriodCycles` | 由若干次经期的**开始日之间隔**读时算：平均周期、下次预计、距下次天数 | 只认 15–45 天的有效间隔；一条记录时按默认 28 天估；文案必须写「按记录推算，不是医学判断」 |
| 亲密记录 | `IntimateLog`（表映射 `CoupleIntimateLogPO`；`couple_intimacy_log`） | 一次边缘亲昵（EDGE）或性生活（SEX）的一行 | 同一天允许多条；上次/本月分类型/累计全部**读时算**（`IntimateLogs`） |
| 亲密日历 | `CoupleCalendarService.CoupleCalendarVO` | 一个自然月的**唯一读模型**：经期覆盖日/开始日/结束日、本月经期区间、周期统计、本月亲密标记 | 跨两个聚合但**只读**；写仍走经期与亲密各自的端点；前端不自己拼两张看板，也不自己数天数 |
| 通知 | `NotifyEntry`（表映射 `CoupleNotifyPO`） | 情侣事件推送的落库副本 | 通知中心已于 2026-10-20 下线，表保留但**已无读写代码**，不再有新行 |
| 邀请 | `Invite`（表映射 `CoupleInvitePO`） | PENDING→ACCEPTED/REJECTED/CANCELED 单向，只能处理发给自己的、只能撤回自己发的 | 同意之后才存在空间；不再顺带写打卡日（打卡已下线） |

> 历轮裁剪删掉的术语（贴贴 `BondAction` / 心情日记 `Mood` / 心情回应 `MoodReaction` / 求抱抱 `ComfortRequest` /
> 安全词 `Safeword`·`SafewordUse` / 饭票 `DineTicket` / 家务轮盘 `SpinTask` / 加班预报与留灯 `QuestOvertime` /
> 愿望券 `WishCoupon` / 好事簿 `Deed` / 刮刮乐与盲盒 `Scratch`·`MysteryBox` / 积分台账 `PointEntry` /
> 共同日历 `Anniversary` / 收藏卡 `UserPin` / 百日回顾 `CoupleMemory` / 心动时刻（`private_message.heart_at` 列保留但已不读不写）
> 已从本表移除，判据见 `docs/adr/0010-couple-trim-to-v8-features.md`。
> 2026-10-20 三轮裁剪又删掉：**每日一问** `QuestionAnswer`·`DailyQuestion`、**打卡日与连续互动** `StreakDay`·`StreakDays`·`StreakTier`·`MakeupPolicy`、
> **愿望清单** `Wish`、**心动值** `Intimacy`·`IntimacyCalculator`·`IntimacySource`、**空间个性化**（宣言 `slogan` / 主题 `theme` / 爱称 `nickOf`·`renamePartner`）——
> 心动值的五项供数里四项随这三张卡一起消失，留着就是一串恒为 0 的数字，故整条下线；`couple_space` 的对应六列留作死存储不读写。
> 再引入同名概念前，必须先回答「它和保留的在一起天数、纪念日、亲密日历是什么关系」。

## 事件命名口径

WS 事件名 = 小写连字符、动词或过去分词结尾，前缀族即聚合归属：现役 **6 个**，全部属于地基（实测命令：
`grep -rho -E 'pushCoupleEvent(Both)?\("[a-z-]+"' src/main/java | sed 's/.*("//' | tr -d '"' | sort -u`）。

| 族 | 事件 |
|---|---|
| 建立与解散 | `invite` `invite-accepted` `invite-rejected` `dissolved` |
| 在一起的日子 | `anniversary-updated`（对方改了日子）、`anniversary-reminder`（`CoupleReminderJob` 的周年倒数） |

随历轮裁剪下线的族（`bond-*` `mood-*` `comfort-*` `catch-*` `dine-*` `factory-*` `quest-*` `ceremony-*`
`echo-*` `scratch-*` `box-*` `anniversaries-changed` `birthday-*` `night-care`，以及 2026-10-20 删的
`streak-*` `question-*` `wish-*` `pet-name-changed` `space-themed`）不得复活。

刻意**不存在**的事件：经期与亲密记录这一条链路**一个事件都不发**。这是两个人的私密账本，
不因对方点了一下就弹提醒——前端写完自己重拉当月日历。

## 产品红线（长期约束）

- 情侣功能**情绪价值优先**；不做照片/视频上传类功能。
- 迁移脚本必须幂等且 H2/MariaDB 双兼容；已入库的 V 脚本不得修改。
