# 定时任务

> 本页回答：后端有哪些定时类、各自什么时间做什么、推送哪些 WS 事件。

现役 **2 个** Job 类，均在 `com.smart.chat.couple.infrastructure.scheduler`，`@Scheduled` 都显式写 `zone = "Asia/Shanghai"`（不依赖 JVM 时区）；实测口径 `grep -rn "@Scheduled" src/main/java` 只命中这两条——**全仓就这 2 条定时任务**。
推送一律经 `messaging` 侧的 `ImPushService.pushCoupleEvent(Both)`，同时落库 `couple_notify`（F41，离线补看）。

> **2026-10-05 二轮裁剪后**：原 `CoupleSurpriseJob`（09:20 生日贺卡）与 `CoupleCareTalkJob`（23:00 深夜陪伴兜底）随求抱抱/心情/生日等功能卡一并删除；`CoupleReminderJob` 也不再扫共同日历。剩这 2 个。历史文档里的「4 类 12 条 / 3 类 4 条」都是裁剪前的口径，作废。

## CoupleQuestionJob — 每日一问定题推送

| 时间 | 方法 | cron / zone | 做什么 | 推送事件 |
|---|---|---|---|---|
| 09:00 | `dailyQuestion()` | `0 0 9 * * ?` / `Asia/Shanghai` | 调 `CoupleQuestionService.remindDailyQuestion()`：遍历 `couple_space` 全部有效空间，**当天已有两行回答的空间跳过**（不打扰答完的人），否则按 `stableHash(spaceId + "v8-question-bank" + day)` 取当日题推双方 | `question-daily`（推双方） |

- **幂等口径**：一天只跑一次，天然按天去重；09:00 是空档（无其它任务占用）。
- 只推题干，不推任何人的答案——回答事件是 `question-answered`，答案内容等双方答完才由 `GET /api/couple/question/today` 互看。

## CoupleReminderJob — 「在一起」纪念日倒数

| 时间 | 方法 | cron / zone | 做什么 | 推送事件 |
|---|---|---|---|---|
| 09:30 | `remindAnniversaryCountdown()` | `0 30 9 * * ?` / `Asia/Shanghai` | 遍历 `couple_space` 有效空间（`CoupleSpaceMapper.findAllActive`），只读 **`couple_space.anniversary` 这一个日子**（可空，空则跳过），按周年倒数，命中 **提前 7 天 / 1 天 / 当天** 各推双方一次 | `anniversary-reminder`（推双方） |

- **裁剪后只倒数 `couple_space.anniversary`**：共同日历表（原 `couple_anniversary` 倒数日清单）已随 ADR-0010 第 8 条删除，本任务不再扫它；倒数仍按**周年**重复，因为它记的是「在一起的那一天」。
- 落位算法 `nextOccurrence`：按今年，已过则顺延明年；**2/29 遇平年落到 2/28**（`withYearSafe`，因 `LocalDate.withYear` 对非法日期会抛异常）；解析失败或已过期返回 null 不推。
- **幂等口径**：每天只跑一次，按天去重，不额外存「上次提醒日」。

## 触发与排障备忘

- 两个任务都靠 `ImPushService` 双写（WS + `couple_notify`）；本地起后端若不想真推外部渠道，须把 `ARECHAT_NOTIFY_*` 系列环境变量全部置空。
- 服务内取「今天」用的是 `LocalDate.now()`（系统时区），与 cron 的 `Asia/Shanghai` 是两个口径——部署机非东八区时日界会与推送时点错位；这是裁剪前就存在的既有口径，记录在 `docs/adr/0007` 后果一节，本轮未动。
- 新增定时任务照这两个的形状：只读现役表、命中即推、按天天然去重、不建额外状态表。
