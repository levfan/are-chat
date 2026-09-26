---
name: db-migration
description: are-chat 数据库增量脚本规范：凡修改数据库表结构（建表、字段/索引/约束增删改）或需要向表中初始化数据，必须在 src/main/resources/db/ 下生成 V{版本}__{描述}.sql 增量脚本，并同步维护 schema.sql 基线
whenToUse: 任务涉及数据库表结构变更（CREATE/ALTER/DROP TABLE、字段、索引、约束）或需要向表中写入初始化数据时使用
---

# 数据库增量脚本规范（db-migration）

## 触发条件（满足任一，必须产出增量脚本）

- 修改数据库表结构：新建表、新增/修改/删除字段、索引、唯一约束等
- 需要向表中初始化数据（种子数据）

只改 Java 代码、配置或 Mapper，不动数据库的，不需要生成脚本。

## 位置与命名

- 目录：`src/main/resources/db/`（目录不存在则创建）
- 文件名：`V{版本号}__{描述}.sql`，中间是两个下划线（Flyway 风格）
- 示例：`V1__init_table.sql`、`V2__change_user.sql`
- 版本号规则：先扫描 `db/` 下已有 `V*.sql`，取最大版本号 +1；禁止重用、跳号，已入库的脚本内容不得再修改（发现写错，用新的版本号写修正脚本）
- 描述：小写 snake_case 英文，见名知意（如 `add_friend_index`、`init_admin_user`）

## 脚本内容要求

1. 只前进不回滚：增量脚本只描述"从上一版本到本版本"的变更。
2. 双兼容：必须同时兼容 MariaDB（生产外部库）与 H2 MODE=MySQL（内存库回退方案，测试也用它）。H2 语法约束参考 `src/main/resources/schema.sql` 头部注释，例如：带 CHARACTER SET/COLLATE 的列，DEFAULT 必须写在字符集声明之前。尽量写成可重复执行：`CREATE TABLE IF NOT EXISTS`；MariaDB 与 H2 支持 `ADD COLUMN IF NOT EXISTS`。
3. 初始化数据：INSERT 必须显式列出列名；时间戳用毫秒 bigint（与现有 `created` 字段约定一致）；主键用 36 位 UUID 字符串。
4. 同步全量基线：涉及表结构变更时，必须同步更新 `src/main/resources/schema.sql` 中对应表的定义（H2 内存库启动时自动执行的是 schema.sql，外部库靠 V 脚本手工执行，两条路径的最终结构必须一致）。纯数据初始化脚本不需要改 schema.sql。
5. 脚本内不要包含 `USE` 或切库语句；文件头用注释说明目的、涉及表、日期。
6. 本项目未启用 Flyway：不要添加 flyway 依赖或配置，脚本按版本号顺序由人工/部署流程在目标库执行。

## 示例

```sql
-- V3__change_user.sql
-- 目的：app_user 增加邮箱字段；2024-06-01
ALTER TABLE `app_user`
    ADD COLUMN `email` varchar(100) DEFAULT NULL CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '邮箱，全局唯一';
```

完成后同步在 `schema.sql` 的 `app_user` 定义中加上同一字段。
