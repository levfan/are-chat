#!/bin/bash
# ============================================================
# WSL2 + Docker 代理一键配置脚本
# 自动获取 WSL 访问 Windows 的网关地址作为代理出口
# ============================================================

set -e

# ---------- 配置区 ----------
PROXY_PORT="7897"  # ← 改成你梯子的实际端口
# ----------------------------

# 从 resolv.conf 提取 WSL 虚拟网关（即 Windows 侧对应网卡地址）
PROXY_HOST=$(grep -m1 nameserver /etc/resolv.conf | awk '{print $2}')

if [[ -z "${PROXY_HOST}" ]]; then
    echo "❌ 无法从 /etc/resolv.conf 提取 nameserver，请检查 WSL 网络配置"
    exit 1
fi

HTTP_PROXY="http://${PROXY_HOST}:${PROXY_PORT}"
HTTPS_PROXY="http://${PROXY_HOST}:${PROXY_PORT}"
NO_PROXY="localhost,127.0.0.1,::1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,.local"

CONF_DIR="/etc/systemd/system/docker.service.d"
CONF_FILE="${CONF_DIR}/http-proxy.conf"
BACKUP_FILE="${CONF_DIR}/http-proxy.conf.bak"

echo "🔧 检测到 WSL→Windows 网关: ${PROXY_HOST}"
echo "🔧 代理地址: ${HTTP_PROXY}"

# ---------- 前置检查：代理是否可达 ----------
echo "🔍 检查代理连通性..."
if ! curl -s --connect-timeout 5 -x "${HTTP_PROXY}" https://www.google.com > /dev/null 2>&1; then
    echo "⚠️  警告：代理 ${HTTP_PROXY} 当前不可达"
    echo "   请确认："
    echo "   1. Windows 侧代理软件已启动且监听端口 ${PROXY_PORT}"
    echo "   2. 代理软件允许来自局域网的连接（Allow LAN / 局域网连接）"
    echo "   3. Windows 防火墙未拦截该端口"
    echo ""
    read -r -p "   是否仍要继续配置？(y/N) " answer
    if [[ ! "${answer}" =~ ^[Yy]$ ]]; then
        echo "❌ 已取消"
        exit 1
    fi
fi

# ---------- 备份旧配置 ----------
sudo mkdir -p "${CONF_DIR}"
if [[ -f "${CONF_FILE}" ]]; then
    sudo cp "${CONF_FILE}" "${BACKUP_FILE}"
    echo "📦 已备份旧配置到 ${BACKUP_FILE}"
fi

# ---------- 写入配置 ----------
echo "📝 写入 Docker systemd 代理配置..."
sudo tee "${CONF_FILE}" > /dev/null <<EOF
[Service]
Environment="HTTP_PROXY=${HTTP_PROXY}"
Environment="HTTPS_PROXY=${HTTPS_PROXY}"
Environment="NO_PROXY=${NO_PROXY}"
EOF

# ---------- 重载并重启 ----------
echo "🔄 重载 systemd 并重启 Docker..."
sudo systemctl daemon-reload
sudo systemctl restart docker

# ---------- 验证 ----------
echo "✅ 配置完成！当前 Docker 代理环境："
systemctl show docker --property=Environment --no-pager

echo ""
echo "🧪 测试拉取镜像..."
time docker pull nginx:alpine

echo ""
echo "🎉 全部完成！"
echo "   如需回滚: sudo cp ${BACKUP_FILE} ${CONF_FILE} && sudo systemctl restart docker"