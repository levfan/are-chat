# CONTEXT.md — are-chat 统一语言（Ubiquitous Language）

> 本文件是**词汇的合同**，不是文档装饰。改代码里的类名/字段名/事件名前，先在这里对齐术语；这里写的词，代码里就必须只用这一个词。
> 来历：2026-10-04 情侣空间裁剪（见 `docs/couple-trim-ranking.md`）后启动 DDD 改造时建立。

## 全局规则

- 一个概念**只有一个名字**。历史别名（如 `letter` 曾同时指「情书」与「异地见面信」）已随裁剪消失，不得再引入。
- 中文文案与代码术语一一对应：界面上叫「每日一问」的概念，代码里是 `question`，不出现 `qa`/`daily-ask` 这类同义异名。
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
| 心动值 | `Intimacy`（`CoupleService.intimacy()`，算式在 `IntimacyCalculator`） | 由五项活数据算出的关系温度计：`daysTogether×1 + checkinDays×2 + longestStreak×3 + answerDays×3 + wishFulfilled×5` | 阶梯阈值 0/60/150/260/400/560/760；2026-10-05 二轮裁剪**已重标定**（旧六项供数全灭，沿用会把称号卡在一级） |
| 答题天数 | `answerDays`（`CoupleQuestionService.bothAnsweredDays(spaceId)`） | **双方当天都答完每日一问**的日子数，单向答了不算 | 与「打卡日」同源，不另立第二套判定 |
| 愿望清单 | `Wish`（表映射 `CoupleWishPO`；`wish`） | 「我想要什么东西」，双方都能添加 | **不是愿望券**（愿望券已随 ADR-0010 第 1 条下线）；`PREPARED` 对被许愿人保密 |
| 偷偷准备 | `Wish.visibleStatusFor(me)` | 对方给我许的愿标了「已准备」，我这边仍显示 OPEN | 唯一回显入口；标记/撤销**不推任何事件**，连 `preparedAt` 也置 null |
| 打卡日 | `StreakDay`（表映射 `CoupleStreakDayPO`；`streak`） | 那一天双方共同活跃：都答完每日一问，或空间建立当天，或补签成功 | 原名 `BondDay`——`bond` 在此表建立之后才随贴贴一起下线，2026-10-05 改名（ADR-0010 第 5 条） |
| 连续互动 | `StreakDays` | 由打卡日集合读时算：`currentStreak` / `longestStreak` | 今天没打**不算断**，只有昨天缺行才叫断；不物化缓存列 |
| 解锁档位 | `StreakTier` | 七档 3/7/14/21/30/50/100 天，管气泡·背景·昵称光效·挂件·称号·贴纸·隐藏页 | 判 `longestStreak`，**单调不回退**；档位 key 是前端挂视觉的合同，上线即冻结 |
| 补签 | `MakeupPolicy` | 免费买回一个缺口 | 只能补最近 7 天、每自然月最多 3 次、今天不许补；**不再扣积分**（积分台账已删，稀缺性换成次数） |
| 每日一问 | `QuestionAnswer`（表映射 `CoupleQuestionAnswerPO`；双方视角的读模型是 `DailyQuestion`，`question`） | 一个空间一天一题，每人一行答案 | 同空间同天同题（`stableHash`）；**双方都答过才互看**；它同时是打卡的触发源（`CoupleQuestionService.answer()` 唯一挂钩） |
| 百日回顾 | `CoupleMemoryService` | 连满 100 天的隐藏页：时间轴 + 一句话总结 | 总结是规则生成（`RelationSummary`），本仓库无 LLM 依赖，见 ADR-0007 第 7 条 |
| 在一起的日子 | `CoupleSpace.anniversary`（列） | 单个日期，恋爱天数 / 里程碑横幅 / 纪念日倒数三处的共同根据 | **不是共同日历**（`couple_anniversary` 倒数日清单已删） |
| 通知 | `NotifyEntry`（表映射 `CoupleNotifyPO`） | 情侣事件推送的落库副本（离线可补看） | 经 `CoupleNotifyRecorder` 挂接，im 不反向依赖 couple；邀请能否被看见全押在这一条上，故算地基不算功能 |
| 邀请 | `Invite`（表映射 `CoupleInvitePO`） | PENDING→ACCEPTED/REJECTED/CANCELED 单向，只能处理发给自己的、只能撤回自己发的 | 建立流程写第 1 个打卡日（`CoupleStreakService.confirmCreationDay`） |

> 本轮删掉的术语（贴贴 `BondAction` / 心情日记 `Mood` / 心情回应 `MoodReaction` / 求抱抱 `ComfortRequest` /
> 安全词与暂停复盘 `Safeword`·`SafewordUse` / 饭票 `DineTicket` / 家务轮盘 `SpinTask` / 加班预报与留灯 `QuestOvertime` /
> 愿望券 `WishCoupon` / 好事簿 `Deed` / 刮刮乐与盲盒 `Scratch`·`MysteryBox` / 积分台账 `PointEntry` /
> 共同日历 `Anniversary` / 收藏卡 `UserPin`）已从本表移除，判据与连带改造见 `docs/adr/0010-couple-trim-to-v8-features.md`。
> 再引入同名概念前，必须先回答「它和保留的四项功能是什么关系」。

## 事件命名口径

WS 事件名 = 小写连字符、动词或过去分词结尾，前缀族即聚合归属：现役 **15 个**，按族分四组（实测命令：
`grep -rho -E 'pushCoupleEvent(Both)?\("[a-z-]+"' src/main/java | sed 's/.*("//' | tr -d '"' | sort -u`）。

| 族 | 事件 |
|---|---|
| 建立与地基 | `invite` `invite-accepted` `invite-rejected` `dissolved` `anniversary-updated` `anniversary-reminder` `space-themed` `pet-name-changed` |
| 连续互动打卡 | `streak-checkin` `streak-unlocked` `streak-makeup` |
| 每日一问 | `question-daily` `question-answered` |
| 愿望清单 | `wish-added` `wish-fulfilled` |

随 2026-10-05 二轮裁剪下线的族（`bond-*` `mood-*` `comfort-*` `catch-*` `dine-*` `factory-*` `quest-*`
`ceremony-*` `echo-*` `scratch-*` `box-*` `anniversaries-changed` `birthday-*` `night-care`）不得复活。
`pet-name-changed` 是**保留**的那一个：爱称的写入口从 `PUT /api/couple/bond/pet-name` 合并进了
`PUT /api/couple/profile`，但「对方给你起了新爱称」必须单独成一条事件——三项个性化共用一条
`space-themed` 会把一件心事说成一次装修（分流见 `CoupleProfilePushTest`）。

刻意**不存在**的事件：`wish-prepared` / `wish-unprepared`。愿望被偷偷标记「已准备」不推任何事件，
这是产品规则而不是遗漏（`CoupleWishServiceTest.markingPreparedPushesNothingAtAll` 用
`verifyNoInteractions(push)` 锁死）——将来谁"顺手补上"这个推送，就等于把惊喜删掉。

## 产品红线（长期约束）

- 情侣功能**情绪价值优先**；不做照片/视频上传类功能。
- 迁移脚本必须幂等且 H2/MariaDB 双兼容；已入库的 V 脚本不得修改。
