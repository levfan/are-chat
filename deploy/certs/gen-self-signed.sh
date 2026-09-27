#!/usr/bin/env bash
# =====================================================================
# are-chat 本地自签证书一键生成（本地根 CA + 由 CA 签发的服务器证书）
#
# 用法（在 deploy/certs 目录或任意位置执行）：
#   bash gen-self-signed.sh                          # SAN 含 localhost/127.0.0.1/本机主 IP
#   SERVER_IP="117.72.73.149" bash gen-self-signed.sh # 追加服务器公网/内网 IP 到 SAN
#   FORCE=1 bash gen-self-signed.sh                  # 已有证书时强制重新生成
#
# 产物（都在本目录）：
#   ca.crt                  本地根 CA 证书 —— 分发给用户设备导入信任（PWA 可装的关键）
#   fullchain.pem           服务器证书链（站点证书 + CA），nginx 用
#   privkey.pem             服务器私钥，nginx 用（600 权限）
#   are-chat-local-ca.key   CA 私钥（600 权限，留着给以后的证书续签；泄露=信任体系作废）
#
# 原理：浏览器只认「受信任的根 CA 签发、且 SAN 匹配访问地址」的证书。
#       直接裸自签（无 CA、无 SAN）在多数平台装不了 PWA；本方案导入 ca.crt 后
#       即为合法安全上下文，Service Worker / PWA 安装 / wss 全部可用。
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")"

CA_DAYS=3650        # CA 有效期 10 年
SERVER_DAYS=825     # 服务器证书 825 天（Safari 对服务器证书有效期的上限策略，兼容所有平台）
FORCE="${FORCE:-0}"
SERVER_IPS="${SERVER_IP:-}"   # 额外 SAN 的 IP（空格分隔）

command -v openssl >/dev/null 2>&1 || { echo "错误：需要 openssl（WSL2/Linux 自带）"; exit 1; }

if [[ -f fullchain.pem && -f privkey.pem && "$FORCE" != "1" ]]; then
  echo "已存在 fullchain.pem / privkey.pem，跳过生成（FORCE=1 可强制重新生成）"
  exit 0
fi

# 自动探测本机主 IP 加进 SAN（在部署服务器上执行时很有用；SERVER_IP 可追加更多）
AUTO_IP="$(hostname -I 2>/dev/null | awk '{print $1}' || true)"

# 组 SAN：localhost + 环回 + 本机 IP + 用户指定 IP
SAN="DNS:localhost,IP:127.0.0.1,IP:0:0:0:0:0:0:0:1"
for ip in "$AUTO_IP" $SERVER_IPS; do
  if [[ -n "$ip" ]]; then
    SAN="$SAN,IP:$ip"
  fi
done
echo "SAN = $SAN"

# —— 1. 本地根 CA（10 年）——
openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days "$CA_DAYS" \
  -keyout are-chat-local-ca.key -out ca.crt \
  -subj "/C=CN/O=ARE Chat/CN=ARE Chat Local CA" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,keyCertSign,cRLSign"

# —— 2. 服务器私钥 + CSR ——
openssl req -new -newkey rsa:2048 -nodes -sha256 \
  -keyout privkey.pem -out server.csr \
  -subj "/C=CN/O=ARE Chat/CN=are-chat-local" \
  -addext "extendedKeyUsage=serverAuth"

# —— 3. 用 CA 签发服务器证书（SAN 在这里写进证书）——
cat > server.ext <<EOF
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=$SAN
EOF
openssl x509 -req -sha256 -days "$SERVER_DAYS" \
  -in server.csr -CA ca.crt -CAkey are-chat-local-ca.key -CAcreateserial \
  -out server.crt -extfile server.ext

# —— 4. 组证书链 + 收尾 ——
cat server.crt ca.crt > fullchain.pem
chmod 600 privkey.pem are-chat-local-ca.key
rm -f server.csr server.ext

echo
echo "✅ 生成完成："
echo "   fullchain.pem / privkey.pem  → docker compose up 直接可用（compose 已挂载本目录）"
echo "   ca.crt                       → 发给每个用户导入设备信任，之后浏览器无警告、PWA 可安装"
echo "   访问地址若用 IP:5443，请确保该 IP 已在 SAN 里（重新生成：SERVER_IP=<IP> FORCE=1 bash gen-self-signed.sh）"
