---
name: git-commit
description: 提交规范：每完成一个功能必须按改动性质拆分产生一个或多个 commit，pull 同步远端后 push，使用 Conventional Commits 中文描述，数据库脚本必须独立成 commit
whenToUse: 完成一个功能/修复需要提交代码，或用户要求生成 commit / 推送远端时使用
---

# Git 提交规范（git-commit）

## 基本要求

- 每完成一个功能（可独立验收的完整改动），只要产生了文件修改：必须形成 commit，pull 同步远端后再 push，不允许把完成的功能长期留在工作区不提交
- 一个功能 = 一个或多个 commit；多个功能禁止共用一个 commit
- 提交前必须确认可构建：后端 `mvn -q compile`（涉及测试改动跑 `mvn test`）；前端 `pnpm build`（涉及逻辑改动跑 `pnpm test`）
- 推送顺序固定：commit → `git pull --no-rebase` → push 当前分支的 upstream；没有 upstream 时用 `git push -u origin <分支名>`

## 拆分原则：先分组，再决定合并

判断标准是"改动性质"，不是文件数量：

1. 通读 `git status` + `git diff`，按性质分组：
   - 依赖与构建配置：pom.xml、package.json、pnpm-lock.yaml、application*.yml、Dockerfile、deploy/ 等
   - 数据库脚本：V*.sql 与 schema.sql 同步（两者必须放进同一个 commit）
   - 功能代码：同一功能模块的改动放一起，不同功能模块拆开
   - 测试：跟随对应功能的 commit；独立补测试可单独 `test:` 提交
   - 文档/杂项：README、.gitignore、deploy 脚本等
2. 合并规则：同一性质且逻辑上不可分割的改动合成一个 commit；整体改动很小、拆开后任一 commit 都无法独立编译/运行的，合并为一个
3. 硬性规则：数据库脚本永远独立成 commit，不与业务代码混提（部署需按序单独执行，必要时单独回滚）

## 提交信息格式

```
type(scope): 祈使句中文描述，不超过 50 字，结尾不加句号
```

- type：feat / fix / db / refactor / perf / test / docs / chore / build / ci；`db` 专用于数据库脚本提交
- scope：模块名。后端：auth、im、upload、friend、system、config 等；前端：chat、friend、profile、admin 等
- body（可选）：说明动机与影响，不逐文件罗列

示例（一个跨前后端的功能 = 3 个 commit）：

```
feat(im): 私信支持撤回两分钟内的消息
db(im): private_message 增加 recalled 字段（V4）
feat(chat): 聊天窗口展示撤回提示
```

## 操作纪律

- 分组后逐组 `git add <具体路径>` + `git commit`；仅当全部改动确属同一逻辑变更时才允许 `git add -A`
- 功能完成立即提交并推送（pull → push），不积压多个功能再补提交
- pull/push 失败（网络、凭据、冲突、非快进）时：保留本地 commit，向用户报告原因等待处理；冲突不自动解决，禁止 force push、自动 rebase 改写历史
- 不 rebase 已有历史、不使用 --no-verify、不 force push
- 用户对拆分/合并或推送时机有明确要求时，以用户要求优先
