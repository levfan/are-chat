# ADR-0003：跨上下文只经端口与发布语言，禁止直连对方的 application/infrastructure

- 状态：已采纳（2026-10-04）
- 相关：`docs/ddd/01-context-map.md` 第二节（4 个环的手法）

## 背景

实测的 22 条跨包边里，最伤的是「直接摸对方 mapper/实体」：`auth → couple` 引 `CoupleInviteMapper`/`CoupleSpace`/`CoupleSpaceMapper`，`auth → im` 引 `FriendMapper`/`PrivateMessageMapper`/`UserProfileMapper`。这让 4 个包级环成立，也让任何一次内部重构都可能砸到别的上下文。

## 决策

1. 跨上下文只允许两种形态：
   - **消费端口**：被依赖方在自己的 `domain/repository` 或 `domain/<something>` 声明接口，实现留在自己的 `infrastructure`。例：`couple.domain.RelationshipQuery`（「某人是否在恋爱中 + 天数」）由 couple 实现，identity 注入使用。
   - **发布语言（Published Language）**：只读视图 record，不带对方 ORM 痕迹。例：`messaging.domain.PeerProfile`（昵称/生日/在线），替代现在直接 import `UserProfile`。
2. 推送能力收成一个端口：`messaging.domain.NotificationPort`（`pushCoupleEvent` / `pushCoupleEventBoth` / `isOnline`），实现是 `ImPushService` 适配器。业务上下文不再 import `ImPushService` 具体类。
3. `CoupleNotifyRecorder` 这类「往推送门面上挂 sink」的写法保留，但 sink 接口下沉到端口侧，避免 `im` 反向知道 `couple`。
4. `sharedkernel` 只放 `ApiResponse`/`BusinessException`/`Sessions`/`GlobalExceptionHandler`/`BaseMapperCompat`。**不允许**再往里加业务类型。

## 后果

- 正面：4 个环变有向；重命名/替换某上下文的 mapper 不再波及别人；`auth ↔ couple` 这一对以后可以单独跑测试。
- 负面：多一层接口与适配器样板；identity 拿"恋爱中徽章"要绕一次端口（但这条路正是 `F44 relationship-of` 的实际语义，不算硬造）。
- 兼容性：HTTP 路由与响应结构不变；`ImPushService` 的类名与 WS 帧格式不变，只是不再被业务直接 import。
