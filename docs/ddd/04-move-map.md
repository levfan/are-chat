# 04 · 逐文件搬迁表（脚本生成，判据 = 文件名后缀 + 显式归属表）

> 由 `.tmp-audit/ddd-apply.mjs plan` 从源码现算，与执行器共用 `.tmp-audit/ddd-rules.mjs`——**不要手改本文件**，规则变了重新生成。
> ⚠️ 本表是 **2026-10-05 DDD 搬迁那一刻的快照**，不是现役清单：同日之后的情侣空间二轮裁剪删掉了 `couple` 上下文
> 16 张表及其 Controller/Service/端口/适配器（`couple/api` 13 → 7、`couple/application` 11 → 6，见
> `docs/adr/0010-couple-trim-to-v8-features.md`）。现役文件数以 `src/main/java` 实测为准（`06-ddd-standard.md` 第二节）。
> 现包 → 上下文：`auth`→`identity`、`im`+`room`→`messaging`、`couple`→`couple`、`system`+`notify`+`tools`→`platform`、`upload`→`filestorage`；
> `BaseMapperCompat` 与 `ApiResponse`/`BusinessException`/`Sessions`/`GlobalExceptionHandler` 进 `sharedkernel`；10 个横切配置进 `bootstrap`。
> `SmartChatApplication` 留在根包不动（`@SpringBootApplication`/`@ConfigurationPropertiesScan`/`@MapperScan` 都以 `com.smart.chat` 为扫描面，保持不变才让本轮是纯移动）。

| 目标包（com.smart.chat 下） | 文件数 | 类 |
|---|---:|---|
| `bootstrap/config` | 5 | FastJsonWebConfig.java LoginInterceptor.java MybatisPlusConfig.java WebConfig.java WebSocketConfig.java |
| `bootstrap/properties` | 5 | AdminProperties.java FileStorageProperties.java ImProperties.java ModerationProperties.java NotifyProperties.java |
| `couple/api` | 13 | CoupleAdminController.java CoupleBondController.java CoupleCareController.java CoupleCatchController.java CoupleCeremonyController.java CoupleController.java CoupleDiningController.java CoupleEchoController.java CoupleFactoryController.java CoupleNotifyController.java CouplePinController.java CoupleQuestController.java CoupleSurpriseController.java |
| `couple/application` | 11 | CoupleBondService.java CoupleCatchService.java CoupleCeremonyService.java CoupleComfortService.java CoupleDiningService.java CoupleEchoService.java CoupleFactoryService.java CouplePinService.java CoupleQuestService.java CoupleService.java CoupleSurpriseService.java |
| `couple/infra/content` | 8 | CoupleCatchBank.java CoupleEchoBank.java CoupleFactoryBank.java CoupleQuestBank.java CoupleRitualBank.java CoupleSurpriseBank.java CoupleTalkBank.java CoupleTermBank.java |
| `couple/infra/notify` | 1 | CoupleNotifyRecorder.java |
| `couple/infra/persistence` | 38 | CoupleAction.java CoupleActionMapper.java CoupleAnniversary.java CoupleAnniversaryMapper.java CoupleCatchSafeword.java CoupleCatchSafewordMapper.java CoupleCatchSafewordUse.java CoupleCatchSafewordUseMapper.java CoupleCeremonyCoupon.java CoupleCeremonyCouponMapper.java CoupleComfort.java CoupleComfortMapper.java CoupleDineTicket.java CoupleDineTicketMapper.java CoupleEchoDeed.java CoupleEchoDeedMapper.java CoupleInvite.java CoupleInviteMapper.java CoupleMood.java CoupleMoodMapper.java CoupleMoodReaction.java CoupleMoodReactionMapper.java CoupleMysteryBox.java CoupleMysteryBoxMapper.java CoupleNotify.java CoupleNotifyMapper.java CouplePointLedger.java CouplePointLedgerMapper.java CoupleQuestOvertime.java CoupleQuestOvertimeMapper.java CoupleScratch.java CoupleScratchMapper.java CoupleSpace.java CoupleSpaceMapper.java CoupleSpinTask.java CoupleSpinTaskMapper.java CoupleUserPin.java CoupleUserPinMapper.java |
| `couple/infra/scheduler` | 3 | CoupleCareTalkJob.java CoupleReminderJob.java CoupleSurpriseJob.java |
| `filestorage/api` | 1 | FileController.java |
| `filestorage/application` | 1 | FileStorageService.java |
| `filestorage/infra/persistence` | 2 | UploadedFile.java UploadedFileMapper.java |
| `identity/api` | 2 | AdminController.java AuthController.java |
| `identity/application` | 4 | AdminService.java AppUserService.java RegistrationService.java SmsCodeService.java |
| `identity/infra/boot` | 1 | AdminBootstrapper.java |
| `identity/infra/persistence` | 6 | AdminAudit.java AdminAuditMapper.java AppUser.java AppUserMapper.java RegistrationApplication.java RegistrationApplicationMapper.java |
| `identity/infra/security` | 1 | PasswordHasher.java |
| `identity/infra/throttle` | 1 | LoginRateLimiter.java |
| `messaging/api` | 5 | FriendController.java PresenceController.java PrivateMessageController.java ProfileController.java StarsController.java |
| `messaging/application` | 4 | FriendService.java ModerationService.java PresenceService.java PrivateMessageService.java |
| `messaging/infra/persistence` | 14 | ConversationPin.java ConversationPinMapper.java Friend.java FriendMapper.java FriendRequest.java FriendRequestMapper.java MessageReaction.java MessageReactionMapper.java MessageStar.java MessageStarMapper.java PrivateMessage.java PrivateMessageMapper.java UserProfile.java UserProfileMapper.java |
| `messaging/infra/throttle` | 1 | MessageRateLimiter.java |
| `messaging/infra/transport` | 5 | ChatEndpoint.java ChatMessage.java ChatSessionRegistry.java ChatWebSocketBridge.java ImPushService.java |
| `platform/api` | 2 | AnnouncementController.java HealthController.java |
| `platform/application` | 2 | AdminNotifyService.java AnnouncementService.java |
| `platform/infra/devtools` | 1 | H2DumpEndpoint.java |
| `platform/infra/persistence` | 4 | Announcement.java AnnouncementMapper.java AnnouncementRead.java AnnouncementReadMapper.java |
| `sharedkernel/persistence` | 1 | BaseMapperCompat.java |
| `sharedkernel/web` | 4 | ApiResponse.java BusinessException.java GlobalExceptionHandler.java Sessions.java |

主源码 147 个类里 146 个需要换包；测试 26 个文件里 21 个跟随被测类同层。

## Phase A 改写量（同一次 plan 实测）

| 项 | 数 |
|---|---:|
| 需改写的文件（含 package/import） | 169 |
| 需补 import 的文件（以前同包、现在跨包） | 75 |
| 补 import 总量 | 296 |
