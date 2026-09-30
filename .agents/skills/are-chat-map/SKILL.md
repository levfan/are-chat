---
name: are-chat-map
description: are-chat 后端项目地图（Spring Boot 4 + MyBatis-Plus + Flyway + MariaDB）。凡在 are-chat 中开发新功能、修复缺陷、评审改动，开工前必须先加载本 skill——它提供模块地图、代码惯例、数据层规范与构建命令，避免重新通读项目。完成功能后若新增模块/表/接口，须同步更新本文件。
whenToUse: 在 are-chat 后端开发新功能、修复缺陷或评审改动，开工前加载本 skill；功能完成后若新增/删除模块、表、接口、定时任务，也要回来更新它
---

# are-chat 后端项目地图

> 本文件是给 AI agent 看的项目速查地图。**每次改完代码，若模块/表/接口有增删，必须同步更新本 skill**（与功能同批提交，commit type 用 `docs`）。

## 一、技术栈与运行

- Java 21 + Spring Boot 4（Web/MVC，无独立前台，会话用 `HttpSession`）
- ORM：MyBatis-Plus 3.5.17（BaseMapper 统一继承 `com.smart.chat.im.BaseMapperCompat`）
- 数据库：生产 MariaDB 10.11 / 测试 H2 MODE=MySQL；结构由 Flyway 管理（`spring.flyway.locations=classpath:db`，禁用 spring.sql.init）
- 鉴权：登录态在 HttpSession；`com.smart.chat.common.Sessions.requireUser(session)` 取当前用户名
- 统一返回：`ApiResponse.ok(data)` / 业务异常 `BusinessException(code, message)`
- 构建：`mvn -q compile`（提交前必跑）；测试 `mvn test`

## 二、包结构（com.smart.chat）

| 包 | 职责 | 关键类 |
|---|---|---|
| `auth` | 注册审批/登录/管理员 | AppUserService, AdminService, AuthController |
| `im` | 好友/私信/在线状态/资料/推送 | FriendService, PrivateMessageService, ImPushService, PresenceService, UserProfile, BaseMapperCompat |
| `couple` | 情侣空间（核心业务，见下节） | CoupleService + 各功能 Service/Controller |
| `room` | 聊天室 WebSocket | ChatSessionRegistry, ChatWebSocketBridge |
| `system` | 系统配置/公告/审计 | Announcement 相关 |
| `upload` | 文件上传 | FileStorageService（本地存储） |
| `tools` | 健康检查/工具 | — |
| `notify` | 管理员推送 | AdminNotifyService |
| `common` | ApiResponse/BusinessException/Sessions | — |
| `config` | 配置类 | FileStorageProperties 等 |

推送机制（重要）：`ImPushService.pushCoupleEvent(event, actor, toUser, detail)` 给单人推 WS 事件（type=couple），`pushCoupleEventBoth(...)` 推双方；每次情侣事件推送同时落库 `couple_notify`（F41 通知中心，`CoupleNotifyRecorder` 启动时经 `ImPushService.setNotifySink` 挂接，im 包不反向依赖 couple 包）；`isOnline(username)` 查在线。前端 store 把 `arechat:couple` 自定义事件按 event 分发刷新。

## 三、情侣空间模块全景（couple 包）

数据约定：所有表主键为 36 位 UUID 字符串；时间统一毫秒 bigint（字段名 `created`/`updated_at` 等）；用户名列 `utf8mb4_bin` 区分大小写。 CoupleSpace 双方固定为 `userA`/`userB`（字典序小者为 A），`partnerOf(me)` 取对方。

分层模式（新功能照抄）：
1. 实体：`@Data @TableName` + `@TableId(IdType.INPUT)` + 静态 `of()` 工厂 + 常量（如 STATUS_*, XXX_MAX）
2. Mapper：`@Mapper interface extends BaseMapperCompat<T>`，常用查询写成 default 方法
3. Service：构造注入（final 字段 + 构造器），VO 用嵌套 `record`，`requireSpace(me)` 取有效空间（无效抛 404）
4. Controller：`@RestController @RequestMapping("/api/couple/...")`，请求体用 record，每个方法一句 javadoc
5. 前端联动：`pushCoupleEvent(Both)` 的事件名需在前端 `stores/couple.ts` 的 `handleCoupleEvent` 中注册 case

现有 Controller 与路由前缀：
- `CoupleController` `/api/couple`：总览/邀请/纪念日/解除/约定/打卡/一问(+F48 互评 reactions)/清单/纪念日历/心情/时光轴/心动值/信箱/一问历史/条约/城市/基金/个性化(profile: 宣言/主题/贴纸)/恋爱状态徽章(relationship-of)
- `CoupleBondController` `/api/couple/bond`：贴贴动作（sendAction/动作流/统计/里程碑）、心情回应、专属爱称
- `CoupleRitualController` `/api/couple/ritual`：甜蜜任务卡、默契大考验、情话抽卡、恋爱运势、晚安故事
- `CoupleCareController` `/api/couple/care`：情绪天气预报、情绪急救箱、和好卡、夸夸墙、生理期关怀
- `CoupleMemoryController` `/api/couple/memory`：徽章墙（里程碑+成就）、那年今天、时光胶囊、倒数日、恋爱月报/数据总览、第一次清单（F46）
- `CoupleLifeController` `/api/couple/life`：甜蜜记账本、家务轮值、约会规划、双人习惯、暗号小本本
- `CoupleGameController` `/api/couple/game`：恋爱加成、互动热力图、心情曲线、恋爱红绿灯
- `CoupleSurpriseController` `/api/couple/surprise`：惊喜与期待——刮刮乐（周卡懒生成/刮开/核销）、恋爱盲盒（装盒/到日开箱）、心动闹钟（24h 内定时送达）、思念速递（5~30min 随机延迟）、藏宝图任务、告白重现（每年今天重播）
- `CoupleGardenController` `/api/couple/garden`：爱情花园（浇水养成 0-6 阶段/缺水会蔫/复活）、每日玫瑰（每人 3 朵+花语）、幸运签（每天为 TA 抽一支可覆盖）
- `CoupleNotifyController` `/api/couple/notify`：空间动态通知中心（F41 列表/全部已读）
- `CoupleAdminController` `/api/couple/admin`：情侣空间运营看板（F45 仅管理员）
- `ProfileController` `/api/profile`：资料卡含生日（F42 本人填写 + friends-birthdays 好友生日列表）

内容库（静态，只增不改顺序）：
- `CoupleQuestions`：今日一问题库（105 题 11 主题，按 epochDay 轮换）
- `CoupleRitualBank`：甜蜜任务/默契题/情话/运势/晚安故事库 + `stableHash`（FNV-1a，按天+空间稳定取值）
- `CoupleSurpriseBank`：惊喜内容库（刮刮乐券面 24 种/盲盒任务灵感 16 条/花语 8 种/幸运签 20 支）+ stableHash 按周稳定抽券

定时任务 `CoupleReminderJob`（Asia/Shanghai）：09:00 约定逾期提醒；09:30 纪念日倒数（7/1/0 天）；09:45 倒数日提醒（7/3/1/0 天）；10:00 情绪急救箱（连续 2 天低落提醒对方）。
定时任务 `CoupleSurpriseJob`（Asia/Shanghai）：每分钟送达心动闹钟与思念速递（alarm-fired/miss-delivered）；09:15 告白重现；09:20 生日彩蛋（读 im 包 UserProfile.birthday）；10:15 花园缺水巡检（garden-withered）。

## 四、数据层规范（硬性）

- 凡改表结构或初始化数据，必须产出 Flyway 增量脚本并同步 schema.sql——触发条件、命名、幂等/双兼容写法、种子数据等完整规范见 `.agents/skills/db-migration/SKILL.md`
- 现有迁移：V1 couple 基础表 → V2 存量基线 → V3 心情 → V4 信箱 → V5 条约/城市/基金 → V6 贴贴动作/心情回应/爱称 → V7 任务卡/默契 → V8 和好卡/夸夸/生理期 → V9 胶囊/倒数日 → V10 记账/家务/约会/习惯/暗号 → V11 空间个性化 → V12 私信心动时刻 → V13 通知中心/生日 → V14 第一次清单/一问互评 → V15 惊喜与期待（刮刮乐/盲盒/闹钟/思念/花园/玫瑰/幸运签/告白/藏宝图）

## 五、代码惯例

- Java 数据类（Lombok）规范：见 `.agents/skills/lombok-data/SKILL.md`
- Git 提交与推送规范：见 `.agents/skills/git-commit/SKILL.md`
- 新增用户可见文案的语气：情侣场景要可爱、口语化、带 emoji（参考现有 Service 里的推送文案）

## 六、测试

- 测试在 `src/test/java`（H2 自动配置），已有 auth/im/room/upload 各模块测试；新 Service 的核心算法（判定/统计/轮换）建议补单测
- 测试资源：`src/test/resources/application.yml`

## 七、给 agent 的快速上手路径

1. 开工先读本 skill + 对应专项 skill（db-migration / lombok-data / git-commit）
2. 改后端：找同类功能照抄分层模式（如加"XX卡"→ 参考 CoupleLetter/CoupleCapsule 全链路）
3. 涉及新事件：后端 push + 前端 store case + 前端 api/types 同步（见 are-chat-web-map skill）
4. 提交前：`mvn -q compile`；改了表：跑 `mvn test` 验证 Flyway 脚本在 H2 可执行
