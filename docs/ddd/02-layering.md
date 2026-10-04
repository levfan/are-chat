# 02 · 分层与依赖方向

> 本页回答：每个上下文内部长什么样、四类文件各归哪层、什么 import 是违规的、PO 与聚合怎么共存。

## 一、每个上下文四层

```
<context>/
├── api/                     入站适配：@RestController + 请求体 record
├── application/             用例编排：Service（事务边界、幂等闸门、编排端口）+ 对外投影 VO（见 ADR-0008 第 5 条）
│                            ★ 不得 import 本上下文 infrastructure.persistence：取数一律经 domain 的仓储端口
├── domain/                  领域层：model（聚合/实体/值对象）、policy（判定规则）、event、repository（端口接口）
│                            ★ 纯 Java：不得 import Spring web、MyBatis、fastjson、HttpSession、ApiResponse，也不得带容器/ORM 注解
└── infrastructure/          出站适配：persistence（PO + Mapper + 端口实现适配器）、content（静态内容库）、
                             transport / notify / scheduler / boot
```

依赖方向（唯一合法方向）：

```
api → application → domain ← infrastructure
```

`domain` 不依赖任何其它层；`infrastructure` 实现 `domain` 声明的端口（依赖倒置）。**`application` 直接用 PO 吗？——不允许**（ADR-0008 第 2、3 条把这条推平到五个上下文）：过渡期仍在直接摸 PO 的上下文由守卫的 `TACTICAL_PENDING` 集合逐上下文记账，**该集合只能缩短，且必须与实测违规一致**（收口了还留在账上，守卫同样红）。

## 二、四类文件的归属判据

| 文件 | 归层 | 理由 |
|---|---|---|
| `*Controller.java` | `api` | HTTP 是入站适配 |
| `*Service.java` | `application` | 现阶段的 Service 本质是用例编排器 |
| `*Mapper.java` | `infrastructure/persistence` | MyBatis 细节 |
| `@TableName` 实体 | `infrastructure/persistence`（改造后改名 `*PO`） | 它是**持久化模型**，不是领域模型 |
| `*Bank.java` / `*Questions.java` | `infrastructure/content` | 静态内容库是适配器（可按需换库），不是领域概念 |
| `*Job.java` | `infrastructure/scheduler` | 定时触发是入站适配，但不属于 HTTP api |
| `ImPushService` / `Chat*` | `messaging/infrastructure/transport` | 传输机制 |
| `CoupleNotifyRecorder` | `couple/infrastructure/notify` | 落库通知的适配器（挂在推送门面上） |
| `BaseMapperCompat` | `sharedkernel/persistence` | 实测被 auth/couple/im/system 四处继承，是跨上下文技术基类 |
| `ApiResponse`/`BusinessException`/`Sessions`/`GlobalExceptionHandler` | `sharedkernel/web` | 4 个上下文都在用 |
| `WebConfig`/`WebSocketConfig`/`FastJsonWebConfig`/`MybatisPlusConfig`/`LoginInterceptor` | `bootstrap/config` | 装配 |
| `*Properties` | `bootstrap/properties` | `@ConfigurationProperties` 值载体 |

判据全部写成 `docs/ddd/04-move-map.md` 生成器里的规则，不靠人记。

## 三、PO 与聚合的关系（Phase B 的核心手法）

MyBatis-Plus 的实体同时扮演了「表映射」和「业务对象」两件事。改造不打算一步登天，采用**同名让位 + 后缀区分**：

- `couple/infrastructure/persistence/CoupleSpacePO`：字段与表一一对应，只做映射（`@TableName("couple_space")` 原样保留），**改名加 PO 后缀**（`02` 里唯一一次全库重命名，脚本化 + 编译门禁保证不漏引用）。
- `couple/domain/model/space/CoupleSpace`：领域聚合，持有**业务名**，带行为（`partnerOf`、`isActive`、`bind`、`dissolve`）与不变式（一用户只在一个有效空间）。
- `couple/domain/repository/CoupleSpaceRepository`：端口，只暴露领域需要的读法（`findActiveByMember`、`save`）。
- `couple/infrastructure/persistence/CoupleSpaceRepositoryImpl`：适配器，PO ↔ 聚合双向翻译。

**为什么值得付这次改名成本**：不改名，领域层就拿不到 `CoupleSpace` 这个业务名词，代码里会出现 `SpaceAggregate`/`DomainCoupleSpace` 这种带脚手架味的名字——统一语言当场破产（见 `CONTEXT.md`）。

## 四、命名红线

- 事件名、状态枚举值（`OPEN/DONE/SEALED`）、`uk_*`/`idx_*` 索引名**不因分层而改名**：它们是对外契约（前端 `stores/couple.ts` 与数据库都在用）。
- 层名不进类名：不写 `CoupleSpaceDomainService`，写 `IntimacyCalculator`（用业务名，见 `CONTEXT.md`）。
- `partnerOf(me)` 是唯一合法的「对方」表述，禁止 `user1/other/peer` 混用。

## 五、违规怎么被拦住

不引 ArchUnit（本机 Maven 离线仓库无该依赖，见 `docs/adr/0004`），改由 `src/test/java/com/smart/chat/ArchitectureGuardTest` 直接读源码做五条断言：

1. `domain/**` 不得 import `org.springframework.web` / `org.apache.ibatis` / `com.baomidou` / `jakarta.servlet` / `com.smart.chat.*.api` / `*.infrastructure`，**也不得出现 `@Component/@Service/@Repository/@Autowired/@Resource/@TableName/@TableId/@TableField`**（按正则匹配，全限定写法一样拦）；
2. `infrastructure/**` 不得 import 同上下文的 `api`；
3. 上下文之间：`<A>/domain` 之外，`<A>/**` 不得 import `<B>/application` 或 `<B>/infrastructure`（只能 import `<B>/domain` 的端口，或走 `sharedkernel`）；
4. 业务上下文只许 import `bootstrap.properties.*`（`@ConfigurationProperties` 值绑定，实测 5 处）；import `bootstrap.config.*` 就是装配层被业务反向依赖，违规；
5. `application/**` 与 `domain/**` 不得 import **本上下文**的 `infrastructure.persistence`——PO 与 Mapper 被封在 `infrastructure` 内，取数一律经 `domain` 的仓储端口（ADR-0008 第 2、3 条把这条推平到五个上下文）。

第 5 条在改造期用 `TACTICAL_PENDING` 集合记账，但它**不是豁免名单**：`tacticalLedgerMatchesReality` 断言该集合必须与「实测仍有 PO 外泄的上下文」完全相等——收口了还挂着（账本过期假装还在改）和没收口就摘掉（放过违规）两边都红。守卫新规则逐条做过注入违规的变红实测，台账见 `03-phase-plan.md` 第 2.7 节。
