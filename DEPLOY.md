# are-chat 部署指南

> 一体化镜像：nginx（前端静态资源 + `/api`、`/ws` 反向代理 + TLS 终止）+ JRE 25（Spring Boot，仅监听容器内 127.0.0.1）
> 请求链路：浏览器 → **nginx:443（HTTPS）** → 静态文件 / 反代 → Java:8080（容器内部）；
> nginx:80 仅做 301 跳转 HTTPS 与 ACME 证书续期挑战
> HTTPS 是 **PWA「一键安装到桌面」的硬性前提**（安全上下文），证书部署见第 11 节
> 部署文件全部在**本目录**（后端项目）：`Dockerfile` · `docker-compose.yml` · `docker/` · `.dockerignore`；
> 构建时前端源码经 `additional_contexts` 引入，要求前端项目 `../are-chat-web` 与本目录同级

---

## 1. 前置条件

| 项目 | 要求 |
|------|------|
| Docker | 24+（`docker version` 查看） |
| Compose | v2.20+（`docker compose version`，注意是 `docker compose` 不是 `docker-compose`） |
| 目录 | `are-chat` 与 `are-chat-web` 同级（前端源码参与构建） |
| 端口 | 宿主机 5443（HTTPS 入口，可改 443）+ 58080（HTTP 跳转，可关）；都可用 `.env` 覆盖 |
| TLS 证书 | `deploy/certs/` 下放 `fullchain.pem` + `privkey.pem`（没有证书 nginx 起不来，获取方式见第 11 节） |
| 资源 | 内存 ≥ 1GB，磁盘 ≥ 2GB |
| 网络 | 构建期需要访问 Docker Hub 与依赖仓库（已内置国内镜像源加速）；离线部署见第 7 节 |

## 2. 快速开始

```bash
# 在本目录（本文件与 docker-compose.yml 同目录）执行
docker compose up -d --build
docker compose ps                          # 状态 healthy 即正常
docker compose logs -f app                 # 实时日志（nginx + Spring Boot 都在这里）
```

浏览器访问 `https://<服务器IP或域名>:5443/`（正式证书无警告；自签证书首次需手动信任，见第 11.4 节）：

- **管理员账号**：`admin` / `admin123456`（库里没有 ADMIN 时启动自动创建；环境变量 `ARECHAT_ADMIN_USERNAME` / `ARECHAT_ADMIN_PASSWORD` 可覆盖）。登录后左侧栏出现「管理后台」入口。
- **普通用户**：点「注册」用手机号提交注册申请（演示环境验证码直接回显，不接短信网关）。**注册不再直接登录**：管理员在「管理后台 → 注册审批」点「通过」后，用户才能登录。
- 旧演示账号 `alice` / `bob` / `carol` 已废弃：启动时会被自动禁用。

> 健康检查：`docker inspect --format '{{.State.Health.Status}}' are-chat` → `healthy`
> （探测容器内 8080 端口 TCP，启动期 40 秒内显示 starting 属正常）

## 3. 配置项

### 3.1 docker-compose.yml 环境变量

| 变量 | 默认 | 说明 |
|------|------|------|
| `APP_PORT`（.env） | `58080` | HTTP 端口：仅 301 跳转 HTTPS 与 ACME 续期挑战 |
| `HTTPS_PORT`（.env） | `5443` | HTTPS 入口端口；公网服务器建议写 `443`，浏览器可不带端口号直接访问 |
| `TZ` | `Asia/Shanghai` | 容器时区 |
| `ARECHAT_STORAGE_BASE_DIR` | `/data/uploads` | 上传文件目录（已挂命名卷持久化） |
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75.0` | JVM 参数，如 `-Xmx1g` |
| `SPRING_PROFILES_ACTIVE` | （空 = H2 内存库） | 设为 `mysql` 连接老库 |
| `MYSQL_HOST/PORT/DB/USERNAME/PASSWORD` | — | MySQL profile 生效时的连接参数 |
| `ARECHAT_ADMIN_USERNAME` | `admin` | 初始管理员用户名（无 ADMIN 时启动自动创建） |
| `ARECHAT_ADMIN_PASSWORD` | `admin123456` | 初始管理员密码（**部署后请立即在管理后台重置**） |
| `ARECHAT_NOTIFY_WECOM_WEBHOOK` | — | 企业微信群机器人 Webhook 地址（78 免费推送推荐渠道，`https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx`） |
| `ARECHAT_NOTIFY_WXPUSHER_TOKEN` | — | WxPusher appToken（免费推送备选） |
| `ARECHAT_NOTIFY_WXPUSHER_UIDS` | — | WxPusher 接收者 UID，多个用英文逗号分隔 |
| `ARECHAT_NOTIFY_SERVERCHAN_KEY` | — | Server酱 SendKey（免费版每天 5 条） |
| `ARECHAT_NOTIFY_XTUIS_KEY` | — | 虾推啥 token（https://www.xtuis.cn，免费版每天 300 条 / 每分钟 30 条） |

> **注册审批推送（78）**：以上四个渠道任配其一，新注册申请就会实时推送给管理员；全部不配则只靠「管理后台」红点提醒（站内兜底）。国内短信没有免费渠道，未实现短信通知。
> **敏感词 / 限流**：`arechat.moderation.enabled/mode/sensitive-words`（默认关闭）与 `arechat.im.send-limit-per-minute`（默认 30 条/分钟）在 `application.yml` 调整，一般保持默认即可。

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
4. **第五轮新增（77–96）**：老库还需补五张新表 + 一条列 —— `registration_application`（注册审批）、`conversation_pin`（会话内置顶）、`announcement` + `announcement_read`（全站公告）、`admin_audit`（审计日志），以及 `app_user` 加列 `role VARCHAR(20) DEFAULT 'USER'`（管理员角色；DDL 见 `schema.sql` 对应注释段）。
5. `docker compose up -d` 重建容器（镜像不变，秒级完成）。
6. 看 `docker compose logs app` 出现 `Started SmartChatApplication` 即成功。

## 6. 日常运维

| 操作 | 命令 |
|------|------|
| 更新发布（改完代码） | `docker compose up -d --build`（依赖层有缓存，只重构建变化部分） |
| 更换/续期证书 | 覆盖 `deploy/certs/*.pem` 后 `docker exec are-chat nginx -s reload`（或重启容器） |
| 发布前留版本 | `docker tag are-chat:1.0.0 are-chat:backup-$(date +%F)` |
| 回滚 | `docker compose down && docker run -d --name are-chat -p 5443:443 -p 58080:80 -v are-chat_uploads:/data/uploads -v ./deploy/certs:/etc/nginx/certs:ro are-chat:backup-2026-09-25` |
| 重启 | `docker compose restart app` |
| 停止并移除 | `docker compose down`（**加 `-v` 会连数据卷一起删，慎用**） |
| 进容器排查 | `docker exec -it are-chat sh`（改配置后 `nginx -t` 校验、`nginx -s reload` 热载） |
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
  -p 5443:443 -p 58080:80 \
  -v are-chat_uploads:/data/uploads \
  -v $(pwd)/certs:/etc/nginx/certs:ro \
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
| 新用户注册后登录 403「等待审批」 | 正常行为（77 审批制）：管理员在「管理后台 → 注册审批」点「通过」后即可登录 |
| 管理员收不到注册推送提醒 | 检查 `ARECHAT_NOTIFY_*` 环境变量是否注入并重建容器；全未配置时只有站内红点兜底 |
| 页面 502 | 后端没起来：`docker compose logs app` 看 Java 堆栈 |
| 容器起来但 nginx 报 `cannot load certificate` | `deploy/certs/` 里没有 `fullchain.pem`/`privkey.pem` 或文件名不对：放好证书后 `docker compose restart app` |
| 浏览器提示「您的连接不是私密连接」 | 自签证书属正常（见 11.4）；正式证书出现则是证书过期/域名不匹配，检查 `deploy/certs` 内容与续期 |
| `ERR_SSL_PROTOCOL_ERROR` | 用 https 访问的端口不对（443/HTTPS_PORT），或防火墙/安全组没放行 HTTPS 端口 |
| 上传报 413 | 调大 `deploy/nginx.conf` 的 `client_max_body_size` 与后端 multipart 限制（改后 `--build` 重建） |
| WebSocket 频繁断开 | 若外层还有网关/CDN，同样要配 `Upgrade/Connection` 头且不要缓冲；容器内 nginx 已配 3600s 读超时 |
| wss 连不上但 https 正常 | DevTools → Network → WS 看握手状态；常见是 CDN/网关没转发 `Upgrade` 头，或自签证书未被信任 |
| PWA 安装入口不出现 | 确认地址是 https 且证书有效；`Application → Manifest` 无报错；Service Worker 已 activated；iOS 只支持「添加到主屏幕」 |
| 健康检查 unhealthy | `docker compose logs app` 看启动错误；注意 start_period 40s |
| 构建时拉不到基础镜像 | 检查 Docker Hub 连通性或为 Docker 配置镜像加速；基础镜像标签可按需替换为可用的 temurin 25.x |
| 控制台中文乱码 | 仅为 Windows 终端显示编码问题（`chcp 65001`），不影响服务 |

## 10. 安全建议

- 不要把真实 `MYSQL_PASSWORD` 提交进版本库：用 `.env` 文件（加入 `.gitignore`）或部署机环境变量注入。
- 对外只暴露 nginx 的 443/80 端口；后端 8080 已绑定 127.0.0.1，天然不外露。
- 服务器防火墙只放行 443（必须）与 80（用于证书续期；不开也能用，只是无法 HTTP-01 自动续期）。
- TLS 证书与私钥只放在服务器 `deploy/certs/`（已 gitignore），绝不进镜像、不进版本库。
- 测试稳定后可放开 `nginx.conf` 里注释的 HSTS 头，强制浏览器只走 HTTPS（开启后该域名一年内无法回退 HTTP）。

## 11. HTTPS 部署（PWA「一键安装到桌面」的前提）

### 11.1 为什么必须 HTTPS

PWA 的「安装到桌面/手机」按钮、Service Worker、消息通知等能力都要求页面处于**安全上下文**（Secure Context）：
只有 `https://` 和 `http://localhost` 满足条件。用 IP 的 HTTP 访问时地址栏永远不出现安装入口。

好消息是**前端代码零改动**：WebSocket 地址本来就会随页面协议自动切换 `wss://`，manifest 与 Service Worker 直接复用。

### 11.2 证书怎么来（三选一）

| 方案 | 条件 | 效果 | 说明 |
|------|------|------|------|
| **Let's Encrypt**（推荐） | 有公网域名并解析到服务器 | 正式证书，全平台无警告 | 免费 90 天，certbot 自动续期，见下方命令 |
| 云厂商免费证书 | 有域名（在云厂商 DNS） | 正式证书，有效期 3-12 个月 | 控制台申请后下载 nginx 格式（.pem + .key） |
| 自签名证书 | 无域名（纯 IP / 内网） | 首次访问需手动信任，移动端体验差 | 只建议内网/过渡用，见 11.4 |

Let's Encrypt 申请（服务器 80 端口可访问时用 webroot 模式，nginx 已放行该路径）：

```bash
sudo apt install certbot                       # 或 yum install certbot
sudo mkdir -p /var/www/certbot
sudo certbot certonly --webroot -w /var/www/certbot -d your.domain.com
# 把证书放进 deploy/certs/（文件名固定）
sudo cp /etc/letsencrypt/live/your.domain.com/fullchain.pem deploy/certs/
sudo cp /etc/letsencrypt/live/your.domain.com/privkey.pem  deploy/certs/
# 续期（certbot renew 定时任务续期后，重启容器或 reload nginx 生效）
sudo certbot renew --dry-run
docker exec are-chat nginx -s reload
```

### 11.3 放好证书后启动

```bash
ls deploy/certs/          # fullchain.pem privkey.pem README.md
docker compose -f deploy/docker-compose.yml up -d --build
curl -vk https://127.0.0.1:5443/            # 自签时 -k 跳过校验；返回 index.html 即通
```

端口规划（`.env` 可改）：`HTTPS_PORT=5443` 正式入口；`APP_PORT=58080` HTTP→HTTPS 跳转。
公网服务器建议 `.env` 写 `HTTPS_PORT=443`，访问 `https://your.domain.com/` 即可。

### 11.4 自签名证书（纯 IP / 内网过渡方案）

```bash
# 生成 10 年期自签证书（CN 与 SAN 都写服务器 IP，浏览器校验 SAN）
openssl req -x509 -newkey rsa:2048 -nodes -days 3650 \
  -keyout deploy/certs/privkey.pem -out deploy/certs/fullchain.pem \
  -subj "/CN=your.server.ip" -addext "subjectAltName=IP:your.server.ip"
```

代价：桌面 Chrome/Edge 首次访问点「高级 → 继续前往」后可以正常注册 Service Worker 并出现安装入口，
但**移动端 PWA 安装基本不可用**、部分浏览器每次清 Cookie 后要重新信任。要好的安装体验，请上域名 + 正式证书。

### 11.5 部署后自检清单（PWA 可安装）

| 检查项 | 方法 |
|--------|------|
| HTTPS 生效 | 地址栏出现锁标志；`curl -vI https://<host>:5443/ 2>&1 \| grep -i "SSL\|200"` |
| HTTP 自动跳转 | `curl -sI http://<host>:58080/` 返回 `301` + `Location: https://...` |
| WebSocket 走 wss | 浏览器 DevTools → Network → WS，聊天连接协议是 `wss`（前端自动，无需配置） |
| manifest 可达 | `https://<host>:5443/manifest.webmanifest` 返回 JSON |
| Service Worker 注册 | DevTools → Application → Service Workers 显示 activated |
| 出现安装入口 | 桌面 Chrome/Edge 地址栏右侧「安装」图标，或菜单 →「安装应用」；Android Chrome 菜单 →「添加到主屏幕/安装应用」 |
| iOS 注意 | iOS 的 Safari 只支持「添加到主屏幕」（菜单分享里），无独立安装提示，属正常 |
