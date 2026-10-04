# CONTEXT.md — are-chat 统一语言（Ubiquitous Language）

> 本文件是**词汇的合同**，不是文档装饰。改代码里的类名/字段名/事件名前，先在这里对齐术语；这里写的词，代码里就必须只用这一个词。
> 来历：2026-10-04 情侣空间裁剪（见 `docs/couple-trim-ranking.md`）后启动 DDD 改造时建立。

## 全局规则

- 一个概念**只有一个名字**。历史别名（如 `letter` 曾同时指「情书」与「异地见面信」）已随裁剪消失，不得再引入。
- 中文文案与代码术语一一对应：界面上叫「贴贴」的概念，代码里是 `bond`，不出现 `cuddle`/`hug-action` 这类同义异名。
- 术语属于**它所在的限界上下文**。`user` 在 `identity` 指账号，在 `messaging` 指好友关系的一端，在 `couple` 指空间成员——同名不同义是允许的，跨上下文传递时必须显式转换（见 `docs/ddd/02-layering.md`）。

## identity（账号与准入）

| 术语 | 代码 | 含义 | 不是什么 |
|---|---|---|---|
| 账号 | `AppUser` | 一个可登录的主体，含 `role: USER/ADMIN` | 不是「好友」、不是「空间成员」 |
| 注册申请 | `RegistrationApplication` | 审批制的入会申请，通过后才产生账号 | 注册本身**不产生登录态** |
| 会话 | `Sessions.requireUser(session)` | 当前登录用户名；HttpSession 是唯一凭据载体 | 不是 token，没有 JWT |

## messaging（私聊与关系）

| 术语 | 代码 | 含义 |
|---|---|---|
| 好友 | `Friend` | 双向已确认的关系边 |
| 私信 | `PrivateMessage` | 一对一消息；「双向会话谓词吃不到索引」是硬口径，取数走 `findLatestCreatedPerPeer` |
| 在线表 | `ChatSessionRegistry` | username → WS session 的在线映射 |
| 推送门面 | `ImPushService` | 所有出站实时帧的唯一出口（含情侣事件） |

## couple（情侣空间 · 核心域）

| 术语 | 代码 | 含义 | 关键约束 |
|---|---|---|---|
| 空间 | `CoupleSpace` | 两个人绑定的唯一容器；`userA`/`userB` 按**用户名（区分大小写）字典序**分配 | 一用户只能在一个有效空间 |
| 我 / TA | `me` / `partnerOf(me)` | 服务层的两个视角；`partnerOf` 是唯一的「对方」定义 | 不得出现 `user1/user2` 这类无语言指称的变量 |
| 有效空间 | `requireSpace(me)` | 取当前有效空间，无效抛 404 | 「未建空间」一律 404 中文文案，不由前端兜 |
| 心动值 | `Intimacy`（`CoupleService.intimacy()`） | 由六项活数据算出的关系温度计：`moodDays×1 + bondDays×2 + deedCount×2 + lampCount×3 + reflectCount×2 + pointEarned×1` | 阶梯阈值 0/50/150/300/500/800/1300 **刻意未重标定** |
| 贴贴 | `CoupleAction`（`bond`） | 一次轻量示好动作；`bondDays` = **双方当天都发过**的日子数，单向不算 | 这是心动值里最容易算错的一项 |
| 心情日记 | `CoupleMood` | 每人每天一行的心情，可当天改写 | 回应（`CoupleMoodReaction`）另算 |
| 求抱抱 | `CoupleComfort` | 一方发出感受、另一方接住 | 「接住」是归属闸门，自己不能接 |
| 安全词 | `CoupleCatchSafeword`（`catch`） | 事先约定的暂停词 | 一天一人只能喊一次 |
| 暂停复盘 | `CoupleCatchSafewordUse.reflect` | 喊停者事后补的一句 | 只有喊停本人能补 |
| 饭票 | `CoupleDineTicket` | 每人每天一票，「吃什么」由服务端按票池 `stableHash` 裁决 | 双方必须看到同一道 |
| 家务轮盘 | `CoupleSpinTask`（`factory`） | 一周一转、对方认账后本人才能打勾 | 双签才生效 |
| 加班预报 / 留灯 | `CoupleQuestOvertime` / lamp | 预报今晚几点回；灯卡**只有对方能留** | 自己留不算 |
| 愿望券 | `CoupleCeremonyCoupon` | 花 10 积分发一张，对方核销 | 积分唯一出水口 |
| 愿望清单 | `CoupleWish`（`wish`） | 「我想要什么东西」，双方都能添加 | **不是愿望券**（见 `docs/adr/0007` 第 9 条）；`PREPARED` 对被许愿人保密 |
| 偷偷准备 | `Wish.visibleStatusFor(me)` | 对方给我许的愿标了「已准备」，我这边仍显示 OPEN | 唯一回显入口；标记/撤销**不推任何事件**，连 `preparedAt` 也置 null |
| 贴贴打卡日 | `CoupleBondDay`（`bond` 的派生） | **双方当天都发过贴贴**的那一天（含补签） | 与心动值的 `bondDays` 同一条口径，不另立「互动日」 |
| 连续互动 | `BondStreak` | 由打卡日集合读时算：`currentStreak` / `longestStreak` | 今天没打**不算断**，只有昨天缺行才叫断；不物化缓存列 |
| 解锁档位 | `StreakTier` | 七档 3/7/14/21/30/50/100 天，管气泡·背景·昵称光效·挂件·称号·贴纸·隐藏页 | 判 `longestStreak`，**单调不回退**；档位 key 是前端挂视觉的合同，上线即冻结 |
| 补签 | `MakeupPolicy` | 花 20 分买回一个缺口 | 只能补最近 7 天、每自然月最多 3 次、今天不许补 |
| 每日一问 | `CoupleQuestionAnswer`（`question`） | 一个空间一天一题，每人一行答案 | 同空间同天同题（`stableHash`）；**双方都答过才互看**；不进心动值公式 |
| 百日回顾 | `CoupleMemoryService` | 连满 100 天的隐藏页：时间轴 + 一句话总结 | 总结是规则生成（`RelationSummary`），本仓库无 LLM 依赖，见 ADR-0007 第 7 条 |
| 好事簿 | `CoupleEchoDeed`（`echo`） | 「TA 为我做的事」，单记录人口径，被记的那位加分 | 加星只归记录人 |
| 刮刮乐 / 盲盒 | `CoupleScratch` / `CoupleMysteryBox` | 周券懒生成、送券人核销；盲盒到日才可拆、装盒人不能自拆 | — |
| 积分台账 | `CouplePointLedger`（`EARN`/`SPEND`） | 心动值与券本的共同账本 | **断言必须锁真插一行，不能只看返回的 VO** |
| 通知 | `CoupleNotify` | 情侣事件推送的落库副本（离线可补看） | 经 `CoupleNotifyRecorder` 挂接，im 不反向依赖 couple |
| 收藏卡 | `CoupleUserPin`（`pin`） | 前端 F207 常用功能置顶，每人 ≤6 | key = 前端卡根 `data-testid` |

## 事件命名口径

WS 事件名 = 小写连字符、动词或过去分词结尾，前缀族即聚合归属：`bond-*` `mood-*` `comfort-*` `catch-*` `dine-*` `factory-*` `quest-*` `ceremony-*` `echo-*` `scratch-*` `box-*` `streak-*` `question-*` `wish-*`，另有地基类 `invite*` `anniversary-*` `space-themed` `dissolved` `birthday-*` `pet-name-changed` `night-care` `anniversaries-changed`。现役 **44 个**（实测：后端 `pushCouple*` 抽取，v8 新增 7 个 = `streak-checkin` `streak-unlocked` `streak-makeup` `question-daily` `question-answered` `wish-added` `wish-fulfilled`）。

刻意**不存在**的事件：`wish-prepared` / `wish-unprepared`。愿望被偷偷标记「已准备」不推任何事件，
这是产品规则而不是遗漏（`CoupleWishServiceTest.markingPreparedPushesNothingAtAll` 用
`verifyNoInteractions(push)` 锁死）——将来谁"顺手补上"这个推送，就等于把惊喜删掉。

## 产品红线（长期约束）

- 情侣功能**情绪价值优先**；不做照片/视频上传类功能。
- 迁移脚本必须幂等且 H2/MariaDB 双兼容；已入库的 V 脚本不得修改。
