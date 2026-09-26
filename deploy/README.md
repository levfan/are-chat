# are-chat 一体化构建与部署（新策略）

> 旧的方案（根目录 `Dockerfile`，在容器内跑 Maven/Node 多阶段构建）已弃用，仅作参考。
> 新策略：**jar 与 dist 都由你在 Windows 上构建好，手动放入 `deploy/`；Docker 只负责打镜像与运行**。
> 镜像分两层：基础镜像（nginx + JRE，一次构建长期复用）+ 业务镜像（只 COPY 产物，秒级完成）。

## 目录约定（deploy/）

```
are-chat/deploy/
├── app.jar             ← 【你准备】后端 jar（Windows 上 mvn package 后复制改名，见第 1 步）
├── dist/               ← 【你准备】前端构建产物（are-chat-web/dist 整个目录复制过来，见第 2 步）
├── Dockerfile.base     基础镜像：JRE 25 + nginx（一般不用动）
├── Dockerfile.business 业务镜像：基于基础镜像 COPY 上面两个产物 + nginx/start 配置
├── build.sh            一键：基础镜像 + 业务镜像（支持 --skip-base / --up）
├── build-business.sh   日常：只打业务镜像（base 已存在时）
├── build-base.sh       仅打基础镜像
├── docker-compose.yml  部署：纯镜像启动，不在 compose 里构建
├── .env                compose 变量：VERSION / APP_PORT（宿主机端口默认 58080）
├── nginx.conf          nginx 主配置（静态资源 + /api、/ws 反代）
├── start.sh            容器入口：Spring Boot(127.0.0.1:8080) + nginx(80)
├── maven-settings.xml  可选：Windows 上 mvn 构建走阿里云加速
└── set-docker-proxy.sh 可选：WSL2 Docker 代理一键配置
```

`app.jar` 与 `dist/` 是本机构建产物，已在 `are-chat/.gitignore` 忽略，不会提交到 git。

## 完整流程

```
Windows（准备产物，每次发版前按需执行）
  are-chat:      mvn -s deploy/maven-settings.xml -DskipTests package
                 → 把 target\are-chat-1.0.0.jar 复制为 deploy\app.jar
  are-chat-web:  pnpm install --frozen-lockfile && pnpm build
                 → 把 dist\ 整个目录复制到 are-chat\deploy\dist\

WSL2（打镜像）
  ① build-base.sh     → are-chat-base:$VERSION（nginx + JRE 25，基本不变）
  ② build-business.sh → are-chat:$VERSION（一体包，秒级）
                          ↓
  docker compose -f deploy/docker-compose.yml up -d
  访问 http://localhost:58080/
```

### 第 1 步：准备后端 jar（Windows，CMD/PowerShell）

```bat
cd /d D:\00.personal\4.code\are-chat
mvn -s deploy\maven-settings.xml -DskipTests package
copy /y target\are-chat-1.0.0.jar deploy\app.jar
```

（`maven-settings.xml` 走阿里云仓库加速；用你自己的 settings 也行，只要最终把 jar 复制为 `deploy\app.jar`。）

### 第 2 步：准备前端 dist（Windows）

```bat
cd /d D:\00.personal\4.code\are-chat-web
pnpm install --frozen-lockfile
pnpm build
robocopy dist ..\are-chat\deploy\dist /MIR
```

（`/MIR` 镜像同步，会清掉 `deploy\dist` 里上次遗留的多余文件。）

### 第 3 步：打镜像（WSL2）

```bash
cd /mnt/d/00.personal/4.code/are-chat

./deploy/build.sh                 # 首次：基础镜像 + 业务镜像
./deploy/build.sh --skip-base     # 日常：base 没变，只重打业务镜像
./deploy/build-business.sh        # 同上（只打业务镜像）
./deploy/build.sh --up            # 打完直接 docker compose 启动
```

### 第 4 步：启动与访问

```bash
docker compose -f deploy/docker-compose.yml up -d
```

- 访问入口：`http://localhost:58080/`（宿主机 58080 → 容器内 nginx 80；`/api`、`/ws` 反代到容器内 8080 后端，后端仅监听 127.0.0.1）
- 宿主机端口：`deploy/.env` 的 `APP_PORT`；镜像版本：`VERSION`（shell 环境变量优先于 .env）
- 上传文件持久化在 named volume `uploads`（容器内 `/data/uploads`）
- 连接 MySQL：在 `deploy/docker-compose.yml` 里取消 `SPRING_PROFILES_ACTIVE: mysql` 等注释并填入真实信息

## 版本升级

1. 基础镜像 + 业务镜像一起升级：`VERSION=1.2.0 ./deploy/build.sh`；
2. 只升业务镜像：`VERSION=1.2.0 ./deploy/build.sh --skip-base`；
3. 同步修改 `deploy/.env` 的 `VERSION`（或启动时 `VERSION=1.2.0 docker compose ...`），保证 compose 引用的 tag 与镜像一致。

## 常见问题

- **`docker: command not found`**：脚本需在装了 docker 的 WSL2 里运行；Docker Desktop 记得开启 WSL 集成。
- **拉取基础镜像慢/失败**：先在 WSL2 里跑一次 `deploy/set-docker-proxy.sh` 配置代理。
- **提示缺 app.jar / dist**：按第 1、2 步把产物放入 `deploy/` 再跑。
- **改了 nginx.conf / start.sh**：重跑 `build-business.sh`（业务镜像层）即可，无需重建 base。
- **改了 JRE/nginx 基线**：修改 `Dockerfile.base` 后跑 `build-base.sh`（或 `build.sh` 全量）。
- **容器健康检查失败**：探测的是容器内 127.0.0.1:8080，确认 jar 与后端端口未被改动。
