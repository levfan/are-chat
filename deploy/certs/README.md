# TLS 证书目录（只读挂载进容器 /etc/nginx/certs）

nginx 固定引用本目录下两个文件（compose 已把本目录只读挂载到 `/etc/nginx/certs`）：

| 文件 | 内容 | 来源 |
|------|------|------|
| `fullchain.pem` | 证书链（站点证书 + 签发 CA，顺序不能乱） | `gen-self-signed.sh` / Let's Encrypt / 云厂商 |
| `privkey.pem` | 证书私钥 | 与上面成对 |

本目录 `*.pem`/`*.key`/`*.crt`/`*.srl` 均已 gitignore，**私钥绝不进版本库**。

---

## 方案一：本地自签（无域名 / 纯 IP，一键生成）

```bash
# 在 deploy/certs 目录下执行（WSL2 / Linux 自带 openssl）
bash gen-self-signed.sh
# 服务器用公网 IP 访问时，把 IP 写进 SAN（多个 IP 空格分隔）
SERVER_IP="117.72.73.149" bash gen-self-signed.sh
# 强制重新生成
FORCE=1 bash gen-self-signed.sh
```

脚本产出三样东西，各自用途：

| 产物 | 放哪里 | 作用 |
|------|--------|------|
| `fullchain.pem` + `privkey.pem` | 留在本目录 | `docker compose up -d` 直接可用，nginx 自动加载 |
| `ca.crt` | **发给每个用户**导入设备 | 导入后浏览器无警告，PWA 可安装（关键一步，别只发链接） |
| `are-chat-local-ca.key` | 留在本目录（600） | CA 私钥，将来给新证书续签用；泄露 = 整套信任作废 |

### 把 ca.crt 导入设备信任（每台设备一次）

> ⚠️ 不导入 ca.crt、只在浏览器点「高级 → 继续前往」是**装不了 PWA 的**：
> 带证书告警的页面不算安全上下文，Service Worker 与安装入口都不会出现。
> 导入 ca.crt 后才是真正受信任的 HTTPS，一键安装全部可用。

| 平台 | 操作 |
|------|------|
| Windows | 双击 `ca.crt` → 安装证书 → 存储位置选「本地计算机」→「将所有的证书都放入下列存储」→ 浏览选「受信任的根证书颁发机构」→ 完成，重启浏览器 |
| macOS | 双击 `ca.crt` 导入「钥匙串访问」→ 找到「ARE Chat Local CA」→ 显示简介 → 信任 → 「始终信任」 |
| Android | 设置 → 安全 → 更多安全设置 → 加密与凭据 → 安装证书 → CA 证书 → 选 `ca.crt`（Chrome 会信任用户 CA） |
| iOS | 用 Safari/AirDrop/邮件打开 `ca.crt` → 安装描述文件 → 设置 → 通用 → 关于本机 → 证书信任设置 → 开启对「ARE Chat Local CA」的完全信任 |
| Linux (Debian/Ubuntu) | `sudo cp ca.crt /usr/local/share/ca-certificates/are-chat-local.crt && sudo update-ca-certificates` |

### 自检

```bash
openssl verify -CAfile ca.crt fullchain.pem   # 应输出 fullchain.pem: OK（先拆开校验 server.crt 同理）
curl -v https://<服务器IP>:5443/              # 导入信任后的设备应无告警返回 index.html
```

---

## 方案二：Let's Encrypt（有公网域名时优先选这个）

```bash
sudo certbot certonly --webroot -w /var/www/certbot -d your.domain.com
sudo cp /etc/letsencrypt/live/your.domain.com/fullchain.pem deploy/certs/
sudo cp /etc/letsencrypt/live/your.domain.com/privkey.pem deploy/certs/
```

## 方案三：云厂商免费证书

控制台申请（绑定域名）→ 下载 nginx 格式 → 按上表文件名放入本目录。

---

## 换证书 / 续期

覆盖本目录的 `fullchain.pem` + `privkey.pem` 后执行：

```bash
docker exec are-chat nginx -s reload    # 或 docker compose restart app
```

文件权限建议 600；90 天期的正式证书记得挂个续期 cron。
