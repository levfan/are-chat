# 情侣空间 v7 迭代验收记录（F350-F399，批次三十一至三十五）

> 逐批留痕：实现 → 测试真实全绿 → 走查 → 提交。**「当场修复」一律核对确已 commit**（v5 曾出现修复只留在工作区、记录却写「已修」的教训）。
> 规格：`docs/couple-features-v7.md`；后端配套地图 `.agents/skills/are-chat-map/SKILL.md`；前端 `.agents/skills/are-chat-web-map/SKILL.md`。
> 门禁数字（本记录收束时）：后端 `mvn test` **604 绿**（54→57 Controller、713→793 映射、273→310 表、V44→V48）；前端 `pnpm test` **166 绿**（154→166）、`pnpm build` 绿、registry 104→114 卡。

## 本迭代新增的验收手段（v5/v6 没有的三件）

1. **静态接线对账**（一次性脚本，`.tmp-audit/` 下，不入库）
   - 模板事件绑定 → 处理器定义：78 个 `.vue`、**729 处** `@click/@change/@submit` 等绑定，未定义处理器 **0**
   - 前端调用 → 后端映射：655→689 处 `http.*('/api/...')` 与 **793 条** Controller 映射对账，路径不存在 **0**（GET/POST/PUT/DELETE 四动词分别匹配，`${}` 与 `{var}` 视为同一段）
   - api 层方法 → 组件引用：667 个 `*Api.method` 中仅 3 个从无引用（`coupleApi.relationshipOf`、`repairApi.repairMakeupOffer`、`profileApi.friendsBirthdays`）——第一个是「查某人与我的关系」备用读接口，第二个是本次修掉的缺入口（见缺陷账 6），第三个是通讯录生日提醒未做入口
   - 组件引用图：64 个情侣组件全部被 `CoupleView` 或其子组件引用，无孤儿卡
2. **Playwright 实时点击巡检**（真实后端 + 真账号，不 mock）
   - 前置：`/api/auth/sms-code` 演示网关回显 `devCode` → 注册两个账号 → 管理员审批 → 加好友 → 建情侣空间 → 设纪念日，全程走真接口
   - 覆盖：按 `coupleCards.registry.ts` 的 **104 张功能卡**（现 114）逐卡逐按钮点击；每点一次判定四类信号（HTTP 5xx/404、页面异常/控制台报错、DOM 变化、弹窗/提示），零信号即记「点了没反应」
   - 两轮：第一轮裸点（撞空态守卫），第二轮 `FILL=1` 先把卡内输入框按占位符语义填样本值再点（真走写入链路）
   - 系统页另开一路：`/chat`（44 个按钮 + 发一条私信验 WS 与落库）、`/contacts`、`/admin`（admin 账号）
   - **护栏**：`/api/couple/dissolve`、`/api/auth/logout`、`/api/auth/deactivate`、`/api/admin/users/*/status` 在路由层直接挡掉；确认框一律点「取消」——第一版误点「确定」把情侣空间解散过一次，已修
3. **实体反射守卫** `src/test/java/com/smart/chat/EntityReflectionGuardTest`
   把所有 `@TableName` 实体过一遍 MyBatis `Reflector`，把「 getter 歧义导致运行时随机 500」变成构建期确定性失败（缺陷账 1 的回归保护）。
   ⚠️ 第一版写成「实例化一次即触发建 Reflector」是**无效的**：歧义 getter 会被包成 `AmbiguousMethodInvoker`，只有真正读属性（MyBatis 给 `#{et.xxx}` 取值时）才抛。改为逐字段 `reflector.getGetInvoker(field.getName()).invoke(instance)` 后当场跑红，点名出 `CoupleLaughJoke.frozen`、`CoupleWorldVow.witnessed` 两处漏网的真歧义（缺陷账 11）

## 缺陷账（全部已修、已复验、已提交推送）

1. **P0｜实体 getter 歧义让两批功能对每个用户全废**：`CoupleWorldGroupReport` 有 `Integer laughA` 字段（Lombok `getLaughA()`）又手写 `public boolean isLaughA()`，同一属性两个不同类型的 getter，MyBatis `Reflector` 按方法枚举顺序**随机**抛 `ReflectionException: ambiguous type for property` → `GET /api/couple/world/world` 与 `GET /api/couple/legacy/vault` 每次 500，两家与朋友、传世系统共 20 个功能前端整页空壳。**单测全用 mock，完全照不出来**，只有真后端浏览器巡检抓得到。全仓扫出 32 处同型冲突（另 30 处是「今天没炸」的哑弹）。
   修复：位判断 helper 统一改名 `xxxFlag()`（44 文件、113 调用点，含 `CoupleItem::isDone` 这类方法引用）；`delivered` 因 `CoupleLoveBank`/`CoupleMissExpress` 是真 `boolean` 字段不能全局改，`CoupleEchoSlow` 按类单改。加反射守卫 + 地图硬性红线。
2. **H2 保留字 `day` 让所有含该列的查询直接语法错**：MyBatis-Plus 生成的 `SELECT id,space_id,day,...` 不带引号，H2(MODE=MySQL) 里 `day` 是保留字 → `GET /api/couple/today`、`/api/couple/daily-life/dashboard` 实测 500；MariaDB 因 `day` 非保留字才侥幸可用（测试库与文档里承诺的 H2 备用部署都是坏的）。
   修复：`MybatisPlusConfig` 设 `DbConfig.columnFormat("`%s`")`，生成列名统一加反引号（两库都合法），两端点复验 200，全量绿后提交。
3. **MySQL 方言函数进 Wrapper**：`CoupleSecureService.depositTrust` 用 `.apply("DATE_FORMAT(FROM_UNIXTIME(created/1000),'%Y-%m-%d') = {0}")`，H2 无 `DATE_FORMAT` → `POST /api/couple/secure/trust` 500。改为毫秒日界 `ge/lt`（全仓此类手写 SQL 仅此一处）。
4. **批次三十二 双点打卡重复推送**：`mealTick/gazeTick/unplugTick/detox` 忽略实体 `tick()` 的「本次是否真的 0→1」返回值，重复点击会反复推 `focus-meal-both` 等事件。改为只在真变更时 update + 推送；25 条用例里有 4 条专锁此行为（含 `times(1)` 断言）。
5. **批次三十三 新家第一晚同型缺陷**：批次三十三的 `moveNight` 一开始也写成「双点就推」，被新写的用例当场抓出（`Wanted 1 time but was 2 times`）→ 与缺陷 4 同法收口。
6. **F324「提前递台阶」没有入口**：后端 `POST /api/couple/repair/makeup/offer`、前端 `repairApi.repairMakeupOffer` 都在，组件里唯独没按钮——卡片能显示台阶卡却递不出去。补按钮 + 单测（RUNNING 中出现、递出后消失并显示台阶卡）。
7. **群聊记者笑点列读反**：`laugh_a` 语义是「A 笑了 B 那条」，但 `CoupleWorldService.build()` 给 A 读的是 `laugh_b` → `iLaughed/partnerLaughed/canLaugh` 三处派生全错（我笑过后还能再笑一次并重复推事件；TA 笑过却把我锁死）。改为各读自己那一列，并补断言锁住三态。
8. **39 处「输入为空就静默 return」**：实时巡检最直接的产出——提交类按钮在输入框为空时**零反馈**，就是用户说的「点了没反应」。分布：`CoupleSecure`(6)、`CouplePoem`(5+1)、`CoupleCoach`(5)、`CoupleFunTalk`(4)、`CouplePlay`(3+2)、`CoupleDailyLife`(3)、`CoupleSpark`(2)、`CoupleSoft`(2)、`CoupleManage`(2)、`CoupleDistance`(1)、`CoupleDining`(2) …逐处按场景配中文微文案（不是统一一句），`CoupleAlmanac` 里 `if (!raw.trim()) return []` 是纯函数解析守卫，保留不动。
9. **恋爱语录机日期脆断言**：`CouplePoemServiceTest` 断言语录含「11」，但模板按 `stableHash(space|quote|day)` 轮换，其中一条不含 `{days}` → 约 1/8 的日子必红。改为按当天实际命中的模板断言（含 `{days}` 才查数字、含 `{partner}` 才查用户名、任何一条都不许留占位符）。
10. **巡检 harness 自身两处**（不是产品缺陷，但记录以免重踩）：确认框收尾点 `.last()` 等于替人按「确定」→ 曾把测试情侣空间解散；页签选择器写成 `[aria-controls$="-pane-x"]`（实际值 `pane-x`，无先导横杠）导致整轮空跑却只报「0 卡片」——补了首屏加载期错误单独记账，避免「整页 500」被误判成正常（缺陷 1 就是这么漏过一次）。

11. **P1｜实体反射守卫是恒绿的假守卫**：第一版只做 `SystemMetaObject.forObject(new 实体())`，从不读取任何属性，而 MyBatis 把歧义 getter 包成 `AmbiguousMethodInvoker`、**取值时才抛**——于是缺陷 1 那两处漏网歧义（`frozen`、`witnessed`）守卫一次都没报过。改成 `findForClass(clazz)` 后对每个非静态字段 `getGetInvoker(name).invoke(instance)`，跑红精确点名两处，改名 `frozenFlag()`/`witnessedFlag()` 后转绿。**教训：守卫自己也要有「它能变红」的证据**，否则等于没有。
12. **P2｜欢笑年报 guessTwin 没过滤年份**：`couple_laugh_guess` 表没有 `day` 列，年报里「两人预判一致」直接对全历史分组计数 → 切 2025 还是 2026 数字一样。改为借「考的是哪条冷笑话」的发出日归年（与周报同一口径），补一条 400 天前的条目 + 双人一致预测断言今年归零。
13. **P2｜服务层查重与库的排序规则不同口径**：`couple_laugh_moment`/`couple_laugh_joke` 的 uk 里 title/content 没写 `COLLATE`，MariaDB 默认 `*_ci` 把「Bo」和「bo」视为同一行，而服务层用 `find(space,user,title)` 精确匹配放行 → 用户看到的是「服务说没重复、insert 撞唯一键 500」。改为在已加载的当日/全空间列表上 `equalsIgnoreCase` 比对（语义与库一致，还省一次查询）。
14. **P2｜F348 周年抽奖奖池跟规格无关**：规格写的是「奖池=当年攒的迷你愿望（积分位）」，实现成了固定 8 条 Bank 静态愿望位——抽到的奖跟你们这一年实际做的事毫无关系。改为取本年 `couple_point_ledger` 里 `EARN` 的条目（去重、按 `PRIZE_MAX` 截断）作奖池，本年一条都没攒过才回落静态位。
15. **P2｜F358 未拆读的信把正文一起下发**：`CoupleEchoService.toSelf()` 不分状态返回 `content`，聚合接口 `GET /vault` 因此把「写给低落的自己」的原文直接交给前端——规格写的是「只在本人点开补给时可读」，前端不渲染也照样能在网络面板里看到，锁等于没锁。改为 SEALED 态返回空串、`readSelf` 置 READ 后才给原文（领补给那条路不吃 `toSelf`，不受影响）。**原先的单测把这条泄漏写成了预期**（断言 SEALED 时 `content` 等于原文），已反向锁住。
16. **P1｜`GET /today` 读时自动签收把「一键收全部」吃掉**：`CoupleFocusService.today()` 里调了 `settleRead()`，每次打开总览就把 `to_user=me` 的留言全部置已读 → `queueUnread` 恒 0、`POST /queue/read` 恒签 0 条、`focus-queue-read` 回执永不推。F362 的「你忙的时候我先把话放这儿，TA 回头看」整个签收仪式被页面加载静默消费掉了。改为只在 `queueRead()` 里结算。
17. **P2｜双点打卡卡不给「我这一格按过没」**：`TodayVO` 只有 `unplugMine`，`meal/gaze/detox` 三张卡只有合计与 `both`，前端只能本地记位——刷新/重进后 `meals=1` 无法归因，界面会把「我已经倒扣了」显示成「TA 已经把手机扣了，就差你这一个」，**是在说谎而不是降级**。补 `mealMine/gazeMine/detoxMine` 三个位，前端删掉本地位改吃服务端。
18. **P2｜F376 新家第一晚「补话」被静默丢弃**：`moveNight()` 只在 `tick()` 真翻转（0→1）时才 `updateById`，所以点过之后再来写那句话只改了内存对象就返回——界面报「补上了」，重进页面什么都没有。改为补话也落库（note 与存库值不同才写），推送仍只在真翻转那一次发。前端跟着放开输入口（原先因此把输入口 `:disabled`，等于让用户永远补不进去）。
19. **性能观察项（未改）**：`CoupleQuestService.build()` 里调 `wallOf()`，一次花 9 个查询，而 27 个写接口全部返回 `build()` 的整份聚合——用户点个「加油」「代记一笔」这种小动作，后端替他重算了整张年度成就墙。同型问题在 v7 各批聚合接口里普遍存在（批次三十一/三十二的 `build()` 也都是十几到几十个查询）。要治得改契约（把 wall 挪成懒读，前端已有 `GET /wall`），属结构改动，与联系人列表 2N 那条一起等重构口径定了统一处理。
20. **P2｜F380 心愿本容量算错，400 的建议无效**：`addWish` 与 `wishQuotaLeft` 都按 `findByOwner(...).size()` 计数，把**已兑现（已揭晓）**的行也算进 PER_OWNER_MAX，于是记满后报 400「先兑现几条」，可兑现根本不腾格——用户照做还是记不进去。改为按「还在藏着的条数」（`secret()`，`revealedAt == null`）计，兑现即揭晓就不占保密名额，文案改「兑现一条就腾出一格」，前端显示的剩余格数同步变准。新用例锁住「记满→400→兑现一条→再记成功」这条链，且验证过还原旧写法该用例当场跑红（`expected: 1 but was: 0`）。

## 误报账（查过、确认不是缺陷）
- 心情卡「😍 恋爱中」点了没反应：`selected` 默认值就是 `LOVE`，再点是同一值，无变化属正常
- `POST /api/couple/dict-quiz`、`/api/couple/chronicle/birthday-look`、`/api/couple/chronicle/archaeology` 的 404：后端 `BusinessException(404, …)` 的空态业务提示（词典还没收录 / TA 没填生日 / 考古层还空），前端 `onError` 直透成 ElMessage，不是路径写错
- `/api/couple/daily-life/soses` 的 400「上一条抱抱还在路上」：同一测试会话内重复点，属正常限流

## 批次三十一 F350-F359 回音壁（后端）

- 提交：`37ee85c`(db V44 八表 + schema 同步) → `fefff4d`(feat) → `acd9c30`(test 12 用例) → `6aefee5`(docs 地图)
- 全量 514 绿
- 口径要点：好事簿**单记录人口径**（`from_user` 写下的即「TA 爱我的证据」）；鼓励语罐取最小空槽复用；补给每人每天一次；慢递到日读时惰性结算；夸夸回执跨模块只读 `couple_praise`；给低落的自己只有本人开读不推对方

## 批次三十二 F360-F369 注意力保护区（后端）

- 提交：`db V45 八表` → `feat 13 映射` → `test 25 用例` → `docs 地图`；全量 539 绿
- 双人列口径：`_a` 属 `couple_space.userA`，连击/点亮一律读时算，不建定时任务
- 重复推送缺陷（缺陷 4）在提交前修掉并写进用例

## 批次三十三 F370-F379 人生关卡（后端）

- 提交：`50a9ba6`(db V46 十一表) → `254db74`(feat 29 映射) → `efb6a0d`(test 26 用例) → `3da9b66`(docs 地图)
- 归属与限流口径全部有对应用例：战报只由打这关的人交且一战一报、盖章只归对方且幂等、灯卡只有对方能留、痊愈只能病人自己宣布、加油卡只能舱外递且每天一张、区块纸箱与完成只认认领人、低谷回升只能本人宣布、小赢奖只能颁对方且一周一颁、到场只能非挂单人点
- **规格偏差记账**：V46 未偏离；实体层对账发现 `pod.cheers varchar(160)` 装不下 `CHEER_CSV_MAX=60` 条 MMdd（需 300 字符）→ 趁 V46 未推送用 `reset --soft` 取回重做，列宽改 `varchar(320)`（不留撤销记录，按用户口径）
- 成就墙计数一律直查原始表按 `yearOf(day)` 过滤，不用已 `limit` 的列表 VO 回算（沿用批次二十六/二十八「钳列表把年报数字一起改小」的教训）

## 批次三十四 F380-F389 聆听者（后端）

- 提交：`ba3e81c`(db V47 十表) → `1ac04a2`(feat 21 映射) → `353dd62`(test 24 用例) → `0c48018`(docs 地图)
- **规格自身矛盾**：F384 写 `uk(space,from_user,status)`，同一行又要求「在途每人 ≤5」——唯一键与限流互斥（第 2 条在途就撞键）。按 `idx_catch_thread` 普通索引实现，V47 头注释与本记录双重记账
- 连带结论：话头查重**不能用 `selectOne`**（同话题存在 DONE 行时会抛 TooManyResults），改为只在途比对；实体里那个 `find(space,from,topic)` 因此删掉，不留死方法
- **保密靠读时过滤**：未揭晓的心愿对主人不可见是在 `build()` 里按 `secret()` 筛的，不在 SQL 里筛——漏一步就把暗中心愿泄露给本人，用例里锁了「记下时一个事件都不推」

## 批次三十五 F390-F399 欢笑银行（后端）

- 提交：`2806cef`(db V48 八表) → `feat 17 映射` → `test 14 用例` → `docs 地图`
- 每日一逗的值班人由本周一锚的周序号奇偶在 userA/userB 间轮换，前端从总览的 `rotationHint` 读，不需要额外接口
- 三种「重复」的口径刻意不同并在用例里固化：判分/盖章/中弹/服用 → **幂等返回不重推**；结冰判定/证词 → **显式 400 不许翻案**
- 社死满一年转好笑是纯读时计算（`turnedFunny(today)`），不写库、不产生推送事件，用例用 `never()` 锁住

## 前端批次二十九 F330-F339 两家与朋友

- 提交（分 4）：`feat 类型与 worldApi 22 方法` → `feat CoupleWorld.vue 十卡 + shared 新增 world 子页签 + registry 104→114` → `test 11 用例（155→166）` → `docs 前端地图`
- 后端接口与规格不一致处（**登记，前端未擅自绕过**）：
  1. 规格与注释说 16 个 POST，Controller 实为 21 个——按实测全覆盖
  2. `visitReport` 不校验双确认，前端自行加 UI 闸门（未双确认不开战报输入口），比后端严
  3. `captionSubmit` 不要求三稿交齐即可被选走，前端不做额外限制，只给三个常驻槽位
  4. `GiftVO` 不下发「我是不是接单侠」，前端用 `useAuthStore().username` 与 `takerUser` 比对（沿用批次二十/二十三先例）
  5. `CoupleWorldBank.VISIT_TIPS` 与 `RELATIVES_SAMPLE` 两块静态内容没有任何 VO 下发，前端拿不到「带什么/聊什么/雷区」建议池与考前卷样例 —— **待裁决的后端补口**
- 落位说明：v6 规格未规定挂载页签，故新增 `shared` 页签下子页签 `world`「👪 两家与朋友」（与批次二十八把 repair 追加进既有子页签的做法一致）。v7 规格已写死五批落位：echo→care/rescue、focus→growth、quest→promises、catch→letters/send、laugh→rituals/fun，后续各批照此

## 前端批次三十 F340-F349 传世系统

- 交付（分 3）：`feat 契约层 types+legacyApi 13 方法` → `feat CoupleLegacy.vue 十卡 + timeline 新增子页签 legacy + registry 114→124` → `test 11 用例（166→177）`
- 主线程复核（不全信 agent 报的绿）：`npx vue-tsc --noEmit` 零错、`npx vitest run` 177 全绿、`npx vite build` 成功；另用脚本把后端 `CoupleLegacyService` 的 11 个 record 与前端 11 个 interface **逐字段比对，0 处不一致**（字段名对不上只会表现为界面空白，编译和单测都照不出）
- 组件口径：闸门不过一律 `ElMessage.warning` 配场景文案，不留静默按钮；后端 400 的中文 message 直透 `ElMessage.error`；写接口一律返回整份 `LegacyVO`，`refresh()` 整体替换后回填「本人当年可改写」的输入口（十问逐格 / 年审 / 发言 / 汇率 / 品牌），行内草稿（评分评语、清单说明）清空重填；主色墨玉绿 `#065f46`，全仓 grep 确认零占用
- 与后端口径的三处裁决：
  1. **F343 写接口回落 `goal=300`**：`build()` 里所有写操作都按 `DEFAULT_GOAL` 重算倒推，只有 `GET /vault?goal=` 认目标值——所以任何一次写操作后倒推卡会跳回默认目标。前端选择「跟着服务端返回的 goal 回填输入框」，宁可让显示的数与实际算的数一致，也不留一个「写着 1000 却按 300 算」的假输入框。**接受后端现状**
  2. **F348 奖池**：**不规格**，已按缺陷账 14 改后端
  3. **F347 清单条目不能删**：规格只写「双签封存」，未要求删除；已封存条目留在清单上不再显示「可封存」，符合规格。**接受**

## 前端批次三十一 F350-F359 回音壁

- 交付（分 4）：`feat 契约层 types+echoApi 15 方法` → `feat CoupleEcho.vue 十卡` → `feat 落位 care/rescue 最末 + registry 124→134` → `test 12 用例（177→189）`
- 主线程复核（不信 agent 报的绿）：`vue-tsc` 零错、`vitest` 189 全绿、`vite build` 成功，三条都由主线程重跑并各自取退出码；11 个 record 与 11 个 interface 逐字段（含顺序）脚本比对 0 不一致；静默守卫审计脚本扫出 15 处 `return`，逐条确认全是纯函数/computed 判定，无一是提交路径的空态 return
- 实时巡检：`ONLY_TABS=care ONLY_SUBS=rescue ONLY_KEY_RE=couple-echo- FILL=1` → 10 卡点开，**0 error / 0 dead / 0 4xx**
- 但巡检「0 dead」不等于「写通了」：读回 `/vault` 发现 deeds/juices/highlights 全空，说明那一轮点的全撞在提示上没落库。另写一次性探针直接对着 UI 走一遍才拿到证据：填框 → 点「记进好事簿」→ `POST /api/couple/echo/deed` 200 → `deeds=1` 且正文落库。**教训：harness 把折叠卡的「收起」按钮也算作有响应的控件，所以 0 dead 只证明「按了有反馈」，写入链路必须另行取证**
- 后端补修 1 处（缺陷账 15）：F358 未拆读的信在聚合接口里连正文一起下发
- 与后端口径的不一致（agent 报 9 条，逐条裁决）：
  1. F351「一键喊 TA」无独立接口，领取本身 `pushCoupleEventBoth("echo-refilled")` 就是喊 TA（规格写的事件名 `echo-refill` 与源码不一致）→ **接受源码**，卡片写明
  2. F359 规格要「夸夸回执率」，`YearlyVO` 只有回执张数无分母 → **前端只显示张数，不编造比率**
  3. F358 规格「只在本人点开时可读」但 `/vault` 下发正文 → **改后端**（缺陷账 15），前端那道更严的闸门保留作纵深
  4. F356 候选句不在 `EchoVO` → onMounted 跨模块只读一次既有 `coupleApi.praises()`（同走 safeLoad）→ **接受**，未新增后端字段
  5. 补给包只在 `POST /refill` 那一次返回，后续写接口 `build()` 重置为空包 → 卡片落「今天的领过了」态而不是报错，**接受**
  6. F54 `receivePraise` 与本批回执是同表两套签收 → 卡片写明区别
  7. `/calendar` `/year` 不在聚合里 → 懒读按钮，失败才 `ElMessage.error`，首屏仍静默 → **接受**
  8. `DEED_PAGE=30` 无分页参数 → 文案标「最近 30 条」，**接受**
  9. 比后端更严的九处前端闸门（当天已领/罐满 5/在途 3/精选满 12/未拆禁写/电量未点格/长度与日期格式/归属钮不渲染）→ **保留**，其中「电量 level 传 null 后端静默按 3 格」是后端该报错没报错，已记账不改

## 前端批次三十二 F360-F369 注意力保护区

- 交付（分 4）：`feat 契约层 types+focusApi 13 方法` → `feat CoupleFocus.vue 十卡` → `feat 落位 growth 页签最末 + registry 134→144` → `test 11 用例（189→200）`
- 主线程复核：`vue-tsc` 零错、`vitest run` 200 全绿、`vite build` 成功（三关各自取退出码）；6 个 record 与 6 个 interface 逐字段（名+序+可空性）脚本比对 0 不一致；静默守卫审计 21 处 `return` 逐条确认全是纯函数/`noData()`（后者自带 warning）
- 实时巡检：`ONLY_TABS=growth ONLY_KEY_RE=couple-focus- FILL=1` → 10 卡 **0 error / 0 dead / 0 4xx**；写入取证：UI 点「攒进队列」→ `POST /api/couple/focus/queue` 200，收件人 bob 连读两次 `GET /today` 未读恒为 1、`POST /queue/read` 后归 0
- 巡检顺带查出两处后端缺陷（缺陷账 16、17），都由 agent 报的「口径不一致」追出来：
  - `todaySettlesReadsSilently` 这条**旧单测把「读时自动签收」写成了预期**，实际让 F362 的一键签收永久签 0 条、回执永不推
  - `TodayVO` 缺 `mealMine/gazeMine/detoxMine`，前端只能本地记位，重进页面把「我已经点了」误显示成「就差你一个」——这是**界面在说谎**，不是降级
- 其余 7 条不一致的裁决：F361「开始前 1h 提醒」后端无实现 → 卡片不承诺提醒；F363/F365 时长后端不校验 → 只写口径不假装计时；F364 月度点亮无接口 → 不做月聚合；F368「周末发起」后端不校验周几 → **前端不加周末闸门**（比后端更严会挡掉合法提交）；Bank 若干静态文案不下发 → 前端自写同类句不改判定；`queue` 只下发 `to_user=me` 致「在途 ≤5」前端算不出 → 交后端 400 直透；`YearlyVO.hours` 是字符串、`topDay` 用空串 → types 照实标

## 前端批次三十三 F370-F379 人生关卡（v7 最大一批）

- 交付（分 4）：`feat 契约层 types+questApi 29 方法` → `feat CoupleQuest.vue 十卡（1622 行）` → `feat 落位 promises 页签最末 + registry 144→154` → `test 14 用例（200→214）`
- 主线程复核：`vue-tsc` 零错、`vitest` 214 全绿、`vite build` 成功（三关各自取退出码）；13 个 record 与 13 个 interface 用脚本逐字段（名+顺序+可空）比对 **0 不一致**（含根 QuestVO 的 20 个字段）；静默守卫改用**只扫「会发请求的 handler」**的方法重跑（1622 行组件的原始 `return` 扫描出 48 处噪音，限定到 27 个 handler 后只剩 `if (noData()) return` 一类，而 `noData()` 自带 warning）——这套审计口径比全文件扫准得多，后续批次沿用
- 实时巡检：`ONLY_TABS=promises ONLY_KEY_RE='couple-quest-' FILL=1 MAX_CLICKS=20` → 10 卡 **0 error / 0 dead / 0 4xx**；写入取证走 UI 点「挂上这一关」→ `POST /api/couple/quest/battle` 200，回读 `/board` 得 `battles=1`（name/kind=INTERVIEW/day=2026-10-20 全对）、`wall.battles=1`。顺带两次撞上前端闸门并给出可见提示（日期框填中文标题 → 「关卡日写成 yyyy-MM-dd」；没选关卡类型 → 「先挑一个关卡类型」），这就是「点了没反应」的反面证据
- 巡检查出缺陷 18（补话静默丢弃），并顺手把探针填错的两次点击变成了闸门有效性的证据
- 其余 8 条不一致的裁决（agent 编号 1-8）：功能编号与批文错位 → 保留十个 testid、卡内按后端源码口径装功能并在地图记映射表；`myPod/partnerPod` 是「最近一舱」不过滤在舱态 → 判在途只看 `in`；`daysLeft` 出舱/回升后恒 0 → 只在 in/low 时挂倒数；无 `lampMine`、无「本周我已颁」→ 按 `lampBy`/`awardedBy` 与 `auth.username` 比对（沿批次二十/二十三/二十九先例）；F374 加油卡与 F376 不说话卡的话术只在 WS 正文里、无 VO 下发 → 界面只显示张数（**待裁决是否补 VO**）；`wall.pods`/`awards` 是两项合计 → chip 照实写合计名；每次 `build()` 都算 wall → 记性能观察项

## 前端批次三十四 F380-F389 聆听者

- 交付（分 4）：`feat 契约层 types+catchApi 21 方法` → `feat CoupleCatch.vue 十卡（1220 行）` → `feat 落位 letters/send 子页签最末 + registry 154→164` → `test 14 用例（214→228）`
- 主线程复核：`vue-tsc` 零错、`vitest` 228 全绿、`vite build` 成功（各自取退出码）；后端 12 个 record 共 **104 个字段**与前端 12 个 interface 逐字段（名+顺序+可空）脚本比对 **0 不一致**；handler 定向静默审计 26 处 `return` 全是 `noData()` / `tooLong()` 两个自带 warning 的小工具
- 实时巡检：`ONLY_TABS=letters ONLY_SUBS=send ONLY_KEY_RE='couple-catch-' FILL=1 MAX_CLICKS=20` → 10 卡 **0 error / 0 dead / 0 4xx**；写入取证 UI 点「悄悄记下」→ `POST /api/couple/catch/wish` 200，回读 `/board` 得 `myWishes=1`、`wishQuotaLeft=11`（容量新口径同时生效）
- 查出一个后端缺陷（缺陷账 20）：心愿本容量把已兑现的行也算进去，记满后 400 却叫用户「先兑现几条」——**兑现根本不腾格，这条建议是无效的**
- 其余 11 条不一致的裁决：`CatchVO` 缺 `usedTodayMine` → 前端从服务端 `uses`（day 倒序，今日行必在最前）派生，**不留本地 ref、也不收输入口**，同时记为待补 VO 位；F383「前 1 天提醒」无定时任务且 `sensitiveRemindLine`/`wishSavedLine` 全仓零调用点 → 只渲染 `remindTomorrow`，不谎称已提醒；`monthUses` 是两人合计、`useCount` 是全历史（规格写「月度统计」）→ 照实标注；规格 F384 的 `uk(space,from_user,status)` 与 V47 DDL 的普通索引 `idx_catch_thread` 不符 → 后端早已按索引实现，接受；`ThreadVO.open` 恒 true 使后端一处分支走不到 → 记账不改（无用户可见影响）；同一 `WishVO` 在 `myWishes` 与 `revealedToMe` 里 `mine` 语义相反 → 兑现钮只挂前者；`dailyHint` 是「今天之前最近一句」而非昨天 → 文案照实；`TopicVO` 无 `statusLabel` → 前端只做文案镜像不参与判定；`mine/avoid` 后端无每日上限 → 前端不加闸门；F383 撤除权只允许代标人 → 按钮按 `!mineAsOwner`

## 尚未完成（接续点）

- 前端批次 欢笑银行（最后一批 10 个功能的界面，后端与 api 契约已就绪）
- 后端补口：`VISIT_TIPS`/`RELATIVES_SAMPLE` 是否下发；`profileApi.friendsBirthdays`、`coupleApi.relationshipOf` 要不要做入口
- 收官：全绿后升 `1.6.0-rc.1`（独立 commit）、双仓地图终稿、全量验收总结
