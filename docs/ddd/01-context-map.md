# 01 · 战略设计：限界上下文与依赖现状

> 本页回答：are-chat 现在有几个上下文、它们怎么划分、彼此的**实测**依赖是什么、哪几个是必须拆的循环。
> 数据来源：`.tmp-audit/ddd-deps.mjs`（解析全部 147 个 java 文件的 `import com.smart.chat.*`，按包聚合，并做包级 DFS 找环）。**本文所有数字是跑出来的，不是估的。**

## 一、实测依赖矩阵（22 条跨包边）

| 条数 | 边 | 被引的类 | 性质 |
|---:|---|---|---|
| 7 | `auth → im` | BaseMapperCompat, FriendMapper, ImPushService, PrivateMessage, PrivateMessageMapper, UserProfile, UserProfileMapper | 既有 ORM 基类依赖，也有直接摸 mapper 的越界 |
| 5 | `couple → im` | BaseMapperCompat, FriendMapper, ImPushService, UserProfile, UserProfileMapper | 情侣空间要读对方资料/好友关系、要推 WS |
| 3 | `auth → common` | ApiResponse, BusinessException, Sessions | 共享内核 ✅ |
| 3 | `auth → couple` | CoupleInviteMapper, CoupleSpace, CoupleSpaceMapper | **越界**：账号上下文直接摸情侣的 mapper 与实体 |
| 3 | `couple → common` / `im → common` / `system → common` | ApiResponse, BusinessException, Sessions | 共享内核 ✅ |
| 2 | `auth → config` | AdminProperties, LoginInterceptor | 配置反向被业务引用 |
| 2 | `auth → system` | Announcement, AnnouncementService | 跨上下文直接调用 |
| 2 | `im → auth` | AppUser, AppUserService | 与 `auth → im` 成环 |
| 2 | `im → config` | ImProperties, ModerationProperties | 配置反向被业务引用 |
| 2 | `system → im` | BaseMapperCompat, ImPushService | ORM 基类 + 推送门面 |
| 2 | `upload → common` | ApiResponse, BusinessException | 共享内核 ✅ |
| 1 | `couple → auth` | AppUserService | 与 `auth → couple` 成环 |
| 1 | `im → room` | ChatSessionRegistry | 与 `room → im` 成环 |
| 1 | `room → im` | PresenceService | 同上 |
| 1 | `notify → config` | NotifyProperties | 配置反向被业务引用 |
| 1 | `system → room` | ChatSessionRegistry | 跨上下文直接引用传输层 |
| 1 | `common → config` | LoginInterceptor | **共享内核反向依赖上层**（最脏的一条） |
| 1 | `config → common` | ApiResponse | 与上一条成环 |
| 1 | `auth → notify` | AdminNotifyService | 跨上下文直接调用 |
| 1 | `upload → config` | FileStorageProperties | 配置反向被业务引用 |

## 二、包级循环（必须拆掉的 4 个）

```
common  → config  → common
auth    → im      → auth
im      → room    → im
auth    → couple  → auth
```

| 环 | 根因 | 拆除手法（Phase C） |
|---|---|---|
| `common ↔ config` | `GlobalExceptionHandler`(common) 引 `LoginInterceptor`(config) 判断跳过路径 | 把「哪些请求不参与鉴权」抽成 common 侧的谓词接口，config 提供实现 → 依赖方向反正 |
| `auth ↔ im` | im 要账号信息，auth 要推送与资料 | `messaging` 暴露发布语言（`PeerProfile` 只读视图）+ `NotificationPort`；auth 不再引 im 的 mapper |
| `im ↔ room` | PresenceService 要在线表，房间要服务 | 在线表归 `messaging/infrastructure/transport`，同上下文内部依赖不算环 |
| `auth ↔ couple` | 注册/管理页要看「是否恋爱中」，情侣侧要查账号 | `couple` 暴露 `RelationshipQuery` 端口实现，`identity` 通过接口消费；couple 不再直接引 `AppUserService` |

## 三、限界上下文划分（5 个 + 共享内核 + 装配层）

| 上下文 | 由现包合成 | 域分类 | 一句话职责 | 为什么这么切 |
|---|---|---|---|---|
| `couple` | couple (74) | **核心域** | 情侣空间全部业务（10 张卡 + 空间地基 + 心动值 + 积分台账） | 全部差异化价值都在这里，唯一做完整战术改造的上下文 |
| `messaging` | im (26) + room (4) | 支撑域 | 好友、私信、在线状态、资料、WS 传输 | room 只是 im 消息的传输适配，分开必成环（实测已证） |
| `identity` | auth (15) | 通用域 | 账号、登录、注册审批、管理员与审计 | 有独立生命周期与合规要求，别和业务互摸 |
| `platform` | system (7) + notify (1) + tools (1) | 通用域 | 公告、健康检查、管理员通知、H2 导出 | 运维支撑类，变更频率低 |
| `filestorage` | upload (4) | 通用域 | 本地文件存储 | 与业务无耦合，边界最干净 |
| `sharedkernel` | common 的 4 个 + `BaseMapperCompat` | 共享内核 | ApiResponse/BusinessException/Sessions/全局异常 + ORM 基类 | 实测 4 个上下文都依赖这 4 个类 + BaseMapperCompat 被 4 处继承，抽掉才不算重复 |
| `bootstrap` | config (10) | 装配层 | Web/WS/MyBatis 装配与 `@ConfigurationProperties` | 配置类必须能被业务依赖而**不反向**依赖业务 |

**共享内核刻意保持极小**：只有「不可能有两套语义」的东西。`UserProfile`、`CoupleSpace` 这类**不进**内核，跨上下文要读就经端口拿只读视图——否则内核会变成新的上帝包。

## 四、上下文集成关系（目标）

```
            ┌────────────┐
            │  bootstrap │  只装配，不被任何上下文 import
            └─────┬──────┘
                  │ 提供 Properties 实现
   ┌──────────┐   ▼    ┌───────────┐   ┌──────────────┐
   │ identity │◄──┐  ┌─┴────────┐  │   │  platform    │
   └────┬─────┘  │  │ messaging│◄─┼───┴──────┬───────┘
        │        │  └────┬─────┘  │          │
        │ RelationshipQuery      │ NotificationPort（推送门面）
        │ 的实现（端口）  │      │  与 PeerProfile 只读视图
        ▼        │        ▼       │
   ┌─────────────────────────────────┐
   │            couple（核心域）        │
   └─────────────────────────────────┘
              全部依赖 sharedkernel
```

规则：**箭头只允许指向「被依赖方声明的端口」**，不允许 import 对方 `application`/`infrastructure` 里的具体类。

## 五、搬迁规模（Phase A 的事实依据）

`docs/ddd/04-move-map.md`（脚本生成）给出逐文件归属：146 个文件迁入 24 个目标包，`SmartChatApplication` 留在根包（组件扫描面 `com.smart.chat` 不变，`@MapperScan` 同样按根包扫，因此搬包对运行时装配零影响）。

**没有 mapper XML**（`find src/main/resources -name "*.xml"` 为空），所以不存在 namespace 改写；实体扫描靠 `classpath*:com/smart/chat/**/*.class` 递归匹配，`EntityReflectionGuardTest` 搬包后仍能发现全部实体。
