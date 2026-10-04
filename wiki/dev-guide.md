# 开发指南

> 本页回答：环境要求、构建/测试命令、硬性流程摘要、产品红线。

## 环境要求

| 项 | 要求 |
|---|---|
| JDK | **Java 25**（`pom.xml` `<java.version>25`，描述明确 "JDK 25 + Spring Boot 4.1 + fastjson2"；注意 `are-chat-map` skill 首节写 21 已过时，以 pom 为准） |
| Maven | 任意近期版本（经 `spring-boot-maven-plugin` 构建；pom 未显式约束 Maven 版本） |
| 数据库 | 本地开发可走 H2 内存库（application.yml 内有注释掉的备用 datasource）；默认连外部 MariaDB 10.11，环境变量 `DB_CONNECT_URL / DB_CONNECT_USER / DB_CONNECT_PASSWORD / DB_CONNECT_DRIVER` 覆盖 |
| Lombok | 必需（注解处理经 `maven.compiler.proc=full` 显式开启）；IDE 需装 Lombok 插件 |

本地启动：`mvn spring-boot:run`（端口 8080；插件配置已压制 fastjson2 在 JDK 25 的 Unsafe 弃用告警）。表结构无需手工建——Flyway 启动时自动重放 `classpath:db` 下 V 脚本（每个脚本幂等，空库/老库都能跑）。

## 构建与测试

| 命令 | 用途 |
|---|---|
| `mvn -q compile` | 提交前必跑的最低验证 |
| `mvn test` | 全量测试；测试库固定 H2（`src/test/resources/application.yml` 覆盖 datasource，MODE=MySQL），Flyway 与生产走同一批 V 脚本——**改了表必须跑 `mvn test` 验证脚本在 H2 可执行** |

测试布局：`src/test/java/com/smart/chat/`，已有 auth / im / room / upload / couple 各模块测试与 `SmartChatApplicationTest` 上下文冒烟，当前基线 **202 用例**（2026-10-05 实测 `mvn -o test` → `Tests run: 202, Failures: 0, Errors: 0`，`BUILD SUCCESS`）。新 Service 的核心算法（判定/统计/轮换）建议补单测。

## 接口访问日志

`/api/**` 上每个请求固定留两行日志，同 `[req N]` 前缀串起来（`bootstrap/config/RequestLogFilter`，装配与开关在 `RequestLogConfig` / `RequestLogProperties`，取舍理由见 [ADR-0006](../docs/adr/0006-request-log-filter.md)）：

```
[req 4] --> POST /api/auth/login user=- query=- body={"account":"admin","password":"..."}
[req 4] <-- POST /api/auth/login user=admin 200 36ms 成功 body={"code":0,"data":{...},"message":"ok"}
```

| 栏 | 口径 |
|---|---|
| 进入行 | 方法、路径、`user`（会话里的登录名，未登录 `-`）、`query`、请求体原文 |
| 完成行 | 方法、路径、`user`、HTTP 状态、耗时、`成功`/`失败`、响应体原文 |
| 成功/失败判据 | HTTP 状态 `<400` 记成功。全仓没有 Controller 直接 `return ApiResponse.error(...)`，业务失败一律经 `GlobalExceptionHandler` 落成 4xx/5xx，所以状态码不会把业务失败记成成功 |
| 开关 | `arechat.request-log.enabled`（缺省即开启，关掉要显式写 `false`） |
| 正文上限 | `arechat.request-log.max-body-chars`（默认 4000 字符，超出打 `...(截断,共 N 字符)`；同时是响应体旁路缓冲的字节上限） |

不覆盖的三类：`POST /api/files` 的 multipart 正文（只打说明，不把附件字节读进内存）、非 JSON 的二进制响应（如 `/api/files/{id}/download`，只打状态与耗时）、WebSocket `/ws/chat/{name}`（`@ServerEndpoint` 不走 Servlet 过滤链，要日志得在 `ChatEndpoint` 另行埋点）。

两条排查用口径：

- 入参在**处理器执行之前**就已落日志，所以请求卡死或进程中途挂掉时，进入行是唯一证据；只有 JSON 且 `Content-Length ≤ 1MB` 才读原文，分块传输与超限都原样透传不碰流。
- 日志**不脱敏**（明文口令与私密正文都会进来），这是用户拍定的口径，风险与止损见 ADR-0006 第 4 节。

## 开发硬性流程摘要（一句话一步）

1. 开工：先读 `.agents/skills/are-chat-map/SKILL.md` 与本 [wiki](Home.md)，按触达领域加载专项 skill。
2. 编码：照抄 couple 包四层模板（Entity→Mapper→Service→Controller），静态 Bank 只增不改顺序。
3. 自检：过 git-commit skill 的「架构师 Code Review 五项」清单。
4. 构建：`mvn -q compile`；动过测试/表则 `mvn test` 全绿。
5. 提交：按改动性质分组、一类一 commit；`V*.sql`+`schema.sql` 永远独立成 db commit；信息 `type(scope): 中文描述`。
6. 推送：commit → `git pull --no-rebase` → push；失败保留本地 commit 并报告，禁止 force push。
7. 收尾：模块/表/接口/定时任务有增删 → 同步更新 are-chat-map skill（及本 wiki）。

以上仅为串联摘要，**规范细节一律以现有文档为准**：仓库规则见 `agents.md`；完整规范见 `.agents/skills/git-commit/SKILL.md`、`.agents/skills/db-migration/SKILL.md`、`.agents/skills/lombok-data/SKILL.md`。

## 数据层硬性约定（速记）

- 改表 = 新增 `V{max+1}__{描述}.sql` + 同步 `schema.sql`，已入库脚本不得修改。
- 所有脚本必须幂等且 H2/MariaDB 双兼容（`CREATE TABLE IF NOT EXISTS`；utf8mb4_bin 列必须 NOT NULL）。
- 主键 UUID 应用层生成；时间一律毫秒 bigint。详见 [database.md](database.md)。

## 产品红线（用户长期约束）

| 红线 | 说明 |
|---|---|
| **不做照片/视频上传类功能** | 服务器存储/带宽要求高；现有 `/api/files` 通道仅服务文件附件场景，任何"晒图"类需求一律不实现 |
| 情绪价值优先 | 情侣空间功能的评判标准是"是否让双方更甜/更被接住"，文案要可爱、口语化、带 emoji；宁可聚合现有数据出仪式感，不加冷冰冰的工具功能 |
| 迁移必须幂等 | baseline-version=0 导致老库会全量重放 V1 起所有脚本——任何脚本不幂等即事故 |

## 与其他文档的分工

- `README.md` / `DEPLOY.md`：面向部署运行；本 wiki 面向理解与二次开发。
- `.agents/skills/are-chat-map`：规范与速查的单一事实源（改代码前必读）；wiki 不复述分层模板/命名规范。
- 前端仓库配套文档：`are-chat-web` 的 `agents.md` 与 `are-chat-web-map` skill（新 WS 事件需两端同步注册）。
