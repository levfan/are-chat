---
name: are-chat-map
description: are-chat 后端项目地图（Spring Boot 4 + MyBatis-Plus + Flyway + MariaDB）。提供模块地图、情侣空间分层模板、数据层规范与交付门禁（第六节）。凡在 are-chat 中开发新功能、修复缺陷、评审改动，开工前必须先加载本 skill，避免重新通读项目。
whenToUse: are-chat 后端开工前加载；新增/删除模块、表、接口、定时任务后回来更新本文件
---

# are-chat 后端项目地图

> 本文件是给 AI agent 看的项目速查地图。维护义务见第六节「收尾」。

## 一、技术栈与运行

- Java 25 + Spring Boot 4.1.1（Web/MVC，无独立前台，会话用 `HttpSession`）
- ORM：MyBatis-Plus 3.5.17（BaseMapper 统一继承 `com.smart.chat.sharedkernel.persistence.BaseMapperCompat`）
- 数据库：生产 MariaDB 10.11 / 测试 H2 MODE=MySQL；结构由 Flyway 管理（`spring.flyway.locations=classpath:db`，禁用 spring.sql.init）
- 鉴权：登录态在 HttpSession；`com.smart.chat.sharedkernel.web.Sessions.requireUser(session)` 取当前用户名
- 统一返回：`ApiResponse.ok(data)` / 业务异常 `BusinessException(code, message)`
- 构建：`mvn -q compile`；测试 `mvn test`（何时必跑见第六节）
- 规模快照（2026-10-05 v8 第一批后）：couple 包 108 个文件 / 17 个情侣 Controller / 70 个情侣映射 / 22 张 `couple_*` 表（迁移链到 V52）/ 全仓 `mvn -o test` **570 用例**基线（2026-10-05 在 commit `4ef60f3` 隔离 worktree 实测 `Tests run: 570, Failures: 0, Errors: 0, Skipped: 0` + BUILD SUCCESS；DDD 战术收口时是 563，其后 +4 来自加好友联想批量化、+2 来自它的真库差分测试、+1 来自接口日志用例）/ 44 个情侣 WS 事件 / 4 条定时任务。**注意**：本行只描述情侣空间，非情侣模块（auth/im/room/upload/system）的规模未变；逐端点与逐表清单见 `wiki/api.md`、`wiki/database.md`，裁剪决策见 `docs/couple-trim-ranking.md`，v8 新增功能的需求与取舍见 `docs/adr/0007-couple-v8-streak-question-wish.md`

### 目录与关键文件

```
are-chat/                             # 单模块 Maven（无多 module），坐标见 pom.xml
├── pom.xml                           # Java 25 / Spring Boot 4.1.1 / MyBatis-Plus 3.5.17 / Flyway
├── src/main/java/com/smart/chat/     # 11 个包，见第二节
│   ├── SmartChatApplication.java     # 主启动类
│   └── ...
├── src/main/resources/
│   ├── application.yml               # 端口 8080、数据源、Flyway、上传 20MB 限制（改配置先读这里的注释）
│   ├── application-mysql.yml         # MySQL 变体数据源
│   ├── db/V*.sql                     # Flyway 增量脚本 = 运行时唯一建表路径（当前链至 V52）
│   └── schema.sql                    # 全量结构文档（当前 35 表：22 couple_* + 13 非情侣基线；不被运行时执行，改表必须同步）
├── src/test/java/                    # Service 单测（Mockito）+ 少量 SpringBootTest 集成（H2 跑 Flyway）
├── src/test/resources/application.yml# 测试库 H2 MODE=MySQL
├── wiki/                             # Repo wiki：Home/architecture/modules/couple-space/database/api/scheduled-jobs/dev-guide
├── docs/                             # couple-features-v1~v6.md 功能规格 + acceptance-v5/v6.md 验收留痕
├── deploy/  Dockerfile  are-chat-1.0.0.tar   # 私有化部署产物与脚本
├── uploads/                          # 本地文件存储根（FileStorageProperties.base-dir=./uploads，随 cwd）
└── AGENTS.md                         # 仓库级 agent 约束（优先级高于个人记忆，如绿后必推）
```

### 端口与联调速查

| 项 | 值/说明 |
|---|---|
| 后端 HTTP | `server.port=8080`；REST 统一前缀 `/api`；健康检查 `/api/health`；HTTPS 由 nginx 终结，`forward-headers-strategy=framework` 使 Cookie 自动带 Secure |
| WebSocket 聊天室 | `@ServerEndpoint("/ws/chat/{name}")`（room/ChatEndpoint；@ServerEndpoint 实例由容器创建，经 ChatWebSocketBridge 桥接静态 Spring 引用） |
| 前端 dev | 5173；vite 代理 `/api`→8080、`/ws`→8080（ws:true） |
| 开发库（默认） | MariaDB `117.72.73.149:3307/smart_collections`；`DB_CONNECT_URL/DB_CONNECT_USER/DB_CONNECT_PASSWORD/DB_CONNECT_DRIVER` 环境变量可整体覆盖（真实口令不落仓库文档） |
| 测试库 | H2 内存 `MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE`，与生产跑同一批 V 脚本 |
| 登录态 | HttpSession Cookie；后端 `Sessions.requireUser(session)`，除 `/api/auth/**`、`/api/health` 外全部需会话 |
| Flyway | `baseline-on-migrate` + `baseline-version=0`：存量老库自动打 0 基线后从 V1 全量执行——所以每个 V 脚本都必须幂等；`clean-disabled=true` |

### 请求与推送链路（一图）

```
Vue 组件 → api/<域>Api → http.ts(get/postJson/putJson/delete，withCredentials)
  → Filter RequestLogFilter(/api/*，进入行 → [BEGIN] 打方法/路径/user/query/入参原文)
  → LoginInterceptor(会话校验) → Controller(/api/**) → Sessions.requireUser → Service(requireSpace / partnerOf)
  → Repository 端口（domain 声明）→ *RepositoryAdapter → Mapper(BaseMapperCompat default 方法 + LambdaQueryWrapper) → MariaDB(生产)/H2(测试)
     ★ Service 与 Controller 都不许 import 本上下文的 *PO/*Mapper，守卫第 5 条会红
  ← Filter 完成行 `✓[SUCCESS] / ⚠[CLIENT_ERR] / ✗[SERVER_ERR]` + 状态/耗时/body=/resp=，一行读全一次请求（口径与排除项见 docs/adr/0006）
Service → ImPushService.pushCoupleEvent(Both) ─┬→ WS 帧 {type:'couple', event, detail}
                                                └→ couple_notify 落库（CoupleNotifyRecorder，F41 通知中心）
前端 im store 收 WS → 派发 `arechat:couple` 自定义事件 → couple store handleCoupleEvent 按 event 刷新 + 通知铃铛
```

### 命令速查

- 编译门禁：`mvn -q compile`（提交前必跑）
- 全量测试：`mvn test`（**当前基线 570 用例**，2026-10-05 在 commit `4ef60f3` 实测 `Tests run: 570, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`；旧的 158/202/267/381/409/563/568 几个写法都是过时快照）；单类：`mvn test -Dtest=CoupleStreakServiceTest`
- 运行：`mvn spring-boot:run`（8080）；打包 `mvn -q -B package` 后按 Dockerfile/deploy 部署
- 数表：`grep -c "^CREATE TABLE" src/main/resources/schema.sql`

## 二、包结构（com.smart.chat，2026-10-04 DDD 改造后）

按**限界上下文**分包，每个上下文内部四层（api / application / domain / infrastructure）。判据与守卫见 `docs/ddd/` 与 `src/test/java/com/smart/chat/ArchitectureGuardTest`（后者属于 `mvn test`，违规直接红）。

| 上下文 | 域分类 | 由旧包合成 | 现状 |
|---|---|---|---|
| `couple` | **核心域** | couple | api(17 Controller) / application(16 Service) / **domain 52 文件**：22 张表逐张有聚合或薄实体 + 22 个 `*Repository` 端口（space/intimacy/bond/mood/comfort/safeword/dine/chore/quest/coupon/wish/streak/question/deed/surprise/pin/invite/anniversary/points/notify/memory）/ infrastructure(persistence 22 PO+22 Mapper+22 适配器、content 9 库、scheduler 4 Job、notify、account)。**战术改造已收口**（2026-10-05）：Service 一律经端口取数，业务判定与话术在领域 |
| `messaging` | 支撑域 | im + room（合并后那条 im/room 互依赖自然消失） | domain 24 文件：7 个发布语言端口（CoupleEventPublisher / PresenceReader / AnnouncementBroadcaster / PeerProfileReader / FriendshipChecker / OutboundNotifySink / NotifySinkRegistry）+ 7 张表的聚合与端口（`friend.Friend`/`FriendRequest`/`FriendshipGate`、`conversation.PrivateMessage`、`pin.Conversation`+`ConversationPin`、`profile.UserProfile`、`reaction.MessageReaction`、`star.MessageStar`），实体一律 `*PO`，transport 归 infrastructure。**已收口** |
| `identity` | 通用域 | auth | **零出向上下文依赖**的纯上游；**战术改造已收口**（2026-10-05）：`domain` 有 `account.Account` 聚合（格式规则在无状态策略 `account.AccountRules`：手机号/用户名/昵称/密码，文案原话照搬）、`registration.RegistrationApplication` 状态机（PENDING→APPROVED/REJECTED 单向，重复处理 409「该申请已处理过（X）」，拒绝原因按 varchar(200) 截断）、`audit.AdminAudit` 薄流水实体、`verification.SmsCode` 验证码规则（重发 60s / 5 分钟 / 试错 5 次）、`RuleViolation`（带对外状态码）；仓储端口 `AccountRepository`/`RegistrationApplicationRepository`/`AdminAuditRepository` 各配一个 `*RepositoryAdapter`（更新只回写聚合纳管的列，app_user 的 `signature`/`presence_status` 由 user_profile 那边负责）；`application` 只剩编排 + `DomainRules` 翻译器，**不再 import 本上下文 persistence**；实体是 `AppUserPO`/`RegistrationApplicationPO`/`AdminAuditPO`，表名列名未动。另有 AccountDirectory（别人问账号只用它，含 Account 最小视图）与 AccountCascade / ProfileProvisioner / WelcomeMessenger / AdminAlerter / AdminNotifyChannel 五个「我需要别人配合」的端口 |
| `platform` | 通用域 | system + notify + tools | 公告管理端点已从 identity 归位到 `AnnouncementAdminController`，路由 `/api/admin/announcements*` 一字未改。**已收口**（2026-10-05）：`domain/announcement/Announcement` 发布/关闭单向状态机 + 同一时刻只一条生效、`AnnouncementRead` 薄流水、`domain/notify` 渠道配置端口，2 个端口 2 个适配器，实体 `AnnouncementPO`/`AnnouncementReadPO` |
| `filestorage` | 通用域 | upload | **已收口**（2026-10-05）：`domain/file/` 有 `UploadedFile` 实体 + `FileNaming`（名字清洗）/`UploadAdmission`（扩展名黑名单，现役口径）/`StoragePath`（内容寻址与防目录穿越）三个策略对象 + `UploadedFileRepository` 端口；`DuplicateKeyException` 由适配器翻成领域事实 `ContentAlreadyStored`，Spring 异常不外泄；实体 `UploadedFilePO` |
| `sharedkernel` | 共享内核 | common + `BaseMapperCompat` | 只放 ApiResponse / BusinessException / Sessions / GlobalExceptionHandler + ORM 基类；**禁止再往里塞业务类型** |
| `bootstrap` | 装配层 | config | WebConfig / WebSocketConfig / FastJsonWebConfig / MybatisPlusConfig / LoginInterceptor + 接口访问日志三件套（RequestLogFilter / CachedBodyRequest / LoggedResponse，注册在 RequestLogConfig，开关见 RequestLogProperties）+ 6 个 `*Properties`；业务只许 import `bootstrap.properties.*`，import 装配类即违规。取舍理由 `docs/adr/0006-request-log-filter.md` |

`SmartChatApplication` 留在根包，`@MapperScan(basePackages = "com.smart.chat")` 与 `@ConfigurationPropertiesScan` 的扫描面没变，所以搬包不影响运行时装配。**新功能照 `docs/ddd/02-layering.md` 的归属判据落层，不要退回旧的平铺写法。**

推送机制（重要）：`ImPushService.pushCoupleEvent(event, actor, toUser, detail)` 给单人推 WS 事件（type=couple），`pushCoupleEventBoth(...)` 推双方；每次情侣事件推送同时落库 `couple_notify`（F41 通知中心，`CoupleNotifyRecorder` 启动时经 `ImPushService.setNotifySink` 挂接，im 包不反向依赖 couple 包）；`isOnline(username)` 查在线。

## 三、情侣空间模块全景（couple 包）

**现役口径（2026-10-04 裁剪留 10 张卡 + 2026-10-05 v8 加 4 张 = 14 张卡）**：108 个 java 文件 / 17 个 Controller / 70 个映射 / 22 张 couple_* 表 / 44 个 WS 事件。
排序与去留的唯一依据是 `docs/couple-trim-ranking.md`（174 张卡五维打分 → 留 10），下面是落地后的实际分层。

数据约定：所有表主键为 36 位 UUID 字符串；时间统一毫秒 bigint（`created`/`updated_at`）；用户名列 `utf8mb4_bin` 区分大小写。CoupleSpace 双方固定 `userA`/`userB`（字典序小者为 A），`partnerOf(me)` 取对方。

分层模式（新功能照抄，判据见 `docs/ddd/05-tactical-playbook.md`）：表映射 `XxxPO`（`@Data @TableName` + `@TableId(IdType.INPUT)`，只有映射不加业务方法）→ Mapper `extends BaseMapperCompat<XxxPO>`，常用查询写 default 方法 → `domain/<集合>/Xxx` 领域类型（私有构造 + `restore()` 不校验 + 带语义的工厂校验并抛 `RuleViolation`，访问器用记录式短名 `id()`）→ `domain/<集合>/XxxRepository` 端口（参数与返回值只允许领域类型与 JDK 类型）→ `infrastructure/persistence/XxxRepositoryAdapter`（PO↔领域翻译只在这里；更新**只回写聚合纳管的列**）→ Service 注入**端口不是 Mapper**、VO 用嵌套 `record`、`requireSpace(me)` 经 `CoupleSpaceRepository` 取聚合、领域异常经 `application/DomainRules.rule|guard` 翻译成 400/403/404/409 → Controller `@RequestMapping("/api/couple/...")`、请求体用 record、每方法一句 javadoc。

### 3.1 现役 14 张卡与它们的落点

| 卡（前端 data-testid） | Controller / 前缀 | Service | 表 |
|---|---|---|---|
| `couple-mood` 心情日记 | CoupleController `/api/couple` | CoupleService | couple_mood, couple_mood_reaction |
| `couple-bond` 贴贴宫格 | CoupleBondController `/bond` | CoupleBondService | couple_action |
| `couple-comfort` 求抱抱 | CoupleCareController `/care` | CoupleComfortService | couple_comfort |
| `couple-catch-safeword` 安全词与暂停复盘 | CoupleCatchController `/catch` | CoupleCatchService | couple_catch_safeword, couple_catch_safeword_use |
| `couple-dine-today` 今晚饭桌 | CoupleDiningController `/dining` | CoupleDiningService | couple_dine_ticket |
| `couple-fy-spin` 家务轮盘 | CoupleFactoryController `/factory` | CoupleFactoryService | couple_spin_task |
| `couple-quest-overtime` 加班预报与留灯 | CoupleQuestController `/quest` | CoupleQuestService | couple_quest_overtime |
| `couple-echo-deed` 好事簿 | CoupleEchoController `/echo` | CoupleEchoService | couple_echo_deed |
| `couple-cere-coupon` 愿望券本 | CoupleCeremonyController `/ceremony` | CoupleCeremonyService | couple_ceremony_coupon |
| `couple-surprise` 刮刮乐与盲盒 | CoupleSurpriseController `/surprise` | CoupleSurpriseService | couple_scratch, couple_mystery_box |
| `couple-streak` 连续互动打卡（v8） | CoupleStreakController `/streak` | CoupleStreakService | couple_bond_day（连续与七档解锁**读时算**，见 `domain/streak`） |
| `couple-question` 每日一问（v8） | CoupleQuestionController `/question` | CoupleQuestionService | couple_question_answer |
| `couple-wish` 愿望清单（v8） | CoupleWishController `/wish` | CoupleWishService | couple_wish |
| `couple-memory` 百日回顾（v8，隐藏页） | CoupleMemoryController `/memory` | CoupleMemoryService | 复用 couple_bond_day / couple_question_answer / couple_wish |

地基（不是一张卡，别删）：`CoupleController` 的邀请建立/纪念日/空间个性化/心动值/relationship-of，`CoupleNotifyController` `/notify`（F41 通知中心），`CouplePinController` `/pin`（F207 常用收藏），`CoupleAdminController` `/admin/stats`（F45 看板，统计项只吃 couple_action 与 couple_echo_deed）。

### 3.2 心跳与积分（口径已随裁剪改写）

- **心动值** `CoupleService.intimacy()`：**读时算无表**，六项全部来自保留卡——
  心情条数×1 + 贴贴双向往来天数×2 + 好事簿条数×2 + 留灯次数×3 + 安全词复盘次数×2 + 台账累计 EARN×1；
  7 级阶梯阈值未重标定（0/50/150/300/500/800/1300）。回归保护见 `CoupleIntimacyTest`。
- **积分台账** `couple_point_ledger` 保留，三个 EARN 入口**两个 SPEND 出口**：
  `CoupleEchoService.earn`（好事簿 +2 给被记的那位、加星 +1）、`CoupleFactoryService`（轮盘干完 +3、周全清双方各 +2）、
  `CoupleSurpriseService`（刮刮乐由**送券人**核销 +5）、`CoupleCeremonyService.issueCoupon`（发券扣发券人 COUPON_COST=10，余额不足 400）、
  `CoupleStreakService.makeup`（v8 补签扣 20 分，item 记 `补签 yyyy-MM-dd`，余额不足 400 并点名去好事簿）。
  券的产出不再依赖已删除的爱情保险柜，改成纯积分购买。
- **每日一问不进心动值公式**（v8）：六项权重与七级阈值是 `CoupleIntimacyTest` 锁死的口径，
  为一新功能重标定整套阶梯不划算，取舍见 `docs/adr/0007` 第 10 条。

### 3.3 内容库与定时任务

- 静态内容库 9 个：`CoupleRitualBank`（`stableHash` 是全空间按天/按空间稳定取值的唯一入口，饭桌裁决、惊喜券面与 **v8 每日一问选题**都吃它）、
  `CoupleTalkBank`（求抱抱话术卡/陪聊话题/深夜陪伴文案）、`CoupleCatchBank` `CoupleEchoBank` `CoupleFactoryBank` `CoupleQuestBank` `CoupleSurpriseBank` `CoupleTermBank`（CoupleService 的农历生日换算仍用）、
  `CoupleQuestionBank`（v8 每日一问 74 题；题号会落进答案表，所以**只增不改顺序**）。
- 定时任务 4 个：`CoupleQuestionJob` 09:00 每日一问（v8 新增，已答完的空间不打扰）；`CoupleSurpriseJob` 09:20 生日贺卡 + 前 3 天预告；`CoupleReminderJob` 09:30 纪念日倒数；`CoupleCareTalkJob` 23:00 深夜陪伴。
  原 09:00 约定逾期、09:45 倒数日、10:00 情绪急救箱、09:15 告白重现、10:15 花园缺水、21:00 情话利息随功能一并删除。

## 四、数据层规范（硬性）

- 凡改表结构或初始化数据，必须产出 Flyway 增量脚本并同步 schema.sql——触发条件、命名、幂等/双兼容写法、种子数据等完整规范见 `.agents/skills/db-migration/SKILL.md`
- 现有迁移：V1 couple 基础表 → … → V49 私聊消息与好友申请补二级索引 → **V50 系统裁剪第一批（drop 193 张已下线功能的表）** → **V51 裁剪第十六/十七轮（再 drop 85 张，情侣空间只留 10 张卡的 19 张表）** → **V52 v8 第一批（新建 `couple_bond_day` / `couple_question_answer` / `couple_wish` 三张，现役 22 张）**。V1-V49 建过的 297 张 `couple_*` 表里，278 张已被 V50+V51 drop，19 张留存 + V52 新建 3 张 = 现役全部情侣表。**已入库脚本内容一律不得再修改**：裁剪时没有重跑 V50 而是新增 V51，v8 也没有回头改 V51 而是新增 V52（改已入库脚本会让那些库在 Flyway 校验和上直接失败）
- V52 的索引名统一 `uk_couple8_/idx_couple8_` 前缀（H2 索引名全库唯一，写之前先对全部 `db/V*.sql` 查重）。V52 **没有**给 `couple_space` 加「一用户一有效空间」的唯一约束：可空 `active_flag` + UK 在存量脏数据（同用户多行 ACTIVE）下会让迁移在启动期失败，这个失败模式在私有化部署不可接受，残留风险登记在 `docs/adr/0007` 第 12 条
- 索引/唯一键名是**全库唯一**（H2 索引不随表隔离，重名报 42S11「Index already exists」）：新增 `uk_*/idx_*` 前先用脚本对全部 `db/V*.sql` 查重，模块前缀（如 `uk_body_*`）是最省事的办法
- **`(from_user=? AND to_user=?) OR (to_user=? AND from_user=?)` 这种双向会话谓词吃不到索引**（硬性口径）：OR 两侧是不同的索引前缀，优化器只能全表扫，V49 加了 `idx_pm_from_to_created` / `idx_pm_to_from_created` 之后实测仍是 ~200ms/次。要按对端取「最后一条」「未读数」一律走 `PrivateMessageMapper.findLatestCreatedPerPeer`（两趟各方向 GROUP BY）与 `FriendMapper.selectUnreadCountsByPeer`（一趟 friend JOIN 聚合），别再回到 per-peer 循环。**同一族还有第二个案发现场**：`/api/friends/suggest` 曾在候选循环里对每个候选跑三条查询（一条边 + 两个方向的待处理单，10 个候选 = 31 趟），2026-10-05 改成按「我」三趟取全（`findAllByOwner` / `listOutgoing` / `listIncoming`）再在内存比对——凡是「列表 × 每条判关系/取最新」的接口都按这个手法写，并补一条 `verifyNoMoreInteractions` 用例锁住（现役两条：`listFriendsNeverFallsBackToPerPeerQueries`、`suggestNeverFallsBackToPerCandidateQueries`），改了取数口径还要配一条**真库差分测试**（`FriendSuggestRelationQueryTest`：同一片 H2 数据上把新旧两套算法逐候选比对，单向好友边、非 PENDING 旧申请、双向同时挂单这三种情形只有真表能照出来）。
  另外：**手写 `@Select`/聚合 SQL 必须有真库测试**（`@SpringBootTest` 打 H2+Flyway），mock 单测只会把 stub 改成新签名然后一路绿灯，SQL 语法与语义错误只有打到真表才暴露。

- **`Integer`/`Long` 位字段禁止配 `isXxx()` 布尔 helper**（硬性）：Lombok 已给字段生成 `getXxx()`，再手写 `isXxx()` 就是同一属性两个不同类型的 getter，MyBatis `Reflector` 会按方法枚举顺序**随机**抛 `ReflectionException: ambiguous type for property`——实测让 `GET /api/couple/world/world` 与 `GET /api/couple/legacy/vault` 整页 500（两家与朋友、传世系统两批 20 个功能对每个用户都不可用），而单测全用 mock 完全照不出来。位判断一律命名 `xxxFlag()`（`laughAFlag()`/`doneFlag()`…）；只有真 `private boolean` 字段才允许 `isXxx()`。回归保护见 `src/test/java/com/smart/chat/EntityReflectionGuardTest`——**必须逐字段 `reflector.getGetInvoker(name).invoke(instance)` 真读一遍属性**：歧义 getter 被 MyBatis 包成 `AmbiguousMethodInvoker`，只实例化实体（旧写法）从不触发取值，守卫会恒绿成假守卫；新增实体后跑它，并确认它真的能变红
- **日期进 CSV 列一律用 `MMdd` 或周几 1-7，不要写 ISO**：`couple_theater_master_day.serves(40)` 存 ISO 三天就满（一周要记七天）、`couple_body_quit.broke_days(160)` 存 ISO 只够 14 条。已统一为周几（师徒侍奉）/`MMdd`（破戒，超 25 条 400）/`MMdd:A|B`（重建双签）；新批次写 CSV 前先按「条数上限 × 记号长度」对一遍列宽
- H2 兼容注意：`CHARACTER SET utf8mb4 COLLATE utf8mb4_bin` 列必须 `NOT NULL`，可空用户名列用普通 `varchar(50) DEFAULT NULL`（V17/V22 踩过坑）

## 五、测试

- 测试在 `src/test/java`（H2 自动配置），已有 auth/im/room/upload 各模块测试；新 Service 的核心算法（判定/统计/轮换）建议补单测
- 测试资源：`src/test/resources/application.yml`

## 六、交付门禁（硬性流程，给 agent 的快速上手路径）

规范全集在各专项 skill 里（db-migration / lombok-data / git-commit），本节只做流程串联与红线登记，不复述细节：

1. **开工**：必读本 skill；任务触及表结构/初始化数据 → db-migration；新建 Java 数据类 → lombok-data；提交推送 → git-commit
2. **编码**：照抄第三节分层模板（加"XX卡"类功能可参考 CoupleLetter/CoupleCapsule 全链路）；静态内容库只增不改顺序（第四节）；用户可见文案要可爱、口语化、带 emoji（模仿现有推送文案）；新增 WS 事件须同步前端 `stores/couple.ts` 注册 case + api/types（见 are-chat-web-map skill）
3. **自检**：git-commit skill 的「架构师 Code Review 五项」清单全过
4. **构建**：提交前必跑 `mvn -q compile`；改了表跑 `mvn test` 验证 Flyway 脚本 H2 可执行；涉测试改动 `mvn test` 全绿
5. **提交**：按改动性质分组，一类一 commit；数据库脚本（V*.sql+schema.sql）永远独立成 commit；信息 `type(scope): 中文描述`
6. **推送**：commit → `git pull --no-rebase` → push；失败保留本地 commit 并报告，不 force push
7. **收尾**：新增/删除模块、表、接口、定时任务 → 更新本 skill 对应小节，与功能同批提交（commit type `docs`）

**产品红线（用户长期约束，各批次均适用）**：情侣空间功能注重情绪价值；**不做照片/视频上传类功能**（服务器部署要求高）；迁移脚本必须幂等且 H2/MariaDB 双兼容（见第四节）。
