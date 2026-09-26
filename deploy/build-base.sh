#!/usr/bin/env bash
# 构建 are-chat 基础镜像（nginx + JRE 25，Ubuntu Noble）
# 产物: are-chat-base:${VERSION:-1.0.0}
# 用法: ./deploy/build-base.sh   （VERSION=1.2.0 ./deploy/build-base.sh 指定版本）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="${VERSION:-1.0.0}"

docker build --progress=plain \
  -t "are-chat-base:${VERSION}" \
  -f "$SCRIPT_DIR/Dockerfile.base" "$SCRIPT_DIR"
