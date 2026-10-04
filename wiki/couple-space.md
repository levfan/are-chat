# 情侣空间业务全景

> 本页回答：couple 包现在有哪些功能、各自对应哪个 Controller/Service/表，以及贯穿全局的核心业务规则与内容库。

**2026-10-04 系统裁剪后的现役口径**：全模块按五维打分（操作轻简 / 吸引兴趣 / 情绪价值 / 具体不虚 / 日常频次）排序后**只保留 10 张功能卡**。
排序表、切线规则与落选理由的唯一依据是 [../docs/couple-trim-ranking.md](../docs/couple-trim-ranking.md)，本页是它落地后的代码事实。

**2026-10-05 v8 第一批**在其上再加 4 张卡（连续互动打卡 / 每日一问 / 愿望清单 / 百日回顾），
需求与取舍见 [../docs/adr/0007-couple-v8-streak-question-wish.md](../docs/adr/0007-couple-v8-streak-question-wish.md)。
现役规模：**108 个 java 文件 / 17 个 Controller / 70 个映射 / 22 张 `couple_*` 表 / 44 个 WS 事件 / 4 条定时任务**。

## 聚合根与基本设定

- `CoupleSpace` 是唯一聚合根：绑定后双方固定存为 `userA`/`userB`（**字典序小者为 A**），`partnerOf(me)` 取对方；各功能表都带 `space_id` 列。
- 建立流程：`POST /api/couple/invites` 邀请 → 对方 accept（事件 `invite-accepted`）；解除走 `/dissolve`（事件 `dissolved`）。
- Service 一律经 `requireSpace(me)` 拿有效空间，拿不到抛 404「还没有建立情侣空间，先邀请一位好友吧」——前端各处 `safeLoad` 静默降级依赖这句。
- 互动数据双方可见；「在途/保密」态由**服务端读时过滤**而不是靠前端不渲染（例：安全词的复盘只有喊停人自己能补）。
- 运营侧：`CoupleAdminController /api/couple/admin/stats` 仅管理员，统计项只吃仍在线的 `couple_action` 与 `couple_echo_deed`。

## 保留的 10 张卡

| 卡 key | 功能 | Controller（前缀） | Service | 表 | 关键规则 |
|---|---|---|---|---|---|
| `couple-mood` | 心情日记 | `CoupleController` `/api/couple` | `CoupleService` | `couple_mood` | 每人每天一条，重复提交=改写；被心动值与深夜陪伴消费 |
| `couple-bond` | 贴贴宫格 | `CoupleBondController` `/bond` | `CoupleBondService` | `couple_action` | 7 种动作；里程碑按累计计数推 `bond-milestone`；带专属爱称与心情回应 |
| `couple-comfort` | 求抱抱 | `CoupleCareController` `/care` | `CoupleComfortService` | `couple_comfort` | 感受五白名单，每人每天一条；**回应只有对方能发**；23:00 未被接住才推 `night-care` |
| `couple-catch-safeword` | 安全词与暂停复盘 | `CoupleCatchController` `/catch` | `CoupleCatchService` | `couple_catch_safeword`, `..._use` | 没约定就喊不出口；一天一人只记一次；复盘只有喊停本人能补；`usedTodayMine/Partner` 由后端下发 |
| `couple-dine-today` | 今晚饭桌 | `CoupleDiningController` `/dining` | `CoupleDiningService` | `couple_dine_ticket` | 每人一票，撞菜推 `dine-hit`；裁决 = 当日票池去重后按 `stableHash(space\|dine-verdict\|day)` 取一道，**两人刷新结果一致** |
| `couple-fy-spin` | 家务轮盘 | `CoupleFactoryController` `/factory` | `CoupleFactoryService` | `couple_spin_task` | 一周一转 ≤8 项；**自己的活自己认不了账**；认账后本人才能打勾；干完 +3 分、周全清双方各 +2 |
| `couple-quest-overtime` | 加班预报与留灯 | `CoupleQuestController` `/quest` | `CoupleQuestService` | `couple_quest_overtime` | 13-23 钳制；**灯卡只有对方能留**且 TA 必须已预报；每人每天一行可改写 |
| `couple-echo-deed` | 好事簿 | `CoupleEchoController` `/echo` | `CoupleEchoService` | `couple_echo_deed` | 同日同人同内容 400；分给**被记的那位**（+2），加星再 +1；加星只归记录人且按行幂等 |
| `couple-cere-coupon` | 愿望券本 | `CoupleCeremonyController` `/ceremony` | `CoupleCeremonyService` | `couple_ceremony_coupon` | 发券先扣发券人 10 分（余额不足 400 并点名去好事簿）；OPEN→USED 已核销再核 400 |
| `couple-surprise` | 刮刮乐与盲盒 | `CoupleSurpriseController` `/surprise` | `CoupleSurpriseService` | `couple_scratch`, `couple_mystery_box` | 券周卡懒生成两张；未刮开时对收券人隐藏券面；**只有送券人能点已兑现**（+5 归送券人）；盲盒到日才可拆且装盒人不能自拆 |

地基（不是一张卡，删不得）：`CoupleController` 的邀请建立 / 纪念日 / 空间个性化 / `relationship-of` / **心动值**，`CoupleNotifyController`（F41 通知中心），`CouplePinController`（F207 常用收藏）。

## v8 第一批新增的 4 张卡（2026-10-05）

| 卡 key | 功能 | Controller（前缀） | Service | 表 | 关键规则 |
|---|---|---|---|---|---|
| `couple-streak` | 连续互动打卡与七档解锁 | `CoupleStreakController` `/streak` | `CoupleStreakService` | `couple_bond_day` | 打卡日 = **双方当天都发过贴贴**（与心动值 `bondDays` 同源口径，无第二个「互动日」定义）；连续天数与七档解锁**读时算**，判 `longestStreak` 因此**单调不回退**；`streak-checkin`/`streak-unlocked`/`streak-makeup` 只在历史最长真的跨过档位时推，断签后重新爬到同一数字不再庆祝第二遍；第 1 天（建立当天）由看板读取时自愈补一行；补签 20 分走 `couple_point_ledger` SPEND，闸门=7 天窗口 + 每自然月 3 次 + 不许补今天 |
| `couple-question` | 每日一问 | `CoupleQuestionController` `/question` | `CoupleQuestionService` | `couple_question_answer` | 同空间同天同题（`stableHash(space\|salt\|day)`），题号与题干都落库存快照；**双方都答过才互看**（我没答时 `partnerAnswer` 返回 null）；每人每天一行可改写；09:00 `CoupleQuestionJob` 推 `question-daily`，答完推 `question-answered`（不带答案内容）；**不进心动值公式** |
| `couple-wish` | 愿望清单 | `CoupleWishController` `/wish` | `CoupleWishService` | `couple_wish` | 与「愿望券本」是两个概念不合并；`owner`（想要的人）与 `creator`（记录的人）可不同；**「已准备」对被许愿人保密**——`visibleStatusFor(me)` 把它回显成 OPEN、`preparedAt` 置 null，且标记/撤销**一个 WS 事件都不推**；只有许愿人能确认实现（此时才公开并推 `wish-fulfilled`）；同空间同 owner 同名 400，未实现上限 30 条 |
| `couple-memory` | 百日回顾（隐藏页） | `CoupleMemoryController` `/memory` | `CoupleMemoryService` | 复用 `couple_bond_day`/`couple_question_answer`/`couple_wish` | 历史最长连续 <100 天时**服务端 400**「还差 N 天」，不靠前端藏页签；时间轴只收有真实时间戳或可精确派生日期的事件，且建立/解锁/答完/实现这四类**优先占位不被截断**（只截"刷峰值的日子"到 80 条）；一句话总结是 `RelationSummary` 规则生成，本仓库无 LLM 依赖（ADR-0007 第 7 条） |

七档解锁的档位与前端挂钩（`StreakTier`，key 上线即冻结）：
`bubble`(3 天·双人专属气泡) → `background`(7 天·空间背景 + 角落电子植物) → `nickname-glow`(14 天·爱称发光)
→ `pendant`(21 天·头像联动挂件) → `title`(30 天·恋爱等级称号挂到空间顶部，**内容沿用心动值七级称号，不造第二套**)
→ `custom-emoji`(50 天·专属贴纸包，纯 unicode 派生，**不用双方照片**——产品红线不做图片) → `easter-egg`(100 天·隐藏页签)。

## 心动值与积分（裁剪后重算的口径）



- **心动值** `CoupleService.intimacy()` 是**读时算、无表**：
  `心情条数×1 + 贴贴双向往来天数×2 + 好事簿条数×2 + 留灯次数×3 + 安全词复盘次数×2 + 台账累计 EARN×1`。
  六项全部由保留卡供数（原「互道早安/晚安」「每日一问双答」两项随功能下线被移除，留着就是永远为 0 的死项）。
  **7 级阶梯阈值未重标定**：0/50/150/300/500/800/1300 → 怦然心动 / 心动初启 / 甜甜热恋 / 形影不离 / 心有灵犀 / 相依相伴 / 相守一生。回归锁在 `CoupleIntimacyTest`。
- **积分台账** `couple_point_ledger` 保留，闭环是「三赚一花」：
  EARN = 好事簿（+2/+1）、家务轮盘（+3/+2）、刮刮乐核销（+5）；SPEND = 愿望券本发券（−10）。
  归属人有两处反直觉、以源码为准：**好事簿的分给「被记的那位」**（做事的人拿分），**刮刮乐的分给「送券人」**（`redeemScratch` 只允许 `fromUser` 点）。

## 核心业务规则（跨卡通用）

1. **每人每天一行**的地方一律 upsert，改写不重复推送（mood / comfort / safeword use / dine ticket / overtime）。
2. **"只有对方能…"**是本模块的情绪价值支点：留灯、回应求抱抱、盲盒开箱、安全词复盘的归属都在服务端校验，前端只负责把不该出现的按钮收口成禁用态。
3. **列表都被 LIMIT 钳制**（如用 `uses` 算 `monthUses`、看板各列表 ≤20/≤30）：需要"全量计数"时后端直查原始表，不从已钳制的列表回算。
4. 静态内容只增不改顺序；按天/按空间稳定取值统一走 `CoupleRitualBank.stableHash`（FNV-1a）。
5. 文案一律中文口语化带 emoji，业务失败抛 `BusinessException(400, 人话)`，前端 `ElMessage.error` 直透不重写。

## 内容库（现役 9 个）

| 库 | 供谁用 |
|---|---|
| `CoupleRitualBank` | `stableHash` 全模块共用（饭桌裁决、盲盒/刮刮乐取面、轮盘开场、每日一问选题） |
| `CoupleTalkBank` | 求抱抱话术卡、陪聊话题卡、深夜陪伴文案 |
| `CoupleCatchBank` | 安全词的约定/喊停/复盘三套话术与 kind 标签 |
| `CoupleEchoBank` | 好事簿记下与加星话术、年报称号（年报已下线，话术仍在用） |
| `CoupleFactoryBank` | 轮盘开场、欠账提醒、生活委员头衔 |
| `CoupleQuestBank` | 加班与留灯话术 |
| `CoupleSurpriseBank` | 刮刮乐券面池、盲盒任务灵感、花语、幸运签 |
| `CoupleTermBank` | `CoupleService` 的农历生日换算（`lunarToSolar/solarToLunar`）仍依赖它 |
| `CoupleQuestionBank` | 每日一问题库 74 道；`indexOf(spaceId, day)` 用 `CoupleRitualBank.stableHash(space\|"v8-question-bank"\|day)` 选题，题号会落进 `couple_question_answer.question_index`，所以**只增不改顺序**（改了历史答案的题号就不指向原题） |

`CoupleCities`（双城城市库）、`CoupleCeremonyBank`、`CoupleDiningBank` 以及 `CoupleChatBank`/`Codex`/`Cozy`/`Focus`/`Growth`/`Laugh`/`Legacy`/`Play` 等已随功能彻底无引用而删除。
被裁掉的 `CoupleQuestions`（今日一问题库 105 题）在 v8 按新口径重建为 `CoupleQuestionBank`：题干全部改成"今天"口径、题号与题干一起落库存快照，旧库与旧表（`couple_answer`/`couple_answer_reaction`）不复活。

## 分层模板（加新卡照抄）

1. 实体：`@Data @TableName` + `@TableId(IdType.INPUT)` + 静态 `of()` 工厂 + 常量（`*_MAX`、`STATUS_*`）。
   **`Integer`/`Long` 位字段禁止配 `isXxx()` 布尔 helper**（与 Lombok 的 `getXxx()` 撞成歧义 getter，MyBatis 反射会随机抛 `ambiguous type`，实测让整页 500）——位判断一律命名 `xxxFlag()`。
2. Mapper：`@Mapper interface X extends BaseMapperCompat<T>`，常用查询写 default 方法。
3. Service：构造注入（final 字段 + 构造器），VO 用嵌套 `record`，写接口返回整份聚合 VO 让前端整体替换。
4. Controller：`@RestController @RequestMapping("/api/couple/xxx")`，请求体用 record，每方法一句 javadoc（`wiki/api.md` 的端点说明就是抓这句生成的）。
5. 新增/删除表必须走 Flyway 增量脚本并同步 `schema.sql`（见 `.agents/skills/db-migration`）；新增 WS 事件必须同步前端 `stores/couple.ts` 的 case。
