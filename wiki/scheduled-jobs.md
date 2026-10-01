# 定时任务

> 本页回答：后端有哪些定时类、各自什么时间做什么、推送哪些 WS 事件。

共 4 个 Job 类，均在 `com.smart.chat.couple` 包，`@Scheduled` 显式 `zone = "Asia/Shanghai"`（不依赖 JVM 时区）。推送一律经 `ImPushService.pushCoupleEvent(Both)`，同时落库 `couple_notify`（离线补看）。

## CoupleReminderJob — 每日提醒主 Job

| 时间 | 方法 | 做什么 | 推送事件 |
|---|---|---|---|
| 09:00 | `remindOverdue` | 扫描逾期未完成的承诺卡（couple_promise），提醒归属方 | `promise-overdue`（推单人） |
| 09:30 | `remindAnniversaryCountdown` | 纪念日倒数命中 7/1/0 天时提醒双方 | `anniversary-reminder`（推双方） |
| 09:45 | `remindCountdowns` | 倒数日（couple_countdown）命中 7/3/1/0 天提醒 | `countdown-reminder` |
| 10:00 | `remindLowMoods` | 一方连续 2 天心情低落时，提醒**对方**用情绪急救箱 | `first-aid` |

## CoupleMemoryJob — 胶囊到期（F87）

| 时间 | 方法 | 做什么 | 推送事件 |
|---|---|---|---|
| 09:05 | `remindDueCapsules` | 扫描当日到期的 SEALED 时光胶囊（`CoupleCapsuleMapper.findByOpenDay`） | `capsule-due`（推双方） |

## CoupleSurpriseJob — 惊喜送达与巡检

| 时间 | 方法 | 做什么 | 推送事件 |
|---|---|---|---|
| 每分钟（`0 * * * * ?`） | `fireDue` | 扫描到期的心动闹钟（24h 内定时）与思念速递（5~30min 随机延迟）并送达 | `alarm-fired`、`miss-delivered` |
| 09:15 | `replayConfessions` | "每年今天"重播收藏的告白 | `confession-replay` |
| 09:20 | `birthdayCards` | 生日彩蛋（F59/F92）：读 im 包 `UserProfile.birthday`，当天给双方发贺卡；**生日前 3 天**先发预告（F92） | `birthday-card`、`birthday-eve` |
| 10:15 | `checkGardens` | 爱情花园缺水巡检，蔫了的花推送预警 | `garden-withered` |

## CoupleCareTalkJob — 晚间关怀

| 时间 | 方法 | 做什么 | 推送事件 |
|---|---|---|---|
| 21:00 | `deliverLoveInterest` | 情话储蓄罐"利息"：每人随机取一句未投递情话正式送达（存入时只发过预告） | `love-bank-interest` |
| 23:00 | `nightCare` | 深夜陪伴：当方心情负面且未被"求抱抱"接住时，提醒对方 | `night-care` |

## 设计要点

- **每分钟 Job 只服务两分钟能完成的送达**（闹钟/思念），其余全部是日级 cron；送达判定靠"到期时间 ≤ now"扫描，天然幂等（送达后行状态翻转）。
- 延迟送达类功能（慢递信箱、醒来第一条 deliver_day）复用**读取时惰性结算**而非定时任务，Job 只负责"需要主动通知"的场景。
- 新增 Job 须同步更新本页与 `are-chat-map` skill 第三节；新增事件名须在前端 `stores/couple.ts` 注册 case。
