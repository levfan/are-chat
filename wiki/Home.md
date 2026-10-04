# are-chat Wiki

> 本页回答：这个项目是什么、wiki 里有什么、新 agent 应按什么顺序开工。

## 30 秒项目简介

are-chat 是一个**情侣向即时社交应用**的 Spring Boot 后端（前端为独立仓库 `are-chat-web`，Vue 3）。两条业务主线：

1. **IM 基础线**：注册审批制账号体系、好友关系、私信（撤回/编辑/表情回应/收藏/心动时刻/置顶/附件）、聊天室 WebSocket、公告与管理员运营。
2. **情侣空间核心线**：一对一绑定空间之上叠加 **14 张情绪价值功能卡**——心情日记、贴贴宫格、求抱抱、安全词与暂停复盘、今晚饭桌、家务轮盘、加班预报与留灯、好事簿、愿望券本、刮刮乐与盲盒（2026-10-04 由约 140 个功能域排序裁剪而来，打分与落选理由见 `docs/couple-trim-ranking.md`），加 2026-10-05 v8 第一批的连续互动打卡、每日一问、愿望清单与百日隐藏回顾（需求与取舍见 `docs/adr/0007`）；配 4 个定时任务与统一 WS 事件推送 + `couple_notify` 落库通知中心。

技术底座：Java 25 + Spring Boot 4.1.1 + MyBatis-Plus 3.5.17 + Flyway + MariaDB（测试 H2）；当前 **35 张表（22 张 `couple_*`）**、couple 侧 **17 个 Controller / 70 个端点**，`mvn -o test` **266 用例**全绿（2026-10-05 实测 `Tests run: 266, Failures: 0, Errors: 0, Skipped: 0` + BUILD SUCCESS）。

## 目录

| 页面 | 回答什么问题 |
|---|---|
| [architecture.md](architecture.md) | 技术栈版本、分层模式、鉴权、统一返回、WS 推送、Flyway 策略、配置要点 |
| [modules.md](modules.md) | `com.smart.chat` 各包的职责、关键类、入口 |
| [couple-space.md](couple-space.md) | 现役 14 张卡与各自 Controller/Service/表、七档解锁与打卡口径、心动值与积分口径、内容库 Bank |
| [database.md](database.md) | 数据约定、V1→V52 迁移时间线、全表清单（按域分组） |
| [api.md](api.md) | 全部 Controller 的 REST 接口总表 |
| [scheduled-jobs.md](scheduled-jobs.md) | 所有定时任务：时间、做什么、推什么事件 |
| [dev-guide.md](dev-guide.md) | 环境、构建测试命令、硬性流程摘要、产品红线 |

## 新 agent 开工顺序

1. **先读本 wiki**：[Home.md](Home.md) → [architecture.md](architecture.md)，需要业务背景时读 [couple-space.md](couple-space.md) / [database.md](database.md)——wiki 提供"理解项目 why/what 的叙述性全景"。
2. **再加载 skill**：仓库内 `.agents/skills/are-chat-map/SKILL.md`——它是**改代码前必读的规范全集与速查地图**（分层模板、命名规范、交付门禁、红线）；任务触达对应领域时再加载专项 skill：`db-migration`、`lombok-data`、`git-commit`。
3. 动手前用 wiki 的 [api.md](api.md) / [modules.md](modules.md) 定位同类功能，照抄该功能域的分层实现。

**分工说明**：skill 是规范与速查的"单一事实源"，本 wiki 不复述"怎么写代码"类规则，只做必要指向；两者互补、冲突时以 skill 与代码为准（wiki 若有滞后以 skill 更新为准）。
