# 情侣空间业务全景

> 本页回答：couple 包现在有哪些功能、各自对应哪个 Controller/Service/表，以及贯穿全局的核心业务规则。
> 事实源：`src/main/java/com/smart/chat/couple/**`（Controller/Service/domain）、`src/main/resources/schema.sql` + `db/V52/V53/V54/V55/V56`、
> 术语合同 `CONTEXT.md`、去留判据 `docs/adr/0010-couple-trim-to-v8-features.md`；其后各轮裁剪的取舍见本页「本轮裁剪」与第 3 节。

## 本轮裁剪（先读这段）

情侣空间在 2026-10-05 经过三轮改造：先加四张功能卡（每日一问 / 愿望清单 / 连续互动打卡 + 七档解锁 / 百日隐藏回顾，见
`docs/adr/0007`），随后按用户指令「除了这几项，其余功能全部删除」做了**一轮大裁剪**——判据从「排名高低」换成
「这四项及其赖以成立的地基还活不活」，取舍、连带改造与被删清单的唯一依据是 **`docs/adr/0010-couple-trim-to-v8-features.md`**；
最后又按「用不上的功能也裁掉」下线了百日隐藏回顾与心动时刻，只留三张卡；2026-10-20 按用户要求依次新增第四张卡「经期记录」、第五张卡「亲密记录」与第六张卡「亲密日历」，同日的**三轮裁剪**再按用户指令「保留在一起天数、纪念日、美柚app上的日历记录，其它功能都不要了」把每日一问、连续互动打卡与六档解锁、愿望清单、心动值与七级恋爱等级、空间个性化（宣言/主题/爱称）整条下线，并把经期与亲密两张记录卡合并进亲密日历一张卡——现役只剩**一张功能卡 + 地基**。

现役规模（实测自源码与 `schema.sql`）：**34 个 java 文件 / 4 个 Controller / 14 个端点 / 5 张 `couple_*` 表（`couple_notify` 已无代码）/ 6 个 WS 事件 / 1 条定时任务**。
按 ADR-0010 与随后几轮已下线、**不再存在**的功能（一句话点名即可，别当现役去找）：心情日记、贴贴宫格、求抱抱、安全词与暂停复盘、
今晚饭桌、家务轮盘、加班预报与留灯、愿望券本、好事簿、刮刮乐与盲盒、共同日历（倒数日清单）、积分台账、卡片收藏与功能搜索、百日隐藏回顾、心动时刻、通知中心、运营看板、
每日一问、连续互动打卡与六档解锁、愿望清单、心动值与七级恋爱等级、空间个性化（宣言/主题/爱称）。
其中三处曾是别的功能算数的根据，按 ADR-0010 重新改挂过：**打卡触发源**改挂每日一问（原挂贴贴）、**补签稀缺性**改由次数守（原扣积分台账）、
**心动值供数**换成五项（原六项的表全部消失）——这三处改挂在 2026-10-20 三轮裁剪后一并作废，问、打卡、愿望、心动值全都没了，没有供数可挂。

## 现役的一张功能卡 + 地基

地基（邀请建立、在一起的日子与天数、解除关系、注销连带清理）不是卡，删不得；经期与亲密的记录写口虽然各自还留一个 Controller，
但入口已经收进亲密日历这一张卡，卡片对应的是那一个月度读口。

| 卡（根 `data-testid`） | Controller（前缀） | Service | 表 |
|---|---|---|---|
| 亲密日历 `couple-calendar` | `CoupleCalendarController` `/api/couple/calendar`（月度读模型，1 个端点）；写口仍分属 `CouplePeriodController` `/api/couple/period`（3 个）与 `CoupleIntimateLogController` `/api/couple/intimate`（2 个） | `CoupleCalendarService`（只读，跨经期与亲密两个聚合）+ `CouplePeriodService` + `CoupleIntimateLogService`（两个都只写不投影） | `couple_period`, `couple_intimacy_log` |
| 地基（建立/在一起的日子/解除） | `CoupleController` `/api/couple`（8 个端点） | `CoupleService` | `couple_space`, `couple_invite` |

> `CoupleNotifyController`（F41 通知中心，2 个端点）与 `CoupleAdminController`（F45 运营看板，1 个端点）已在 2026-10-20 删除；
> `couple_notify` 表保留未删但已无读写代码。`CouplePinController`（收藏卡 + 功能搜索）随 ADR-0010 第 7 条删除，
> 百日隐藏页的 `CoupleMemoryController` / `CoupleMemoryService` / `RelationSummary` 也在随后一轮下线。
> 同日三轮裁剪再删掉 `CoupleQuestionController`（每日一问 2 个端点）、`CoupleStreakController`（连续互动打卡 2 个端点）、
> `CoupleWishController`（愿望清单 6 个端点），以及 `CoupleController` 的 `PUT /profile`（空间个性化）与 `GET /intimacy`（心动值）、
> 经期与亲密两个看板读口 `GET /period/board`、`GET /intimate/board`。

## 1 · 邀请建立（状态机）

- **唯一容器**：`CoupleSpace` 是聚合根，绑定后双方固定存为 `userA`/`userB`（**用户名区分大小写、字典序小者为 A**，规范化只在 `CoupleSpace.open()` 一处）；`partnerOf(me)` 是全系统唯一的「对方」定义。各功能表都带 `space_id`。
- **邀请状态机**（`domain/invite/Invite`）：`PENDING → ACCEPTED / REJECTED / CANCELED` 单向，落子状态后不可再动（`requirePending` 一处收口）。归属闸：`acceptBy/rejectBy` 只能处理**发给自己的**（否则 403「只能处理发给自己的邀请」）；`cancelBy` 只能撤回**自己发出的**（403「只能撤回自己发出的邀请」）；已处理过再点 → 409「该邀请已经处理过了」；留言上限 100 字（`MESSAGE_MAX`）。
- **建立流程**（`CoupleService`）：`invite()` → 对方 `accept()`。`accept()` 经 `CoupleSpace.open()` 走聚合工厂建行（ADR-0007 第 1 条修复的领域漂移：不再 `PO` 直插），落库后推事件。事件：`invite` / `invite-accepted` / `invite-rejected`；`cancel` **不推事件**。（曾经在建空间当天写第 1 个打卡日、把当天算成连续第 1 天，随连续互动打卡卡于 2026-10-20 下线。）
- **前置闸门**（全在 `CoupleService.invite()`，400/409）：未选人「想邀请谁？请先选择一位好友」；对自己「不能和自己建立情侣空间哦」；查无此人「查无此人：对方还没注册或已注销」；非好友「只能邀请自己的好友，先去通讯录加个好友吧」；已在别的空间 409「你已经在情侣空间里啦…」「对方已经在别的情侣空间里了」；同两人已有待处理单 409「你们之间已有待处理的情侣邀请…」。
- **未建空间**一律 `requireSpace(me)` 抛 **404「还没有建立情侣空间，先邀请一位好友吧」**，不由前端兜。
- **解除**：`POST /api/couple/dissolve`（本人）或注销级联 `purgeUser()`，历史数据保留但不再互可见，事件 `dissolved`。
- **推送可见性**：情侣事件只经 WS 推给在线端；F41 通知中心已删除，`couple_notify` 不再落库，离线用户上线后看不到历史提醒（ADR-0010 第 1 条的通知中心部分已作废）。

## 2 · 空间本体（在一起的日子 = 纪念日、在一起天数）

- **在一起的日子 `anniversary`**：`PUT /api/couple/anniversary` 绑定单个日期（`yyyy-MM-dd`，缺省取 `created`），是一起天数与纪念倒数共同的根据（事件 `anniversary-updated`）。**这不是共同日历**——`couple_anniversary` 倒数日清单已随 ADR-0010 第 8 条删除。
- **在一起天数 `days`**：`SpaceVO.days` / `RelationshipVO.days` 都由那个日子读时算到今天，含当天（建立当天算第 1 天），库里只存日子不存数。
- **纪念倒数（现役唯一一条定时任务）**：`CoupleReminderJob` 每天 09:30（Asia/Shanghai）扫全部 ACTIVE 空间，按周年重复算倒数，提前 7 天 / 1 天 / 当天各给双方推一次 `anniversary-reminder`（每天只跑一次，天然按天去重）。原 `CoupleQuestionJob`（09:00 定每日一问的题）已随每日一问删除。
- **已下线的三项个性化**：宣言 `slogan`（≤60 字）、主题 `theme`（白名单 classic/cherry/ocean/forest/night）、爱称 `petName`（≤30 字，只能由对方改）连同写入口 `PUT /api/couple/profile` 与 `space-themed` / `pet-name-changed` 两个事件，于 2026-10-20 三轮裁剪整条删除（ADR-0010 第 12 条那套「按真正改了什么分流推送」的口径随之作废，`CoupleProfilePushTest` 也删了）。`couple_space` 的 `nick_a`/`nick_b`/`slogan`/`theme`/`city_a`/`city_b` 六列**保留为死存储**——与 `couple_notify`、`message_star` 同口径，删列要动聚合与适配器两侧的映射、收益只是六列 NULL，而 `CoupleSpace` 聚合已不再持有它们。
- **已删除的旧列**：`couple_space.stickers`（F28 贴纸墙）随 ADR-0010 第 6 条 DROP——它与 50 天档「专属贴纸包」语义撞车；那一档曾在前端按 `tierUnlocked('custom-emoji')` 放行固定 unicode 贴纸，六档解锁本身已于 2026-10-20 下线。

## 3 · 已下线的三张卡与心动值（历史，别当现役去找）

这四条 2026-10-05 起还是现役，2026-10-20 三轮裁剪整条删掉；下面的描述都是**当时**的口径，留着只为解释老库与老文档。

- **每日一问**（`CoupleQuestionController` / `CoupleQuestionService` / `couple_question_answer`，V52 建、V56 drop）：一天一题按 `stableHash(spaceId|v8-question-bank|day)` 取题、双方都答完才互看、`CoupleQuestionBank` 75 题只增不改顺序；`question-daily` / `question-answered` 两个事件与 `CoupleQuestionJob`（09:00 定题）都没有生产者了。
- **连续互动打卡 + 六档解锁**（`CoupleStreakController` / `CoupleStreakService` / `couple_streak_day`，V52 建时叫 `couple_bond_day`、V53 改名重建、V56 drop）：`StreakDays` 读时算连续与峰值、`MakeupPolicy` 免费补签（7 天窗口 / 每月 3 次 / 今天不许补）、`StreakTier` 六档 key 冻结（`bubble`/`background`/`nickname-glow`/`pendant`/`title`/`custom-emoji`）；`streak-checkin` / `streak-unlocked` / `streak-makeup` 三个事件随之消失。
- **愿望清单**（`CoupleWishController` / `CoupleWishService` / `couple_wish`，V52 建、V56 drop）：`OPEN → PREPARED → FULFILLED` 单向、「已准备」对被许愿人脱敏、标记与撤销刻意不推事件（`wish-added` / `wish-fulfilled` 已无生产者）。
- **心动值与七级恋爱等级**（`CoupleController` 的 `GET /intimacy`、`CoupleService.intimacy()`、`domain/intimacy/IntimacyCalculator` + `IntimacySource`）：读时算无表，五项供数 `daysTogether`×1 + `checkinDays`×2 + `longestStreak`×3 + `answerDays`×3 + `wishFulfilled`×5，七级阈值 `0/60/150/260/400/560/760`（怦然心动→相守一生）。五项里四项随本轮三张卡消失，留着就是一串恒为 0 的数字，故整条删除；`couple.domain.intimacy` 包、`IntimacyVO`/`IntimacyBreakdown`、`IntimacyCalculatorTest`/`CoupleIntimacyTest` 一并没了。这是 ADR-0010 时代的口径，别按它找现役代码。

## 4 · 经期记录（`couple_period` · 写口，展示在日历卡上）

- **一行 = 一次经期的开始日**：`start_day` 必填（`yyyy-MM-dd`，不能晚于今天），`end_day` 可当天或后补，未结束为 `NULL`；`recorder_user` 只留痕——这是两个人共享的记录，**双方都能记、都能补结束日、都能删**，不设「只有本人能改」的闸门（`PeriodRecord` 类注释锁死这条设计）。
- **同一空间同一天只能一条**：`uk_couple10_period_start(space_id, start_day)`；重复记 400「这一天已经记过啦，看看下面的记录 🩸」。
- **平均周期与下次预测读时算**（`domain/period/PeriodCycles`）：只取相邻开始日的间隔，**15–45 天之外视为漏记/补记造成的假样本并忽略**，有效样本取均值（四舍五入）；一条有效样本都没有时按默认 28 天估。表里**没有 average/predicted 缓存列**。
- **`daysUntilNext` 可以为负**：预测日已过就是「已推迟 N 天」，不自动滚动预测；没有记录时 `predictedNextDay`/`daysUntilNext` 都是 `null`。推算只是提醒，前端文案固定写「按记录推算 / 先按 X 天估」，不做任何健康判断。
- **三个写口（`/add` `/end` `/remove`）都只回成功与否**：原 `GET /period/board` 已随卡片合并下线，展示统一由 `GET /api/couple/calendar/month` 下发，前端写完重拉当月；**都不推 WS 事件**，对方记一条不会给你弹提醒（私密记录的口径与曾经的愿望「已准备」相同，但连事件都不发）。
- **不做任何算分**：心动值整条已于 2026-10-20 下线，经期记录只做展示与提醒。
- 关键闸门话术：空开始日「开始日得选一个日子呀」、格式错「开始日得是 yyyy-MM-dd 这样的日子呀」、未来日「还没到的日子记不了呀，今天之后的事留给明天 🩸」、重复日「这一天已经记过啦…」、重复结束「这次已经记过结束日啦」、越界记录 404「这条经期记录不在你们的空间里」。

## 5 · 亲密记录（`couple_intimacy_log` · 写口，展示在日历卡上）

- **一行 = 一次亲密**：`day` 必填（`yyyy-MM-dd`，不能晚于今天），`kind` 只有 `EDGE`（边缘亲昵）/`SEX`（性生活）两种，`note` 选填（≤60 字）；`recorder_user` 只留痕——这是两个人共享的记录，**双方都能记、都能删**，不设「只有本人能删」的闸门。
- **同一天允许多条**：本表**没有唯一键**，只建普通索引 `idx_couple11_intimacy_space_day(space_id, day)`；同一天记两次不同类型的记录是合法事实，重复提交由前端按钮 loading 拦，不在库上做「一天一次」的假设。
- **统计仍走读时算值对象**（`domain/intimatelog/IntimateLogs`）：`last()` 取全量里最近一次、`monthCount()`/`monthEdgeCount()`/`monthSexCount()` 按**当前自然月**统计、`totalCount()` 是累计条数。原 `GET /intimate/board` 那 30 条截断已随卡片合并下线，日历只取 `intimateTotal`（**累计走全量、不从本月列表回算**，与曾经的愿望清单同一条口径）。
- **两个写口（`/add` `/remove`）都只回成功与否**：本月标记由 `GET /api/couple/calendar/month` 下发，前端写完重拉当月；**都不推 WS 事件**，对方记一条不会给你弹提醒（与经期记录同属私密记录，连事件都不发）。
- **不做任何算分**：心动值整条已于 2026-10-20 下线，亲密记录只做展示与统计。
- 关键闸门话术：空日期「哪天呀？日子得选一个」、格式错「日子得是 yyyy-MM-dd 这样的」、未来日「还没到的日子记不了呀，今天之后的事留给明天 💞」、类型不认识「这类记录认不出来，选「边缘」或「生活」就好」、备注超长「备注最多 60 个字，留白也很甜」、越界记录 404「这条亲密记录不在你们的空间里」。

## 6 · 亲密日历（现役唯一一张卡 · `CoupleCalendarService` 只读，无自有表）

- **一张卡三种行为**：经期 🩸、边缘亲昵 ✨、性生活 💞 三种图标铺在月份格子上，点某天就在下面记那天、看那天的记录、删记错的——经期与亲密记录都从这一张卡进出，两张记录表只是它的底层账本。
- **一份月度读模型**：`GET /api/couple/calendar/month?month=yyyy-MM` 一次给全这个月要铺的东西——`periodDays`（被经期覆盖的天）、`periodStartDays`/`periodEndDays`（哪几天算开始、哪几天算结束）、`periods`（与本月经期区间有关的记录：id/startDay/endDay，`endDay` 为 `null` 就是还没结束）、`predictedNextDay`/`averageCycleDays`/`cycleSampleCount`/`daysUntilNext`（照 `PeriodCycles` 口径转发）、`intimate`（本月亲密标记：id/day/kind/note/recorderUser）、`intimateTotal`（累计次数）。
- **为什么不让前端拼**：前端手里只有两张记录卡各自的看板，拼一张日历就得再敲一次 `/period/board`——同一页两份经期真相、两个请求。单测把这条抓出来了（`couple.spec.ts` 断言 `periodBoard` 只被调一次），所以收成后端读模型；三轮裁剪把卡片合并后，两个看板读口也删了，日历这个读口成了唯一一份真相。
- **区间判定留在领域里**：`PeriodRecord.covers(day, today)`——记了结束日按 [开始日, 结束日] 含首含尾，还没记结束日只到今天；跨月的经期在两个月各铺各的那段；脏日期静默算没覆盖，一行历史数据不该让日历 500。前端只按后端给的天列表渲染。
- **只读**：日历不写库、不推 WS 事件；写仍然走经期与亲密各自的写口。`periods`/`intimate` 都下发 `id`，因为「删某天记错的那条」就发生在这张卡上（删除仍调 `/period/remove`、`/intimate/remove`）；不下发 `created`。
- 关键闸门话术：缺月份或空白「要看哪个月呀？给我 yyyy-MM 这样的」、格式错「月份得是 yyyy-MM 这样的」、无空间 404「还没有建立情侣空间，先邀请一位好友吧」。未来的月份正常返回空覆盖日，不报错。

## 核心业务规则（跨功能现役）

1. **一天一行只发生在经期开始日**：`uk_couple10_period_start` 挡重复；亲密记录没有唯一键，同一天允许多条。（曾经的每日一问「每人每天一行 upsert、改写不重复推送」随卡片于 2026-10-20 下线。）
2. **私密记录不弹提醒**：经期与亲密的写口与日历读口一条 WS 事件都不推，对方记一条不会给你推送——这条口径替代了原先「只有对方能…」那类归属校验（愿望「已准备」与爱称归属都随功能下线，前端已无须收按钮）。
3. **列表被 LIMIT 钳制时全量计数另走全量**：日历的 `intimateTotal` 由 `IntimateLogs.totalCount()` 按全量算，不从本月那一页列表回算。
4. **静态内容只增不改顺序**：这是给以后新增 Bank 留的口径；couple 包现役已无静态内容库（`CoupleQuestionBank.stableHash` 的 FNV-1a 随每日一问删除）。
5. **文案一律中文口语化带 emoji**，领域话术即用户所见原话；业务失败经 `DomainRules` 抛 `BusinessException(400/403/404/409, 人话)`，前端直透不重写。

## 内容库（现役 0 个）

`CoupleQuestionBank`（每日一问题库 75 题 + `stableHash` FNV-1a 32 位）随每日一问于 2026-10-20 三轮裁剪删除；更早的
`CoupleRitualBank`/`CoupleTalkBank`/`CoupleCatchBank`/`CoupleEchoBank`/`CoupleFactoryBank`/`CoupleQuestBank`/`CoupleSurpriseBank`/`CoupleTermBank`
八个随各功能卡删除。couple 包现在没有 `infrastructure/content` 这一层。

## 分层模板（加新卡照抄，判据见 `docs/ddd/05-tactical-playbook.md`）

1. 表映射 `XxxPO`：`@Data @TableName` + `@TableId(IdType.INPUT)` + 静态工厂 + 常量。**`Integer`/`Long` 位字段禁止配 `isXxx()` 布尔 helper**（与 Lombok `getXxx()` 撞成歧义 getter，MyBatis 反射随机抛 `ambiguous type`）——位判断一律命名 `xxxFlag()`（现役例子在 messaging：`PrivateMessage.readFlag()`/`recalledFlag()`、`Friend.blockedFlag()`）。
2. Mapper：`interface XxxMapper extends BaseMapperCompat<XxxPO>`，常用查询写 default 方法；Mapper 不感知领域类型。
3. 领域：`domain/<集合>/Xxx`（私有构造 + `restore()` 不校验 + 工厂校验抛 `RuleViolation`，访问器用短名）+ `XxxRepository` 端口；`infrastructure/persistence/XxxRepositoryAdapter` 做 PO↔领域翻译，更新只回写聚合纳管的列。couple 现役 5 张 `couple_*` 表里除无代码的 `couple_notify` 外一张表一端口一适配器（4 个）；全仓 18 张表 = 16 端口 = 16 适配器。
4. Service：注入**端口不是 Mapper**，VO 用嵌套 `record`；投影集中在只读服务（现役例子是 `CoupleCalendarService` 那一份月度结果），经期与亲密的写口只落库与守闸门、返回 void 由前端重拉当月；判定与话术在领域，异常经 `DomainRules.rule|guard` 翻译。
5. Controller：`@RestController @RequestMapping("/api/couple/xxx")`，请求体用 record，每方法一句 javadoc（`wiki/api.md` 的端点说明就是抓这句生成的）。
6. 新增/删除表走 Flyway 增量脚本并同步 `schema.sql`；新增 WS 事件同步前端 `stores/couple.ts` 的 case。
