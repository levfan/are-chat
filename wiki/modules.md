# 包结构导览

> 本页回答：`src/main/java/com/smart/chat/` 下每个包负责什么、关键类有哪些、从哪个入口看起。

顶层入口：`SmartChatApplication.java`（标准 `@SpringBootApplication`）。以下按包叙述。

## auth — 账号与注册审批

职责：注册审批制账号体系的全部服务端；管理员运营接口也在此包。

| 关键类 | 说明 |
|---|---|
| `AuthController` `/api/auth` | 验证码、注册申请、审批进度轮询、登录/登出、`/me`、改密、自助注销（F84：标记 CLOSED + 清理双向好友 + 失效会话） |
| `AppUserService` / `AppUser` / `AppUserMapper` | 用户表与状态（含禁用/关闭） |
| `RegistrationService` / `RegistrationApplication` | 注册申请单：提交→待审→通过/驳回 |
| `SmsCodeService` | 验证码（演示环境直接回显 devCode） |
| `LoginRateLimiter`、`PasswordHasher` | 登录防爆破、口令散列 |
| `AdminController` `/api/admin` | 审批（通过/驳回）、待办数、用户启停/重置密码、审计日志、公告发布（代理 system 包） |
| `AdminService` / `AdminAudit` / `AdminAuditMapper` | 管理员操作审计落库 |
| `AdminBootstrapper` | 库内无 ADMIN 时按 `arechat.admin.*` 自动建管理员 |

入口建议：`AuthController` → `RegistrationService`。

## im — 好友 / 私信 / 在线状态 / 推送门面

职责：社交与 IM 主线，且承载全项目共用的两个基础设施（`BaseMapperCompat`、`ImPushService`）。

| 关键类 | 说明 |
|---|---|
| `FriendController` `/api/friends` | 好友列表、联想、申请（收/发/接受/拒绝）、修改备注、删除 |
| `FriendService` / `Friend` / `FriendRequest` | 双向好友关系与申请流 |
| `PrivateMessageController` `/api/messages` | 会话消息：历史、分页搜索、导出、发送、已读、撤回、2 分钟内编辑、表情回应、收藏、心动时刻（F36 标记 + 全局列表）、全局搜索、置顶、清空、附件墙 |
| `PrivateMessageService` / `PrivateMessage` | 消息落库与推送编排 |
| `MessageRateLimiter`、`ModerationService` | 每分钟限流、敏感词 censor/block |
| `MessageStar` / `MessageReaction` / `ConversationPin` | 收藏、表情回应、会话置顶（各含 Mapper） |
| `ProfileController` `/api/profile` | 本人资料读写、好友资料卡（仅好友可见昵称/签名/头像）、F42 好友生日列表 |
| `UserProfile` / `UserProfileMapper` | 头像/昵称/签名/生日；birthday 被 couple 生日彩蛋（F59/F92）跨包读取 |
| `PresenceController` `/api/presence` | `GET /online` 在线名单 |
| `PresenceService` | 在线状态维护 |
| `StarsController` `/api/stars` | 收藏消息聚合列表 |
| `ImPushService` | **WS 推送门面**（见 architecture.md 推送机制节；含 `setNotifySink` 扩展点） |
| `BaseMapperCompat` | 全部 Mapper 的公共基接口 |

## couple — 情侣空间（核心业务，占代码量大头）

职责：F1–F219 全部情侣功能域；包内 350 个文件按"实体+Mapper 平铺 + 每域一对 Service/Controller + Bank 静态库 + 4 个 Job"组织。

- 聚合根：`CoupleSpace`（userA/userB 字典序）、`CoupleSpaceMapper`、`CoupleService`（总览/邀请/解除/纪念日/清单/心情/信箱/条约/城市/基金/个性化等基础域）。
- 域控制器全名单与前缀见 [couple-space.md](couple-space.md) 与 [api.md](api.md)。
- 内容库：`CoupleQuestions`、`CoupleRitualBank`、`CoupleSurpriseBank`、`CoupleTalkBank`、`CoupleGrowthBank`、`CoupleCommBank`、`CouplePlayBank`、`CoupleDailyLifeBank`、`CoupleCoachBank`、`CouplePoemBank`、`CoupleSparkBank`、`CoupleManageBank`、`CoupleMuseumBank`、`CoupleDistanceBank`、`CoupleSecurityBank`、`CouplePraiseBank`（静态内容 + `stableHash` 工具）。
- 定时任务：`CoupleReminderJob`、`CoupleSurpriseJob`、`CoupleCareTalkJob`、`CoupleMemoryJob`（详见 [scheduled-jobs.md](scheduled-jobs.md)）。
- 通知落库：`CoupleNotifyRecorder`（经 `ImPushService.setNotifySink` 挂接）、`CoupleNotifyController` `/api/couple/notify`。

## room — 聊天室 WebSocket

| 关键类 | 说明 |
|---|---|
| `ChatEndpoint` | `@ServerEndpoint("/ws/chat/{name}")` 聊天室广播端点 |
| `ChatSessionRegistry` | username→会话在线注册表，`ImPushService` 的底层 |
| `ChatWebSocketBridge` | Spring 容器与 JSR-356 端点之间的桥 |
| `ChatMessage` | WS 消息载体 |

## system — 公告 / 健康检查

| 关键类 | 说明 |
|---|---|
| `AnnouncementController` `/api/announcements` | 当前生效公告拉取、已读回执（发布/关闭走 `/api/admin/announcements`） |
| `Announcement` / `AnnouncementRead` + Mapper | 公告与已读记录 |
| `AnnouncementService` | 公告业务 + 经 ImPushService 广播 |
| `HealthController` `/api/health` | 免登录健康检查（登录页状态点） |

## upload — 文件上传

| 关键类 | 说明 |
|---|---|
| `FileController` `/api/files` | multipart 上传（20MB/25MB 限额）、`GET /{id}/download` 下载 |
| `FileStorageService` | 本地磁盘存储（`arechat.storage.base-dir=./uploads`） |
| `UploadedFile` / `UploadedFileMapper` | 文件元数据（会话附件墙按 type=image/file 归类） |

注意：产品红线**不做照片/视频上传类功能**——现有上传通道仅服务文件附件场景，扩展前先看红线（[dev-guide.md](dev-guide.md)）。

## tools — 运维小工具

`H2DumpEndpoint`：actuator 自定义端点 `h2dump`，对 H2 内存库执行 `SCRIPT TO` 导出（生产 MariaDB 形态下无意义）。

## notify — 管理员外部通知

`AdminNotifyService`：注册审批待办的免费外推通道聚合（企业微信 webhook / WxPusher / Server酱 / 虾推啥），配置见 `arechat.notify.*`；未配置则仅站内待办。

## common — 全局三件套 + 会话工具

`ApiResponse`（code=0 成功）、`BusinessException`（code 兼 HTTP 状态码）、`GlobalExceptionHandler`、`Sessions`（requireUser/username）。

## config — 配置类集中地

| 类 | 作用 |
|---|---|
| `WebConfig` | CORS + LoginInterceptor 注册 |
| `LoginInterceptor` | `/api/**` 登录拦截，SESSION_USER 键 |
| `WebSocketConfig` | `ServerEndpointExporter` |
| `MybatisPlusConfig` | Boot 4 下手工会启 MP（拦截器/分页等） |
| `FastJsonWebConfig` | fastjson2 接管 MVC 消息转换 |
| `FileStorageProperties` / `ImProperties` / `ModerationProperties` / `NotifyProperties` / `AdminProperties` | `arechat.*` 各段配置绑定 |
