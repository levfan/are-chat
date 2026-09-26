#!/usr/bin/env bash
# ============================================================================
# are-chat 一键打镜像（在 WSL2 中运行）
#
# 前置：构建产物已手动放入 deploy/（见 deploy/README.md）：
#   - app.jar ：Windows 上 mvn package 出的后端 jar
#   - dist/   ：前端 pnpm build 的产物目录
#
# 步骤：
#   1. 构建基础镜像  are-chat-base:$VERSION（nginx + JRE 25，--skip-base 跳过）
#   2. 构建业务镜像  are-chat:$VERSION（一体包）
#   3. （可选 --up）docker compose 启动
#
# 用法：
#   ./deploy/build.sh                    # 基础镜像 + 业务镜像
#   ./deploy/build.sh --skip-base        # base 没变，只重打业务镜像
#   ./deploy/build.sh --up               # 打完直接 compose 启动
#   VERSION=1.2.0 ./deploy/build.sh      # 指定镜像版本
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="${VERSION:-1.0.0}"

SKIP_BASE=0; RUN_UP=0
for arg in "$@"; do
  case "$arg" in
    --skip-base) SKIP_BASE=1 ;;
    --up)        RUN_UP=1 ;;
    *) echo "未知参数: $arg（支持 --skip-base --up）" >&2; exit 1 ;;
  esac
done

log() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }

command -v docker >/dev/null 2>&1 || { echo "错误: 未找到 docker（请在 WSL2 内运行本脚本）" >&2; exit 1; }
[ -f "$SCRIPT_DIR/app.jar" ] || { echo "错误: 缺少 $SCRIPT_DIR/app.jar —— 先在 Windows 上 mvn package，把 target/are-chat-*.jar 复制为 deploy/app.jar（见 deploy/README.md）" >&2; exit 1; }
[ -d "$SCRIPT_DIR/dist" ] || { echo "错误: 缺少 $SCRIPT_DIR/dist/ —— 先把前端构建产物（are-chat-web/dist）整个复制到 deploy/dist（见 deploy/README.md）" >&2; exit 1; }

# ---- 1. 基础镜像（nginx + JRE） ----
if [ "$SKIP_BASE" -eq 0 ]; then
  log "1/2 构建基础镜像 are-chat-base:${VERSION}"
  bash "$SCRIPT_DIR/build-base.sh"
else
  log "1/2 跳过基础镜像（沿用已有 are-chat-base:${VERSION}）"
fi

# ---- 2. 业务镜像（一体包） ----
log "2/2 构建业务镜像 are-chat:${VERSION}"
bash "$SCRIPT_DIR/build-business.sh"

# ---- 可选：compose 启动 ----
if [ "$RUN_UP" -eq 1 ]; then
  log "docker compose 启动"
  (cd "$SCRIPT_DIR/.." && VERSION="$VERSION" docker compose -f deploy/docker-compose.yml up -d)
  echo "访问 http://localhost:${APP_PORT:-58080}/"
fi

log "完成: are-chat:${VERSION}"
