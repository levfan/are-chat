# 包结构导览

> 本页回答：代码现在按什么分包、每个上下文里放什么、去哪找一类东西。
> 2026-10-04 由 11 个功能包合成 5 个限界上下文（ADR-0001），2026-10-05 五个上下文全部完成战术改造
> （ADR-0008；判据与实测见 `docs/ddd/06-ddd-standard.md`，施工模板见 `docs/ddd/05-tactical-playbook.md`）；
> 同日 couple 又经历一轮**大裁剪**（`docs/adr/0010`），现役功能卡之外的功能、表、类全部下线。
> 统计口径（2026-10-20 三轮裁剪后实测）：主源文件 187 个、测试文件 61 个、`@TableName` 实体 16 个 = 仓储端口 16 个 = 适配器 16 个（18 张表里 `couple_notify` 与 `message_star` 已无代码，其余一张表一端口一适配器，无遗漏）。

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

## couple — 情侣空间（核心域，34 个文件）

> 2026-10-05 二轮裁剪（ADR-0010）与 2026-10-20 后续几批裁剪（百日回顾/心动时刻、通知中心/运营看板，以及三轮裁剪的每日一问/连续互动打卡/愿望清单/心动值/空间个性化）后只剩一张功能卡「亲密日历」+ 地基；下表为现役实测。

| 层 | 内容 |
|---|---|
| api | 4 个 Controller（14 端点）：`CoupleController`（地基：邀请建立/纪念日/`relationship-of`/解除，8）、`CouplePeriodController`（经期记录写口，3）、`CoupleIntimateLogController`（亲密记录写口，2）、`CoupleCalendarController`（亲密日历月度读模型，1）。原 10 个历史卡 Controller、`CouplePinController`、`CoupleMemoryController`、`CoupleNotifyController`、`CoupleAdminController` 与 `CoupleQuestionController`、`CoupleStreakController`、`CoupleWishController` 已下线 |
| application | 4 个 Service（`CoupleService`/`CouplePeriodService`/`CoupleIntimateLogService`/`CoupleCalendarService`——最后这个只读，跨经期与亲密两个聚合出一份月度结果，两张记录卡合并进日历后它是唯一的读口；经期与亲密那两个只落库与守闸门，返回 void 不再投影看板）+ `DomainRules`（`RuleViolation` → 400/403/404/409，文案不重写）；VO 是 Service 内嵌 `record` |
| domain | 11 文件。分包：`space`（`CoupleSpace` 聚合根：字典序定 A/B、单向解散、`bindAnniversary` 闸；`CoupleSpaceRepository` 端口）、`invite`（`Invite` 单向状态机 + 两处归属闸 + 端口）、`period`（`PeriodRecord` 实体：`covers`/`startsOn`/`endsOn` 判某天在不在经期里，没记结束日的只到今天 + `PeriodCycles` 读时算平均周期与下次预测 + `PeriodRepository` 端口）、`intimatelog`（`IntimateLog` 实体：类型 EDGE/SEX、备注 ≤60、未来日拒绝；`IntimateLogs` 读时算上次/本月分类/累计 + `IntimateLogRepository` 端口）、`RuleViolation`。`streak`/`question`/`wish`/`intimacy` 四个包随其功能于 2026-10-20 一并删除 |
| infrastructure | `persistence` 4 `*PO` + 4 Mapper + 4 `*RepositoryAdapter`（共 12 文件）；`scheduler` 1 个 Job（`CoupleReminderJob`，09:30 纪念日倒数）；`account/SpaceCascadeAdapter`（实现 identity 的 `AccountCascade` 端口，供注销级联）。`content/CoupleQuestionBank` 每日一问题库已随卡片下线 |

## messaging — 好友 / 私信 / 在线状态 / 推送门面（支撑域，59 个文件）

- **零业务出向依赖之外只依赖 `identity.domain`**；对外发布 5 个语言端口：`CoupleEventPublisher`（6 个情侣事件的唯一出口）、`PeerProfileReader`、`FriendshipChecker`、`PresenceReader`、`AnnouncementBroadcaster`。
- domain：`friend/Friend`+`FriendRequest`+`FriendshipGate`（双向边与申请状态机）、`conversation/PrivateMessage`、`pin/Conversation`+`ConversationPin`、`profile/UserProfile`、`reaction/MessageReaction`，各配端口（6 个）；消息收藏 `MessageStar` 已删，`message_star` 表保留未删。
- infrastructure：6 `*PO` + 6 Mapper + 6 适配器；`transport`（`ImPushService`/`ChatSessionRegistry`/`ChatEndpoint`/`ChatWebSocketBridge`/`ChatMessage`）、`throttle/MessageRateLimiter`、`identity/` 下 4 个实现 identity 端口的适配器。
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
