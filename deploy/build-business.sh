#!/usr/bin/env bash
# 打最终业务镜像（日常构建入口，在 WSL2 中运行）
#
# 前置（产物放在 deploy/ 下，见 deploy/README.md）：
#   - app.jar  Windows 上 mvn package 出的后端 jar
#   - dist/    前端 pnpm build 的产物目录
#   - 基础镜像 are-chat-base:$VERSION 已存在（没有就先跑 build-base.sh 或 build.sh）
#
# 用法：
#   ./deploy/build-business.sh                # 打 are-chat:${VERSION:-1.0.0}
#   VERSION=1.2.0 ./deploy/build-business.sh  # 指定版本（需已有同版本 base）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="${VERSION:-1.0.0}"

if [ "$#" -gt 0 ]; then
  echo "本脚本不接收参数。需要 --skip-base / --up 请改用 build.sh" >&2
  exit 1
fi

command -v docker >/dev/null 2>&1 || { echo "错误: 未找到 docker（请在 WSL2 内运行本脚本）" >&2; exit 1; }
[ -f "$SCRIPT_DIR/app.jar" ] || { echo "错误: 缺少 $SCRIPT_DIR/app.jar —— 先在 Windows 上 mvn package，把 target/are-chat-*.jar 复制为 deploy/app.jar" >&2; exit 1; }
[ -d "$SCRIPT_DIR/dist" ] || { echo "错误: 缺少 $SCRIPT_DIR/dist/ —— 先把前端构建产物（are-chat-web/dist）整个复制到 deploy/dist" >&2; exit 1; }

docker build --progress=plain \
  --build-arg "BASE_IMAGE=are-chat-base:${VERSION}" \
  -t "are-chat:${VERSION}" \
  -f "$SCRIPT_DIR/Dockerfile.business" "$SCRIPT_DIR"
