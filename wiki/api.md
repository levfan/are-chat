# REST 接口总表

> 本页回答：全部 Controller 的端点清单（方法 + 路径 + 请求体 + 响应字段 + 错误码）。路径省略类前缀 `/api`；couple 系前缀见各节标题。
> **当前口径（2026-10-20 新增亲密日历后逐条实测）**：全仓 **17 个 Controller / 76 个端点**（逐条读各 `*Controller.java` 的 `@GetMapping/@PostMapping/@PutMapping/@DeleteMapping`，类上 `@RequestMapping` 是基址；`GlobalExceptionHandler` 不是 Controller，不计入）。
> 分上下文：identity 15、messaging 25、platform 6、filestorage 2、couple 28（合计 76）。其中 **couple 侧 7 个 Controller / 28 个端点**——2026-10-05 按 `docs/adr/0010-couple-trim-to-v8-features.md` 从 70 收缩到 26，随后再下线百日隐藏页收缩到 25，2026-10-20 再删除通知中心（2）与运营看板（1）后为 22，同日再删问答回看与愿望改备注（2）后为 20，随后新增经期记录（4）、亲密记录（3）与亲密日历（1）后为 28。
> 非 couple 侧是 **10 个 Controller**（identity 2 + messaging 4 + platform 3 + filestorage 1）。

来源：`src/main/java` 各 `*Controller.java` 的映射注解与 javadoc；响应字段取自对应 Service 内嵌的 `public record XxxVO(...)`。返回统一 `ApiResponse{code,message,data}`；除 `/api/auth/**` 与 `/api/health` 外均需登录会话。业务失败抛 `BusinessException(code, 中文原话)`，`code` 同时作为 HTTP 状态码。可空性按字段类型判断：`Long`/`Integer`/包装类型可为 `null`，`long`/`int`/`boolean` 不为 `null`。

## 非 couple 接口

> 本节端点逐条与源码注解核对过，方法/路径与现役一致（identity 15 + messaging 25 + platform 6 + filestorage 2 = 48）。

### `AuthController` `/auth`
- POST `/sms-code` 注册验证码（演示回显 devCode）｜ POST `/register` 提交注册审批申请
- GET `/register-status` 审批进度轮询 ｜ POST `/login` 登录 ｜ POST `/logout` 登出
- GET `/me` 当前用户 ｜ PUT `/password` 改密（旧密码+强度校验）｜ POST `/deactivate` 自助注销（清好友+失效会话）

### `AdminController` `/admin`（identity · 仅管理员）
- GET `/applications` 审批列表 ｜ POST `/applications/{id}/approve` `/reject` 通过/驳回
- GET `/pending-count` 待办数 ｜ GET `/users` 用户列表 ｜ POST `/users/{username}/status` 启/停 ｜ POST `/users/{username}/reset-password` 重置密码

### `AnnouncementAdminController` `/admin`（platform · 仅管理员）
- POST `/announcements` 发布公告 ｜ POST `/announcements/{id}/close` 关闭 ｜ GET `/announcements` 列表

### `FriendController` `/friends`
- GET `` 好友列表 ｜ GET `/suggest` 加好友联想 ｜ POST `/requests` 发申请
- GET `/requests/incoming` `/requests/outgoing` 收/发申请 ｜ POST `/requests/{id}/accept` `/reject`
- PUT `/{id}` 改备注 ｜ DELETE `/{id}` 删除好友

### `PrivateMessageController` `/messages`
- GET `/{peer}` 会话历史 ｜ GET `/{peer}/search` 会话内搜索
- POST `/{peer}` 发送（`type` 认 text/image/poke/file；card 与 location 已下线，落到 `PrivateMessage.requireWellFormed` 直接拒「名片与位置分享已下线」，历史行仍按原类型读出） ｜ POST `/{peer}/read` 已读 ｜ POST `/{id}/recall` 撤回 ｜ PUT `/{id}` 2 分钟内编辑
- POST `/{id}/reactions` 表情回应 toggle
- POST/DELETE/GET `/{peer}/pin` 置顶/取消/查询 ｜ DELETE `/{peer}` 清空会话

### `ProfileController` `/profile`
- GET `` 我的资料 ｜ GET `/{username}` 好友资料卡 ｜ PUT `` 保存资料 ｜ GET `/friends-birthdays` 好友生日列表（F42）

### 其他
- `PresenceController` GET `/presence/online` 在线名单
- `AnnouncementController` GET `/announcements/current` 生效公告 ｜ POST `/announcements/{id}/read` 已读
- `HealthController` GET `/health` 健康检查（免登录）
- `FileController` POST `/files`（multipart）上传 ｜ GET `/files/{id}/download` 下载
- WS：`/ws/chat/{name}` 聊天室与推送通道（非 REST，见 [architecture.md](architecture.md)）

## couple 接口（现役 7 个 Controller / 28 个端点）

> 逐端点写明：方法、路径、请求体字段、响应 record 逐字段（含可空性）、错误码与中文原话。
> 原 10 张历史卡的 Controller（贴贴 `CoupleBondController`、求抱抱 `CoupleCareController`、安全词 `CoupleCatchController`、
> 愿望券 `CoupleCeremonyController`、饭桌 `CoupleDiningController`、家务 `CoupleFactoryController`、加班 `CoupleQuestController`、
> 好事簿 `CoupleEchoController`、惊喜 `CoupleSurpriseController`、收藏 `CouplePinController`）已随 ADR-0010 全部下线；
> 百日隐藏页 `CoupleMemoryController` 与其 `/page` 端点也在随后一轮下线；
> 通知中心 `CoupleNotifyController`（2 个端点）与 F45 运营看板 `CoupleAdminController`（1 个端点）在 2026-10-20 删除；
> `CoupleController` 上的 `/moods`、`/anniversaries`（复数 CRUD）也一并删除。详见 [couple-space.md](couple-space.md)。

### `CoupleController` `/api/couple`（地基：建立 + 空间本体 + 心动值 · 10 个）

**公共响应 record**（本节多处复用）
- `SpaceVO(String id, PartnerVO partner, Long created, String anniversary, long days, String slogan, String theme)`
  - `id` 空间 UUID｜`partner` 对方视角（下）｜`created` 建立毫秒时间戳（`Long`，可空）｜`anniversary` 在一起的日子 `yyyy-MM-dd`（可空）｜`days` 在一起天数（`long`，非空，含当天≥1）｜`slogan` 宣言（可空）｜`theme` 主题（缺省回显 `classic`）
- `PartnerVO(String username, String nickname, String avatar, boolean online, String petName)`
  - `username` 对方用户名｜`nickname` 昵称（缺省回退用户名）｜`avatar` 头像标识（无则空串）｜`online` 是否在线（`boolean`）｜`petName` TA 给你的专属爱称（可空）
- `InviteVO(String id, String fromUser, String toUser, String message, String status, Long created)`
  - `message` 留言（null 归一为空串）｜`status` ∈ `PENDING/ACCEPTED/REJECTED/CANCELED`｜`created` 邀请毫秒时间戳（`Long`）

**端点**

1. `GET /overview` → `OverviewVO(SpaceVO space, List<InviteVO> incoming, List<InviteVO> outgoing)`
   - `space`：未建空间时为 `null`（前端据此显示建立指引）；`incoming`/`outgoing` 我的收/发待处理邀请（按时间新→旧）。
2. `POST /invites` body `InviteRequest{ String username; String message }`（`message` 可空，≤100 字）→ `InviteVO`
   - 错误：400「想邀请谁？请先选择一位好友」／400「不能和自己建立情侣空间哦」／400「查无此人：对方还没注册或已注销」／400「只能邀请自己的好友，先去通讯录加个好友吧」／400「邀请留言最长 100 个字」／409「你已经在情侣空间里啦，先解除才能发起新邀请」／409「对方已经在别的情侣空间里了」／409「你们之间已有待处理的情侣邀请，等对方处理吧」。事件 `invite`。
3. `POST /invites/{id}/accept` → `SpaceVO`
   - 错误：404「邀请不存在」／403「只能处理发给自己的邀请」／409「该邀请已经处理过了」／409「无法同意：有一方已经进入其他情侣空间」。事件 `invite-accepted`；建空间当天写第 1 个打卡日。
4. `POST /invites/{id}/reject` → `Void`
   - 错误：404「邀请不存在」／403「只能处理发给自己的邀请」／409「该邀请已经处理过了」。事件 `invite-rejected`。
5. `DELETE /invites/{id}` → `Void`（撤回自己发出的待处理邀请）
   - 错误：404「邀请不存在」／403「只能撤回自己发出的邀请」／409「该邀请已经处理过了」。**不推事件**。
6. `PUT /anniversary` body `AnniversaryDateRequest{ String date }`（`yyyy-MM-dd`）→ `SpaceVO`
   - 错误：404「还没有建立情侣空间，先邀请一位好友吧」／400「纪念日格式应为 yyyy-MM-dd」／400「已解散的空间不再改纪念日」。事件 `anniversary-updated`。
7. `PUT /profile` body `ProfileUpdateRequest{ String slogan; String theme; String petName }`（任一 `null`=不改该项，空串=清除）→ `SpaceVO`
   - 错误：404「还没有建立情侣空间，先邀请一位好友吧」／400「宣言最多 60 字，留白也很美」／400「这个主题还没上架哦」（theme 须 ∈ classic/cherry/ocean/forest/night）／400「爱称最长 30 个字」／400「已解散的空间不能改装扮」。推送按真正变了什么分流：爱称变推 `pet-name-changed`（含清除话术）、宣言或主题变推 `space-themed`、都没变不推（ADR-0010 第 12 条，`CoupleProfilePushTest` 锁死）。
8. `GET /relationship-of/{username}` → `RelationshipVO(boolean inRelationship, Long days, String anniversary)`（F44 恋爱中徽章，好友资料卡用）
   - `days`/`anniversary` 在 `inRelationship=false` 时为 `null`。错误：400「用户名不能为空」／403「只有好友才能查看恋爱状态」。
9. `POST /dissolve` → `Void`（本人解除）。错误 404「还没有建立情侣空间，先邀请一位好友吧」；事件 `dissolved`。
10. `GET /intimacy` → `IntimacyVO(int score, int level, String title, String icon, Integer nextLevelAt, int levelProgress, IntimacyBreakdown breakdown)`
    - `score` 五项加权总分｜`level` 1-7｜`title`/`icon` 七级称号与图标｜`nextLevelAt` 下一级阈值（`Integer`，满级为 `null`）｜`levelProgress` 距下一级 0-100（满级=100）｜`breakdown = IntimacyBreakdown(long daysTogether, long checkinDays, long longestStreak, long answerDays, long wishFulfilled)`（五项原始供数）。
    - 权重 `×1/×2/×3/×3/×5`，阈值 `0/60/150/260/400/560/760`。错误 404「还没有建立情侣空间，先邀请一位好友吧」。

### `CoupleStreakController` `/api/couple/streak`（连续互动打卡 · 2 个）

**响应 record**：`StreakBoardVO(String day, int currentStreak, int longestStreak, int confirmedDays, boolean checkedToday, boolean missedYesterday, List<TierVO> tiers, String nextTierKey, String nextTierLabel, int daysToNext, List<StripCellVO> strip, int makeupWindowDays, int makeupLeftThisMonth, boolean canMakeup)`
- `day` 今天 `yyyy-MM-dd`｜`currentStreak` 当前连续（今天没打从昨天起算，不算断）｜`longestStreak` 历史最长峰值｜`confirmedDays` 累计打卡天数（非连续）｜`checkedToday` 今天是否已打｜`missedYesterday` 昨天是否缺行（断签判据）｜`tiers` 六档逐档（下）｜`nextTierKey`/`nextTierLabel` 下一未解锁档（`String`，全解锁为 `null`）｜`daysToNext` 距下一档还差几天（按当前连续）｜`strip` 近 21 格日历条（下）｜`makeupWindowDays` 补签窗口（固定 7）｜`makeupLeftThisMonth` 本自然月还能补几次（3−已用）｜`canMakeup` 后端算好=昨天断了且还有额度。
  - **注**：补签已改为**免费**，不再有「积分价格 / 账户余额」这类字段（原积分口径字段随台账一并删除），额度改由 `makeupWindowDays`/`makeupLeftThisMonth` 表达。
- `TierVO(String key, int days, String label, String icon, String detail, boolean unlocked, String unlockedDay)`——`unlockedDay` 首次跨过的日期（`String`，未解锁 `null`）。
- `StripCellVO(String day, boolean checked, boolean makeupFlag, boolean todayFlag)`。

11. `GET /board` → `StreakBoardVO`。错误 404「还没有建立情侣空间，先邀请一位好友吧」。
12. `POST /makeup` body `MakeupRequest{ String day }`（`yyyy-MM-dd`）→ `StreakBoardVO`（补签成功落一行 `MAKEUP`，返回整份看板）。
    - 错误（闸门在 `MakeupPolicy`，**免费**）：400「想补哪一天？日期没给呢」／400「日期格式应为 yyyy-MM-dd」／400「今天还不能补——两个人都答完今天的每日一问就算打卡 😉」／400「只能补最近 7 天里的缺口，太久以前的那天就让它过去吧」／400「<day> 已经打过卡了，不用补」／400「这个月已经补过 3 次了，下个月再来吧（<年-月>）」／404「还没有建立情侣空间…」。事件 `streak-makeup`（+ 跨档补推 `streak-unlocked`）。

### `CoupleQuestionController` `/api/couple/question`（每日一问 · 2 个）

**响应 record**：`TodayVO(String day, int index, String question, AnswerVO mine, String partnerAnswer, boolean answeredByMe, boolean answeredByPartner, boolean bothAnswered, int answerMax)`
- `index` 当日题号（`stableHash(spaceId|v8-question-bank|day)` mod 75）｜`mine` 我的作答（`AnswerVO`，我没答为 `null`）｜`partnerAnswer` 对方答案（`String`，**双方都答完才有，否则 `null`**）｜`answerMax` 回答字数上限（300）。
- `AnswerVO(String username, String answer, Long createdAt, Long updatedAt)`（`updatedAt` 未改写为 `null`）。

13. `GET /today` → `TodayVO`。错误 404「还没有建立情侣空间…」。
14. `POST /answer` body `AnswerRequest{ String answer }`（当天可改写，每人每天一行）→ `TodayVO`。
    - 错误：400「写一句再交卷呀 📝」／400「回答最多 300 个字，短一点更像人话」／404「还没有建立情侣空间…」。事件 `question-answered`；**若对方已答则本次触发打卡**（`streak-checkin`，可能补推 `streak-unlocked`）。

### `CoupleWishController` `/api/couple/wish`（愿望清单 · 6 个）

**响应 record**（写接口一律返回整份清单，前端按请求者视角整体替换）：`WishBoardVO(List<WishVO> open, List<WishVO> prepared, List<WishVO> fulfilled, int openCount, int limit, int titleMax, int noteMax)`
- 三组按**当前请求者视角**分组：`prepared` 只对标记人自己出现，许愿人那里那条仍在 `open`。`limit` 未实现上限（30）｜`titleMax` 标题上限（80）｜`noteMax` 说明上限（200）。
- `WishVO(String id, String ownerUser, String creatorUser, String title, String note, String status, boolean mineFlag, boolean preparedFlag, boolean preparableFlag, boolean canFulfillFlag, Long preparedAt, Long fulfilledAt, Long created)`
  - `status` **脱敏后**状态（对被许愿人 `PREPARED` 回显成 `OPEN`）｜`mineFlag` 是不是我许的愿｜`preparedFlag` 标记人视角是否已准备（对许愿人恒 `false`）｜`preparableFlag` 我能不能点「已准备」（非我许的、未实现、未标过）｜`canFulfillFlag` 我能不能确认实现（我许的且未实现）｜`preparedAt` **只对标记人下发**，许愿人处为 `null`｜`fulfilledAt`/`created` 时间戳（`Long`，可空）。

15. `GET /board` → `WishBoardVO`。错误 404「还没有建立情侣空间…」。
16. `POST /add` body `AddRequest{ String title; String note; String ownerUsername }`（`note`/`ownerUsername` 可空，owner 缺省=给自己许）→ `WishBoardVO`
    - 错误：400「想要什么总得写一句呀」／400「愿望最多 80 个字，剩下的见面再说」／400「补充说明最多 200 个字」／400「愿望只能许给自己或者你们的另一半」／400「这条愿望已经在清单上了，别再写一遍啦」／400「愿望清单最多同时挂 30 条，先实现几条再加吧」。给别人许时事件 `wish-added`。
17. `POST /prepare` body `WishIdRequest{ String id }` → `WishBoardVO`（**不推任何 WS 事件**）
    - 错误：404「这条愿望不在你们的清单里」／400「这条愿望是你自己许的，「已准备」那一格是给 TA 留的」／400「这个愿望已经实现啦」／400「已经标过「已准备」了，别再点一次」。
18. `POST /unprepare` body `WishIdRequest{ String id }` → `WishBoardVO`（**不推事件**）
    - 错误：404「这条愿望不在你们的清单里」／400「这条愿望是你自己许的…」／400「这条愿望没被标记过「已准备」」／400「「已准备」是谁标的，就只能由谁撤掉」。
19. `POST /fulfill` body `WishIdRequest{ String id }` → `WishBoardVO`（许愿人本人确认，这时才公开）
    - 错误：404「这条愿望不在你们的清单里」／400「这个愿望已经实现啦，不用再点一次」／400「只有许愿的人自己能确认愿望实现了」。事件 `wish-fulfilled`。
20. `POST /remove` body `WishIdRequest{ String id }` → `WishBoardVO`（仅记录人可删，已实现删不掉）
    - 错误：404「这条愿望不在你们的清单里」／400「已经实现的愿望要留在记录里，删不掉咯」／400「只有记这条愿望的人能删掉它」。**不推事件**。

### `CouplePeriodController` `/api/couple/period`（经期记录 · 4 个）

**响应 record**（写接口一律返回整份看板，前端整体替换）：`PeriodBoardVO(List<PeriodRecordVO> records, int averageCycleDays, String predictedNextDay, Integer daysUntilNext, int cycleSampleCount)`
- `records` 按 `startDay` 新→旧｜`averageCycleDays` 相邻开始日间隔的均值（只取 15–45 天样本，四舍五入；无有效样本时按 28 天估）｜`predictedNextDay` 最后一次开始日 + 平均周期（无记录为 `null`）｜`daysUntilNext` 预测日距今天的天数（已过期为负数，无预测为 `null`）｜`cycleSampleCount` 参与均值的有效间隔数（0=仍在按默认周期估）。
- `PeriodRecordVO(String id, String startDay, String endDay, String recorderUser, Long created)`——`endDay` 还没记结束为 `null`｜`recorderUser` 记录人（只留痕，不作权限闸门，双方都能记/改/删）｜`created` 毫秒时间戳。
- 周期与预测全部**读时算、不落缓存列**（`PeriodCycles`）；四个端点**都不推 WS 事件**（私密记录，不因对方操作弹提醒）。

21. `GET /board` → `PeriodBoardVO`。错误 404「还没有建立情侣空间，先邀请一位好友吧」。
22. `POST /add` body `AddRequest{ String startDay }`（`yyyy-MM-dd`，不能晚于今天；同空间同一天只能一条）→ `PeriodBoardVO`
    - 错误：400「开始日得选一个日子呀」／400「开始日得是 yyyy-MM-dd 这样的日子呀」／400「还没到的日子记不了呀，今天之后的事留给明天 🩸」／400「这一天已经记过啦，看看下面的记录 🩸」／404「还没有建立情侣空间…」。**不推事件**。
23. `POST /end` body `RecordIdRequest{ String id }` → `PeriodBoardVO`（把结束日记成**今天**，只给还没结束的记录补）
    - 错误：404「这条经期记录不在你们的空间里」／400「这次已经记过结束日啦」／400「结束日不能早于开始日呀」（开始日被记到未来时才可能，正常入口已被 add 拦掉）。**不推事件**。
24. `POST /remove` body `RecordIdRequest{ String id }` → `PeriodBoardVO`（删一条错记，双方都能删，不进回收站）
    - 错误：404「这条经期记录不在你们的空间里」。**不推事件**。

### `CoupleIntimateLogController` `/api/couple/intimate`（亲密记录 · 3 个）

**响应 record**（写接口一律返回整份看板，前端整体替换）：`IntimateBoardVO(List<IntimateLogVO> records, String lastDay, String lastKind, int monthCount, int monthEdgeCount, int monthSexCount, int totalCount)`
- `records` 最近 30 条，按 `day` 新→旧、同日再按 `created` 新→旧｜`lastDay`/`lastKind` 最近一次发生的日期与类型（无记录均为 `null`）｜`monthCount` 本月（自然月）总次数｜`monthEdgeCount`/`monthSexCount` 本月按类型拆分｜`totalCount` 累计次数（**统计口径覆盖全部记录，不受 30 条截断影响**）。
- `IntimateLogVO(String id, String day, String kind, String note, String recorderUser, Long created)`——`kind` ∈ `EDGE`（边缘亲昵）/`SEX`（性生活）｜`note` 选填（≤60 字，无则 `null`）｜`recorderUser` 记录人（只留痕，不作权限闸门，双方都能记/删）｜`created` 毫秒时间戳。
- 同一天**允许多条**（无唯一键，与经期卡不同）；三个端点**都不推 WS 事件**、不影响心动值（私密记录，不因对方操作弹提醒）。

25. `GET /board` → `IntimateBoardVO`。错误 404「还没有建立情侣空间，先邀请一位好友吧」。
26. `POST /add` body `AddRequest{ String day; String kind; String note }`（`day` 为 `yyyy-MM-dd` 且不能晚于今天；`kind` 必须是 `EDGE`/`SEX`；`note` 可空）→ `IntimateBoardVO`
    - 错误：400「哪天呀？日子得选一个」／400「日子得是 yyyy-MM-dd 这样的」／400「还没到的日子记不了呀，今天之后的事留给明天 💞」／400「这类记录认不出来，选「边缘」或「生活」就好」／400「备注最多 60 个字，留白也很甜」／404「还没有建立情侣空间…」。**不推事件**。
27. `POST /remove` body `RecordIdRequest{ String id }` → `IntimateBoardVO`（删一条，双方都能删，不进回收站）
    - 错误：404「这条亲密记录不在你们的空间里」。**不推事件**。

### `CoupleCalendarController` `/api/couple/calendar`（亲密日历 · 1 个）

**响应 record**：`CoupleCalendarVO(String month, List<String> periodDays, List<String> periodStartDays, List<String> periodEndDays, String predictedNextDay, List<CalendarIntimateVO> intimate)`
- `month` 后端回写的 `yyyy-MM`（入参补齐后的原值）｜`periodDays` 这个月里被经期覆盖的天，`yyyy-MM-dd` 升序，**只含本月的天**（跨月的经期在两个月各铺各的那段）。
- `periodStartDays`/`periodEndDays` 覆盖日里哪几天算开始、哪几天算结束——开始日/结束日落在上个月时这里就是空数组，前端不必自己判区间。
- `predictedNextDay` 照 `PeriodCycles` 的推算口径原样给出（可能为 `null`，也可能落在别的月份，前端只标那一天，不铺预测区间）。
- `intimate` 本月的亲密标记，`CalendarIntimateVO(String day, String kind, String note, String recorderUser)`，按 `day` 升序、同日按记录时刻升序；**没有 `id`/`created`**——日历只看不删，删除回记录卡。条数不受亲密看板那 30 条上限影响。
- 覆盖日的判据在领域里：`PeriodRecord.covers(day, today)`——记了结束日按 [开始日, 结束日] 含首含尾，还没记结束日只到今天；脏日期静默算没覆盖，不让一行历史数据把整卡打成 500。

28. `GET /month?month=yyyy-MM` → `CoupleCalendarVO`。未来的月份正常返回（`periodDays`/`intimate` 为空，只可能带 `predictedNextDay`）。
    - 错误：400「要看哪个月呀？给我 yyyy-MM 这样的」（缺参或空白）／400「月份得是 yyyy-MM 这样的」（格式错）／404「还没有建立情侣空间，先邀请一位好友吧」。**不推事件**，也不参与心动值。

> 这个读口跨两个聚合（`domain/period` 与 `domain/intimatelog`），是**只读**的：写仍然走经期卡与亲密卡各自的端点。当初让前端自己拿 `/period/board` + 亲密看板拼日历被单测拦下——同一页会打两次 `/period/board`、留下两份经期真相，故收成一个后端读模型。
