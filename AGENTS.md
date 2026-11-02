# are-chat 项目规则（Spring Boot 后端）

本文件只做入口，不复述细则：规则一律以 `.agents/skills/` 下的 SKILL.md 为准，命中场景先读再做，不要靠记忆或通读源码。

| 场景 | 必读 |
|------|------|
| 开发功能、修复缺陷、评审改动（开工前） | [.agents/skills/are-chat-map/SKILL.md](.agents/skills/are-chat-map/SKILL.md) — 模块地图、分层模板、数据层规范、交付门禁 |
| 改数据库表结构，或要向表里初始化数据 | [.agents/skills/db-migration/SKILL.md](.agents/skills/db-migration/SKILL.md) — V 脚本命名、幂等与双兼容、schema.sql 同步 |
| 新建或修改实体、DTO、VO、配置属性类 | [.agents/skills/lombok-data/SKILL.md](.agents/skills/lombok-data/SKILL.md) — 注解选型与 MyBatis-Plus/fastjson2 注意事项 |
| 提交与推送（含提交前构建门禁） | [.agents/skills/git-commit/SKILL.md](.agents/skills/git-commit/SKILL.md) — 架构师复审清单、拆分原则、commit 格式、pull 后 push |

长期约束与产品红线见 [CONTEXT.md](CONTEXT.md)；本文件每轮自动进上下文，所以以上四个场景只要出现，就必须先加载对应 skill。
