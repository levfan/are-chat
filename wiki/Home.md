# are-chat Wiki

> 本页回答：这个项目是什么、wiki 里有什么、新 agent 应按什么顺序开工。

## 30 秒项目简介

are-chat 是即时通讯应用**天枢**（英文代号 DUBHE）的 Spring Boot 后端（前端为独立仓库 `are-chat-web`，Vue 3）。产品名只允许从 `sharedkernel.Brand` 取（欢迎语、审批推送、系统消息、日志前缀都走它），别再往字符串里写死站名。两条业务主线：

1. **IM 基础线**：注册审批制账号体系、好友关系、私信（撤回/编辑/表情回应/置顶/附件）、聊天室 WebSocket、公告与管理员运营（审批/用户/公告）。
2. **情侣空间核心线**：一对一绑定空间（`couple_space`/`couple_invite`）之上，经 2026-10-05 与 2026-10-20 多轮裁剪只保留**六张功能卡 + 地基**——每日一问、连续互动打卡（含六档解锁）、亲密日历、愿望清单、经期记录、亲密记录，加邀请建立 / 空间本体（宣言·主题·爱称·在一起的日子）/ 心动值。这些之外的心情日记、贴贴、求抱抱、安全词、饭桌、家务轮盘、加班留灯、愿望券、好事簿、刮刮乐盲盒、共同日历、积分台账、卡片收藏与搜索、通知中心与运营看板，以及百日隐藏回顾、心动时刻全部下线（需求见 `docs/adr/0007`，裁剪取舍见 `docs/adr/0010`）。配 **2 个定时任务**与 **15 个** 情侣 WS 事件（含 `pet-name-changed`）；WS 只推在线端，`couple_notify` 表保留但已无落库代码。日历卡不推事件，走 `CoupleCalendarService` 那一个月度只读口。

技术底座：Java 25 + Spring Boot 4.1.1 + MyBatis-Plus 3.5.17 + Flyway + MariaDB（测试 H2）；当前 **21 张表（8 张 `couple_*`，其中 `couple_notify`/`message_star` 已无代码 = 19 个仓储端口 = 19 个适配器）**、全仓 76 个端点中 couple 侧 **7 个 Controller / 28 个端点**，`mvn -o test` **437 用例**全绿（2026-10-20 四轮裁剪 + 经期记录 + 亲密记录 + 亲密日历后实测 `Tests run: 437, Failures: 0, Errors: 0, Skipped: 0` + BUILD SUCCESS）。

## 目录

| 页面 | 回答什么问题 |
|---|---|
| [architecture.md](architecture.md) | 技术栈版本、分层模式、鉴权、统一返回、WS 推送、Flyway 策略、配置要点 |
| [modules.md](modules.md) | `com.smart.chat` 各包的职责、关键类、入口 |
| [couple-space.md](couple-space.md) | 现役六张功能卡 + 地基与各自 Controller/Service/表、六档解锁与打卡口径、心动值五项与七级阈值、经期周期推算、亲密记录口径、月度日历读模型、内容库 |
| [database.md](database.md) | 数据约定、V1→V55 迁移时间线、现役 21 张表清单（按上下文分组）、随 ADR-0010 下线的 16 张表 |
| [api.md](api.md) | 全部 Controller 的 REST 接口总表 |
| [scheduled-jobs.md](scheduled-jobs.md) | 所有定时任务：时间、做什么、推什么事件 |
| [dev-guide.md](dev-guide.md) | 环境、构建测试命令、硬性流程摘要、产品红线 |

## 新 agent 开工顺序

1. **先读本 wiki**：[Home.md](Home.md) → [architecture.md](architecture.md)，需要业务背景时读 [couple-space.md](couple-space.md) / [database.md](database.md)——wiki 提供"理解项目 why/what 的叙述性全景"。
2. **再加载 skill**：仓库内 `.agents/skills/are-chat-map/SKILL.md`——它是**改代码前必读的规范全集与速查地图**（分层模板、命名规范、交付门禁、红线）；任务触达对应领域时再加载专项 skill：`db-migration`、`lombok-data`、`git-commit`。
3. 动手前用 wiki 的 [api.md](api.md) / [modules.md](modules.md) 定位同类功能，照抄该功能域的分层实现。

**分工说明**：skill 是规范与速查的"单一事实源"，本 wiki 不复述"怎么写代码"类规则，只做必要指向；两者互补、冲突时以 skill 与代码为准（wiki 若有滞后以 skill 更新为准）。
