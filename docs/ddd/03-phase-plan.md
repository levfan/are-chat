# 03 · 执行计划与改造状态表

> 本页回答：分几步做、每步的验收判据是什么、现在每个上下文改造到哪一层了。
> 原则：**每步都要能编译、测试全绿才提交**；行为不变的步骤绝不夹带逻辑修改；半改完的状态不允许散落，一律登记在下面的状态表里。

## 一、阶段

| 阶段 | 内容 | 行为变化 | 验收判据 | 提交粒度 |
|---|---|---|---|---|
| **A 战略分层** | 146 个文件按 `04-move-map.md` 迁入 5 上下文 × 四层 + `sharedkernel` + `bootstrap`；改写 `package`/`import`，补齐因搬包而缺失的同包引用 import；测试类跟着镜像 | **无**（纯移动） | 每个上下文一次 `mvn -q -o clean compile` EXIT=0；阶段末 `mvn -o test` 158 用例 0 失败 EXIT=0 | 一上下文一 commit |
| **B 核心域战术改造** | 仅 `couple`：实体改名 `*PO` 落 persistence；抽 `CoupleSpace`/`Intimacy` 聚合与 `IntimacyCalculator` 策略；声明 `CoupleSpaceRepository`/`PointLedgerRepository`/`CoupleEventPublisher` 端口并写适配器；Service 退化为编排 | 有，但**必须行为等价**：现有 158 用例（含 `CoupleIntimacyTest` 锁的六项权重、单向贴贴不算一天、阶梯阈值）不动断言直接通过 | `mvn -o test` 全绿 + 心动值六项逐项断言仍锁台账真插一行 | 端口/聚合/适配器各一 commit |
| **C 破环** | 拆 `01` 第二节列出的 4 个包级环 | 有（依赖方向），无功能变化 | 重跑 `ddd-deps.mjs` 输出「无环」；先证明脚本**能测出环**（以现有 4 个环为阳性对照） | 一环一 commit |
| **D 守卫与文档** | `ArchitectureGuardTest` 四条规则 + allowlist；`EntityReflectionGuardTest` 加实体总数断言；更新 map skill / wiki / `CONTEXT.md` | 无 | 守卫插违规必红、还原必绿；文档与代码逐条对齐 | 测试与文档各一 commit |

不在本轮范围（明确不做，避免"铺开而半吊子"）：`messaging`/`identity`/`platform`/`filestorage` 的**战术改造**（聚合与端口）。它们只拿到 A 的分层骨架，状态在下表标为「战略分层」，由 allowlist 记账，未来按需求逐域推进。

## 二、改造状态表（唯一权威，改一处代码就改一行）

实测于 2026-10-04 Phase A/C/D 收官后（判据：`ArchitectureGuardTest` 4 条 + `.tmp-audit/ddd-deps.mjs` 现扫）：

| 上下文 | 分层实况 | domain 层 | 跨上下文 | 备注 |
|---|---|---|---|---|
| `identity` | api/application/domain/infrastructure | **有**（AccountDirectory、Account、AccountCascade、ProfileProvisioner、WelcomeMessenger、AdminAlerter、AdminNotifyChannel） | **零出向依赖**，纯上游 | 账号是谁都要问的通用域，方向理应当如此 |
| `messaging` | api/application/domain/infrastructure | **有**（发布语言 7 个：CoupleEventPublisher、PresenceReader、AnnouncementBroadcaster、PeerProfileReader、FriendshipChecker、OutboundNotifySink、NotifySinkRegistry） | → identity.domain | 传输细节（帧格式、payload、注册表）不再外泄 |
| `couple` | api/application/domain/infrastructure | **有**（CoupleSpace 聚合 + CoupleSpaceRepository 端口 + IntimacySource/IntimacyCalculator 策略） | → identity.domain、messaging.domain | 心动值六项加权与七级阶梯已从 CoupleService 整块搬进 domain；空间写路径（注销连带解散）走聚合+仓储 |
| `platform` | api/application/infrastructure | 无 | → identity.domain、messaging.domain | 公告端点已从 identity 归位回来（路由未变） |
| `filestorage` | api/application/infrastructure | 无 | 只到 sharedkernel/bootstrap.properties | 4 个文件，暂无改造需求 |

**Phase B 待做的具体事**（写清楚，别让它变成"以后再说"）：19 个 `@TableName` 实体改名 `*PO`（ADR-0002）；抽 `couple.domain` 的 CoupleSpace 聚合、IntimacyCalculator（心动值六项加权，`CoupleIntimacyTest` 已锁死权重与阈值）、Repository 端口与适配器。**不动**的行为口径：WS 事件名、路由、响应字段、心动值分值。

## 2.5 验收台账（本轮实际退出码，不是计划）

| 步骤 | 命令 | 结果 |
|---|---|---|
| Phase A 搬包 | `mvn -q -o clean compile` / `mvn -o test` | EXIT=0 / 158 用例 0 失败 MVN_EXIT=0；纯移动证据：170 个移动文件里 169 个正文（剥 package/import 后）逐字节一致 |
| 竞态修复对照 | HEAD 独立 worktree 打同一处屏障后 `mvn -o test` | 158 用例 0 失败 EXIT=0（证明修复与搬包无关） |
| Phase C 断环 | `mvn -o test` | 158 用例 0 失败 MVN_EXIT=0；包级环 4 → 0 |
| 端口化 | `mvn -o test` | 158 用例 0 失败；跨上下文越界 24 → 0 |
| Phase D 守卫 | `mvn -o test` | **162 用例 0 失败 MVN_EXIT=0**（158 + 4 条守卫）；四条均做过反向注入变红，其中 domain 纯净一条曾为假绿已修 |
## 三、硬约束（全程适用，来自仓库既有红线）

- 不动数据库：本轮无表结构变化，**不产出 V 脚本**（若过程中发现必须动表，停下改走 db-migration 规范）。
- 不改对外契约：路由、请求体字段、响应 VO 字段、WS 事件名、`couple_notify` 语义全部原样。
- 不连远端 MariaDB：验证只用 H2（`mvn -o test` 走 `src/test/resources/application.yml`）。
- 通知类环境变量保持为空，测试不得真的推微信。
- 提交按性质分项，本轮**不 push**（与裁剪轮同口径，等人工过一遍）。
- `mvn` 一律 `-o`（离线），不新增依赖（本机仓库没有的包一律不碰）。

## 四、风险与对策

| 风险 | 对策 |
|---|---|
| 搬包后同包引用变跨包，漏 import 编译炸 | 生成器在改写阶段对「引用了某已知类且该类已改包但本文件无 import」自动补 import；以 `mvn -q -o clean compile` 为唯一判据 |
| Spring 扫描/装配行为变化 | 根包 `com.smart.chat` 不变：`@SpringBootApplication`、`@ConfigurationPropertiesScan`、`@MapperScan(basePackages="com.smart.chat")` 全部保持，实测无 XML namespace 需要改 |
| 实体改名 `*PO` 漏引用 | 改名与 import 改写同批，编译 + `EntityReflectionGuardTest`（加总数断言）双保险 |
| 心动值改造悄悄改算法 | Phase B 明确**不动** `CoupleIntimacyTest` 的断言；断言不过说明行为变了，必须回退而非改测试 |

### 2.6 Phase B 完成账（2026-10-04）

- B-1 改名：19 个实体 → `*PO`，818 处标识符、65 个文件，表名列名未动。
- B-2 领域层：`couple/domain/intimacy/{IntimacySource,IntimacyCalculator}`（六项权重 1/2/2/3/2/1 与阈值 0/50/150/300/500/800/1300 原样搬入，负数供数直接拒）、`couple/domain/space/{CoupleSpace,CoupleSpaceRepository}` + `infrastructure/persistence/CoupleSpaceRepositoryAdapter`（**只回写聚合持有的列**，cityA/cityB 有测试锁住不被清空）。`SpaceCascadeAdapter` 改走聚合。
- 新增 18 条测试（计算器 8 / 聚合 7 / 适配器 3），全仓 `mvn -o test` **180 用例 0 失败**；`CoupleIntimacyTest` 断言一字未改仍然通过 = 行为等价护栏生效。
- **仍待做的 B 部分（不含糊过去）**：其余 18 张表尚无聚合（好事簿/贴贴/安全词/饭票/轮盘/加班/愿望券/刮刮乐/盲盒等），目前仍是「Service 直接操作 PO」的事务脚本；`CoupleService` 里除心动值外仍兼着取数与拼装。按 ADR-0005 的节奏，等某张卡真要加规则时再抽，不预先造空聚合。
