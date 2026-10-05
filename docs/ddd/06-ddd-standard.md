# 06 · DDD 标准与完成度判定

> 本页回答两个问题：**「改成 DDD」的判据到底是什么**，以及**现在每一项到什么程度**。
> 写法约定：每条判据都给「怎么测」，测出来的才叫结论；测不出来的一律写成"未证"，不写成"已做"。
> 口径来源：Vaughn Vernon《实现领域驱动设计》的聚合/仓储/领域事件三件套 + Eric Evans 的战略设计，
> 落到本仓的具体形式是 `docs/ddd/01`～`05` 与 `docs/adr/0001-0008`。

## 一、标准（三块，共 12 条）

### A. 战略设计（划边界）

| # | 判据 | 怎么测 |
|---|---|---|
| A1 | 限界上下文按业务语义划分，不是按技术分层划分 | `docs/ddd/01-context-map.md` 的 5 个上下文 + 每个上下文一句话职责；`ls src/main/java/com/smart/chat` |
| A2 | 上下文之间**无循环依赖** | `ArchitectureGuardTest.contextsHaveNoCyclicDependencies`（DFS 找环，非空即红） |
| A3 | 跨上下文只能经被依赖方 `domain` 声明的端口/发布语言 | `ArchitectureGuardTest.crossContextImportsGoThroughPorts` |
| A4 | 通用语言有单一词表，代码里的名词与它一致 | `CONTEXT.md` + 类名/方法名抽查（`partnerOf` 这类唯一表述） |

### B. 战术设计（每个上下文内部）

| # | 判据 | 怎么测 |
|---|---|---|
| B1 | 四层齐备且方向唯一：`api → application → domain ← infrastructure` | `ArchitectureGuardTest.domainLayerStaysPure`（domain 不 import 框架/其它层，也不带 `@Component/@TableName` 等注解） |
| B2 | 持久化模型让位业务名：表映射类一律 `*PO`，业务名归领域类型 | 每个上下文 `grep "@TableName"` 命中的类名全部以 `PO` 结尾 |
| B3 | 仓储是领域声明的端口、基础设施的实现（依赖倒置） | 每个聚合有 `domain/**/XxxRepository` + `infrastructure/persistence/XxxRepositoryAdapter` |
| B4 | `application`/`domain` 不摸 PO 与 Mapper | `ArchitectureGuardTest.persistenceTypesStayBehindRepositoryPorts` |
| B5 | 不变式与状态迁移在聚合里，话术（用户看到的那句原话）也在领域里 | 领域类型带 `RuleViolation` 抛出点；`application/DomainRules` 只翻译状态码不改文案 |
| B6 | Service 是用例编排器：取身份 → 组聚合 → 调领域 → 投影 VO / 推事件，不做业务判定 | 人工 diff + B4 的机械门禁共同兜（判定语句留在 Service 的数量是本轮主要削减项） |

### C. 工程护栏（让改造不退化）

| # | 判据 | 怎么测 |
|---|---|---|
| C1 | 守卫测试是活的：注入违规必须变红 | 每条规则都做过注入实测（见第三节台账） |
| C2 | 债务账本与实测相等，不许过期挂账 | `ArchitectureGuardTest.tacticalLedgerMatchesReality`（账本 ≠ 实测违规集合就红，两个方向都拦） |
| C3 | 行为等价可复核：路由、WS 事件名、VO 字段、对外文案 | `tools/ddd-contract.mjs`（随仓库入库）对基线 commit 与工作树各跑一次，逐条比 routes / events / strings 三个多重集；允许的差异只有「中文原话改成片段+常量拼接」，且必须贴证据说明拼出的整句一字不差 |

## 二、完成度判定（2026-10-05 收口时实测）

统计口径：`find src/main/java -name '*.java'` = 327 个主源文件、96 个测试文件；
`@TableName` 实体 35 个，仓储端口 35 个，`*RepositoryAdapter` 35 个——**一张表一个端口一个适配器，无遗漏**。

| 判据 | couple | messaging | identity | platform | filestorage |
|---|---|---|---|---|---|
| A1～A4 战略（边界/无环/只经端口/统一语言） | ✅ | ✅ | ✅ | ✅ | ✅ |
| B1 四层 + 方向唯一 | ✅ domain 52 文件 | ✅ 24 | ✅ 15 | ✅ 8 | ✅ 8 |
| B2 `*PO` 让名 | ✅ 22/22 | ✅ 7/7 | ✅ 3/3 | ✅ 2/2 | ✅ 1/1 |
| B3 端口 + 适配器 | ✅ 22/22 | ✅ 7/7 | ✅ 3/3 | ✅ 2/2 | ✅ 1/1 |
| B4 PO 不外泄（api/application/domain） | ✅ 0 处 | ✅ 0 处 | ✅ 0 处 | ✅ 0 处 | ✅ 0 处 |
| B5 不变式与话术在领域 | ✅ | ✅ | ✅ | ✅ | ✅ |
| B6 Service 只编排 | ✅ 16 个 | ✅ 4 个 | ✅ 4 个 | ✅ 2 个 | ✅ 1 个 |
| C1～C3 护栏 | ✅ 见第三节 | 同左（守卫是全仓一体的） | | | |

`posBad=0` 那一列的判据是脚本算的：`grep "@TableName"` 命中的类名全部以 `PO` 结尾；
`B4` 的 0 处由 `ArchitectureGuardTest` 在 CI 里持续保证（见第三节，含变异证明）。

**整体判定：本轮 ADR-0008 定义的五个上下文战术改造全部完成，无遗留欠账。**
上一轮 ADR-0005「四个支撑域停在战略分层」的口径已作废并保留为历史记录。

### 有意没做（不是漏了，别当缺陷顺手补）

| 没做的事 | 理由 |
|---|---|
| 进程内领域事件总线（Spring `ApplicationEventPublisher`） | 事件语义由 `messaging.domain.CoupleEventPublisher` + WS 帧 + `couple_notify` 落库承担，有真实消费者；再叠一层会把"监听者跑在事务提交前还是提交后"变成隐藏语义，而推送时机是对外契约 |
| 多模块 Maven / 微服务化 | 单模块 + 包级上下文是本仓既定选择（ADR-0001），本轮不动 |
| CQRS / 读写模型分离 | 读路径经端口返回领域类型或标量计数；`CoupleService` 的心动值六项仍从原始流水重算，不引入投影表 |
| 把 VO 搬到 `api` 层 | ADR-0008 第 5 条：VO 是用例输出投影，搬走只产生 17 Controller + 数十测试的类型改名 churn，对 JSON 契约零影响 |
| 给 append-only 流水表编造行为方法 | 台账 `PointEntry`、打卡日 `BondDay`、通知 `NotifyEntry`、好事 `Deed` 之外的心情/动作等都是薄实体——只有校验过的工厂与访问器（`05` 第 2.2 条） |
| 修掉 DDD 之外的技术债 | `AdminNotifyService.notifyNewRegistration` 无调用方、`CoupleCatchService` 为守字符串契约保留的死私有方法——都不属于"分层"，另案处理。**性能那条已从本表撤下**：原文写的「联系人列表 N+1」是过期口径（`8a91786` 已把 `listFriends` 改成常量 5 趟），同族的 `suggest` 每候选 3 趟已在收口后的同一天（2026-10-05）单独改掉，`build()` 重算项按调用点实测结案——三条裁决与证据见 `docs/adr/0009-im-performance-items-closed.md` |

## 三、实测台账

| 步骤 | 命令 | 实际结果 |
|---|---|---|
| 起点基线 | `mvn -o test`（commit 80ef002） | 267 用例 0 失败 MVN_EXIT=0 |
| 守卫第 5 条上线 | `mvn -o test -Dtest=ArchitectureGuardTest` | 6/6 绿；注入 `@org.springframework.stereotype.Component` 到 `CoupleSpace` → domain 纯净变红；把 filestorage 从账本删掉 → 两条账本断言变红（MUTANT_EXIT=1）；还原后 6/6 绿 |
| identity 收口 | `mvn -o test`（分支 ddd/ident） | 310 用例 0 失败，路由逐条一致、中文文案 0 新增 |
| messaging 收口 | `mvn -o test`（分支 ddd/mess） | 345 用例，仅账本条目红；文案 0 新增 |
| platform+filestorage 收口 | `mvn -o test`（分支 ddd/platfs） | 329 用例，仅账本条目红 |
| couple 全量收口 + 账本清空 | `mvn -o clean test` | **563 用例 0 失败 MVN_EXIT=0，BUILD SUCCESS** |
| 账本清空后的变异证明 | 在 `CouplePinService` 注入不带 import 的 `Supplier<...persistence.CoupleUserPinPO>` | `persistenceTypesStayBehindRepositoryPorts` + `tacticalLedgerMatchesReality` 双双变红（报出 `内联引用 com.smart.chat.couple.infrastructure.persistence.CoupleUserPinPO`）；还原（`git diff` 为空）后 6/6 绿 |
| 对外契约 | `node tools/ddd-contract.mjs <基线commit|WORK> <目录>` 两侧输出做集合 diff（全 `src/main/java`） | 路由 **137 种 0 消失 0 新增**；WS 事件 **44 种 0 消失 0 新增**；中文文案 1215→1220 种，差异逐条核过 |
| 收口后的同日性能结案（ADR-0009） | `mvn -o test -Dtest=FriendServiceTest` / 隔离 worktree `mvn -o clean test` / `node tools/ddd-contract.mjs HEAD src/main/java/com/smart/chat/messaging` | 加好友联想 31 趟 → 常量 3 趟，`FriendServiceTest` 14→**18** 用例；变异证明该锁能变红（`NoInteractionsWanted`，MVN_EXIT=1）；messaging 契约面路由 32 / 文案 73 **0 消失 0 新增**；另补真库差分测试 `FriendSuggestRelationQueryTest`（批量取法 vs 逐条查，H2+Flyway 真表，四种翻车情形），它对调 outgoing/incoming 也会变红；全仓基线 563 → **570**（`Tests run: 570, Failures: 0, Errors: 0` + BUILD SUCCESS，`4ef60f3`，隔离 worktree 实测） |

### 契约差异的完整交代（不做"差不多"）

- **13 条"消失"的中文文案全部是等价改写**：`"爱称最长 30 个字"` → `"爱称最长 " + NICK_MAX + " 个字"` 这类"片段 + 常量"拼接，运行期整句一字不差。判据是同一句在 `identity/messaging/couple` 各出现一次以上时被合成一份。
- **5 条新文案在 filestorage**，其中 `同一内容已在库中：` 是把 `DuplicateKeyException` 翻译成领域事实 `ContentAlreadyStored` 的载体——旧代码走 `catch (DuplicateKeyException)` 回读既有行，新代码语义一致且有两条用例分别锁住适配器翻译与用例回读（`UploadedFileRepositoryAdapterTest` / `FileStorageServiceTest`）。
  另外 `文件必须有名字`、`存储路径不能为空`、`文件指纹不合法` 是 `UploadedFile.register` 的入口不变式：现役管道上游已清洗与裁决过，**这三条走不到**（不是放宽，是加严；加严到不可达处不改变对外行为）。
- 心动值六项权重与七级阈值、`bondDays` 双向口径、按天/按周 `stableHash` 裁决、44 个事件名、全部路由与 VO 字段：**逐项有用例锁着，563 条全绿**。
