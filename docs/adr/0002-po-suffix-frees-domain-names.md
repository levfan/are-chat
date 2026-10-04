# ADR-0002：MyBatis-Plus 实体降级为 `*PO`，业务名让给领域聚合

- 状态：已采纳（2026-10-04），**先只用于 couple 上下文**
- 相关：`docs/ddd/02-layering.md` 第三节

## 背景

现有实体（`CoupleSpace`、`CoupleMood`…共 19 个 `@TableName` 类）同时承担两件事：表映射（`@TableName`/`@TableId`/Lombok getter/setter）与业务载体（Service 直接读写其字段、`partnerOf` 这类判定挂在实体上）。领域层若复用同一个类，`domain` 就必然 import `com.baomidou` 与 MyBatis 注解，分层形同虚设。

## 决策

1. `couple` 上下文的 19 个实体改名为 `*PO` 并归 `couple/infrastructure/persistence`，只保留映射职责（`@TableName` 值、列名、`@TableId(IdType.INPUT)` 原样不动）。
2. 业务名（`CoupleSpace`、`CoupleMood`…）**让给** `couple/domain/model` 里的聚合与值对象。
3. PO ↔ 聚合的翻译只发生在 `infrastructure/persistence/*RepositoryImpl` 适配器内。
4. 其余四个上下文暂不执行本 ADR（实体仍叫原名、仍在 `infrastructure/persistence`），在其改造轮次再套同一条规则。

## 理由

- 不改名，领域模型只能叫 `SpaceAggregate`/`DomainCoupleSpace`，统一语言当场破产（`CONTEXT.md` 规定一个概念只有一个名字）；加 `PO` 后缀是一次性、可机械验证的成本。
- 只动 couple：这是唯一的核心域，也是唯一本轮做战术改造的上下文；对支撑域做同样的全库重命名，换来的只有 churn。

## 后果

- 正面：`domain` 可以真的不依赖框架（守卫规则 1 能过）；聚合能带不变式而不被 ORM 生命周期绑架。
- 负面：同一次交互里存在两套类型（PO 与聚合），样板翻译代码增加；改名波及面大（Service/Mapper/测试全要跟着改 import）——用编译 + 既有测试 + `EntityReflectionGuardTest`（本轮加实体总数断言）三重兜底，且 `@TableName` 的表名与列名不改，数据库与 Flyway 完全无感。
