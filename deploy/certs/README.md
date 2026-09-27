# TLS 证书目录（只读挂载进容器 /etc/nginx/certs）

把以下两个文件放到本目录（文件名固定，nginx.conf 按此引用）：

| 文件 | 内容 | 来源 |
|------|------|------|
| `fullchain.pem` | 证书链（站点证书 + 中间证书，顺序不能乱） | Let's Encrypt / 云厂商免费证书 / 自签 |
| `privkey.pem` | 证书私钥 | 与上面成对 |

```bash
# Let's Encrypt（certbot webroot 模式，在有公网域名解析的服务器上执行）
sudo certbot certonly --webroot -w /var/www/certbot -d your.domain.com
sudo cp /etc/letsencrypt/live/your.domain.com/fullchain.pem deploy/certs/
sudo cp /etc/letsencrypt/live/your.domain.com/privkey.pem deploy/certs/
```

⚠️ `*.pem` / `*.key` 已被 .gitignore 排除，不要把私钥提交进版本库。
文件权限建议 600；证书续期后 `docker compose restart app`（或 `docker exec are-chat nginx -s reload`）生效。
