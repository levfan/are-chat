# 情侣空间业务全景

> 本页回答：couple 包现在有哪些功能、各自对应哪个 Controller/Service/表，以及贯穿全局的核心业务规则与内容库。
> 事实源：`src/main/java/com/smart/chat/couple/**`（Controller/Service/domain）、`src/main/resources/schema.sql` + `db/V52/V53`、
> 术语合同 `CONTEXT.md`、去留判据 `docs/adr/0010-couple-trim-to-v8-features.md`。

## 本轮裁剪（先读这段）

情侣空间在 2026-10-05 经过两轮改造：先加四张功能卡（每日一问 / 愿望清单 / 连续互动打卡 + 七档解锁 / 邀请建立补口，见
`docs/adr/0007`），随后按用户指令「除了这几项，其余功能全部删除」做了**一轮大裁剪**——判据从「排名高低」换成
「这四项及其赖以成立的地基还活不活」，取舍、连带改造与被删清单的唯一依据是 **`docs/adr/0010-couple-trim-to-v8-features.md`**。

现役规模（实测自源码与 `schema.sql`）：**57 个 java 文件 / 7 个 Controller / 26 个端点 / 6 张 `couple_*` 表 / 15 个 WS 事件 / 2 条定时任务**。
按 ADR-0010 已下线、**不再存在**的功能（一句话点名即可，别当现役去找）：心情日记、贴贴宫格、求抱抱、安全词与暂停复盘、
今晚饭桌、家务轮盘、加班预报与留灯、愿望券本、好事簿、刮刮乐与盲盒、共同日历（倒数日清单）、积分台账、卡片收藏与功能搜索。
其中三处曾是别的功能算数的根据，已按 ADR-0010 重新改挂：**打卡触发源**改挂每日一问（原挂贴贴）、**补签稀缺性**改由次数守（原扣积分台账）、
**心动值供数**整体换成现役五项（原六项的表全部消失）。

## 现役的四张功能卡 + 地基

四张卡各有自己的 Controller/Service；地基（邀请建立、空间本体、心动值、通知中心、运营看板）不是卡，删不得。

| 卡（`?card=` 深链 key） | Controller（前缀） | Service | 表 |
|---|---|---|---|
| 每日一问 `couple-question` | `CoupleQuestionController` `/api/couple/question` | `CoupleQuestionService` | `couple_question_answer` |
| 连续互动打卡 `couple-streak` | `CoupleStreakController` `/api/couple/streak` | `CoupleStreakService` | `couple_streak_day` |
| 愿望清单 `couple-wish` | `CoupleWishController` `/api/couple/wish` | `CoupleWishService` | `couple_wish` |
| 百日回顾（隐藏页）`couple-memory` | `CoupleMemoryController` `/api/couple/memory` | `CoupleMemoryService` | 只读复用 streak_day / question_answer / wish |
| 地基（建立/本体/心动值） | `CoupleController` `/api/couple` | `CoupleService` | `couple_space`, `couple_invite` |
| 通知中心（F41） | `CoupleNotifyController` `/api/couple/notify` | `CoupleNotifyService` | `couple_notify` |
| 运营看板（F45，仅管理员） | `CoupleAdminController` `/api/couple/admin` | （直连端口，无独立 Service） | 读 `couple_space` / `couple_streak_day` / `couple_question_answer` |

> `CoupleAdminController.stats` 的两个计数在二轮裁剪后从「贴贴动作数 / 好事簿条数」换成 `totalCheckinDays`（`couple_streak_day`）与
> `totalAnswers`（`couple_question_answer`）；原来描述的 `CouplePinController`（收藏卡 + 功能搜索）已随 ADR-0010 第 7 条整体删除。

## 1 · 邀请建立（状态机）

- **唯一容器**：`CoupleSpace` 是聚合根，绑定后双方固定存为 `userA`/`userB`（**用户名区分大小写、字典序小者为 A**，规范化只在 `CoupleSpace.open()` 一处）；`partnerOf(me)` 是全系统唯一的「对方」定义。各功能表都带 `space_id`。
- **邀请状态机**（`domain/invite/Invite`）：`PENDING → ACCEPTED / REJECTED / CANCELED` 单向，落子状态后不可再动（`requirePending` 一处收口）。归属闸：`acceptBy/rejectBy` 只能处理**发给自己的**（否则 403「只能处理发给自己的邀请」）；`cancelBy` 只能撤回**自己发出的**（403「只能撤回自己发出的邀请」）；已处理过再点 → 409「该邀请已经处理过了」；留言上限 100 字（`MESSAGE_MAX`）。
- **建立流程**（`CoupleService`）：`invite()` → 对方 `accept()`。`accept()` 经 `CoupleSpace.open()` 走聚合工厂建行（ADR-0007 第 1 条修复的领域漂移：不再 `PO` 直插），落库后立刻 `CoupleStreakService.confirmCreationDay(space)` 写第 1 个打卡日——**建空间当天即算连续第 1 天**。事件：`invite` / `invite-accepted` / `invite-rejected`；`cancel` **不推事件**。
- **前置闸门**（全在 `CoupleService.invite()`，400/409）：未选人「想邀请谁？请先选择一位好友」；对自己「不能和自己建立情侣空间哦」；查无此人「查无此人：对方还没注册或已注销」；非好友「只能邀请自己的好友，先去通讯录加个好友吧」；已在别的空间 409「你已经在情侣空间里啦…」「对方已经在别的情侣空间里了」；同两人已有待处理单 409「你们之间已有待处理的情侣邀请…」。
- **未建空间**一律 `requireSpace(me)` 抛 **404「还没有建立情侣空间，先邀请一位好友吧」**，不由前端兜。
- **解除**：`POST /api/couple/dissolve`（本人）或注销级联 `purgeUser()`，历史数据保留但不再互可见，事件 `dissolved`。
- **可见性地基**：每条情侣事件都经 `CoupleNotifyRecorder` 落一份 `couple_notify`，是「对方不在线也能看见邀请」的唯一通道——通知中心算地基不算功能（ADR-0010 第 1 条）。

## 2 · 空间本体（宣言 / 主题 / 爱称 / 在一起的日子）

一个 `PUT /api/couple/profile` 写入口一次改三件（`CoupleService.updateProfile`）：

- **宣言 `slogan`**：≤60 字（`CoupleSpace.SLOGAN_MAX`），超长 400「宣言最多 60 字，留白也很美」；`null`=不动、空串=清除。
- **主题 `theme`**：白名单 `{classic, cherry, ocean, forest, night}`（`CoupleSpace.THEMES`），不在表内 400「这个主题还没上架哦」；建空间默认 `classic`。**7 天档动态背景叠在主题上**，所以 `theme` 保留。
- **爱称 `petName`**：给对方起的专属爱称，≤30 字（`NICK_MAX`），超长 400「爱称最长 30 个字」；只能由**对方**改（`renamePartner` 守这条），落 `couple_space.nick_a/nick_b`。爱称变化**单独推 `pet-name-changed`**（「TA 给你起了新爱称「X」🏷️」/「TA 把给你的爱称收回了 🏷️」），宣言或主题变化推 `space-themed`，都没变一条都不推——只改爱称却推「装扮了小空间」是把一件心事说成一次装修（ADR-0010 第 12 条）。
- **在一起的日子 `anniversary`**：`PUT /api/couple/anniversary` 绑定单个日期（`yyyy-MM-dd`，缺省取 `created`），是一起天数 / 百日回顾 / 纪念日倒数三处的共同根据（事件 `anniversary-updated`）。**这不是共同日历**——`couple_anniversary` 倒数日清单已随 ADR-0010 第 8 条删除。
- **已删除的旧列**：`couple_space.stickers`（F28 贴纸墙）随 ADR-0010 第 6 条 DROP——它与 50 天档「专属贴纸包」语义撞车；50 天档实现在前端按 `tierUnlocked('custom-emoji')` 放行固定 unicode 贴纸，不依赖这一列。

## 3 · 每日一问（`couple_question_answer`）

- **一天一题**：`CoupleQuestionBank.indexOf(spaceId, day)` = `floorMod(stableHash(spaceId + "|" + SALT + "|" + day), 75)`，`stableHash` 是类内 **FNV-1a 32 位**（SALT=`v8-question-bank`），同空间同天双方看到**同一道题**；题库现役 **75 题**。题号与题干都落库存快照，**只增不改顺序**（改了历史答案的 `question_index` 就不指向原题）。
- **双方都答完才互看**（`domain/question/DailyQuestion`）：`partnerAnswerText()` 同时满足「我已答」与「TA 已答」才返回，否则给前端 `null`；这条判断留在领域里，不是显示开关。每人每天一行（`uk_couple8_question_day_user`），当天可改写（`rewrite`）。答案闸门：空「写一句再交卷呀 📝」、超 300 字「回答最多 300 个字，短一点更像人话」（`ANSWER_MAX`）。
- **它同时是打卡的触发源**（ADR-0010 第 2 条）：`CoupleQuestionService.answer()` 末尾 `if (partnerAnswered) streakService.confirmBothAnswered(space, me)`——这是 `couple_streak_day` 里 `AUTO` 行的**唯一挂钩**，别再新增第二个入口。
- **事件**：`question-answered`（答完推给对方，不带答案内容），`question-daily`（09:00 定题推双方）。

## 4 · 连续互动打卡（`couple_streak_day` + `StreakDays` 读时算）

- **打卡日 `StreakDay`（薄实体）**：一行 = 那天双方共同活跃。`source` = `AUTO`（双方当天都答完每日一问，或空间建立当天）/ `MAKEUP`（补签）。唯一键 `uk_couple9_streak_day(space_id, day)` 保证一天一行。**V52 建表时叫 `couple_bond_day`（索引 `uk_couple8_bond_day`），V53 改名重建为 `couple_streak_day` / `uk_couple9_streak_day`**——`bond` 在统一语言里专属「贴贴」，贴贴卡本轮删除，不能一词二义（ADR-0010 第 5 条）。
- **连续天数读时算，不物化缓存列**（`domain/streak/StreakDays`，唯一输入是 day 集合）：`currentStreak()` / `longestStreak()` / `confirmedDays()` / `missedYesterday()` / `nextTier()` / `unlockedAt(tier)`。
- **今天没打不算断**：`currentStreak()` 今天没打卡就从**昨天**往回数（今天还有机会）；**只有昨天缺行才叫断签**（`missedYesterday()`），这也是补签入口该不该亮的条件。
- **解锁判历史最长、单调不回退**：档位一律看 `longestStreak()`（峰值），断签后已到手的外观**不没收**；`tiersCrossed(prev, now)` 只在峰值真的跨过档位时推 `streak-unlocked`，重新爬回同一数字不再庆祝第二遍。
- **补签 `MakeupPolicy`（免费）**：买回一个缺口，闸门三条——**只能补最近 7 天**（`WINDOW_DAYS`）、**每自然月最多 3 次**（`MONTHLY_QUOTA`）、**今天不许补**；**不再扣积分**（积分台账已随 ADR-0010 第 3 条删除，稀缺性换成次数）。原话（用户所见）：日期没给「想补哪一天？日期没给呢」；补今天「今天还不能补——两个人都答完今天的每日一问就算打卡 😉」；超窗口「只能补最近 7 天里的缺口…」；已打「<day> 已经打过卡了，不用补」；满 3 次「这个月已经补过 3 次了，下个月再来吧（<年-月>）」；格式「日期格式应为 yyyy-MM-dd」。
- **事件**：`streak-checkin`（首次落 AUTO 行）、`streak-unlocked`（跨过档位）、`streak-makeup`（补签成功）。

## 5 · 七档解锁（`StreakTier`，key 上线即冻结）

| 天数 | key | 标签 | 图标 | detail |
|---|---|---|---|---|
| 3 | `bubble` | 双人专属气泡 | 🫧 | 你们的消息气泡换成情侣款，只有你们俩看得到 |
| 7 | `background` | 空间背景 | 🌱 | 小空间有了动态背景，那棵你们一起养的植物发芽了 |
| 14 | `nickname-glow` | 昵称特效 | ✨ | TA 给你起的爱称开始发光，打开对话框就能看到 |
| 21 | `pendant` | 双人挂件 | 🧸 | 你们头像边上挂上了联动小挂件 |
| 30 | `title` | 恋爱等级称号 | 🏷️ | 恋爱等级称号挂上空间顶部，旁边的人都能看到 |
| 50 | `custom-emoji` | 专属贴纸包 | 🎨 | 解锁一组只有你们俩能用的专属贴纸 |
| 100 | `easter-egg` | 隐藏彩蛋页 | 🥚 | 多了一个谁都不给看的隐藏页，里面是你们的一百天 |

- 档位天数/key/文案一律不动（ADR-0010 第 10 条）——前端按 key 挂视觉，改 key 等于没收已解锁用户的外观。
- **30 天档只控制「把称号挂到顶部」这个动作**，称号内容取心动值七级 `IntimacyVO.title`，**不造第二套等级**（ADR-0007 第 5 条）。

## 6 · 愿望清单（`couple_wish`）

- `owner_user`（想要的人）与 `creator_user`（记录的人）可以不同；上限 30 条未实现、标题 ≤80 字、补充说明 ≤200 字（`OPEN_MAX` / `TITLE_MAX` / `NOTE_MAX`）。
- **状态单向**：`OPEN → PREPARED（可撤销回 OPEN）→ FULFILLED`，实现后不可回退；`uk_couple8_wish_title(space_id, owner_user, title)` 挡同名。
- **「已准备」对被许愿人保密**（`Wish.visibleStatusFor(me)`）：对方给我标的 `PREPARED`，我这边仍回显 `OPEN`，且连 `preparedAt` 也置 null；归属校验先过（许愿人自己碰这格 → 400「这条愿望是你自己许的，「已准备」那一格是给 TA 留的」），话术不透露当前状态。
- **标记/撤销不推任何 WS 事件**（推了就等于替对方说出去）：`prepare`/`unprepare` 一个事件都不发，由 `CoupleWishServiceTest.markingPreparedPushesNothingAtAll`（`verifyNoInteractions(push)`）锁死。只有 `add`（给别人许时）推 `wish-added`、`fulfill` 推 `wish-fulfilled`。**刻意不存在** `wish-prepared`/`wish-unprepared`。
- 关键闸门话术：空标题「想要什么总得写一句呀」、超 80 字「愿望最多 80 个字，剩下的见面再说」、非本人 confirm「只有许愿的人自己能确认愿望实现了」、已实现再点「这个愿望已经实现啦，不用再点一次」、删已实现「已经实现的愿望要留在记录里，删不掉咯」、他人删/改「只有记这条愿望的人能删掉它 / 能改它」。

## 7 · 心动值（五项供数 · 七级阈值 0/60/150/260/400/560/760）

- **读时算、无表**（`CoupleService.intimacy()`）：取数只攒「多少」，加权与定级在 `domain/intimacy/IntimacyCalculator`。
- **五项权重（越少见的行为越贵，ADR-0010 第 4 条）**：

  | 供数（`IntimacySource`） | 权重 | 取自 |
  |---|---|---|
  | `daysTogether` 在一起天数 | ×1 | `couple_space.anniversary`（缺省按 `created`） |
  | `checkinDays` 累计打卡天数 | ×2 | `couple_streak_day` 行 |
  | `longestStreak` 历史最长连续 | ×3 | 同一张表读时算 |
  | `answerDays` 双方都答完每日一问的天数 | ×3 | `couple_question_answer` 按 day 聚合（口径与打卡日同源） |
  | `wishFulfilled` 已实现愿望条数 | ×5 | `couple_wish` status=FULFILLED |

- **七级阶梯（阈值已随二轮裁剪重标定；原六项供数全灭，旧阈值 `0/50/150/300/500/800/1300` 会把等级永久卡在一级）**：

  | 级 | 称号 | 图标 | 阈值 |
  |---|---|---|---|
  | L1 | 怦然心动 | ✨ | 0 |
  | L2 | 心动初启 | 💫 | 60 |
  | L3 | 甜甜热恋 | 🍬 | 150 |
  | L4 | 形影不离 | 🧡 | 260 |
  | L5 | 心有灵犀 | 💞 | 400 |
  | L6 | 相依相伴 | 🌷 | 560 |
  | L7 | 相守一生 | 💍 | 760 |

  > 称号名与阈值以 `IntimacyCalculator` 为准，`IntimacyCalculatorTest`/`CoupleIntimacyTest` 逐条钉边界。
  > 注意 `CONTEXT.md` 第 103 行、`docs/adr/0010` 第 103 行括注的**图标串**与本表按序对齐存在偏差（详见同步报告的「不一致清单」）——
  > 本页按代码 `ICONS` 数组给值。满勤 100 天约 754 分落在 L6，**故意不满级**：一百天是隐藏页门槛，不该同时把「相守一生」发完。

## 8 · 百日隐藏页（`CoupleMemoryService`）

- 连续满 100 天（`StreakTier.EASTER_EGG.days()`）才打开。**服务端也挡一道**：`longestStreak < 100` → 400「这一页要连续贴满 100 天才打开，现在还差 N 天」，不靠前端藏页签。
- **时间轴**（`TimelineItemVO`，上限 `TIMELINE_MAX=80`）只收有真实时间戳或可精确派生日期的事件；建立（`space`）/解锁（`unlock`）/答完的题（`question`）/实现的愿望（`wish`）四类**优先占位不被截断**，只有「刷峰值的日子」（`streak`）这类填充项从新往旧补到 80 为止。
- **一句话总结**（`domain/memory/RelationSummary`）**规则生成、本仓库无 LLM 依赖**（ADR-0007 第 7 条）：只说数据里真有的事（在一起天数 / 打卡天数 / 最长连续 / 补签次数 / 双方答完题数 / 已实现愿望数 / 心动值称号 / 最近解锁档位），零值整段消失。
  > 该生成器与看板话术仍沿用「贴」「花钱补」等**旧文案用字**（如总结里「一起贴了 N 天的卡」「其中 N 天是花钱补回来的」、隐藏页闸门「连续贴满」）——这是代码里的既有用词，非现役机制；补签实际已免费。

## 核心业务规则（跨功能通用）

1. **「每人每天一行」一律 upsert**，改写不重复推送（每日一问答题）。
2. **「只有对方能…」是情绪价值支点**：愿望「已准备」的标记/撤销、爱称的归属都在服务端校验，前端只把不该出现的按钮收成禁用态。
3. **列表被 LIMIT 钳制**：需要「全量计数」时后端直查原始表（`answerDays`/`countFulfilled` 等），不从已钳制列表回算。
4. **静态内容只增不改顺序**；按天/空间稳定取值走 `CoupleQuestionBank.stableHash`（FNV-1a）。
5. **文案一律中文口语化带 emoji**，领域话术即用户所见原话；业务失败经 `DomainRules` 抛 `BusinessException(400/403/404/409, 人话)`，前端直透不重写。

## 内容库（现役 1 个）

| 库 | 供谁用 |
|---|---|
| `CoupleQuestionBank` | 每日一问题库 75 题 + `stableHash`（FNV-1a 32 位，现就在本类里、包私有）。原 `CoupleRitualBank`/`CoupleTalkBank`/`CoupleCatchBank`/`CoupleEchoBank`/`CoupleFactoryBank`/`CoupleQuestBank`/`CoupleSurpriseBank`/`CoupleTermBank` 八个随功能删除。 |

## 分层模板（加新卡照抄，判据见 `docs/ddd/05-tactical-playbook.md`）

1. 表映射 `XxxPO`：`@Data @TableName` + `@TableId(IdType.INPUT)` + 静态工厂 + 常量。**`Integer`/`Long` 位字段禁止配 `isXxx()` 布尔 helper**（与 Lombok `getXxx()` 撞成歧义 getter，MyBatis 反射随机抛 `ambiguous type`）——位判断一律命名 `xxxFlag()`（现役：`StreakDay.makeup()`/`Wish.preparedFlag()`/`NotifyEntry` 走 `readFlag()`）。
2. Mapper：`interface XxxMapper extends BaseMapperCompat<XxxPO>`，常用查询写 default 方法；Mapper 不感知领域类型。
3. 领域：`domain/<集合>/Xxx`（私有构造 + `restore()` 不校验 + 工厂校验抛 `RuleViolation`，访问器用短名）+ `XxxRepository` 端口；`infrastructure/persistence/XxxRepositoryAdapter` 做 PO↔领域翻译，更新只回写聚合纳管的列。couple 现役 6 张表 = 6 端口 = 6 适配器。
4. Service：注入**端口不是 Mapper**，VO 用嵌套 `record`，写接口返回整份聚合 VO 让前端整体替换；判定与话术在领域，异常经 `DomainRules.rule|guard` 翻译。
5. Controller：`@RestController @RequestMapping("/api/couple/xxx")`，请求体用 record，每方法一句 javadoc（`wiki/api.md` 的端点说明就是抓这句生成的）。
6. 新增/删除表走 Flyway 增量脚本并同步 `schema.sql`；新增 WS 事件同步前端 `stores/couple.ts` 的 case。
