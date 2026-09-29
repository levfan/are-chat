# are-chat 项目规则（Spring Boot 后端）

## 项目地图（每个任务开工前必读）

- 凡在本项目开发功能、修复缺陷、评审改动：先加载 `.agents/skills/are-chat-map/SKILL.md`（项目模块地图、代码惯例、构建命令），不要重新通读源码
- 功能完成后若新增/删除了模块、表、接口、定时任务：必须同步更新 `are-chat-map` skill 的对应小节，与功能同批提交（docs 不单独成 commit 时随 feat 一起提）

## 数据库变更（硬性）

- 凡修改数据库表结构（建表、字段/索引/约束增删改）或需要初始化表数据的任务：必须在 `src/main/resources/db/` 下生成 `V{版本}__{描述}.sql` 增量脚本，并同步更新 `src/main/resources/schema.sql` 中对应表定义
- 版本号 = db 目录已有 V*.sql 最大版本号 + 1；已入库脚本内容不得再修改
- 完整规范：`.agents/skills/db-migration/SKILL.md`

## Java 数据类（Lombok，硬性）

- 新建实体/DTO/VO/配置属性类：用 Lombok `@Data`（只需访问器时用 `@Getter`/`@Setter`），禁止手写成堆的 getter/setter
- 需要身份比较的实体显式 `@EqualsAndHashCode(of = "id")`；有继承的加 `callSuper = true`
- 完整规范：`.agents/skills/lombok-data/SKILL.md`

## Git 提交（硬性）

- 每完成一个功能必须产生 commit：按改动性质分组，一类一个 commit（依赖/配置、数据库脚本、后端代码、测试、文档）；整体改动小且不可拆时可并为一个
- 数据库脚本独立成 commit，不与业务代码混提
- 提交信息：`type(scope): 中文描述`（type：feat/fix/db/refactor/perf/test/docs/chore/build/ci）
- 完整规范：`.agents/skills/git-commit/SKILL.md`
- 提交前 `mvn -q compile`（涉及测试改动跑 `mvn test`）通过；commit 后先 `git pull --no-rebase` 再 push 到远端（pull/push 失败保留本地 commit 并报告）
