---
name: release-assets
description: 后端 are-chat 只有 major 版本更新（X.0.0，含 rc 等后缀）才触发的发布纪律：重建后端 jar 与业务镜像 tar、同步 are-chat GitHub Releases 的 assets 并写 SHA-256；minor/patch 不自动发版。前端 are-chat-web 的静态包由同级仓库的 release-assets skill 负责。
whenToUse: pom.xml 判到 major 升级且验证提交完成后；用户明确说后端 major 发版、传 assets；git-commit 判到 major 时。
agent_created: true
---

# are-chat Release Assets 同步

## 触发条件

- 只有 major（X.0.0，含 `-alpha` / `-beta` / `-rc` 后缀）自动触发；minor/patch 不跑
- 版本唯一来源是 `pom.xml` 的 `<version>`；`deploy/build*.sh` 与 `deploy/compose/opt/.env` 的 `VERSION` 必须与之一致，否则脚本直接退出
- 判到 major 且验证全绿、改动已提交后才跑本技能；已跟踪工作区必须干净
- 同版本 Release 已存在时只补差异：资产尺寸与 SHA-256 都一致就跳过
- 版本号没变不跑；`--skip-build` 只用于复用现有产物排障
- 用户点名要给 minor/patch 发版时才加 `--force`；产物不入库
- 前端静态包不在这里发：`are-chat-web` 仓库有自己的 `release-assets` skill，两端 major 版本对齐后各自发版

## 一条命令

```bash
python .agents/skills/release-assets/scripts/release_assets.py
```

- `--skip-build`：复用现有 `deploy/app.jar` 与 `deploy/are-chat-<版本>.tar`，不重跑构建
- `--allow-dirty`：仅排障用，跳过已跟踪文件洁净检查
- `--no-upload`：只出本地资产与 SHA-256，不碰 GitHub
- `--force`：越过 major 闸门，仅用于用户明确要求发 minor/patch

## 脚本动作

- 把旧的 `deploy/app.jar` 与 `deploy/are-chat-<版本>.tar` 移到 `.workbuddy/release-backup-v<版本>-<时间戳>/`，绕开批量删除保护
- `mvn -q -DskipTests package`（显式指定 JDK 25），把 `target/are-chat-<版本>.jar` 复制成 `deploy/app.jar`
- 在 WSL2（或 Git Bash）中跑 `deploy/build.sh --skip-base`，产出 `deploy/are-chat-<版本>.tar`
- 复制两个资产到 `target/release-assets/v<版本>/`，逐项算 SHA-256
- 建或找 `v<版本>` Release，写说明，上传 / 替换 / 跳过资产，最后复核清单

## 硬约束

- 脚本开头判 major；非 X.0.0 直接退出，没有 `--force` 不许发
- 令牌只从环境变量或 `git credential fill` 取，禁止写进仓库与日志
- 带 `-alpha` / `-beta` / `-rc` 的版本标 prerelease，正式版不标
- 资产名固定为 `are-chat-<版本>-app.jar` 与 `are-chat-<版本>-image.tar`，不许改名或增删
- `deploy/compose/opt/.env` 与 MariaDB init SQL 含真实口令，**绝不打包进 Release 资产**；部署凭据由运维单独分发
- GitHub 偶发 500 由脚本重试；最终失败保留本地资产并报 HTTP 码与资产名
- 上传前先确认已跟踪工作区干净，避免把半截改动发出去

## 与提交纪律衔接

- 判档仍归 `git-commit`；只有判到 major 才接本技能，不再等用户开口
- 汇报必须给 Release URL、两个资产名、以及 SHA-256 是否一致；前端 Release 由 `are-chat-web` 的 skill 单独汇报
- 详细流程、API 坑位与排障口径见 `references/release-flow.md`
