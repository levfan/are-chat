# REST 接口总表

> 本页回答：全部 Controller 的端点清单（方法 + 路径 + 请求体 + 响应字段 + 错误码）。路径省略类前缀 `/api`；couple 系前缀见各节标题。
> **当前口径（2026-10-05 二轮裁剪后实测）**：全仓 **18 个 Controller / 82 个端点**（逐条读各 `*Controller.java` 的 `@GetMapping/@PostMapping/@PutMapping/@DeleteMapping`，类上 `@RequestMapping` 是基址）。
> 分上下文：identity 16、messaging 32、platform 6、filestorage 2、couple 26。其中 **couple 侧 7 个 Controller / 26 个端点**——2026-10-05 按 `docs/adr/0010-couple-trim-to-v8-features.md` 从 70 收缩到 26。
> 更正一处旧口径：非 couple 侧是 **11 个 Controller**（不是旧文档写的 10）——`/api/admin` 基址同时由 identity 的 `AdminController` 与 platform 的 `AnnouncementAdminController` 两个类贡献。

来源：`src/main/java` 各 `*Controller.java` 的映射注解与 javadoc；响应字段取自对应 Service 内嵌的 `public record XxxVO(...)`。返回统一 `ApiResponse{code,message,data}`；除 `/api/auth/**` 与 `/api/health` 外均需登录会话。业务失败抛 `BusinessException(code, 中文原话)`，`code` 同时作为 HTTP 状态码。可空性按字段类型判断：`Long`/`Integer`/包装类型可为 `null`，`long`/`int`/`boolean` 不为 `null`。

## 非 couple 接口

> 本节端点逐条与源码注解核对过，方法/路径与现役一致（identity 16 + messaging 32 + platform 6 + filestorage 2 = 56）。

### `AuthController` `/auth`
- POST `/sms-code` 注册验证码（演示回显 devCode）｜ POST `/register` 提交注册审批申请
- GET `/register-status` 审批进度轮询 ｜ POST `/login` 登录 ｜ POST `/logout` 登出
- GET `/me` 当前用户 ｜ PUT `/password` 改密（旧密码+强度校验）｜ POST `/deactivate` 自助注销（清好友+失效会话）

### `AdminController` `/admin`（identity · 仅管理员）
- GET `/applications` 审批列表 ｜ POST `/applications/{id}/approve` `/reject` 通过/驳回
- GET `/pending-count` 待办数 ｜ GET `/users` 用户列表 ｜ POST `/users/{username}/status` 启/停 ｜ POST `/users/{username}/reset-password` 重置密码
- GET `/audit` 审计日志

### `AnnouncementAdminController` `/admin`（platform · 仅管理员）
- POST `/announcements` 发布公告 ｜ POST `/announcements/{id}/close` 关闭 ｜ GET `/announcements` 列表

### `FriendController` `/friends`
- GET `` 好友列表 ｜ GET `/suggest` 加好友联想 ｜ POST `/requests` 发申请
- GET `/requests/incoming` `/requests/outgoing` 收/发申请 ｜ POST `/requests/{id}/accept` `/reject`
- PUT `/{id}` 改备注 ｜ DELETE `/{id}` 删除好友

### `PrivateMessageController` `/messages`
- GET `/{peer}` 会话历史 ｜ GET `/{peer}/search` 会话内搜索 ｜ GET `/{peer}/export` 导出 JSON ｜ GET `/search/global` 全局搜索
- POST `/{peer}` 发送 ｜ POST `/{peer}/read` 已读 ｜ POST `/{id}/recall` 撤回 ｜ PUT `/{id}` 2 分钟内编辑
- POST `/{id}/reactions` 表情回应 toggle ｜ POST `/{id}/star` 收藏 toggle ｜ POST `/{id}/heart` 心动时刻 toggle ｜ GET `/hearts` 心动列表
- POST/DELETE/GET `/{peer}/pin` 置顶/取消/查询 ｜ DELETE `/{peer}` 清空会话 ｜ GET `/{peer}/attachments?type=image|file` 附件墙

### `ProfileController` `/profile`
- GET `` 我的资料 ｜ GET `/{username}` 好友资料卡 ｜ PUT `` 保存资料 ｜ GET `/friends-birthdays` 好友生日列表（F42）

### 其他
- `PresenceController` GET `/presence/online` 在线名单
- `StarsController` GET `/stars` 收藏消息聚合
- `AnnouncementController` GET `/announcements/current` 生效公告 ｜ POST `/announcements/{id}/read` 已读
- `HealthController` GET `/health` 健康检查（免登录）
- `FileController` POST `/files`（multipart）上传 ｜ GET `/files/{id}/download` 下载
- WS：`/ws/chat/{name}` 聊天室与推送通道（非 REST，见 [architecture.md](architecture.md)）

## couple 接口（现役 7 个 Controller / 26 个端点）

> 逐端点写明：方法、路径、请求体字段、响应 record 逐字段（含可空性）、错误码与中文原话。
> 原 10 张历史卡的 Controller（贴贴 `CoupleBondController`、求抱抱 `CoupleCareController`、安全词 `CoupleCatchController`、
> 愿望券 `CoupleCeremonyController`、饭桌 `CoupleDiningController`、家务 `CoupleFactoryController`、加班 `CoupleQuestController`、
> 好事簿 `CoupleEchoController`、惊喜 `CoupleSurpriseController`、收藏 `CouplePinController`）已随 ADR-0010 全部下线；
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

**响应 record**：`StreakBoardVO(String day, int currentStreak, int longestStreak, int confirmedDays, boolean checkedToday, boolean missedYesterday, String lastCheckinDay, List<TierVO> tiers, String nextTierKey, String nextTierLabel, int daysToNext, List<StripCellVO> strip, int makeupWindowDays, int makeupLeftThisMonth, boolean canMakeup)`
- `day` 今天 `yyyy-MM-dd`｜`currentStreak` 当前连续（今天没打从昨天起算，不算断）｜`longestStreak` 历史最长峰值｜`confirmedDays` 累计打卡天数（非连续）｜`checkedToday` 今天是否已打｜`missedYesterday` 昨天是否缺行（断签判据）｜`lastCheckinDay` 最近打卡日（`String`，无则 `null`）｜`tiers` 七档逐档（下）｜`nextTierKey`/`nextTierLabel` 下一未解锁档（`String`，全解锁为 `null`）｜`daysToNext` 距下一档还差几天（按当前连续）｜`strip` 近 21 格日历条（下）｜`makeupWindowDays` 补签窗口（固定 7）｜`makeupLeftThisMonth` 本自然月还能补几次（3−已用）｜`canMakeup` 后端算好=昨天断了且还有额度。
  - **注**：补签已改为**免费**，不再有「积分价格 / 账户余额」这类字段（原积分口径字段随台账一并删除），额度改由 `makeupWindowDays`/`makeupLeftThisMonth` 表达。
- `TierVO(String key, int days, String label, String icon, String detail, boolean unlocked, String unlockedDay)`——`unlockedDay` 首次跨过的日期（`String`，未解锁 `null`）。
- `StripCellVO(String day, boolean checked, boolean makeupFlag, boolean todayFlag)`。

11. `GET /board` → `StreakBoardVO`。错误 404「还没有建立情侣空间，先邀请一位好友吧」。
12. `POST /makeup` body `MakeupRequest{ String day }`（`yyyy-MM-dd`）→ `StreakBoardVO`（补签成功落一行 `MAKEUP`，返回整份看板）。
    - 错误（闸门在 `MakeupPolicy`，**免费**）：400「想补哪一天？日期没给呢」／400「日期格式应为 yyyy-MM-dd」／400「今天还不能补——两个人都答完今天的每日一问就算打卡 😉」／400「只能补最近 7 天里的缺口，太久以前的那天就让它过去吧」／400「<day> 已经打过卡了，不用补」／400「这个月已经补过 3 次了，下个月再来吧（<年-月>）」／404「还没有建立情侣空间…」。事件 `streak-makeup`（+ 跨档补推 `streak-unlocked`）。

### `CoupleQuestionController` `/api/couple/question`（每日一问 · 3 个）

**响应 record**：`TodayVO(String day, int index, String question, AnswerVO mine, String partnerAnswer, boolean answeredByMe, boolean answeredByPartner, boolean bothAnswered, int answerMax)`
- `index` 当日题号（`stableHash(spaceId|v8-question-bank|day)` mod 75）｜`mine` 我的作答（`AnswerVO`，我没答为 `null`）｜`partnerAnswer` 对方答案（`String`，**双方都答完才有，否则 `null`**）｜`answerMax` 回答字数上限（300）。
- `AnswerVO(String username, String answer, Long createdAt, Long updatedAt)`（`updatedAt` 未改写为 `null`）。

13. `GET /today` → `TodayVO`。错误 404「还没有建立情侣空间…」。
14. `POST /answer` body `AnswerRequest{ String answer }`（当天可改写，每人每天一行）→ `TodayVO`。
    - 错误：400「写一句再交卷呀 📝」／400「回答最多 300 个字，短一点更像人话」／404「还没有建立情侣空间…」。事件 `question-answered`；**若对方已答则本次触发打卡**（`streak-checkin`，可能补推 `streak-unlocked`）。
15. `GET /history?days=`（`days` 可空，1-90，缺省/非法取 14）→ `HistoryListVO(List<HistoryVO> items, int answeredDays, int bothAnsweredDays)`
    - `HistoryVO(String day, String question, String myAnswer, String partnerAnswer, boolean bothAnswered)`（未答一侧 `myAnswer`/`partnerAnswer` 为 `null`）；`answeredDays` 我答过的天数、`bothAnsweredDays` 双方都答完的天数。错误 404「还没有建立情侣空间…」。

### `CoupleWishController` `/api/couple/wish`（愿望清单 · 7 个）

**响应 record**（写接口一律返回整份清单，前端按请求者视角整体替换）：`WishBoardVO(List<WishVO> open, List<WishVO> prepared, List<WishVO> fulfilled, int openCount, int limit, int titleMax, int noteMax)`
- 三组按**当前请求者视角**分组：`prepared` 只对标记人自己出现，许愿人那里那条仍在 `open`。`limit` 未实现上限（30）｜`titleMax` 标题上限（80）｜`noteMax` 说明上限（200）。
- `WishVO(String id, String ownerUser, String creatorUser, String title, String note, String status, boolean mineFlag, boolean preparedFlag, boolean preparableFlag, boolean canFulfillFlag, Long preparedAt, Long fulfilledAt, Long created)`
  - `status` **脱敏后**状态（对被许愿人 `PREPARED` 回显成 `OPEN`）｜`mineFlag` 是不是我许的愿｜`preparedFlag` 标记人视角是否已准备（对许愿人恒 `false`）｜`preparableFlag` 我能不能点「已准备」（非我许的、未实现、未标过）｜`canFulfillFlag` 我能不能确认实现（我许的且未实现）｜`preparedAt` **只对标记人下发**，许愿人处为 `null`｜`fulfilledAt`/`created` 时间戳（`Long`，可空）。

16. `GET /board` → `WishBoardVO`。错误 404「还没有建立情侣空间…」。
17. `POST /add` body `AddRequest{ String title; String note; String ownerUsername }`（`note`/`ownerUsername` 可空，owner 缺省=给自己许）→ `WishBoardVO`
    - 错误：400「想要什么总得写一句呀」／400「愿望最多 80 个字，剩下的见面再说」／400「补充说明最多 200 个字」／400「愿望只能许给自己或者你们的另一半」／400「这条愿望已经在清单上了，别再写一遍啦」／400「愿望清单最多同时挂 30 条，先实现几条再加吧」。给别人许时事件 `wish-added`。
18. `POST /prepare` body `WishIdRequest{ String id }` → `WishBoardVO`（**不推任何 WS 事件**）
    - 错误：404「这条愿望不在你们的清单里」／400「这条愿望是你自己许的，「已准备」那一格是给 TA 留的」／400「这个愿望已经实现啦」／400「已经标过「已准备」了，别再点一次」。
19. `POST /unprepare` body `WishIdRequest{ String id }` → `WishBoardVO`（**不推事件**）
    - 错误：404「这条愿望不在你们的清单里」／400「这条愿望是你自己许的…」／400「这条愿望没被标记过「已准备」」／400「「已准备」是谁标的，就只能由谁撤掉」。
20. `POST /fulfill` body `WishIdRequest{ String id }` → `WishBoardVO`（许愿人本人确认，这时才公开）
    - 错误：404「这条愿望不在你们的清单里」／400「这个愿望已经实现啦，不用再点一次」／400「只有许愿的人自己能确认愿望实现了」。事件 `wish-fulfilled`。
21. `POST /note` body `NoteRequest{ String id; String note }` → `WishBoardVO`（仅记录人可改）
    - 错误：404「这条愿望不在你们的清单里」／400「只有记这条愿望的人能改它」／400「补充说明最多 200 个字」。**不推事件**。
22. `POST /remove` body `WishIdRequest{ String id }` → `WishBoardVO`（仅记录人可删，已实现删不掉）
    - 错误：404「这条愿望不在你们的清单里」／400「已经实现的愿望要留在记录里，删不掉咯」／400「只有记这条愿望的人能删掉它」。**不推事件**。

### `CoupleMemoryController` `/api/couple/memory`（百日隐藏页 · 1 个）

23. `GET /page` → `MemoryVO(String summary, long daysTogether, int confirmedDays, int longestStreak, int currentStreak, int makeupDays, int bothAnsweredDays, int fulfilledWishes, String intimacyTitle, String unlockedDay, List<TimelineItemVO> timeline)`
    - `summary` `RelationSummary` 规则生成的一句话（**无 LLM**）｜`unlockedDay` 首次满 100 天之日（`String`，理论可达）｜`TimelineItemVO(String day, String kind, String title, String detail)`，`kind` ∈ `space/unlock/question/wish/streak`，时间轴上限 80 条，建立/解锁/答完/实现四类不被截断。
    - 错误（服务端闸门）：404「还没有建立情侣空间，先邀请一位好友吧」／400「这一页要连续贴满 100 天才打开，现在还差 N 天」（`longestStreak<100`）。

### `CoupleNotifyController` `/api/couple/notify`（通知中心 · 2 个）

24. `GET ``（基址 `/api/couple/notify`） → `NotifyListVO(List<NotifyVO> items, long unread)`
    - `items` 最近通知（`NotifyVO(String id, String event, String actor, String detail, boolean read, Long created)`；`actor` 可为 `system`=定时任务；`created` 可空）｜`unread` 未读条数。**不需已建空间**（邀请到达时对方离线也靠这里补看）。
25. `POST /notify/read-all` → `Void`（全部标记已读）。

### `CoupleAdminController` `/api/couple/admin`（运营看板 · 1 个）

26. `GET /stats`（基址 `/api/couple/admin`） → `CoupleStatsVO(long activeSpaces, long dissolvedSpaces, long avgDays, long totalCheckinDays, long totalAnswers, long spacesCreatedThisMonth)`（record 定义在 Controller 内，字段全 `long` 非空）
    - `totalCheckinDays` 来自 `couple_streak_day`、`totalAnswers` 来自 `couple_question_answer`（裁剪后统计项不再读贴贴/好事簿）。错误：403「仅管理员可查看运营看板」。
