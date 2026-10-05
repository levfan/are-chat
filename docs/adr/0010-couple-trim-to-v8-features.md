# ADR-0010：情侣空间二轮裁剪——只留「邀请 / 每日一问 / 愿望清单 / 连续打卡解锁」

- 状态：已采纳（2026-10-05）
- 相关：`docs/adr/0007-couple-v8-streak-question-wish.md`（第一轮：加四项）、
  `docs/couple-trim-ranking.md`（2026-10-04 那轮按「留 10 张卡」的排名，本轮作废其结论但保留文档）、
  `CONTEXT.md` 统一语言、`docs/ddd/06-ddd-standard.md` 收口账本
- 用户指令原话：「除了以上这几个功能，情侣空间其余功能全部删除。不要找我确认，你自己决定就好」。
  与 ADR-0007 同规格执行：下面每一条自定取舍都写清依据与代价，供事后复核，不留成口头决定。

## 背景

ADR-0007 那一轮是**加**：新增每日一问、愿望清单、连续打卡与七档解锁，同时把 2026-10-04 排名留下的
十张卡一起保住（"10 张卡 + 4 张新卡"）。这一轮是**删**：用户认定四项之外的东西都不该在情侣空间里。
于是判据从「排名高低」换成「这四项及其赖以成立的地基还活不活」。

删除前 `couple` 上下文的实况：167 个 Java 文件里只有四张功能卡是 ADR-0007 那一轮新写的，其余（心情日记、贴贴宫格、
求抱抱、安全词与暂停复盘、今晚饭桌、家务轮盘、加班预报与留灯、愿望券本、刮刮乐与盲盒、好事簿、
共同日历、卡片收藏与功能搜索）都是历史迭代攒下来的。表 22 张、Controller 17 个、Service 16 个、
`/api/couple/**` 端点 70 个、WS 事件 44 种。删完是：表 6 张、Controller 7 个、Service 6 个、
文件 57 个、端点 26 个、事件 15 种。

真正的难点不在删文件，而在**三处依赖被删的东西当初是别人算数的根据**：

1. 打卡的触发源是「双方当天都发过贴贴」——贴贴卡（`couple_action`）本轮删除；
2. 补签的稀缺性靠积分台账扣 20 分——三个赚分入口（好事簿/家务轮盘/刮刮乐）与一个花分出口（愿望券本）全在被删之列；
3. 心动值六项供数（心情天数/双向贴贴天数/好事簿条数/留灯次数/复盘次数/赚分合计）——**六项的表全部要消失**。

这三处如果只跟着删不做替换，结果分别是：打卡永远不会发生、补签变成无限次、恋爱等级永久停在一级。
所以本 ADR 的主体积不在"删了什么"，而在"这三条链子改挂在哪儿"。

## 决策

### 1. 判据：四项功能 + 它们的地基，其余一律下线

保留的六张表与理由：

| 表 | 保留理由 |
| --- | --- |
| `couple_space` | 空间本体：`anniversary`（在一起的日子）、`slogan`（宣言）、`theme`（7 天档动态背景的载体）、`nick_a`/`nick_b`（14 天档昵称特效的显示对象） |
| `couple_invite` | 功能①「向好友发起邀请，对方同意建立情侣空间」 |
| `couple_streak_day` | 功能④的打卡日（原 `couple_bond_day`，见第 5 条改名） |
| `couple_question_answer` | 功能②每日一问的作答，同时是打卡的触发源 |
| `couple_wish` | 功能③愿望清单 |
| `couple_notify` | 通知中心：每条情侣事件都落一份库，是「对方不在页面上时也能看见邀请」的唯一通道 |

下线 16 张表：`couple_action` `couple_anniversary` `couple_catch_safeword` `couple_catch_safeword_use`
`couple_ceremony_coupon` `couple_comfort` `couple_dine_ticket` `couple_echo_deed` `couple_mood`
`couple_mood_reaction` `couple_mystery_box` `couple_point_ledger` `couple_quest_overtime`
`couple_scratch` `couple_spin_task` `couple_user_pin`（+ `couple_bond_day` 改名重建，不计数）。

**通知中心为什么算地基而不是功能**：它没有独立业务规则，只做「WS 推送的持久化与已读」。
删掉它的后果是邀请到达时对方若不在线就永远看不见——而功能①的成功与否完全取决于这一条。
`couple_notify` 的 event 字段不做白名单，事件前缀族收缩不影响它。

### 2. 打卡触发源改挂「双方当天都答完每日一问」，而不是自造一个新交互

贴贴被删之后，「双方当天都做了同一件事」这个语义在空间里只剩每日一问（每天每题、两人都答）。
改挂它有三条好处：口径只有一条（不必再解释"贴贴不算了为什么还连着"）、它是**每天必然发生**的行为
（不依赖情绪）、且它在同一个事务边界内可判定（`CoupleQuestionService.answer()` 末尾就知道对方答没答）。

落点唯一：`CoupleQuestionService.answer()` 里 `if (partnerAnswered) streakService.confirmBothAnswered(space, me)`。
另外保留一条既有特例：`CoupleStreakService.confirmCreationDay(space)` 在建空间当天写第 1 天，
否则「连续」要从第二天才开始，第一天白等。

代价（明示）：只聊天不答题的那天**不算打卡**。这与第 1 条判据一致——答题是保留功能，聊天不是。
`MakeupPolicy` 的话术同步改成「今天还不能补——两个人都答完今天的每日一问就算打卡 😉」，
领域话术就是用户所见原话，留着旧句子等于界面在说谎。

### 3. 积分台账整体删除，补签改成「免费 + 7 天窗口 + 每自然月 3 次」

`couple_point_ledger` 的四个进出账口全在被删之列，留着就是一张永远为 0 的空表，
而「补签扣 20 分」的闸门会当场变成"永远补不起"。取舍有两个方向：

- **A（采纳）**：稀缺性从"钱"换成"次数"——补签不花钱，但只能补最近 7 天、一个自然月最多 3 次。
- B（否决）：保留积分表、把赚分入口改成"答题给分"。这等于新造一条第 1 条判据里不存在的功能，
  而且用户明确要求删掉积分以外的东西。

A 的代价：连续 100 天档现在**可以在满打满算 3 次/月 × 若干次补签后抵达**，比原来更松。
接受的理由是这条档位的意义是"你们在一起一百天"，补签本来就带着明确的 `MAKEUP` 标记，
百日回顾页与看板都把补的次数量单独显示出来（`MemoryVO.makeupDays`），不假装它没发生过。
`StreakBoardVO` 因此去掉 `makeupCost` 与 `balance` 两个字段，新增 `makeupWindowDays` / `makeupLeftThisMonth`，
`canMakeup` 仍由后端算好下发，前端不再自拼闸门。

### 4. 心动值换供数并重新标定阈值，称号阶梯保持七级不动

原六项全部失效，现役五项（`IntimacySource`）：

| 供数 | 权重 | 取自 |
| --- | --- | --- |
| `daysTogether` 在一起天数 | ×1 | `couple_space.anniversary`（无则按 `created`） |
| `checkinDays` 累计打卡天数 | ×2 | `couple_streak_day` 行 |
| `longestStreak` 历史最长连续 | ×3 | 同一张表读时算 |
| `answerDays` 双方都答完每日一问的天数 | ×3 | `couple_question_answer` 按 day 聚合 |
| `wishFulfilled` 已实现愿望条数 | ×5 | `couple_wish` status=FULFILLED |

权重的取向：**越少见的行为越贵**。实现一条愿望是"有人为另一个人做了一件事"，给最高分；
答题天数与最长连续并列其次（都是持续投入）；在一起天数按自然日白送，所以最低。

阈值随之重标定：`{0, 60, 150, 260, 400, 560, 760}`（原 `{0,50,150,300,500,800,1300}` 是按六项供数调的，
供数换了还沿用的话，30 天档的「恋爱等级称号」解锁后等级仍会卡在怦然心动——那是称号与等级两套说法，
正是 CONTEXT.md 要避免的分叉）。新阶梯下按第 2 条口径满勤 100 天约 754 分，落在六级「相依相伴」，
**故意不给满级**：一百天是隐藏彩蛋页的门槛，不该同时把「相守一生」也发完。
七级的称号名与图标（怦然心动 ✨ / 心动初启 💫 / 甜甜热恋 🍬 / 形影不离 🧡 / 心有灵犀 💞 / 相依相伴 🌷 / 相守一生 💍）一字未改，
`IntimacyCalculatorTest` 把每条边界钉住。

### 5. `couple_bond_day` → `couple_streak_day`，`bond` 这个词还给它原来的意思

`CONTEXT.md` 里 `bond` 专属「贴贴」，贴贴卡本轮删除。继续叫 `couple_bond_day` / `BondDay` /
`BondStreak` 等于让一个词同时指「贴贴」和「打卡」，而下线的是前者。
改名范围：表名、列注释、`StreakDay` / `StreakDayRepository` / `StreakDays` / `StreakDayRepositoryAdapter` /
`CoupleStreakDayPO` / `CoupleStreakDayMapper`、索引 `uk_couple8_bond_day` → `uk_couple9_streak_day`
（索引名在本库是全局唯一的，`couple9_` 前缀实测未占用）。

用 **DROP + CREATE 而不是 RENAME**：H2 与 MariaDB 对「跨库改索引名」没有两边都认的写法；
而这张表是 2026-10-05 V52 才建的，仓库之外的环境里没有数据，改名的代价只是脚本行数。

### 6. `couple_space.stickers`（F28 贴纸墙）删列，与 50 天档不是一回事

`stickers` 是一张独立于七档解锁的老功能卡（用户在空间里自定义贴纸键值墙），
语义与 50 天档「专属贴纸包」撞车——两处并存会让人以为同一个功能有两个入口。
50 天档的实现本来就在前端（`src/utils/coupleVisual.ts` 里固定的一组贴纸，按 `tierUnlocked('custom-emoji')`
放行到 `EmojiPicker`），不依赖这一列。删列后 `SpaceVO` 少一个字段，
`CoupleSpace` 去掉 `STICKER_MAX`/`STICKER_KEY_MAX` 两个常量与 `stickers()` 方法。

`theme`（空间主题）**保留**：7 天档的动态背景是叠在主题上的，删了它档位奖励就没有落点。

### 7. F206 功能搜索与 F207 常用收藏随卡片数量一起下线

这两张卡是为"在十几张卡里找路"造的导航设施。四张卡一屏滚动即是全部，导航失去对象，
于是 `couple_user_pin` 表、`UserPin` 聚合、`UserPinRepository` 端口、`UserPinRepositoryAdapter`、
`CouplePinService`、`CouplePinController`、三个测试文件一并删除。
空间功能搜索框与 ⭐ 常用面板在前端同批拆除（`coupleCards.registry.ts` 仍保留四条目——
它是 `?card=` 深链与实时巡检脚本 `e2e-live/couple-audit.spec.ts` 的卡索引，不是导航功能）。

### 8. 共同日历删除，「在一起纪念日」保留

`couple_anniversary`（多人可增删的倒数日清单）删除；`couple_space.anniversary`（单个日期列）保留。
连带：`CoupleReminderJob` 不再扫日历表，只倒数在一起纪念日的 7/1/0 天；
`PUT /api/couple/anniversary` 与前端「纪念日」按钮保留，`/api/couple/anniversaries` 的复数 CRUD 全删。
判定依据同第 1 条：在一起的日子是百日回顾、恋爱天数、里程碑横幅三处的共同根据，倒数清单不是。

### 9. 不做数据迁移、不建备份表

被删功能的数据随之作废（心情、贴贴、好事簿、券本……）。这与 ADR-0007 第 10 条一致：
产品裁剪，不是结构演进。生产库执行 V53 前另行手动备份。
`schema.sql` 基线同步：全库 20 → 19 张表，`couple_*` 22 → 6，被删表按既有惯例只留
`-- smart_collections.X definition` 占位注释行。

### 10. 七档解锁的天数、key、文案一律不动

档位是产品事实：`bubble` 3 / `background` 7 / `nickname-glow` 14 / `pendant` 21 / `title` 30 /
`custom-emoji` 50 / `easter-egg` 100，key 一旦上线就冻结——前端按 key 挂视觉，
改了 key 等于把已解锁用户的外观没收。本轮唯一变的是**到达档位的行为**（第 2 条），不是档位本身。

### 11. 有意没做（沿用 ADR-0007 第 12 条）

不给 `couple_space` 加「一用户同时只有一个 ACTIVE 空间」的数据库唯一键。
理由不变：存量脏数据会让迁移在应用启动期直接失败，而这条不变式目前由
`CoupleSpaceRepository.findActiveByMember` 与建立流程的幂等判定守住。

### 12. 爱称合入 `PUT /profile` 之后，推送仍按「改了什么」分流

贴贴卡删除后，爱称的写入口从 `PUT /api/couple/bond/pet-name` 合并进 `PUT /api/couple/profile`
（宣言/主题/爱称三项一次提交）。合并写入口是对的——三项都是"空间本体的可改字段"，
各留一个端点等于把同一张表的三列拆成三个用例。但**推送不能跟着合并**：
只改爱称却给对方弹「TA 打扮了你们的小空间 ✨」，是把一件心事说成一次装修。

所以 `updateProfile` 保存前后各读一次三位的旧值，按实际变化分别推
`pet-name-changed`（爱称，含「TA 把给你的爱称收回了 🏷️」这条清除话术）与
`space-themed`（宣言或主题），两者都没变就一条都不推。`pet-name-changed` 因此是从裁剪前保留下来的事件，
现役事件 **15 个**而不是 14 个。回归保护：`CoupleProfilePushTest` 六条，
其中 `unchangedPetNameDoesNotCelebrateAgain` 已用变异验证过能变红
（把闸门改成 `if (true)` 后 2 条失败，还原后 6/6 绿）。

## 验证

- `mvn -o test`：**Tests run: 404, Failures: 0, Errors: 0, Skipped: 0 / BUILD SUCCESS**
  （删前 416，差的 12 条是三个 pin 测试文件）。
- `V53` 在本地 H2 文件库从 V1 全量重放通过（53 个迁移），`schema.sql` 基线与 Flyway 结果逐表核对。
- 守卫：`ArchitectureGuardTest` 五条全绿——新增表/端口/适配器与删掉的 16 张表同步收口，
  账本第 5 条（新增表必须同批建聚合 + 端口 + 适配器）无条件生效。
- 契约面：`/api/couple/**` 端点从 70 个收缩到 26 个；WS 事件从 44 个收缩到 15 个（`pet-name-changed` 保留，见第 12 条）。
- 前端：`pnpm build`（vite + vue-tsc）与 `pnpm test` 由主线程按新契约重写三张 spec 后取实测值。

## 后果

- 正面：情侣空间的边界第一次和需求一致（四张卡），心动值与打卡的每一分供数都能指到一张现役表，
  不再有"改了读路径没人发现"的隐形的账。
- 代价：v5–v7 三轮迭代攒下的 16 张表与其数据一次性作废；`docs/couple-features-v5.md`、
  `docs/acceptance-v5.md`、`docs/couple-trim-ranking.md` 里描述的功能从此过期——
  按 `feedback-docs-scope` 的口径不回写这些历史文档，只在本 ADR 与 CONTEXT.md 说明作废。
- 风险：一轮删除动了 100+ 文件，回归只能靠测试与真浏览器巡检兜。所以第 2/3/4 条每条都有对应单测
  （`CoupleStreakServiceTest`、`CoupleQuestionServiceTest` 的两条触发用例、`IntimacyCalculatorTest`
  的阶梯与权重、`CoupleIntimacyTest` 的端到端算分），而不是只改代码。
