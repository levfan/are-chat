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

## Flyway 执行机制（本仓库已启用，禁止绕开）

- 除下文「脚本内容要求」第 6 项列的 `locations` 与 `baseline-version` 外，本仓库还设了 `baseline-on-migrate=true`、`clean-disabled=true`、`spring.sql.init.mode=never`：schema.sql 已退役为结构文档，**不要恢复 sql.init，也不要在 V 脚本之外建表**
- Boot 4 中 Flyway **先于** `spring.sql.init` 执行；已应用的脚本记在 `flyway_schema_history` 里，启动只跑未应用的那些——所以禁止手工到目标库执行脚本
- 全新空库：从 V1 顺序执行即可建全（`V1__add_couple_space.sql` = couple 系列表，`V2__legacy_tables_baseline.sql` = 存量业务表基线）；链长一律现扫 `db/` 取最大版本号，别信任何写死的数字
- 存量老库（已有表、无 history 表）首启自动打 **0 基线**，随后从 V1 全量重放——这就是每个脚本必须幂等的由来
- 排障备忘：Boot 4 对 Flyway 12 验证过的 H2 版本是 2.3.232，测试库换成 H2 2.4 会多一条版本较新的 WARN，无害，忽略即可

## 位置与命名

- 目录：`src/main/resources/db/`（目录不存在则创建）
- 文件名：`V{版本号}__{描述}.sql`，中间是两个下划线（Flyway 风格）
- 示例：`V1__init_table.sql`、`V2__change_user.sql`
- 版本号规则：先扫描 `db/` 下已有 `V*.sql`，取最大版本号 +1；禁止重用、跳号，已入库的脚本内容不得再修改（发现写错，用新的版本号写修正脚本）
- **草稿冻结**：还没 commit 的 V 脚本一旦被任何库执行过（本地 H2、远端联调库都算），内容同样就地冻结——再改就是校验和漂移，那批库下次启动 `Validate failed: checksum mismatch` 直接起不来（2026-10-05 V53 实测）。要改只有两条路：新增版本号写修正脚本，或把草稿库 `flyway_schema_history` 里那行删掉/repair 后重放。所以写 V 脚本要一次成型，先跑库后改内容前先想清楚有没有库吃过它
- 描述：小写 snake_case 英文，见名知意（如 `add_friend_index`、`init_admin_user`）

## 脚本内容要求

1. 只前进不回滚：增量脚本只描述"从上一版本到本版本"的变更。
2. 双兼容：必须同时兼容 MariaDB（生产外部库）与 H2 MODE=MySQL（内存库回退方案，测试也用它）。H2 语法约束参考 `src/main/resources/schema.sql` 头部注释，例如：带 CHARACTER SET/COLLATE 的列，DEFAULT 必须写在字符集声明之前。尽量写成可重复执行：`CREATE TABLE IF NOT EXISTS`；MariaDB 与 H2 支持 `ADD COLUMN IF NOT EXISTS`。每种 DDL 的幂等写法（数字是 56 个现役脚本里的实测用量）：
   - 建表：``CREATE TABLE IF NOT EXISTS``——现役 316 条建表语句全部带 `IF NOT EXISTS`，无一例外
   - 加字段：``ALTER TABLE `t` ADD COLUMN IF NOT EXISTS `c` ...``，`IF NOT EXISTS` 紧贴 `ADD COLUMN`、写在列定义**之前**（现役 10 条加列里 6 条这么写）。**不要**把它挪到列定义末尾——`V5__add_couple_pact_city_fund.sql` 文件头记着实测：H2 MODE=MySQL 不认 MariaDB 方言的末尾写法，直接 42000 语法错。剩下 4 条干脆不加，靠 history 挡重复执行（V5 明说这策略），代价是手工重跑会报 duplicate column
   - 加索引：优先内联在表定义里（340 处 `KEY` / `UNIQUE KEY`），确需后补只用中性的 ``CREATE INDEX `idx_x` ON `t` (`c`)``（只有 V49 的 4 条）。**不要**用只有 MariaDB 支持的 `CREATE OR REPLACE INDEX`，也**不要**用只有 H2 支持的 `CREATE INDEX IF NOT EXISTS`——这是 V49 头部写下的实测取舍；重复执行靠 Flyway history 兜底。索引与唯一键名全库唯一，命名规则见 `are-chat-map` 第四节
   - 删列：用 `` ALTER TABLE `t` DROP COLUMN IF EXISTS `c` ``（注意是 `IF EXISTS` 不是 `IF NOT EXISTS`）——`V53__trim_couple_space_to_v8_features.sql` 里就这么写过，双端跑得通
   - 改列（`MODIFY` / `CHANGE`）：两端都没有 `IF ... EXISTS` 形态，靠 history 保证只执行一次；写完必须 `mvn test` 在 H2 上真跑一遍
   - 种子数据：要可重复执行用 ``INSERT IGNORE INTO`` 配固定 36 位 UUID 主键。注意现役 56 个脚本里一条 `INSERT` 都没有，这写法在本仓未经 H2 验证——落笔前先在 H2 上跑一次，报错就退回「靠 history 只跑一次」
3. 初始化数据：INSERT 必须显式列出列名；时间戳用毫秒 bigint（与现有 `created` 字段约定一致）；主键用 36 位 UUID 字符串。
4. 同步全量结构文档：涉及表结构变更时，必须同步更新 `src/main/resources/schema.sql` 中对应表的定义。schema.sql 仅作对照用的全量结构文档，运行时不执行；外部库与 H2 测试库的表结构统一由 Flyway 执行 V 脚本得到，文档必须与脚本最终结构一致。纯数据初始化脚本不需要改 schema.sql。
5. 脚本内不要包含 `USE` 或切库语句；文件头用注释说明目的、涉及表、日期，并写明幂等靠什么保证（`IF NOT EXISTS`、`INSERT IGNORE`，还是 Flyway 按版本号只跑一次）。
6. 迁移由 Flyway 启动时自动执行：pom 已显式引入 spring-boot-flyway + flyway-core/flyway-mysql（Boot 4 必须显式引入该模块，否则依赖不生效），`spring.flyway.locations=classpath:db`、`baseline-version=0`（存量老库首启自动打 0 基线后从 V1 全量执行——这也是每个 V 脚本必须幂等的原因）；不要再引入其它迁移工具，也不要手工去目标库按序执行脚本。

## 示例

```sql
-- V3__change_user.sql
-- 目的：app_user 增加邮箱字段；2024-06-01；幂等：ADD COLUMN IF NOT EXISTS
ALTER TABLE `app_user`
    ADD COLUMN IF NOT EXISTS `email` varchar(100) DEFAULT NULL CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '邮箱，全局唯一';
```

完成后同步在 `schema.sql` 的 `app_user` 定义中加上同一字段（那里不加 `IF NOT EXISTS`，与文件内其它表保持一致）。

后补索引照 `V49__add_message_and_request_indexes.sql` 的写法——只用两端都认的中性语法，靠 history 保证只跑一次：

```sql
-- V49__add_message_and_request_indexes.sql
-- 目的：private_message 与 friend_request 补二级索引（建表时只有 PRIMARY KEY(id)，会话查询全表扫）
-- 涉及表：private_message、friend_request（仅加索引，不动列与数据）
-- 兼容：只用语义中性的 `CREATE INDEX ... ON ...`，MariaDB 与 H2(MODE=MySQL) 都支持；
--       不用 MariaDB 才有的 `CREATE OR REPLACE INDEX`，也不用 H2 才有的 `CREATE INDEX IF NOT EXISTS`。
-- 日期：2026-10-03
-- 说明：Flyway 按版本号执行，本脚本每个库只跑一次；若中途失败需重跑，请先人工确认索引是否已建。
CREATE INDEX `idx_pm_from_to_created` ON `private_message` (`from_user`, `to_user`, `created`);
```
