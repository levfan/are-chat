# 定时任务

> 本页回答：后端有哪些定时类、各自什么时间做什么、推送哪些 WS 事件。

共 **3 个** Job 类，均在 `com.smart.chat.couple` 包，`@Scheduled` 显式 `zone = "Asia/Shanghai"`（不依赖 JVM 时区）。推送一律经 `ImPushService.pushCoupleEvent(Both)`，同时落库 `couple_notify`（F41，离线补看）。

> 2026-10-04 情侣空间裁剪后，任务从 4 个类 12 条压到 3 个类 4 条。被删的 8 条（约定逾期、倒数日、情绪急救箱、告白重现、花园缺水、情话储蓄罐利息、胶囊到期、周轮值班相关）随其功能卡一并下线，见 [../docs/couple-trim-ranking.md](../docs/couple-trim-ranking.md) 第五节。

## CoupleReminderJob — 纪念日倒数

| 时间 | 方法 | 做什么 | 推送事件 |
|---|---|---|---|
| 09:30 | `remindAnniversaryCountdown` | 先算「在一起」纪念日（`couple_space.anniversary`），再扫共同日历 `couple_anniversary`；倒数命中 7/1/0 天时推双方 | `anniversary-reminder`（推双方） |

- `yearly` 的按今年落位（已过则明年），非 `yearly` 只认未来或当天；**2/29 遇平年落到 2/28**（`withYearSafe`，因为 `LocalDate.withYear` 对非法日期会抛异常）。
- 每天只跑一次，天然按天去重，所以不再单独存"上次提醒日"。

## CoupleSurpriseJob — 生日彩蛋

| 时间 | 方法 | 做什么 | 推送事件 |
|---|---|---|---|
| 09:20 | `birthdayCards` | 读 `im` 包 `UserProfile.birthday`（跨包只读），生日当天给本人一张贺卡、给 TA 一份提醒；**生日前 3 天**先给对方一条预告 | `birthday-card`（推双方各一条不同文案）、`birthday-eve`（推对方） |

- 生日为空或格式不满 10 位时按 MM-dd 兜底比对；同一天两人都是生日时各自都收到一套。
- 原同一类里的「每分钟送达心动闹钟/思念速递」「09:15 告白重现」「10:15 花园缺水巡检」三条已随功能删除，本类现在只做生日。

## CoupleCareTalkJob — 深夜陪伴兜底

| 时间 | 方法 | 做什么 | 推送事件 |
|---|---|---|---|
| 23:00 | `nightCare` | 遍历生效中的空间：某人今天心情是 SAD/ANGRY/SICK/TIRED **且还没被求抱抱接住**时，提醒对方去陪 TA | `night-care`（推对方） |

- "已经被接住"的判据是当天那条 `couple_comfort` 的 `handled=true`——所以求抱抱被回应后不会再打扰。
- 原 21:00 的「情话储蓄罐利息」随该功能下线删除。

## 触发与排障备忘

- 三个任务都靠 `ImPushService` 双写（WS + `couple_notify`），所以本地起后端时若不想真推微信，必须把 `ARECHAT_NOTIFY_*` 系列环境变量全部置空。
- 任务里的"今天"一律 `LocalDate.now()`（Asia/Shanghai 由 `@Scheduled` 的 zone 决定），不要在方法里再换时区。
- 新增定时任务请照这三个类的形状：只读活表、命中即推、按天天然去重、不建额外状态表。
