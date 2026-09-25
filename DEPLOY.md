# are-chat 部署指南

> 一体化镜像：nginx（前端静态资源 + `/api`、`/ws` 反向代理）+ JRE 25（Spring Boot，仅监听容器内 127.0.0.1）
> 请求链路：浏览器 → nginx:80 → 静态文件 / 反代 → Java:8080（容器内部）
> 部署文件全部在**本目录**（后端项目）：`Dockerfile` · `docker-compose.yml` · `docker/` · `.dockerignore`；
> 构建时前端源码经 `additional_contexts` 引入，要求前端项目 `../are-chat-web` 与本目录同级

---

## 1. 前置条件

| 项目 | 要求 |
|------|------|
| Docker | 24+（`docker version` 查看） |
| Compose | v2.20+（`docker compose version`，注意是 `docker compose` 不是 `docker-compose`） |
| 目录 | `are-chat` 与 `are-chat-web` 同级（前端源码参与构建） |
| 端口 | 默认占用宿主机 80 端口（可用 `APP_PORT` 改） |
| 资源 | 内存 ≥ 1GB，磁盘 ≥ 2GB |
| 网络 | 构建期需要访问 Docker Hub 与依赖仓库（已内置国内镜像源加速）；离线部署见第 7 节 |

## 2. 快速开始

```bash
# 在本目录（本文件与 docker-compose.yml 同目录）执行
docker compose up -d --build
docker compose ps                          # 状态 healthy 即正常
docker compose logs -f app                 # 实时日志（nginx + Spring Boot 都在这里）
```

浏览器访问 `http://localhost/`：

- 演示账号：`alice` / `bob` / `carol`，密码统一 `arechat123`（库里没有用户时启动自动播种）；
- 或点「注册」用手机号注册新账号（演示环境验证码直接回显，不接短信网关）。

> 健康检查：`docker inspect --format '{{.State.Health.Status}}' are-chat` → `healthy`
> （探测容器内 8080 端口 TCP，启动期 40 秒内显示 starting 属正常）

## 3. 配置项

### 3.1 docker-compose.yml 环境变量

| 变量 | 默认 | 说明 |
|------|------|------|
| `APP_PORT`（.env） | `80` | 宿主机映射端口，如 `APP_PORT=8080` 写入同目录 `.env` 文件 |
| `TZ` | `Asia/Shanghai` | 容器时区 |
| `ARECHAT_STORAGE_BASE_DIR` | `/data/uploads` | 上传文件目录（已挂命名卷持久化） |
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75.0` | JVM 参数，如 `-Xmx1g` |
| `SPRING_PROFILES_ACTIVE` | （空 = H2 内存库） | 设为 `mysql` 连接老库 |
| `MYSQL_HOST/PORT/DB/USERNAME/PASSWORD` | — | MySQL profile 生效时的连接参数 |

### 3.2 内置限制（需要更大值时改两处）

| 限制 | 值 | 修改位置 |
|------|----|----------|
| 上传单文件 | 20MB | `src/main/resources/application.yml` → `spring.servlet.multipart.max-file-size` |
| 请求总体 | 25MB | 同上 `max-request-size` |
| nginx 请求体 | 25MB | `docker/nginx.conf` → `client_max_body_size` |

## 4. 数据持久化

- 上传的文件本体 → 命名卷 `are-chat_uploads` → 容器内 `/data/uploads`
- 备份：
  ```bash
  docker run --rm -v are-chat_uploads:/data -v "${PWD}:/backup" alpine \
      tar czf /backup/uploads-$(date +%F).tar.gz -C /data .
  ```
- 恢复：
  ```bash
  docker run --rm -v are-chat_uploads:/data -v "${PWD}:/backup" alpine \
      sh -c "cd /data && tar xzf /backup/uploads-2026-09-25.tar.gz"
  ```

> ⚠️ **默认 H2 是内存库**：重启容器后 `uploaded_file` 与 IM 各表数据清空（仅文件本体保留在卷里）。
> 长期使用请二选一：
> ① **接老 MySQL**（推荐，见第 5 节）；
> ② H2 改文件模式：在 compose 的 `environment` 加
> `SPRING_DATASOURCE_URL: jdbc:h2:file:/data/h2/arechat;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE`
> 并把卷挂载点从 `/data/uploads` 改为 `/data`、`ARECHAT_STORAGE_BASE_DIR=/data/uploads` 保持不变（建表脚本会自动执行）。

## 5. 连接老项目 MySQL

1. 编辑 `docker-compose.yml`，取消注释：
   ```yaml
   SPRING_PROFILES_ACTIVE: mysql
   MYSQL_HOST: 192.168.1.100        # 老库地址
   MYSQL_PORT: "3306"
   MYSQL_DB: arechat
   MYSQL_USERNAME: root
   MYSQL_PASSWORD: change-me
   ```
2. 确认 MySQL 账号允许从 Docker 网段（默认 172.17.0.0/16）或宿主机 IP 连入。
3. **IM 是 2026 新增模块**：老库里没有 `friend` / `friend_request` / `private_message` / `user_profile` 四张表，需手工执行 `src/main/resources/schema.sql` 中 IM 段的 DDL（老项目原有的 `uploaded_file` 表不用动；`person` 表已随档案功能移除，可留可删）。若之前已按旧版建过 `friend` / `private_message`，再补执行 IM 段注释里的两条 ALTER（`muted` / `last_seen_at` / `reply_to_id`）。
4. `docker compose up -d` 重建容器（镜像不变，秒级完成）。
5. 看 `docker compose logs app` 出现 `Started SmartChatApplication` 即成功。

## 6. 日常运维

| 操作 | 命令 |
|------|------|
| 更新发布（改完代码） | `docker compose up -d --build`（依赖层有缓存，只重构建变化部分） |
| 发布前留版本 | `docker tag are-chat:1.0.0 are-chat:backup-$(date +%F)` |
| 回滚 | `docker compose down && docker run -d --name are-chat -p 80:80 -v are-chat_uploads:/data/uploads are-chat:backup-2026-09-25` |
| 重启 | `docker compose restart app` |
| 停止并移除 | `docker compose down`（**加 `-v` 会连数据卷一起删，慎用**） |
| 进容器排查 | `docker exec -it are-chat sh` |
| nginx 访问/错误日志 | `docker compose logs app`（已重定向 stdout/stderr） |

## 7. 离线部署（服务器无外网）

```bash
# —— 在有网的构建机（本目录）——
docker compose build
docker save are-chat:1.0.0 -o are-chat-1.0.0.tar
# 拷贝 are-chat-1.0.0.tar 与 docker-compose.yml 到服务器

# —— 在目标服务器 ——
docker load -i are-chat-1.0.0.tar
docker compose up -d            # 镜像已存在，不会再触发构建
```

不用 compose 时的等价裸命令：

```bash
docker run -d --name are-chat \
  -p 80:80 \
  -v are-chat_uploads:/data/uploads \
  --restart unless-stopped \
  are-chat:1.0.0
```

不通过 compose 手动构建镜像（等价于 compose 的 additional_contexts）：

```bash
docker buildx build --build-context web=../are-chat-web -t are-chat:1.0.0 .
```

## 8. 单副本限制（务必了解）

登录会话（HttpSession）与聊天室在线注册表都在**内存**中：
- 只能运行 **1 个容器副本**；扩多副本需要引入 Redis Session + WebSocket 广播/粘性会话（当前未实现）。
- 重启容器后用户需重新登录，IM 在线状态清零。

## 9. 常见问题排查

| 现象 | 排查 |
|------|------|
| 页面 502 | 后端没起来：`docker compose logs app` 看 Java 堆栈 |
| 80 端口被占 | `.env` 写 `APP_PORT=8080` 后 `docker compose up -d` |
| 上传报 413 | 调大 `docker/nginx.conf` 的 `client_max_body_size` 与后端 multipart 限制（改后 `--build` 重建） |
| WebSocket 频繁断开 | 若外层还有网关/CDN，同样要配 `Upgrade/Connection` 头且不要缓冲；容器内 nginx 已配 3600s 读超时 |
| 健康检查 unhealthy | `docker compose logs app` 看启动错误；注意 start_period 40s |
| 构建时拉不到基础镜像 | 检查 Docker Hub 连通性或为 Docker 配置镜像加速；基础镜像标签可按需替换为可用的 temurin 25.x |
| 控制台中文乱码 | 仅为 Windows 终端显示编码问题（`chcp 65001`），不影响服务 |

## 10. 安全建议

- 不要把真实 `MYSQL_PASSWORD` 提交进版本库：用 `.env` 文件（加入 `.gitignore`）或部署机环境变量注入。
- 对外只暴露 nginx 的 80/自定义端口；后端 8080 已绑定 127.0.0.1，天然不外露。
- 服务器防火墙只放行对外端口；如需 HTTPS，在最外层网关终止 TLS 再转发到本容器 80 端口。
