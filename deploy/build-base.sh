#!/usr/bin/env bash
# 构建 are-chat 基础镜像（nginx + JRE 25，Ubuntu Noble）
# 产物: are-chat-base:${VERSION:-1.0.0}
#       构建成功后自动导出镜像 tar 包到脚本同目录: are-chat-base-${VERSION:-1.0.0}.tar
# 用法: ./deploy/build-base.sh   （VERSION=1.2.0 ./deploy/build-base.sh 指定版本）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="${VERSION:-1.0.0}"

docker build --progress=plain \
  -t "are-chat-base:${VERSION}" \
  -f "$SCRIPT_DIR/Dockerfile.base" "$SCRIPT_DIR"

# ---- 导出镜像 tar 包到脚本同目录（docker save -o 直接落盘，不 gzip、不走临时文件，/mnt/d 上最稳） ----
TAR_FILE="$SCRIPT_DIR/are-chat-base-${VERSION}.tar"
echo ">> 导出镜像 are-chat-base:${VERSION} → $TAR_FILE"
docker save -o "$TAR_FILE" "are-chat-base:${VERSION}"
ls -lh "$TAR_FILE"
echo "✅ 导出完成: $TAR_FILE"
