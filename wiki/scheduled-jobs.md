# 定时任务

> 本页回答：后端有哪些定时类、各自什么时间做什么、推送哪些 WS 事件。

现役 **1 个** Job 类，在 `com.smart.chat.couple.infrastructure.scheduler`，`@Scheduled` 显式写 `zone = "Asia/Shanghai"`（不依赖 JVM 时区）；实测口径 `grep -rn "@Scheduled" src/main/java` 只命中这一条——**全仓就这 1 条定时任务**。
推送经 `messaging` 侧的 `ImPushService.pushCoupleEvent(Both)` 走 WS；`couple_notify` 表虽保留，但通知中心的落库与补看代码已随功能下线，**不再有写入**。

> **历轮裁剪的来路**：原 `CoupleSurpriseJob`（09:20 生日贺卡）与 `CoupleCareTalkJob`（23:00 深夜陪伴兜底）随求抱抱/心情/生日等功能卡删除；`CoupleQuestionJob`（09:00 每日一问定题）随 2026-10-20 三轮裁剪的每日一问一起删除。历史文档里的「4 类 12 条 / 3 类 4 条 / 2 条」都是当时的口径，作废。

## CoupleReminderJob — 「在一起」纪念日倒数

| 时间 | 方法 | cron / zone | 做什么 | 推送事件 |
|---|---|---|---|---|
| 09:30 | `remindAnniversaryCountdown()` | `0 30 9 * * ?` / `Asia/Shanghai` | 遍历 `couple_space` 有效空间（`CoupleSpaceMapper.findAllActive`），只读 **`couple_space.anniversary` 这一个日子**（可空，空则跳过），按周年倒数，命中 **提前 7 天 / 1 天 / 当天** 各推双方一次 | `anniversary-reminder`（推双方） |

- **只倒数 `couple_space.anniversary`**：共同日历表（原 `couple_anniversary` 倒数日清单）已随 ADR-0010 第 8 条删除，本任务不再扫它；倒数仍按**周年**重复，因为它记的是「在一起的那一天」。
- 落位算法 `nextOccurrence`：按今年，已过则顺延明年；**2/29 遇平年落到 2/28**（`withYearSafe`，因 `LocalDate.withYear` 对非法日期会抛异常）；解析失败或已过期返回 null 不推。
- **幂等口径**：每天只跑一次，按天去重，不额外存「上次提醒日」。
- 这是现役唯一一条与「在一起天数 / 纪念日」这条保留主线相关的推送；亲密日历（经期、边缘、性生活）刻意不发任何事件。

## 触发与排障备忘

- 任务靠 `ImPushService` 推 WS；本地起后端若不想真推外部渠道，须把 `ARECHAT_NOTIFY_*` 系列环境变量全部置空。
- 服务内取「今天」用的是 `LocalDate.now()`（系统时区），与 cron 的 `Asia/Shanghai` 是两个口径——部署机非东八区时日界会与推送时点错位；这是裁剪前就存在的既有口径，记录在 `docs/adr/0007` 后果一节，本轮未动。
- 新增定时任务照这个形状：只读现役表、命中即推、按天天然去重、不建额外状态表。
