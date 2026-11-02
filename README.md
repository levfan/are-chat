# are-chat

天枢（DUBHE）的后端：Spring Boot 提供 REST 与 WebSocket，界面在同级目录 `../are-chat-web`。

Java 25 · Spring Boot 4.1.1 · MyBatis-Plus · Flyway · MariaDB

## 主要功能

62 个 REST 端点 + 一条 WebSocket 通道，按功能分：

| 功能 | 里面有什么 | 接口 | 端点数 |
|------|-----------|------|--------|
| 账号与注册审批 | 手机号 + 验证码提交注册，管理员通过后才能登录；改密、自助注销 | `/api/auth` | 8 |
| 管理后台 | 注册审批（通过/驳回）、用户启停与重置密码、发布/关闭全站公告 | `/api/admin` | 10 |
| 私聊 | 文本/图片/文件消息、撤回、2 分钟内编辑、表情回应、会话内搜索、清空会话 | `/api/messages` | 11 |
| 通讯录 | 加好友（输入联想 → 申请 → 同意/拒绝）、备注/置顶/免打扰/拉黑、资料卡、好友生日、在线名单 | `/api/friends` `/api/profile` `/api/presence` | 14 |
| 情侣空间 | 邀请建立、在一起天数与纪念日（每天 09:30 倒数推送）、亲密日历（经期 🩸 / 边缘 ✨ / 性生活 💞 都在这一张卡里记） | `/api/couple` | 14 |
| 文件存储 | 按 SHA-256 内容去重 + 扩展名黑名单，是图片与文件消息的落点 | `/api/files` | 2 |
| 公告与自检 | 登录用户取当前生效公告并标记已读；`/api/health` 免登录看运行时长与在线人数 | `/api/announcements` `/api/health` | 3 |
| 实时推送 | 单通道下发：`dm`/`read`/`recall`/`message-edit`/`reaction`/`typing`/`pin`/`presence`/`friend-*`/`announcement`/`admin-pending` + 情侣 6 个事件（`invite*`/`anniversary-*`/`dissolved`） | `/ws/chat/{用户名}` | — |

## 运行与测试

```bash
mvn spring-boot:run     # 8080；REST 前缀 /api，WebSocket /ws/chat/{用户名}
mvn test                # H2 内存库，跑同一批 Flyway 脚本
```

- 建表不用手工执行，启动时 Flyway 自动迁移。
- 连别的库用环境变量整体覆盖，别把口令写进配置文件：`DB_CONNECT_URL` / `DB_CONNECT_USER` / `DB_CONNECT_PASSWORD` / `DB_CONNECT_DRIVER`。
- 注册要经管理员审批才能登录。库里没有 ADMIN 时启动会自动建一个，账号密码用 `ARECHAT_ADMIN_USERNAME` / `ARECHAT_ADMIN_PASSWORD` 覆盖。

## 部署

前后端合成一个镜像：容器里 nginx 发前端静态资源，把 `/api`、`/ws` 反代到 127.0.0.1:8080，对外只开 nginx 一个端口。产物在 Windows 上构建，Docker 只负责打镜像和运行。

| 步 | 在哪执行 | 做什么 |
|----|----------|--------|
| 1 | `are-chat` | `mvn -DskipTests package`，把 `target/are-chat-1.0.0.jar` 复制成 `deploy/app.jar` |
| 2 | `are-chat-web` | `pnpm build`，把 `dist/` 整个复制到 `are-chat/deploy/dist/` |
| 3 | `are-chat`（WSL2） | `./deploy/build.sh`（首次连 base 一起打；日常 `--skip-base`） |
| 4 | 同上 | `docker compose -f deploy/docker-compose.yml up -d` |

访问 `http://<主机>:58080/`。端口和镜像 tag 在 `deploy/.env`（`APP_PORT` / `VERSION`）；上传文件落命名卷 `uploads`；换库仍是上面那四个 `DB_CONNECT_*`，注进 compose 的 `environment`。当前走 HTTP，HTTPS 配置归档在 `deploy/nginx-https.conf`，切回按该文件头部说明。分步命令与排障见 [deploy/README.md](deploy/README.md)。

## 文档

接口总表 [wiki/api.md](wiki/api.md) ｜ 表结构 [wiki/database.md](wiki/database.md) ｜ 架构 [wiki/architecture.md](wiki/architecture.md) ｜ 情侣空间 [wiki/couple-space.md](wiki/couple-space.md) ｜ 证书与回滚 [DEPLOY.md](DEPLOY.md)
