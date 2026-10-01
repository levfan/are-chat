# 架构总览

> 本页回答：技术栈与版本、分层模式、鉴权链路、统一返回与异常、WS 推送机制、Flyway 迁移策略、配置要点。

## 技术栈与版本（以 `pom.xml` 为准）

| 组件 | 版本 | 备注 |
|---|---|---|
| Java | 25（`<java.version>25`） | 注意：`are-chat-map` skill 首节仍写 Java 21，已过时 |
| Spring Boot | 4.1.1（starter parent） | 无独立前台页面，纯 REST + WebSocket |
| MyBatis-Plus | 3.5.17 | starter 面向 Boot 3，Boot 4 下**排除其自动配置**，由 `config/MybatisPlusConfig` 手工装配 |
| fastjson2 | 2.0.65 | JSON 序列化，经 `config/FastJsonWebConfig` 接管 MVC；JDK 25 上有 Unsafe 弃用告警（构建插件已压制） |
| 数据库 | 生产 MariaDB 10.11 / 测试 H2（MODE=MySQL） | 表结构统一由 Flyway 管理 |
| Flyway | Boot 4 需显式引 `spring-boot-flyway` 模块 | 否则加了依赖也不会执行迁移 |
| WebSocket | Jakarta `@ServerEndpoint`（JSR-356） | `WebSocketConfig` 提供 `ServerEndpointExporter` |
| Lombok | 配合 `maven.compiler.proc=full` | JDK 23+ 默认关闭隐式注解处理，必须显式开启 |

其他：`spring-boot-starter-validation`、`spring-boot-starter-actuator`（暴露 `health,info,h2dump`）。

## 分层模式（Controller → Service → Mapper → Entity）

每个功能域严格四层，实体/Mapper/Service/Controller 同包平铺（couple 包 329 个文件即此模式）。要点：

- 实体：`@Data @TableName` + `@TableId(IdType.INPUT)`（UUID 由应用层生成）+ 静态 `of()` 工厂 + 常量。
- Mapper：`@Mapper interface extends com.smart.chat.im.BaseMapperCompat<T>`（项目对 MP `BaseMapper` 的统一兼容层），常用查询写成 default 方法。
- Service：构造注入（final 字段 + 构造器），VO 用嵌套 `record`。
- Controller：`@RestController @RequestMapping("/api/...")`，请求体用 record，每方法一句 javadoc。

照抄模板与命名细则属于规范，见 `.agents/skills/are-chat-map/SKILL.md` 第三节；包级地图见 [modules.md](modules.md)。

## 鉴权链路（HttpSession）

1. `config/WebConfig` 注册 `LoginInterceptor`：拦截 `/api/**`，放行 `/api/auth/**` 与 `/api/health`（登录页状态点用）；未登录直接返回 401 + `ApiResponse.error(401, 文案)`。CORS 全放开、允许凭证（前端带 session cookie）。
2. 登录态存 session 属性 `CurrentUser`（常量 `LoginInterceptor.SESSION_USER`）；业务代码统一经 `common/Sessions.requireUser(session)` 取当前用户名，取不到抛 `BusinessException(401)`。
3. 注册为**审批制**：`/api/auth/register` 只提交申请，管理员 approve 后才能登录；首个管理员由 `auth/AdminBootstrapper` 在库中无 ADMIN 时按 `arechat.admin.*` 引导创建。
4. `config/ImProperties`、`ModerationProperties` 等驱动敏感词过滤（censor/block）与每分钟发送限流；`auth/LoginRateLimiter` 防爆破。
5. HTTPS 部署形态：nginx 终止 TLS 后 HTTP 反代，`server.forward-headers-strategy=framework` 让后端识别 https，session cookie 自动带 Secure。

## 统一返回与异常

- 所有接口返回 `ApiResponse<T>(code, message, data)`，`code=0` 成功；失败用 `ApiResponse.error(code, message)`。
- 业务异常统一抛 `common/BusinessException(code, message)`——**code 同时作为 HTTP 状态码返回**（默认 400），message 是口语化整活文案。
- `common/GlobalExceptionHandler` 兜底：BusinessException → 对应状态码 + ApiResponse；其他异常 → 500。

## WS 推送机制

- 端点：`room/ChatEndpoint` `@ServerEndpoint("/ws/chat/{name}")`（昵称 URL 编码）；`room/ChatSessionRegistry` 维护 username→session 在线表；`room/ChatWebSocketBridge` 做 Spring Bean 与端点间的桥接。
- 推送门面：`im/ImPushService` —— `push(username, payload)` / `pushAll` / `pushToUsers` / `isOnline(username)`；payload 均为带 `type` 字段的 record（dm/typing/recall/friend/reaction/edit/read/pin/announcement/admin 等）。
- **情侣事件**：`pushCoupleEvent(event, actor, toUser, detail)` 推单人、`pushCoupleEventBoth(...)` 推双方，payload `type=couple`；前端（are-chat-web）把 `arechat:couple` 自定义事件按 event 名分发到 `stores/couple.ts` 刷新对应面板——**新增事件名必须前后端同步注册**。
- **落库通知中心（F41）**：每次情侣推送同时向 `couple_notify` 表落一条，离线用户上线后可补看。挂接方式：`couple/CoupleNotifyRecorder` 启动时经 `ImPushService.setNotifySink` 注册，保持 im 包不反向依赖 couple 包。
- 全库情侣事件名 180+ 个（如 `bond-action`、`capsule-due`、`birthday-eve`、`sync-tap-hit`），命名规则为小写连字符、动词/过去分词结尾。

## Flyway 迁移策略

- `spring.flyway.locations=classpath:db`，脚本命名 `V{n}__{描述}.sql`，当前 V1→V28（见 [database.md](database.md)）。
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
| `arechat.storage.base-dir` | 上传文件本地目录，默认 `./uploads`（`config/FileStorageProperties`） |
| `arechat.admin.*` | 管理员引导账号（`ARECHAT_ADMIN_USERNAME/PASSWORD` 覆盖默认值） |
| `arechat.notify.*` | 注册审批外部通知四通道（可同时启用）：企业微信 webhook / WxPusher / Server酱 / 虾推啥，全免费渠道；都不配则仅站内待办 |
| `arechat.moderation.*` | 敏感词：`enabled` / `mode=censor`（替换为＊）或 `block` / 词库 |
| `arechat.im.send-limit-per-minute` | 防刷屏限流，默认 30 |
| `management.endpoints` | 暴露 `health,info,h2dump`（`tools/H2DumpEndpoint` 仅对 H2 内存库有意义） |
