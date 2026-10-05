# 架构总览

> 本页回答：技术栈与版本、分层模式、鉴权链路、统一返回与异常、WS 推送机制、Flyway 迁移策略、配置要点。

## 技术栈与版本（以 `pom.xml` 为准）

| 组件 | 版本 | 备注 |
|---|---|---|
| Java | 25（`<java.version>25`） | 注意：`are-chat-map` skill 首节仍写 Java 21，已过时 |
| Spring Boot | 4.1.1（starter parent） | 无独立前台页面，纯 REST + WebSocket |
| MyBatis-Plus | 3.5.17 | starter 面向 Boot 3，Boot 4 下**排除其自动配置**，由 `bootstrap/config/MybatisPlusConfig` 手工装配 |
| fastjson2 | 2.0.65 | JSON 序列化，经 `bootstrap/config/FastJsonWebConfig` 接管 MVC；JDK 25 上有 Unsafe 弃用告警（构建插件已压制） |
| 数据库 | 生产 MariaDB 10.11 / 测试 H2（MODE=MySQL） | 表结构统一由 Flyway 管理 |
| Flyway | Boot 4 需显式引 `spring-boot-flyway` 模块 | 否则加了依赖也不会执行迁移 |
| WebSocket | Jakarta `@ServerEndpoint`（JSR-356） | `WebSocketConfig` 提供 `ServerEndpointExporter` |
| Lombok | 配合 `maven.compiler.proc=full` | JDK 23+ 默认关闭隐式注解处理，必须显式开启 |

其他：`spring-boot-starter-validation`、`spring-boot-starter-actuator`（暴露 `health,info,h2dump`）。

## 分层模式（DDD：api → application → domain ← infrastructure）

按**限界上下文**分包（`couple` / `messaging` / `identity` / `platform` / `filestorage` + `sharedkernel` + `bootstrap`），
每个上下文内部四层，2026-10-05 五个上下文全部完成战术改造（判据与实测见 `docs/ddd/06-ddd-standard.md`、决策见 `docs/adr/0001-0008`）；
同日核心域 `couple` 又经历一轮功能大裁剪（`docs/adr/0010-couple-trim-to-v8-features.md`），四张功能卡（每日一问 / 连续互动打卡 / 愿望清单 / 百日隐藏页）之外的表、Controller、领域包与内容库全部下线，`couple` 从 167 个 Java 文件缩到 57 个、`couple_*` 表从 22 缩到 6。要点：

- 表映射（`infrastructure/persistence/XxxPO`）：`@Data @TableName` + `@TableId(IdType.INPUT)`（UUID 由应用层生成），**只有映射职责**，业务名让给领域类型（ADR-0002）。
- Mapper：`@Mapper interface extends com.smart.chat.sharedkernel.persistence.BaseMapperCompat<XxxPO>`（项目对 MP `BaseMapper` 的统一兼容层），常用查询写成 default 方法；**Mapper 不感知领域类型**。
- 领域（`domain/<集合>/`）：`Xxx` 聚合或薄实体（私有构造 + `restore()` 不校验 + 带语义的工厂校验并抛 `RuleViolation`，访问器用记录式短名）、`XxxRepository` **端口**、无状态规则做策略对象（`IntimacyCalculator`、`MakeupPolicy`、`AccountRules`）。domain 不 import Spring/MyBatis，也不带容器与 ORM 注解。
- 适配器（`infrastructure/persistence/XxxRepositoryAdapter`）：PO↔领域双向翻译**只在这里**；更新只回写聚合纳管的列，聚合没建模的列保持原值。
- Service（`application/`）：构造注入**端口而非 Mapper**，只做「取会话身份 → 组聚合 → 调领域方法 → 落库 → 推事件 → 投 VO」；VO 用嵌套 `record` 留在 application（ADR-0008 第 5 条）；领域异常经 `application/DomainRules.rule|guard` 翻成 `BusinessException(400/403/404/409)`，**文案由领域说了算**。
- Controller（`api/`）：`@RestController @RequestMapping("/api/...")`，请求体用 record，每方法一句 javadoc；不 import 本上下文的 PO/Mapper。

四条越界一律被 `src/test/java/com/smart/chat/ArchitectureGuardTest` 拦成构建失败（含「账本必须等于实测违规集合」那条，防止债务挂账过期）。
照抄模板与命名细则属于规范，见 `.agents/skills/are-chat-map/SKILL.md` 第三节与 `docs/ddd/05-tactical-playbook.md`；包级地图见 [modules.md](modules.md)。

## 鉴权链路（HttpSession）

1. `bootstrap/config/WebConfig` 注册 `bootstrap/config/LoginInterceptor`：拦截 `/api/**`，放行 `/api/auth/**` 与 `/api/health`（登录页状态点用）；未登录直接返回 401 + `ApiResponse.error(401, 文案)`。CORS 全放开、允许凭证（前端带 session cookie）。
2. 登录态存 session 属性 `CurrentUser`（常量 `LoginInterceptor.SESSION_USER`）；业务代码统一经 `sharedkernel/web/Sessions.requireUser(session)` 取当前用户名，取不到抛 `BusinessException(401)`。
3. 注册为**审批制**：`/api/auth/register` 只提交申请，管理员 approve 后才能登录；首个管理员由 `identity/infrastructure/boot/AdminBootstrapper` 在库中无 ADMIN 时按 `arechat.admin.*` 引导创建。
4. `bootstrap/properties/ImProperties`、`ModerationProperties` 等驱动敏感词过滤（censor/block）与每分钟发送限流；`identity/infrastructure/throttle/LoginRateLimiter` 防爆破。
5. HTTPS 部署形态：nginx 终止 TLS 后 HTTP 反代，`server.forward-headers-strategy=framework` 让后端识别 https，session cookie 自动带 Secure。

## 统一返回与异常

- 所有接口返回 `ApiResponse<T>(code, message, data)`，`code=0` 成功；失败用 `ApiResponse.error(code, message)`。
- 业务异常统一抛 `sharedkernel/web/BusinessException(code, message)`——**code 同时作为 HTTP 状态码返回**（默认 400），message 是口语化整活文案。
- `sharedkernel/web/GlobalExceptionHandler` 兜底：BusinessException → 对应状态码 + ApiResponse；其他异常 → 500。

## WS 推送机制

- 端点：`messaging/infrastructure/transport/ChatEndpoint` `@ServerEndpoint("/ws/chat/{name}")`（昵称 URL 编码）；`messaging/infrastructure/transport/ChatSessionRegistry` 维护 username→session 在线表；`messaging/infrastructure/transport/ChatWebSocketBridge` 做 Spring Bean 与端点间的桥接。
- 推送门面：`messaging/infrastructure/transport/ImPushService` —— `push(username, payload)` / `pushAll` / `pushToUsers` / `isOnline(username)`；payload 均为带 `type` 字段的 record（dm/typing/recall/friend/reaction/edit/read/pin/announcement/admin 等）。
- **情侣事件**：`pushCoupleEvent(event, actor, toUser, detail)` 推单人、`pushCoupleEventBoth(...)` 推双方，payload `type=couple`；前端（are-chat-web）把 `arechat:couple` 自定义事件按 event 名分发到 `stores/couple.ts` 刷新对应面板——**新增事件名必须前后端同步注册**。
- **落库通知中心（F41）**：每次情侣推送同时向 `couple_notify` 表落一条，离线用户上线后可补看。挂接方式：`couple/CoupleNotifyRecorder` 启动时经 `ImPushService.setNotifySink` 注册，保持 im 包不反向依赖 couple 包。
- 情侣空间事件名现役 **15 个**（实测 `grep -rhoE 'pushCoupleEvent(Both)?\("[a-z-]+"' src/main/java | sort -u`），按前缀族分四组（口径见 `CONTEXT.md`「事件命名口径」）：建立与地基 `invite`/`invite-accepted`/`invite-rejected`/`dissolved`/`anniversary-updated`/`anniversary-reminder`/`space-themed`/`pet-name-changed`（爱称的写入口在 `PUT /profile`，推送按改了什么分流，见 ADR-0010 第 12 条）；连续互动打卡 `streak-checkin`/`streak-unlocked`/`streak-makeup`；每日一问 `question-daily`/`question-answered`；愿望清单 `wish-added`/`wish-fulfilled`。命名规则为小写连字符、动词/过去分词结尾。**刻意不存在** `wish-prepared`/`wish-unprepared`（偷偷标记不推事件是产品规则）。原 `bond-*`/`mood-*`/`comfort-*`/`catch-*`/`dine-*`/`factory-*`/`quest-*`/`ceremony-*`/`echo-*`/`scratch-*`/`box-*` 等事件族已随功能卡于 2026-10-05 二轮裁剪（ADR-0010）下线，从 44 种收缩到 15 种。全列见 are-chat-web/wiki/ws-events.md。

## Flyway 迁移策略

- `spring.flyway.locations=classpath:db`，脚本命名 `V{n}__{描述}.sql`，当前 V1→V53（V50/V51 是 2026-10-04 裁剪的下线脚本，V52 是 2026-10-05 v8 第一批的三张新表，V53 是 2026-10-05 二轮裁剪 `drop` 16 张 + `couple_bond_day` 改名重建为 `couple_streak_day` + 删 `couple_space.stickers` 列；见 [database.md](database.md)）。
- `baseline-on-migrate=true` + `baseline-version=0`：存量老库（有表无 flyway 历史）首次启动自动打 0 基线后**从 V1 全量重放**——推论：**每个脚本必须幂等**（`CREATE TABLE IF NOT EXISTS` / `ADD COLUMN ... IF NOT EXISTS`）。
- `spring.sql.init.mode=never`：`src/main/resources/schema.sql` 已退役为**全量结构文档**，不再被执行；改表时必须"新增 V 脚本 + 同步 schema.sql"双写。
- 已入库脚本不可再修改；新版本号 = db 目录最大版本 + 1。完整规范见 `.agents/skills/db-migration/SKILL.md`。
- H2 兼容坑：`CHARACTER SET utf8mb4 COLLATE utf8mb4_bin` 列必须 NOT NULL；可空用户名列用普通 `varchar(50) DEFAULT NULL`（V17/V22 踩过坑）。

## 配置要点（`src/main/resources/application.yml`）

| 键 | 值 / 说明 |
|---|---|
| `server.port` | 8080 |
| `spring.datasource.*` | 默认外部 MariaDB；环境变量 `DB_CONNECT_URL/USER/PASSWORD/DRIVER` 整体覆盖；注释内保留 H2 内存库备用方案 |
| `spring.flyway.*` | 见上节 |
| `spring.servlet.multipart` | 单文件 20MB / 单请求 25MB |
| `spring.autoconfigure.exclude` | 排除 MP 自动配置（Boot 4 手工装配） |
| `arechat.storage.base-dir` | 上传文件本地目录，默认 `./uploads`（`bootstrap/properties/FileStorageProperties`） |
| `arechat.admin.*` | 管理员引导账号（`ARECHAT_ADMIN_USERNAME/PASSWORD` 覆盖默认值） |
| `arechat.notify.*` | 注册审批外部通知四通道（可同时启用）：企业微信 webhook / WxPusher / Server酱 / 虾推啥，全免费渠道；都不配则仅站内待办 |
| `arechat.moderation.*` | 敏感词：`enabled` / `mode=censor`（替换为＊）或 `block` / 词库 |
| `arechat.im.send-limit-per-minute` | 防刷屏限流，默认 30 |
| `management.endpoints` | 暴露 `health,info,h2dump`（`platform/infrastructure/devtools/H2DumpEndpoint` 仅对 H2 内存库有意义） |
