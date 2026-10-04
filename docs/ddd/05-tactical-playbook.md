# 05 · 战术改造手法（全上下文统一执行）

> 本页回答：一张表、一个 Service 具体怎么改才算改造完成。
> 目的：让不同人（含并行 agent）在不同上下文里产出**同一个形状**，避免一边写 `XxxRepository`、一边写 `XxxStore`，
> 也避免为了看起来完整而造空聚合。判据全部可被 `ArchitectureGuardTest` 与 grep 复核。

## 一、什么算"改造完成"（每上下文的验收判据）

一个上下文（`couple` / `messaging` / `identity` / `platform` / `filestorage`）达到战术改造完成，必须同时满足五条：

1. **统一语言归位**：所有 `@TableName` 实体改名 `*PO` 并留在 `infrastructure/persistence`（ADR-0002 全项目推广）。业务名（`Friend`、`Account`、`Announcement`…）让给 `domain`。
2. **持久化不外泄**：`<ctx>/application/**` 与 `<ctx>/domain/**` 不得 import `<ctx>.infrastructure.persistence.*`。取数一律经 `domain` 声明的仓储端口。
3. **不变式在领域**：`CONTEXT.md` 里为该概念写明的"关键约束"，代码里必须由领域类型的方法守（抛 `RuleViolation`），不得只在 Service 里 `if` 一遍。
4. **Service 是编排器**：`application` 只做四件事——取会话身份、组聚合、调领域方法、把结果投影成 VO/推 WS。判定语句（`if (status.equals(...))` 这类业务裁决）不出现在 Service。
5. **测试跟着换接线**：原有行为断言**期望值一字不改**，只把 mock 对象从 Mapper 换成端口；缺覆盖的新聚合补单测。

五条里第 2 条由守卫强制，第 1、3、4、5 条由编译 + 用例 + 人工 diff 复核。

## 二、五种文件的模板

### 2.1 PO（`infrastructure/persistence`）

只做表映射：`@TableName` 值、列名、`@TableId(IdType.INPUT)` **原样不动**；Lombok `@Data` 保留；不加业务方法。
改名只动类名后缀，表名/列名/Flyway 全链路无感。

### 2.2 领域类型（`domain/<集合名>/<业务名>.java`）

```java
public final class DineTicket {                 // 不可变字段 + 明确的状态迁移方法
    public static final String STATUS_OPEN = "OPEN";   // 枚举值字符串是对外契约，照 PO 原值写死
    private final String id; ...
    private DineTicket(...) {}                   // 私有构造，禁止外部裸 new
    public static DineTicket offer(...)          // 新建：校验入参，违规抛 RuleViolation（中文原话照搬现有文案）
    public static DineTicket restore(...)        // 从 PO 重建：不校验（历史数据必须能读出来）
    public void redeemBy(String me) { ... }      // 领域行为：归属闸门 + 状态迁移 + 时间戳
    public String id() { ... }                   // 访问器用记录式短名，不写 getX
}
```

- **包按"集合名"划分**（`domain/dine/`、`domain/space/`），不按类名堆平铺；一个包 = 一个聚合 + 它的端口 + 值对象。
- append-only 流水表（台账、通知副本、打卡日）：领域类型是**薄实体**——只保留校验过的工厂与访问器，**不编造行为**。宁薄勿假。
- 纯规则无状态的东西（`IntimacyCalculator`、`MakeupPolicy`、`StreakTier`、`stableHash` 裁决）放 `domain/<集合>/` 下做策略对象，不进 Service。

### 2.3 仓储端口（`domain/<集合名>/<业务名>Repository.java`）

```java
public interface DineTicketRepository {
    Optional<DineTicket> findBySpaceAndUserAndDay(String spaceId, String user, String day);
    List<DineTicket> listBySpaceAndDay(String spaceId, String day);
    void save(DineTicket ticket);        // 无 id 则 insert，有 id 则 update
    void deleteById(String id);
}
```

- 方法名用领域语言（`listBySpaceAndDay`），不出现 `select`/`update`/`Wrapper`。
- 参数只允许领域类型与 JDK 类型，**不得出现 PO**。
- 统计类读法（`countEarnedBySpace`）允许直接返回 `long`：读投影不是聚合，别为它造类型。

### 2.4 适配器（`infrastructure/persistence/<业务名>RepositoryAdapter.java`）

```java
@Component
public class DineTicketRepositoryAdapter implements DineTicketRepository { ... }
```

- PO ↔ 领域**双向翻译只发生在这里**（含 `CoupleSpaceRepositoryAdapter` 已确立的"只回写聚合持有的列"纪律：聚合没纳管的列一律不碰，先 `selectById` 再改再写回）。
- 原先写在 `*Mapper` 里的 `default` 查询方法（`BaseMapperCompat` 那批）保留在 Mapper，适配器调它；**Mapper 不感知领域类型**。

### 2.5 Service（`application/`）

改后形态：构造器注入端口（不再注入 Mapper），流程是
`requireSpace/me → 端口取聚合 → DomainRules.guard(聚合.行为) → 端口.save → 推 WS → 投 VO`。

- 领域异常经 `application/DomainRules.rule|guard` 翻译成 `BusinessException`（404/400 由 `RuleViolation.notFound()` 决定，**文案不重写**）。
- `@Transactional` 留在 Service 方法上（事务边界属于用例，不属于领域）。
- VO record 留在原 Service 内（见 ADR-0008 第 5 条，本轮不搬 api）。

## 三、行为等价红线（违反即回退，不是"顺手改"）

| 不许动 | 为什么 |
|---|---|
| HTTP 路由、请求体字段、VO 字段名与顺序 | 前端 `are-chat-web` 逐字段对账，改名只表现为界面空白 |
| WS 事件名（44 个现役）与推送时机、推送对象 | 前端 store 按事件名派发；`wish-prepared` 这类"刻意不存在"的事件不得补上 |
| `RuleViolation` / `BusinessException` 的中文文案 | 文案是产品口径，也是用例断言的期望值 |
| 心动值权重与阶梯、`bondDays` 口径、按天 `stableHash` 裁决 | `CoupleIntimacyTest` 等已锁死；改断言迁就实现 = 改造失败 |
| 表名、列名、索引名、`@TableName` 值、`status` 枚举字面量 | 数据库与存量数据；迁移脚本必须幂等 |
| 现有跨上下文端口的**方法签名** | `identity.domain` / `messaging.domain` 被多个上下文消费；要扩能力就**新增**方法，不改不删既有签名 |

断言失败时**只能改实现**，不能改期望值。接线改动（mock 对象从 Mapper 换成端口）不算改断言。

## 四、一片改造的自检顺序

1. `mvn -q -o clean compile` EXIT=0
2. `mvn -o test` EXIT=0，且 `Tests run` 数 ≥ 本片开始前（新增聚合要补测）
3. `grep -rn "infrastructure.persistence" src/main/java/com/smart/chat/<ctx>/application src/main/java/com/smart/chat/<ctx>/domain` **无输出**
4. `grep -rn "@TableName" src/main/java/com/smart/chat/<ctx>` 命中的类名全部以 `PO` 结尾
5. `grep -rn "@Component\|@Service" src/main/java/com/smart/chat/<ctx>/domain` **无输出**（领域层不带框架注解）

## 五、禁止事项

- 不新增 Maven 依赖（本机 `mvn -o` 离线，仓库里没有的包一律不碰，见 ADR-0004）。
- 不建空包充数：没有领域内容的上下文不许出现只有 `package-info` 的 `domain/`。
- 不引入进程内领域事件总线、不引 CQRS/读写库分离、不拆多模块 Maven（理由见 ADR-0008）。
- 不动 `src/main/resources/db/` 与 `schema.sql`：本轮零表结构变化，任何"顺手加个列/索引"都要停下另议。
- 不改 `ArchitectureGuardTest`、`EntityReflectionGuardTest`、`SmartChatApplicationTest`、`pom.xml`、`CONTEXT.md`、`docs/**`——这几个文件归主线程独占（并行改造时防互踩）。
