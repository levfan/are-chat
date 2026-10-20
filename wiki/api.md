# REST 接口总表

> 本页回答：全部 Controller 的端点清单（方法 + 路径 + 请求体 + 响应字段 + 错误码）。路径省略类前缀 `/api`；couple 系前缀见各节标题。
> **当前口径（2026-10-20 三轮裁剪后逐条实测）**：全仓 **14 个 Controller / 62 个端点**（逐条读各 `*Controller.java` 的 `@GetMapping/@PostMapping/@PutMapping/@DeleteMapping`，类上 `@RequestMapping` 是基址；`GlobalExceptionHandler` 不是 Controller，不计入）。
> 分上下文：identity 15、messaging 25、platform 6、filestorage 2、couple 14（合计 62）。其中 **couple 侧 4 个 Controller / 14 个端点**——2026-10-05 按 `docs/adr/0010-couple-trim-to-v8-features.md` 从 70 收缩到 26，随后再下线百日隐藏页收缩到 25，2026-10-20 再删除通知中心（2）与运营看板（1）后为 22，同日再删问答回看与愿望改备注（2）后为 20，随后新增经期记录（4）、亲密记录（3）与亲密日历（1）后为 28，同日三轮裁剪按「只保留在一起天数与日历记录」再下线每日一问（2）、连续互动打卡（2）、愿望清单（6）、空间个性化与心动值（2）、经期与亲密两个看板读口（2）后为 14。
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

## couple 接口（现役 4 个 Controller / 14 个端点）

> 逐端点写明：方法、路径、请求体字段、响应 record 逐字段（含可空性）、错误码与中文原话。
> 原 10 张历史卡的 Controller（贴贴 `CoupleBondController`、求抱抱 `CoupleCareController`、安全词 `CoupleCatchController`、
> 愿望券 `CoupleCeremonyController`、饭桌 `CoupleDiningController`、家务 `CoupleFactoryController`、加班 `CoupleQuestController`、
> 好事簿 `CoupleEchoController`、惊喜 `CoupleSurpriseController`、收藏 `CouplePinController`）已随 ADR-0010 全部下线；
> 百日隐藏页 `CoupleMemoryController` 与其 `/page` 端点也在随后一轮下线；
> 通知中心 `CoupleNotifyController`（2 个端点）与 F45 运营看板 `CoupleAdminController`（1 个端点）在 2026-10-20 删除；
> `CoupleController` 上的 `/moods`、`/anniversaries`（复数 CRUD）也一并删除。
> 2026-10-20 三轮裁剪再删掉每日一问 `CoupleQuestionController`（2 个端点）、连续互动打卡 `CoupleStreakController`（2 个端点）、
> 愿望清单 `CoupleWishController`（6 个端点），以及 `CoupleController` 的 `PUT /profile`（空间个性化）与 `GET /intimacy`（心动值）。详见 [couple-space.md](couple-space.md)。

### `CoupleController` `/api/couple`（地基：建立 + 空间本体 · 8 个）

**公共响应 record**（本节多处复用）
- `SpaceVO(String id, PartnerVO partner, Long created, String anniversary, long days)`
  - `id` 空间 UUID｜`partner` 对方视角（下）｜`created` 建立毫秒时间戳（`Long`，可空）｜`anniversary` 在一起的日子 `yyyy-MM-dd`（可空）｜`days` 在一起天数（`long`，非空，含当天≥1）
- `PartnerVO(String username, String nickname, String avatar, boolean online)`
  - `username` 对方用户名｜`nickname` 昵称（缺省回退用户名）｜`avatar` 头像标识（无则空串）｜`online` 是否在线（`boolean`）
- `InviteVO(String id, String fromUser, String toUser, String message, String status, Long created)`
  - `message` 留言（null 归一为空串）｜`status` ∈ `PENDING/ACCEPTED/REJECTED/CANCELED`｜`created` 邀请毫秒时间戳（`Long`）

**端点**

1. `GET /overview` → `OverviewVO(SpaceVO space, List<InviteVO> incoming, List<InviteVO> outgoing)`
   - `space`：未建空间时为 `null`（前端据此显示建立指引）；`incoming`/`outgoing` 我的收/发待处理邀请（按时间新→旧）。
2. `POST /invites` body `InviteRequest{ String username; String message }`（`message` 可空，≤100 字）→ `InviteVO`
   - 错误：400「想邀请谁？请先选择一位好友」／400「不能和自己建立情侣空间哦」／400「查无此人：对方还没注册或已注销」／400「只能邀请自己的好友，先去通讯录加个好友吧」／400「邀请留言最长 100 个字」／409「你已经在情侣空间里啦，先解除才能发起新邀请」／409「对方已经在别的情侣空间里了」／409「你们之间已有待处理的情侣邀请，等对方处理吧」。事件 `invite`。
3. `POST /invites/{id}/accept` → `SpaceVO`
   - 错误：404「邀请不存在」／403「只能处理发给自己的邀请」／409「该邀请已经处理过了」／409「无法同意：有一方已经进入其他情侣空间」。事件 `invite-accepted`。
4. `POST /invites/{id}/reject` → `Void`
   - 错误：404「邀请不存在」／403「只能处理发给自己的邀请」／409「该邀请已经处理过了」。事件 `invite-rejected`。
5. `DELETE /invites/{id}` → `Void`（撤回自己发出的待处理邀请）
   - 错误：404「邀请不存在」／403「只能撤回自己发出的邀请」／409「该邀请已经处理过了」。**不推事件**。
6. `PUT /anniversary` body `AnniversaryDateRequest{ String date }`（`yyyy-MM-dd`）→ `SpaceVO`
   - 错误：404「还没有建立情侣空间，先邀请一位好友吧」／400「纪念日格式应为 yyyy-MM-dd」／400「已解散的空间不再改纪念日」。事件 `anniversary-updated`。
7. `GET /relationship-of/{username}` → `RelationshipVO(boolean inRelationship, Long days, String anniversary)`（F44 恋爱中徽章，好友资料卡用）
   - `days`/`anniversary` 在 `inRelationship=false` 时为 `null`。错误：400「用户名不能为空」／403「只有好友才能查看恋爱状态」。
8. `POST /dissolve` → `Void`（本人解除）。错误 404「还没有建立情侣空间，先邀请一位好友吧」；事件 `dissolved`。

### `CouplePeriodController` `/api/couple/period`（经期记录 · 3 个）

> 卡片合并后这一张只剩写口：三个端点都返回 `Void`，展示数据统一由 `GET /api/couple/calendar/month` 下发，前端写完重拉当月（原 `GET /period/board` 已随卡片下线）。
- 周期与预测全部**读时算、不落缓存列**（`PeriodCycles`），由日历读模型转发生成；三个端点**都不推 WS 事件**（私密记录，不因对方操作弹提醒）。

9. `POST /add` body `AddRequest{ String startDay }`（`yyyy-MM-dd`，不能晚于今天；同空间同一天只能一条）→ `Void`
    - 错误：400「开始日得选一个日子呀」／400「开始日得是 yyyy-MM-dd 这样的日子呀」／400「还没到的日子记不了呀，今天之后的事留给明天 🩸」／400「这一天已经记过啦，看看下面的记录 🩸」／404「还没有建立情侣空间…」。**不推事件**。
10. `POST /end` body `RecordIdRequest{ String id }` → `Void`（把结束日记成**今天**，只给还没结束的记录补）
    - 错误：404「这条经期记录不在你们的空间里」／400「这次已经记过结束日啦」／400「结束日不能早于开始日呀」（开始日被记到未来时才可能，正常入口已被 add 拦掉）。**不推事件**。
11. `POST /remove` body `RecordIdRequest{ String id }` → `Void`（删一条错记，双方都能删，不进回收站）
    - 错误：404「这条经期记录不在你们的空间里」。**不推事件**。

### `CoupleIntimateLogController` `/api/couple/intimate`（亲密记录 · 2 个）

> 同样只剩写口：两个端点都返回 `Void`，本月标记与累计次数由 `GET /api/couple/calendar/month` 下发（原 `GET /intimate/board` 已随卡片下线）。
- `kind` ∈ `EDGE`（边缘亲昵）/`SEX`（性生活）｜`note` 选填（≤60 字，无则 `null`）｜`recorderUser` 记录人（只留痕，不作权限闸门，双方都能记/删）。
- 同一天**允许多条**（无唯一键，与经期记录不同）；两个端点**都不推 WS 事件**（私密记录，不因对方操作弹提醒；心动值已整体下线，谈不上参与）。

12. `POST /add` body `AddRequest{ String day; String kind; String note }`（`day` 为 `yyyy-MM-dd` 且不能晚于今天；`kind` 必须是 `EDGE`/`SEX`；`note` 可空）→ `Void`
    - 错误：400「哪天呀？日子得选一个」／400「日子得是 yyyy-MM-dd 这样的」／400「还没到的日子记不了呀，今天之后的事留给明天 💞」／400「这类记录认不出来，选「边缘」或「生活」就好」／400「备注最多 60 个字，留白也很甜」／404「还没有建立情侣空间…」。**不推事件**。
13. `POST /remove` body `RecordIdRequest{ String id }` → `Void`（删一条，双方都能删，不进回收站）
    - 错误：404「这条亲密记录不在你们的空间里」。**不推事件**。

### `CoupleCalendarController` `/api/couple/calendar`（亲密日历 · 1 个）

**响应 record**：`CoupleCalendarVO(String month, List<String> periodDays, List<String> periodStartDays, List<String> periodEndDays, List<CalendarPeriodVO> periods, String predictedNextDay, int averageCycleDays, int cycleSampleCount, Integer daysUntilNext, int intimateTotal, List<CalendarIntimateVO> intimate)`
- `month` 后端回写的 `yyyy-MM`（入参补齐后的原值）｜`periodDays` 这个月里被经期覆盖的天，`yyyy-MM-dd` 升序，**只含本月的天**（跨月的经期在两个月各铺各的那段）。
- `periodStartDays`/`periodEndDays` 覆盖日里哪几天算开始、哪几天算结束——开始日/结束日落在上个月时这里就是空数组，前端不必自己判区间。
- `periods` 与本月相交的那些经期记录本身，`CalendarPeriodVO(String id, String startDay, String endDay)`——`endDay` 为 `null` 就是还没结束，日历据此给「今天结束了」，删除也认这个 `id`。
- `predictedNextDay`/`averageCycleDays`/`cycleSampleCount`/`daysUntilNext` 照 `PeriodCycles` 的推算口径原样给出（预测日可能为 `null`，也可能落在别的月份，前端只标那一天，不铺预测区间）。
- `intimate` 本月的亲密标记，`CalendarIntimateVO(String id, String day, String kind, String note, String recorderUser)`，按 `day` 升序、同日按记录时刻升序；带 `id` 是因为日历就是删这些记录的唯一入口，条数不受历史 30 条看板上限影响。`intimateTotal` 走 `IntimateLogs` 的全量累计次数。
- 覆盖日的判据在领域里：`PeriodRecord.covers(day, today)`——记了结束日按 [开始日, 结束日] 含首含尾，还没记结束日只到今天；脏日期静默算没覆盖，不让一行历史数据把整卡打成 500。

14. `GET /month?month=yyyy-MM` → `CoupleCalendarVO`。未来的月份正常返回（`periodDays`/`intimate` 为空，只可能带 `predictedNextDay`）。
    - 错误：400「要看哪个月呀？给我 yyyy-MM 这样的」（缺参或空白）／400「月份得是 yyyy-MM 这样的」（格式错）／404「还没有建立情侣空间，先邀请一位好友吧」。**不推事件**。

> 这个读口跨两个聚合（`domain/period` 与 `domain/intimatelog`），是**只读**的：写仍然走经期与亲密各自的端点。当初让前端自己拼看板被单测拦下——同一页会打两次 `/period/board`、留下两份经期真相，故收成一个后端读模型；三轮裁剪把两张记录卡合并进这一张后，看板读口也随之只剩这一个。
