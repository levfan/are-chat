# syntax=docker/dockerfile:1
# ⚠️ 已弃用：旧的一体化多阶段构建（Maven/Node 构建都在容器内执行）。
#    新流程：在 Windows 上构建好 jar 与前端 dist，放入 deploy/ 后用
#            deploy/build.sh（或 build-business.sh）打两层镜像，
#            文档见 deploy/README.md。本文件保留仅作参考，可安全删除。
#
# ============================================================================
# are-chat 前后端一体化镜像（构建文件都在后端项目内）
#
# 构建上下文：本目录（are-chat）
# 额外上下文：web = ../are-chat-web（前端源码，由 compose 的
#             additional_contexts 或 docker buildx --build-context 传入）
#
# 构建：cd are-chat && docker compose up -d --build
# ============================================================================

########## 1. 后端构建：Maven + JDK 25 ##########
FROM maven:3.9-eclipse-temurin-25 AS app-build
WORKDIR /build
# 阿里云镜像（构建加速）
COPY deploy/maven-settings.xml /root/.m2/settings.xml
# 先只拷 pom 预热依赖层：源码改动时无需重新下载依赖
COPY pom.xml ./pom.xml
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package && cp target/are-chat-*.jar /app.jar

########## 2. 前端构建：Node 26 + pnpm（源码取自 web 额外上下文） ##########
FROM node:26-alpine AS web-build
WORKDIR /web
ARG NPM_REGISTRY=https://registry.npmmirror.com
RUN npm config set registry "$NPM_REGISTRY" \
 && npm install -g pnpm@11.21.0
# 只拷贝构建所需文件（不拷本机 node_modules/dist）
COPY --from=web package.json pnpm-lock.yaml pnpm-workspace.yaml index.html vite.config.ts tsconfig.json vitest.setup.ts playwright.config.ts ./
COPY --from=web src ./src
COPY --from=web tests ./tests
COPY --from=web e2e ./e2e
# pnpm build = vite build + vue-tsc 类型检查；pnpm-workspace.yaml 已放行 esbuild 构建脚本
RUN pnpm install --frozen-lockfile && pnpm build

########## 3. 运行时：单镜像同时跑 nginx 与 Spring Boot ##########
FROM eclipse-temurin:25-jre-noble
RUN apt-get update \
 && apt-get install -y --no-install-recommends nginx \
 && rm -rf /var/lib/apt/lists/* \
 && rm -f /etc/nginx/sites-enabled/default

COPY --from=app-build /app.jar /app/app.jar
COPY --from=web-build /web/dist /usr/share/nginx/html
COPY deploy/nginx.conf /etc/nginx/nginx.conf
COPY deploy/start.sh /usr/local/bin/start.sh
RUN chmod +x /usr/local/bin/start.sh && mkdir -p /data/uploads

# 上传目录持久化到卷；后端只监听 127.0.0.1，外部统一走 nginx 80 端口
ENV ARECHAT_STORAGE_BASE_DIR=/data/uploads \
    TZ=Asia/Shanghai
VOLUME ["/data/uploads"]
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' 2>/dev/null || exit 1
CMD ["/usr/local/bin/start.sh"]
