# ADR-0008：战术改造收口到全部五个上下文（部分推翻 ADR-0005）

- 状态：已采纳（2026-10-05）
- 相关：`docs/adr/0002-po-suffix-frees-domain-names.md`、`docs/adr/0005-only-couple-gets-tactical-refactor.md`、`docs/ddd/03-phase-plan.md`、`docs/ddd/05-tactical-playbook.md`

## 背景

ADR-0005 决定「只有 `couple` 做战术改造，其余四个上下文停在战略分层」，理由是四个支撑域没有真实的领域专家输入，凭空划聚合边界是半吊子。这个理由在**当时**成立。

但它留下了三笔被明文登记、却没人来收的债（`docs/ddd/03-phase-plan.md` 第 2.5/2.6 节）：

1. `couple` 除心动值与空间外，其余表仍是「Service 直接操作 PO」的事务脚本；
2. `messaging`/`identity`/`platform`/`filestorage` 的实体还没让出业务名（ADR-0002 第 4 条把自己排除在外）；
3. 24 个 Service（含 628 行的 `CoupleService`、521 行的 `PrivateMessageService`）直接 import 本上下文的 `infrastructure.persistence`。

用户 2026-10-05 的指令是「完成全部 ddd 改造，不要让我参与过程确认」。
上一轮我把"Phase B 的 couple 领域层还没建"写进状态表当"有意不做"，得到的回复是「这个需要做完，你不要半途而废啊」——
所以本轮的边界由我自己定，但**定完就得做完**，不许再挂下一轮。

## 决策

1. **ADR-0005 第 3、4 条作废**：四个支撑域一律做到战术改造完成（判据见 `docs/ddd/05-tactical-playbook.md` 第一节五条）。第 1、2 条（Phase A 全项目分层、Phase B 先做 couple）保留，是本轮的起点。
2. **ADR-0002 全项目推广**：剩余上下文的 `@TableName` 实体一律改名 `*PO`，业务名让给 `domain`。表名、列名、`@TableName` 值不变，数据库与 Flyway 无感。
3. **持久化边界由守卫强制**：`ArchitectureGuardTest` 新增第 5 条——业务上下文的 `application`/`domain` 不得 import 本上下文 `infrastructure.persistence`；过渡期用 `TACTICAL_PENDING` 集合记账，**每收口一个上下文就删一项，最终必须为空**。这条集合只许缩短。
4. **`platform` 与 `filestorage` 补 `domain` 层**：不是建空包。`Announcement` 有真实的发布/撤回状态机与阅读记录，`UploadedFile` 有存储路径与大小/扩展名裁决——够用就不薄。
5. **VO 留在 `application`**（推翻 `docs/ddd/02-layering.md` 第二节"响应 VO 归 api"的写法）：VO 是用例的输出投影，与用例同生命周期；搬到 api 只产生 17 个 Controller + 26 个测试文件的类型改名 churn，对对外 JSON 契约零影响。真正的越界是 PO 外泄，已由第 3 条拦住。`02-layering.md` 同步改口径。
6. **不引入进程内领域事件总线**：现役事件语义由 `messaging.domain.CoupleEventPublisher` 端口 + WS 帧 + `couple_notify` 落库承担，有真实消费者；再叠一层 Spring `ApplicationEventPublisher` 会把事务边界（监听者是 BEFORE-COMMIT 还是 AFTER-COMMIT）变成隐藏语义，而本项目已实证"推送时机"是对外契约。
7. **不做 CQRS / 读写分离 / 多模块拆分**：读路径经仓储端口的 query 方法返回领域类型或标量；统计重算项的性能问题属 v8 待拍板的独立议题（见 `docs/ddd/03-phase-plan.md` 第四节硬约束），不用分层改造冒充性能优化。

## 理由

- 支撑域的聚合边界"没有需求单就没法划"这个担心，在读了代码之后被证伪了一半：`CONTEXT.md` 已经逐条写明了每个概念的"关键约束"（双向边、一人一票、只有对方能留灯、双签才生效……），这些就是领域专家输入，只是此前被记在文档里而没落进类型。
- 反过来，也**不假装**每张表都值得胖聚合：append-only 流水表（积分台账、通知副本、打卡日）按 `05-tactical-playbook.md` 第 2.2 条建薄实体，编造行为比空聚合更糟。

## 后果

- 正面：五个上下文同一形状；守卫第 5 条把「Service 摸 PO」变成构建期失败，债务不会自己长回来。
- 负面：PO ↔ 聚合的双向翻译是新增样板（约 40 个适配器），读代码要多跳一层；这是 ADR-0002 已经认过的成本，本轮把它付到全项目。
- 判据：任何人看到 `filestorage/domain` 里的薄实体时，能在本页与 `05` 第二节查到"有意保持薄"，而不是"改造没做完"。
- 完成度以 `docs/ddd/03-phase-plan.md` 的状态表与本轮验收台账为准。

## 落地结果（2026-10-05 收口，实测）

七个决策全部执行完毕，无遗留欠账。逐项对账：

| 决策 | 落地证据 |
|---|---|
| 1 五上下文做到战术改造完成 | `docs/ddd/06-ddd-standard.md` 第二节判定表全 ✅；267 → **563 用例 0 失败**，MVN_EXIT=0 |
| 2 `*PO` 推广全项目 | 35 个 `@TableName` 类全部以 `PO` 结尾（脚本算的 `posBad=0`），业务名让给 35 个领域类型 |
| 3 守卫第 5 条 + 账本 | `TACTICAL_PENDING` 现为**空集**；账本清空后注入不带 import 的 `Supplier<...persistence.CoupleUserPinPO>` 仍双双变红，还原后 6/6 绿 |
| 4 platform / filestorage 补 domain | 各 8 个 domain 文件、2 与 1 个端口，都不是空包 |
| 5 VO 留 application | 全仓 `find src/main/java -name '*VO.java'` = 0 个独立文件，VO 仍是 Service 内嵌 record，JSON 字段名与顺序未动 |
| 6 不引事件总线 | 44 个 WS 事件名与推送时机集合与基线**逐条一致**（`ddd-contract.mjs` diff = 0） |
| 7 不做 CQRS / 不拆模块 | 无新模块、无新依赖（`pom.xml` 本轮零改动） |

执行中额外发现并修掉的两个"守卫自己会漏"的洞（都因实测而改，不是设计时想到）：
`domainLayerStaysPure` 只按 import 查框架依赖会被全限定注解绕过；`poLeaks` 只查 import 会被内联
`com.smart.chat.x.infrastructure.persistence.FooPO` 绕过——两条都改成"正文也扫"，并各自做了变红证明。

一处诚实记账：filestorage 的 `UploadedFile.register` 新增三条入口不变式文案
（`文件必须有名字`／`存储路径不能为空`／`文件指纹不合法`），现役管道上游已清洗裁决过，走不到；
`同一内容已在库中：` 是 `DuplicateKeyException` 的领域翻译，回读语义与改造前一致（两条用例分别锁适配器翻译与用例回读）。
