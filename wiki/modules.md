# 包结构导览

> 本页回答：代码现在按什么分包、每个上下文里放什么、去哪找一类东西。
> 2026-10-04 由 11 个功能包合成 5 个限界上下文（ADR-0001），2026-10-05 五个上下文全部完成战术改造
> （ADR-0008；判据与实测见 `docs/ddd/06-ddd-standard.md`，施工模板见 `docs/ddd/05-tactical-playbook.md`）。
> 统计口径：主源文件 327 个、测试文件 96 个、`@TableName` 实体 35 个 = 仓储端口 35 个 = 适配器 35 个。

## 每个上下文内部的四层（方向唯一）

```
<上下文>/
├── api/                     @RestController + 请求体 record
├── application/             Service（用例编排 + 对外投影 VO + DomainRules 翻译器）
├── domain/                  聚合 / 薄实体 / 策略对象 / RuleViolation / *Repository 端口
│                            ★ 不 import Spring·MyBatis·fastjson·servlet，也不带容器与 ORM 注解
└── infrastructure/          persistence（*PO + Mapper + *RepositoryAdapter）、content、transport、
                             scheduler、notify、account、boot、throttle、devtools
```

`api`/`application`/`domain` 任一层 import 本上下文的 `infrastructure.persistence` 都会让构建红
（`ArchitectureGuardTest.persistenceTypesStayBehindRepositoryPorts`，账本 `TACTICAL_PENDING` 现为空集）。

## couple — 情侣空间（核心域，167 个文件）

| 层 | 内容 |
|---|---|
| api | 17 个 Controller：`CoupleController`（空间/邀请/日历/心情/心动值）、`CoupleBondController`（贴贴·爱称·心情回应）、`CoupleCareController`（求抱抱·夜间关怀）、`CoupleCatchController`（安全词）、`CoupleCeremonyController`（愿望券）、`CoupleDiningController`（饭票）、`CoupleFactoryController`（家务轮盘）、`CoupleQuestController`（加班预报·留灯）、`CoupleEchoController`（好事簿）、`CoupleSurpriseController`（刮刮乐·盲盒）、`CoupleStreakController`（连续打卡·七档解锁）、`CoupleQuestionController`（每日一问）、`CoupleWishController`（愿望清单）、`CoupleMemoryController`（百日回顾）、`CouplePinController`（收藏卡）、`CoupleNotifyController`（通知中心）、`CoupleAdminController`（运营看板） |
| application | 16 个 Service + `DomainRules`（`RuleViolation` → 400/403/404/409，文案不重写）；VO 是 Service 内嵌 `record` |
| domain | 52 文件 / 22 个端口。按卡分包：`space`（`CoupleSpace` 聚合：字典序定 A/B、单向解散、装饰与爱称闸）、`intimacy`（`IntimacyCalculator` 六项权重与七级阈值）、`bond`（`BondAction` 贴贴流水 + kind 词表/emoji/里程碑）、`mood`（`Mood` 键白名单、`MoodReaction` 回应闸）、`comfort`（`ComfortRequest` 接住归属）、`safeword`（`Safeword`/`SafewordUse` 每日一次与复盘归属）、`dine`（`DineTicket` 内容闸 + `sameDish`）、`chore`（`SpinTask` 一周一转 + 双签）、`quest`（`QuestOvertime` 小时钳制 + 灯只能对方留）、`coupon`（`WishCoupon` 余额闸门）、`wish`（`Wish` 的「已准备」对许愿人保密）、`question`（`QuestionAnswer` 每人行 + `DailyQuestion` 双方读模型）、`streak`（`BondDay`/`BondStreak`/`StreakTier`/`MakeupPolicy`）、`memory`（`RelationSummary` 规则生成，无 LLM）、`points`（`PointEntry` 只追加台账）、`notify`（`NotifyEntry` 落库副本）、`invite`（`Invite` 单向状态机 + 两处归属闸）、`anniversary`（`Anniversary` 类型回退 NORMAL、农历以 `lunarMd` 为真源）、`deed`（`Deed` 加星幂等与记录人归属）、`pin`（`UserPin` ≤6 与 key 合法性）、`surprise`（`Scratch`/`MysteryBox` 收券人刮、送券人核销、装盒人不能自拆） |
| infrastructure | 22 `*PO` + 22 Mapper + 22 适配器；`content` 9 个静态话术/题库；`scheduler` 4 个 Job；`notify/CoupleNotifyRecorder`（挂在推送门面上落副本）；`account/SpaceCascadeAdapter`（实现 identity 的 `AccountCascade`） |

## messaging — 好友 / 私信 / 在线状态 / 推送门面（支撑域，67 个文件）

- **零业务出向依赖之外只依赖 `identity.domain`**；对外发布 7 个语言端口：`CoupleEventPublisher`（44 个情侣事件的唯一出口）、`PeerProfileReader`、`FriendshipChecker`、`PresenceReader`、`AnnouncementBroadcaster`、`OutboundNotifySink`、`NotifySinkRegistry`。
- domain：`friend/Friend`+`FriendRequest`+`FriendshipGate`（双向边与申请状态机）、`conversation/PrivateMessage`、`pin/Conversation`+`ConversationPin`、`profile/UserProfile`、`reaction/MessageReaction`、`star/MessageStar`，各配端口（7 个）。
- infrastructure：7 `*PO` + 7 Mapper + 7 适配器；`transport`（`ImPushService`/`ChatSessionRegistry`/`ChatEndpoint`/`ChatWebSocketBridge`/`ChatMessage`）、`throttle/MessageRateLimiter`、`identity/` 下 4 个实现 identity 端口的适配器。
- 取数硬口径：双向会话谓词吃不到索引，「最后一条」「未读数」走 `findLatestCreatedPerPeer` / `selectUnreadCountsByPeer`，不许退回 per-peer 循环。

## identity — 账号与准入（通用域，35 个文件）

- **零出向上下文依赖**的纯上游。domain：`account/Account`（+ 无状态策略 `AccountRules`：手机号/用户名/昵称/密码）、`registration/RegistrationApplication`（PENDING→APPROVED/REJECTED 单向，重复处理 409）、`audit/AdminAudit`（薄流水）、`verification/SmsCode`（重发 60s / 5 分钟 / 试错 5 次）、`RuleViolation`（带对外状态码）；3 个端口 3 个适配器。
- 另有 `AccountDirectory`（别人问账号只用它，含 `Account` 最小视图）与 `AccountCascade`/`ProfileProvisioner`/`WelcomeMessenger`/`AdminAlerter`/`AdminNotifyChannel` 五个「需要别人配合」的端口，由 messaging / platform 提供实现。
- infrastructure：`AppUserPO`/`RegistrationApplicationPO`/`AdminAuditPO`、`security/PasswordHasher`、`throttle/LoginRateLimiter`、`boot/AdminBootstrapper`。

## platform — 公告 / 健康检查 / 管理员通知（通用域，23 个文件）

- domain：`announcement/Announcement`（发布/关闭单向状态机 + 同一时刻只一条生效）、`announcement/AnnouncementRead`（薄流水）、`notify/NotifyChannel`+`NotifyChannelConfig`+`EnabledNotifyChannels`（渠道目录与开关）；2 个仓储端口 2 个适配器。
- 公告管理端点在 `AnnouncementAdminController`，路由 `/api/admin/announcements*`；`HealthController` 与 `devtools/H2DumpEndpoint` 留在各自层。
- `AdminNotifyService` 的并发模型（单线程异步队列 + 每请求 8s 超时）已核查过不是缺陷，本轮只改取数路径。

## filestorage — 本地文件存储（通用域，14 个文件）

- domain：`file/UploadedFile` + 三个策略对象 `FileNaming`（名字清洗）/`UploadAdmission`（扩展名黑名单，现役口径）/`StoragePath`（内容寻址与防目录穿越）+ `UploadedFileRepository`；唯一索引冲突由适配器翻译成领域事实 `ContentAlreadyStored`，Spring 异常不外泄。
- 大小上限在 `spring.servlet.multipart`（20MB/25MB）不在代码里；落盘根目录 `FileStorageProperties.base-dir`。

## sharedkernel / bootstrap（不属于任何上下文）

- `sharedkernel`：`ApiResponse`、`BusinessException`、`Sessions`、`GlobalExceptionHandler` + `persistence/BaseMapperCompat`。**禁止再往里塞业务类型**。
- `bootstrap`：`config/`（Web/WebSocket/FastJson/MyBatis-Plus 装配、`LoginInterceptor`、`RequestLogFilter`+`RequestLogConfig`）与 `properties/`（5 个 `@ConfigurationProperties`）。业务上下文只能 import `bootstrap.properties.*`，import `bootstrap.config.*` 即违规。

## 去哪找一类东西

`SmartChatApplication` 留在根包，`@MapperScan(basePackages = "com.smart.chat")` 与 `@ConfigurationPropertiesScan` 扫描面未变。
新增功能一律照 `docs/ddd/05-tactical-playbook.md` 第二节的五种模板落层：先决定表与不变式，再建端口与适配器，最后写编排；
**不要退回「Service 直接注入 Mapper」的旧写法**——守卫会红。
