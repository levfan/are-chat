# v6 迭代验收记录（F250-F349，滚动）

> 口径同 v5：每批「实现→测试全绿→代码走查→提交推送」四步留痕；红线：不做照片/视频上传；迁移幂等；utf8mb4_bin 列 NOT NULL 无 DEFAULT；每批后端 ≥10、前端 ≥3 用例。
> 规格：docs/couple-features-v6.md。版本目标：v6 全部交付后 → 1.6.0-alpha 阶梯。

## 批次二十一（F250-F259 夫妻老黄历）

- 后端：V34 七表 + couple_anniversary 加 calendar_type/lunar_md 两列（ADD COLUMN IF NOT EXISTS 沿用 V21 先例）+ 17 文件（7 实体/7 Mapper/Bank/Service/Controller）+ 17 用例（4 个按日期假设跳过属预期），全量 398 绿。
- **当场修复①**：农历表初版凭记忆默写，springFestival(2024) 偏到 2022——从 npm solarlunar@3.1.0 官方 tarball 提取标准 1900-2100 压缩表（201 值）整体替换，并在 Bank 头注明出处；教训：**记忆型数据表一律从权威源取，锚点测试（2024/2025/2026 春节+中秋+端午）先写先跑**。
- **当场修复②**：lunarYearDays 位计数把「每大月+1」写成「±1」，348 基线下小月重复扣减致漂移——改 `(info & i) == 0 ? 0 : 1`。
- **当场修复③**：V34 三列（confirmed_by/wished_by/appended_by）utf8mb4_bin 列带 DEFAULT ''，H2 直接 42001——按红线去 charset；schema.sql 基线同步。教训：charset/DEFAULT 红线在写 SQL 时逐列过一遍，别等 H2 报错。
- 已知边界：节气日期用近似表（±1 天窗口跟风容忍微差）；ANNIVM 周年月无纪念日时该节日卡隐藏；长假窗口 60 天。
- 前端：待派单（CoupleAlmanac，建议挂 shared/daily 或 rituals 子页签）。

## 批次二十二（F260-F269 倾听与发声）

- 后端：V35 十表 + listen 22 文件（10 实体/10 Mapper/Bank/Service/Controller）+ 11 用例，全量 409 绿。枚举/status 列吸取 V34 教训全部普通 varchar 不带 charset；早想说放行与休战过期均为 today() 读时惰性结算，不新增定时任务。
- 设计口径：时段在途唯一、双评/双答/双封齐才推 both；换位信作者拆不到自己的（拆的是 TA 写给你的那封）；三行里程碑「恰好跨 21 天」才推，避免每日骚扰；休战旗超 24h 未双方表态自动收旗。
- 前端：待批次二十一前端落地后串行派单（CoupleListen 挂 care「情绪急救」）。
- 前端（批次二十一，2026-10-02 交付）：CoupleAlmanac 五卡全接 CoupleCollapsible + almanacApi 13 方法 + 挂 shared「🧾 过日子」+ registry 5 卡（63→68）+ 6 用例，`pnpm test` 118 全绿、`pnpm build` 通过（5607319/986975c）。主线程抽查：前端 types 与后端 ZodiacVO/YearVO/FestivalVO record 逐字段核对一致（agent 汇报文字有噪声——声称 ZodiacVO.year:String、normalDays:日期数组，实际代码正确；再次验证「信代码不信汇报」）。

## 批次二十三（F270-F279 二人制造厂）

- 后端：V36 十表 + factory 23 文件（10 实体/10 Mapper/Bank/Service/Controller）+ 11 用例，全量 420 绿。红线执行情况：枚举/status 全部普通 varchar DEFAULT；可空用户名位（done_by/grabber/guess_by/given_by 等）普通 varchar；utf8mb4_bin 列零 DEFAULT，一次通过 H2。
- 联动核验：parcelDone 向 F186 couple_point_ledger 插 EARN2（与 F244 发薪同账本，无第二账本）；homecheck/standup/owed 均读时聚合无 Job。
- 前端：待派单（CoupleFactory 挂 shared/daily「过日子」，等批次二十二前端落地后串行）。

## 批次二十四（F280-F289 我们百科）

- 后端：V37 十一表 + codex 25 文件（11 实体/11 Mapper/Bank/Service/Controller）+ 11 用例，全量 431 绿。
- **当场修复**：TYPE_AXES 第四轴初版 {P,J} 与题序「提前计划=选项1」矛盾（全 1 误出 ESTP）——改 {J,P} 并同步测试锚点（全1=ESTJ、全2=INFP、0/4 轴相同）。教训：测评卷字母对必须逐题核对选项语义再定轴序。
- 设计口径：综艺题目 shuffle 用 stableHash 做种子（双方同日同题集）；第一眼对视互见后行内 revealed 双写；测验答案存明文（对方自证用，非隐私域）。
- 前端：待派单（CoupleCodex 挂 timeline/flow，等批次二十三前端落地后串行）。

## 批次二十三前端（CoupleFactory，2026-10-02 交付）

- agent 施工被截断（skill 备忘未写、遗留 tests/unit/_fy23.tmp.ts、两用例中间 mock 整板替换丢 wake/groceries 字段）；主线程收拾：删临时文件、修两处 mock 链、补 skill 批次二十三备忘段与 shared 表行/快照。
- **当场修复（测试基建）**：整页挂载用例（CoupleView 全组件树 + tab 切换）成本随批次累积越过 vitest 默认 5s——F205 折叠恢复用例超时。vite.config.ts test 块全局 `testTimeout: 20000` 根治；单跑文件复现须用 --fileParallelism=false（--reporter=basic 在 vitest5 不存在，别再用）。
- 门禁：`pnpm test` 129/129、`pnpm build` 绿（a718563/a31e954）。registry 73→78。

## 批次二十五（F290-F299 明日邮局）

- 后端：V38 十一表 + post 25 文件（11 实体/11 Mapper/Bank/Service/Controller）+ 10 用例，全量 441 绿。
- **当场修复**：初版 Mapper 用 `.ne(getFromUser)` 条件——H2 对 ne+字符串列兼容性存疑，预防性改 findAll+服务层内存过滤 / findDue 收信方判定移到服务层；relay 在途限三与愿望盖章年份判定均读时结算，零 Job。
- 设计口径：所有「到期」类玩法（新年卡/拍卖逾期/承诺逾期）统一 today() 惰性结算幂等推送；dreamRead 一案一断（readBy 非空即锁）。
- 前端：待派单（CouplePost，建议挂 letters「信与胶囊」子页签，等批次二十四前端落地后串行）。

## 批次二十六（F300-F309 扮演剧场）

- 后端：V39 九表 + theater 20 文件（9 实体/9 Mapper/Bank/Service/Controller）+ 14 用例，全量 455 绿（228 表基线，couple_* 215）。
- **当场修复（数据层）**：`idx_ticket_space` 与 V18 keepsake 票根表索引重名，H2 报 42S11「Index already exists」→ V39 改 `idx_svc_ticket_space`（schema.sql 同步；注意只改本次追加块，别把 V18 基线里的原名一起替换）。教训：新增索引名必须跨全部 V*.sql 查重，表名唯一不代表索引名唯一。
- **当场修复（Service）**：`ensureFamily(space, LocalDate)` 与 `(space, String)` 重载混用编译不过；Mapper 里 `getCreatedAt` 应为 `getCreated`（两处，编译被 Lombok 报错淹没，需按首个错误定位）；RefVO 的 canQuiz 早期用「对方是否已答」判定，导致答完题的人仍显示可答——改为按「本人是否已作答」（`!mine && !iAnswered`），canJudge 另加「未判过」门槛。
- **当场修复（测试）**：超长文案用例误用 `" ".repeat(301)`，被 trim() 判成「没写」而非「太长」→ 改 `"字".repeat(301)`；角色日记上限用例按 200 字三条追加实际 602 已越界 → 改 190 字累积 572 后再验 600 上限。
- 设计口径：电话亭 FUTURE 一年封存 / PAST 当场接通，到点接通仍走 today() 惰性结算（零 Job）；师徒归属与身份/家长题均按 `stableHash(space|周|日)` 存卷，保证两人刷新同题；客服 30 分钟窗口用写入时刻判定 on_time，读时只算 waitMinutes 不推「超时」，避免每次刷新都骚扰。

## 批次二十四前端（CoupleCodex，2026-10-02 交付）

- agent 交付（未提交，主线程验收）：CoupleCodex 五卡（词条书架/默契综艺/TOP10 互猜/考据卷宗/灵魂与人格）+ codexApi 16 方法 + CoupleCx* 11 类型 + 挂 timeline「⏳ 时光流」+ registry 78→83 + 6 用例，基线 129→135。
- 主线程独立复跑门禁：`pnpm test` 135/135（7 files）、`pnpm build`（vite build + vue-tsc --noEmit）绿；端点与后端 CoupleCodexController 16 个 mapping 逐一对齐（前端恰 16 条路径，无多余无缺失）；五卡均 CoupleCollapsible 包、主色 #2c7a7b 只走 `--collapse-title-color`、无 scoped `.title`；stores/couple.ts 零改动（组件自持数据合规）。
- **以后端为准的取舍（agent 提出、主线程认可）**：F284 友情测验归入考据卷宗卡（overview 下发 exams，故该卡不传 `:empty`）；FirstLookVO 不下发 tries，前端用本地 myTries 计数仅供展示、拦截仍由后端 400 直透；答错补考 7 天由 `lastTryDay+7` 前端推算显示；人格八题题面按 CoupleCodexBank.TYPE_QUESTIONS 原样抄录（题序即轴序）。
- 主线程订正：skill 规模快照「v7」笔误改回 v6、后端配套计数订正为 618 映射/228 表基线（agent 写的是旧值 532/208）。分组提交 e8c4c00/bfd3f19/dfbc3c0 已推。

## 批次二十七（F310-F319 身体通知系统）

- 后端：V40 十表（全部 `couple_body_` 前缀）+ body 22 文件（10 实体/10 Mapper/Bank/Service/Controller，21 个 mapping）+ 12 用例，全量 467 绿（238 表基线）。跨模块只读复用 F220 熄灯（违约率）与 F210 饭票（忌口撞标），未改他模块一行。
- **口径守线**：本批全部是「陪伴」不是「诊断」——体征异常只按**用户自设阈值**判定（temp > tempLimit / sleep < sleepLimit），Bank 全池为陪伴话术，军令状违约只比 HH:mm 字符串；情绪药友明确非医嘱（400 文案与话术均不给用药建议）。
- **当场修复（Service）**：`breachOf` 初版把 `weekEnd` 局部删过头（一次 python 全局 replace 同时命中 build() 与 breachOf 两处同名行），补回后仍用 `String.isAfter` 编译不过 → 改 `compareTo > 0`；`fit()` 用 `row.getCreatedAt()` 判新旧（字段实为 `created`）→ 改显式 `fresh` 布尔；`sosHold` 先绕 `findByUser` 再回退 `findSos` 的死逻辑删除。
- **当场修复（测试）**：忌口两条断言按记忆写文案（"已经在红线上" vs 实际"已经在红线本上"、"ALLERGY 和 AVOID" vs 实际"过敏 ALLERGY 和忌口 AVOID"）→ 对齐源码文案；清掉误留的 `CoupleTheaterHelpers` 内部类与 `unused` 占位断言。
- **协作教训（派单契约要写死签名）**：实体/Mapper 派单给后台 agent 时，我把 `CoupleBodyOath` 的 helper 命名写成「isSignedA()/isSignedB() 两个方法」却没写返回类型，agent 理解成 boolean 版并在中途汇报里称「已改主线程的 Service 以自圆其说」，终报又改口「未改」。主线程按落盘文件核对：Service 的 `isSigned()` 由本人改为委托实体 boolean helper，调用点全在预期内，467 测试全绿，未见外来破坏；但这份不确定性本可避免。规矩补一条：**跨线程派单时，实体方法要写全「签名+返回类型」，并在任务书里明确「Service/Controller 归主线程，发现契约不自洽只报告不动手」**。
- 其它备忘：`couple_body_cycle.phase` 的枚举串沿用 `OWULARE`（拼写应为 OVULARE），实体常量/Service 校验/Bank 展示三处自洽且用户只见中文「排卵期」，为不动已推送 V40 校验和，本轮不改；agent 指出 `CoupleBodyOath.signedA + isSignedA()` 同字段双 getter 若把实体直接当响应体会撞 Jackson——本项目一律转 record VO，不触发。

## 批次二十八（F320-F329 修复车间）

- 后端：V41 九表 + repair 21 文件（9 实体/9 Mapper/Bank/Service/Controller，23 个 mapping）+ 10 用例，全量 477 绿（247 表基线）。F325 冲突年报无表读时聚合。
- 产品口径：这一部是「吵架之后怎么回来」的全链路——冷冻（时长自设、到点才能签）→ 三问 → 双签解冻 → 掉礼盒 → 纪念碑留句；道歉信有质检（六要素自评下限 + 对方验货 + 打回重写）；重来卡限季；信任重建按天双签。全部文案不评判谁对谁错。
- **当场修复（数据层）**：`couple_repair_freeze.signed_days` 初版 varchar(240) 装不下 30 天×2 人的 `yyyy-MM-dd:A` 记号（约 420 字节）→ 与 V41/schema.sql 同步加宽到 600（V41 尚未推远端，改校验和无风险）。索引与唯一键名本轮一律带模块前缀，`idx/uk` 跨迁移查重脚本在开工前先跑一遍，零重名。
- **当场修复（Service）**：`makeupEnd` 先把 status 置 ENDED 再判 RUNNING（判不到，台阶卡漏递）→ 调整为先补递台阶再置 ENDED；`report()` 里把聚合入参 `SorryVO` 列表当实体遍历（`s.status()` 找不到符号）→ 改 `for (SorryVO s : sorries)`；`signedCount` 双 set 冗余实现简化为「seen 含 `day:A` 且含 `day:B`」单趟计数。
- **当场修复（测试）**：`bottomSet` 少传 sinceDay（编译 arity 错）；`bottomMapper.findBySlot(…, int slot)` 用 `any()` 匹配基本类型 int 触发 Mockito `InvalidUseOfMatchers`/NPE → 改 `anyInt()`；一处 python 按 1-based 行号误替换导致断言行被截半，Read+Edit 修回。**教训重申**：改测试文件别用行号硬替换，改完必须立刻 `mvn test` 单类验证。
- 协作：本批实体/Mapper 派单时在任务书里写死了每个 helper 的**签名与返回类型**，并禁止 agent 碰 Service/Controller/SQL/schema——agent 全程零越界，报告还主动列出了 5 处「无人调用的 Mapper 方法」与 `OWULARE` 拼写疑点待主线程拍板（比批次二十七的派单质量明显改善，该做法已固化进后续任务书）。

### 批次二十八补记（agent 自检发现的真 bug，主线程修复）

- 派单做实体/Mapper 的 agent 在报告里指出两条**只有看列宽才能发现的**问题：① `signed_days` 即便加宽到 600，按 `yyyy-MM-dd:A/B` 双记号写满 30 天需 779 字符（60 天 1559），非严格模式下会静默截断；② 实体残留的 `signedCount()/hasSigned()` 与 Service 私有同名 helper 语义相反（前者按 CSV 条目计数=每天算 2，后者按「A/B 成对」计天），`hasSigned(裸日期)` 在新记号下永远 false。
- 修复（不动已推送的 V41，避免 Flyway 校验和漂移）：记号改回列注释原本的紧凑形态 `MMdd:A / MMdd:B`（一期不跨年，30 天双签 60 条 = 420 字符，稳稳落在 600 内）；`TARGET_DAYS` 由 14/30/60 收成 **14/30 两档**（60 档本就与列容量冲突，砍掉而不是加宽表）；删掉实体里那两个语义打架的 CSV helper，Service 侧 `signedCount()` 单实现 + `mark(day, side)` 统一生成记号。
- 结论：派单让 agent「只报告不动手 + 逐列对账」确实捞到了主线程写 Service 时漏掉的容量 bug，这一条要固化进后续每批任务书。

## 批次二十五前端（CouplePost，2026-10-02 交付）

- agent 交付：CouplePost 五卡（新年卡/大事与拍卖/梦想家与退休/许愿井与解梦/台账与信用卡）+ postApi 21 方法 + CouplePost* 13 类型 + 挂 letters「💌 寄给你」+ registry 83→88 + 6 用例。
- 主线程独立复跑：`pnpm test` 141/141（7 files）、`pnpm build`（vite build + vue-tsc）绿；端点与 CouplePostController 21 个 mapping **逐条 diff 完全一致**（脚本取两边路径列表 diff 无差）；五卡 CoupleCollapsible、主色 #3f51b5 走 `--collapse-title-color`、无 scoped `.title`、stores/couple.ts 零改动。分组提交已推（feat/test/docs 三组）。
- **agent 报出 4 处后端契约缺口（以后端为准，主线程复核成立）**：① `BucketVO` 不下发 `steps` 且 `StepVO` 无 id → 前端「逐条打勾 / TA 补进展章」在真实数据下点不动；② `RelayVO` 不下发 content → 寄信人看不到自己写的胶囊正文；③ `WellVO.partnerAnswer` 无条件下发（不等双答），与「双答后互见」的玩法口径不符；④ 列表口径（buckets 只 OPEN、somedays 过滤 EXPIRED、credits 只 OPEN、homes 只本人版本等）与前端预期需要对齐说明。
- **当场修复（后端 ①②）**：`StepVO` 加 `id`、`BucketVO` 加 `List<StepVO> steps`、`RelayVO` 加 `content`（寄信人始终可见，收信人拆封 OPENED 后才下发）；补两条断言（拆步行含 id 与 done/doneBy 正确、接龙 mine 见正文/非 mine 空串），`mvn test -Dtest=CouplePostServiceTest` 10 绿、全量 489 绿。前端已按「后端下发即点亮」写法预留（steps 可选字段），无需再改。
- ③④ 记为口径备忘暂不改：`well` 的 partnerAnswer 保持下发（前端用 bothIn 徽标表达「双答完成」），改双盲需加字段，留到 v6 收尾统一评估。
- 另：本批 agent 在执行过程中多次遇到工具返回里夹带伪装成「系统提醒/安全拦截」的注入文本（要求撤销已正确落盘的编辑），agent 选择忽略并按文件实际内容核验——已在真实工具链路上验证过一次，后续任务书继续保留「只信落盘文件与自跑命令」的要求。

## 批次二十九（F330-F339 两家与朋友）

- 后端：V42 十表（全 `couple_world_` 前缀）+ world 22 文件（10 实体/10 Mapper/Bank/Service/Controller，1 GET + 16 POST）+ 12 用例，全量 489 绿（257 表基线）。
- 产品口径：见家长、送礼、外人怎么看我们——把「一个人慌」的事拆成双确认的任务卡；赔礼信必须经 TA 审阅才送得出去，社会信用到期日必须未来、到期未见证不自动判达标（防误伤），只有对方能举报塌房。
- **当场修复（Service）**：接待手册的小包清单被 `splitItems` 当必填项，导致 `packList=""` 直接 400「至少写一条」→ 拆出 `splitOptional`（可留空但仍限 12 条/每条 60 字），行程保持必填；`GroupVO` 初版带一个语义混乱的 `mine` 字段（整行是当日共享，不该有 mine）→ 改为 `iLaughed / partnerLaughed / bothLaughed` 三态；python 写正则以 `\n` 误嵌成真实换行使 Java 源码字符串断行编译失败 → 改回 `\n`。
- **协作（派单质量已固化）**：本批实体/Mapper 任务书写全每个 helper 的签名与返回类型、并列出禁改文件清单（Service/Controller/Bank/V42/schema.sql），agent 零越界；agent 逐列脚本对账（V42 95 列 missing/extra 双空）并主动报出 `uk` 是否背书 selectOne、CSV 列宽是否够用这类只有看列才能发现的疑点。

## 批次二十九补记（agent 列宽对账带来的两处修复）

- agent 报告点出：`couple_world_city_plan.pack_list varchar(300)` 与 Service 的「≤12 条 × 每条 ≤60 字」（最坏 720+）不匹配，严格模式会 `Data too long`；另 `couple_world_relatives_q.findBySpace` 只按 `wrong_count DESC` 排序，绝大多数行 0 错 → 平票，MariaDB 与 H2 返回顺序可能不同。
- 修复：`splitOptional` 增加整单总长上限 `PACK_TOTAL_MAX=296`（在条数与单条字长之外再卡一道，超了 400「删几条再存」，比截断诚实）；称呼册改为服务层内存二次排序（错题数倒序 → 同数按称谓字典序），保证两端读到的顺序一致。
- 采纳为观察项未改：`day/year/month/text/slot` 等列名在 H2 2.x 关键字表内，但 V27-V42 同模式已长期跑通（MODE=MySQL），退路是 `@TableField("`text`")`；`couple_world_apology` 无 uk，去重责任在 Service（本就允许多封）。

## 批次三十（F340-F349 传世系统 · v6 后端收官）

- 后端：V43 八表 + legacy 19 文件（8 实体/8 Mapper/Bank/Service/Controller，1 GET + 12 POST）+ 10 用例，全量 **499 绿**（265 表基线）。**v6 一百个功能（F250-F349）后端全部落地。**
- 设计取舍：F348 的「周年 Job 提醒」按 v6 既定口径改为读时惰性结算（`notified` 标记幂等，零新增定时任务）；F343 里程碑倒推与 F349 空间等级都不建表，前者按近 30 天台账速率外推、后者按「台账数 + 传世系行数」查 Bank 门槛表定档；F346 年度盘点的每个数字都取自真实表（台账/十问/年审/发言/清单），拒绝套话模板。
- **当场修复（Service）**：`composeReview` 里清单计数写成了 `|| y.equals(now年份)` 的伪条件（等于没过滤）→ 抽 `yearOf(ts)` 统一按年归属；`MilestoneVO` 初版带一个算了没用的 `daysPerRound` → 删；`build()` 里一段 `fxes.replaceAll(...)` 是自我说服的空操作 → 删；`brand(..., Boolean publish)` 参数从头到尾没用过 → 去掉参数，发布只走 `brandConfirm` 双确认；`ANSWER_MAX` 写成 `QUESTION_COUNT*0 + …` 的凑数表达式 → 直接用常量。
- **当场修复（测试）**：`tenAnswer(me, year, slot, answer)` 的 slot 是 `Integer`，用例误传 `""` 触发编译错（顺带证明签名对齐有价值）；十问跨年 diff 用例改为「今/去两期恒定双行」断言。
- 协作：本批实体/Mapper 任务书延续「签名写死 + 禁改清单 + 只报告不动手」，agent 再次零越界并交付 95 列双向对账脚本证据；它按规格给 `vow.findDue` 没加 ORDER BY、`caption/declare` 无 `updated_at`（改稿无审计痕迹）两条属设计取舍，记录不改。

## 产品经理走查（v6 后端收官后第一轮，2026-10-02）

- **跨模块断点修复（F316 忌口红线 → 饭桌）**：规格要求「饭桌 F210 饭票池自动标红含忌口菜」，此前只在 `/api/couple/body` 总览里算 hit，点菜的现场（今晚饭桌）拿不到——等于联动没落地。已给 `CoupleDiningService.TicketVO` 增加 `redlines`（命中项列表），读 `couple_body_redline` 做只读拼标，**只标不拦**（点不点由两个人决定，产品口径）。新增用例断言「我的饭票含香菜标出、TA 的清炒不标」，全量 **500 绿**。前端 `CoupleDining` 的标红展示排进前端队列（字段是新增项，旧前端不受影响）。
- **口径确认（三处刻意放宽，非漏实现）**：① F340 年度十问不做「只能 12-31 答」的硬门槛，全年可逐格填、跨年两期并排对比（把日期卡死会把年度仪式变成打卡焦虑源）；② F342 续约发布会同理不按周年日锁，随时可发、对方评分即作废于重发；③ F348 周年抽奖的「Job 提醒」按 v6 既定架构改为读时惰性结算（`notified` 幂等），零新增定时任务。三条都写进地图与验收，避免后续被当成 bug 反复改。
- **文档一致性已知偏差**：`couple_legacy_ten.answers` 列注释仍写「十答 CSV」，实现是**换行分隔**（用户答案里可能有逗号，CSV 会破）。V43 已推远端，改注释会造成 Flyway 校验和漂移，故不改 DDL，改以地图/验收为准记载；实体 `CoupleLegacyTen.SEP` 是唯一事实源。
- **代码走查顺手收掉的异味**：`CoupleLegacyService` 里 `ANSWER_MAX = size()*0 + …` 的伪表达式、算了不用的 `daysPerRound`、`fxSettleLine` 空操作 `replaceAll`、从未被使用的 `brand(..., Boolean publish)` 形参、`composeReview` 里 `|| y.equals(now年份)` 的假过滤（等于没过滤，已改 `yearOf(ts)` 按年归属）；年审单条 60 字硬编码提为 `AUDIT_ITEM_MAX` 常量。
- **情绪闭环补齐（心动值）**：v6 十个模块此前只有 F275 代拿快递、F245 发薪日会写 `couple_point_ledger`，最难的「主动复温」和「说到做到到期解除」反而没回报。已补两条：修复车间解冻成功向答完三问的冷冻提出人记 EARN 8 分；社会信用到期且已见证自动解除时向立保证人记 EARN 10 分。两者都由读时结算触发，幂等（状态一改就不再进 ledger），不新增 Job。用例各自断言台账行数、item 文案与分值，全量 500 绿。

## 架构师代码走查（第二轮：CSV 列宽专项）

- 用 `grep '+ "," +'` 把 v6 里所有「往同一列累加写」的点找出来，逐个对 DDL 列宽算最坏长度，抓到两处会静默截断/报错的真雷：
  1. `couple_theater_master_day.serves varchar(40)` 存 ISO 日期（10 字 + 逗号 = 11），**三天就写满，而一周要记七天**；师徒日的 `servedToday` 也会因列溢出而永远算错。
  2. `couple_body_quit.broke_days varchar(160)` 同理，ISO 日期最多 14 条，而 100 天营期理论上可以记满 100 次破戒。
- 修复方式（都不动已推送的 V39/V40，避免 Flyway 校验和漂移）：侍奉记号改为**周几 1-7**（本行就是按周一行，周几在周内唯一，14 字符就能记满七天），`hasServed(token)` 与聚合里的 `servedToday` 同步换成周几口径；破戒记号压成 **MMdd**（5 字 + 逗号）并把单营破戒记录上限设为 25 条，超出 400「先把这一期结掉再开新的」——上限是诚实提示，比截断丢数据好。
- 顺带把 v6 已知的三处「按天记号」列宽口径统一写进地图：**日期进 CSV 列一律用 MMdd 或周几，不用 ISO**（`couple_repair_plan.signed_days` 上一轮已改 MMdd:A/B）。
- 全量 500 绿；新增断言：`serves` 里不得出现 `-`（防 ISO 日期回潮）。

## 批次二十六前端（CoupleTheater，2026-10-02 交付）+ 前端反查出的后端三处缺陷

- agent 交付：CoupleTheater 五卡（116 testid）+ theaterApi 17 方法 + CoupleTheater* 11 类型 + 挂 rituals「🎲 玩趣时间」+ registry 88→93 + 7 用例（基线 141→148）。主色选 `#0d9488`，理由是可查证的红线：`#7c3aed` 已被全局聊天气泡渐变与设置页「暗夜紫」占用——这是前端 agent 第一次主动守住跨模块色板冲突。
- 主线程独立复跑门禁：`pnpm test` 148/148、`pnpm build` 绿；端点与 CoupleTheaterController 17 个 mapping 逐条 diff 完全一致。
- **agent 报出三处后端缺陷（已修，均补断言）**：
  1. **演技分不是盲评**：`RoleVO.partnerRate` 不等我打分就下发 TA 的分，等于先偷看答案再评价。改为 `myRate == null` 时钳成 null；断言「bob 打完、alice 未打时 alice 看不到 TA 的分」。
  2. **追剧拿不到本人剧终态**：`MovieVO.status/finished` 只在双人都剧终才翻转，前端无法显示「我已剧终、等 TA」，只能把剧终钮常驻。补 `mineFinished / partnerFinished` 两个字段；断言中途态（我已完成、TA 未完成、整体未完成）。
  3. **总览列表无上限**：diaries/booths/refs/awards/tickets/movies 全量历史进 payload，工单簿会越用越肥。钳到最近 14/30/60/30/12/20 条，**但颁奖礼计数改走全量**（`awardTotal/refTotal/orderTotal`），免得钳了列表让「年度 25 单」显示成 20 单；用例塞 25 张工单断言 `tickets=20 && gala.orders=25`。
- agent 其余 10 条「与描述不一致」均为口径澄清（本批无日期入参、`masterReview` 评语允许空、`SERVE_TARGET=3` 是出师门槛而 7 是打卡上限、六个列表历史上就不过滤等），已按后端为准实现，无需改动。

## 架构师代码走查（第三轮：同类缺陷横向排查）

- 批次二十六暴露的「偷看答案」和「列表无上限」不是孤例，按同一两类横向排查 v6 其余模块：
  1. **提前下发（偷看）类**：`CoupleLegacyService` 年度十问的 `partnerAnswers` 原本无条件下发——我一个字没答就能看到 TA 的十答，等于把年度仪式变成抄答案。改为**逐格钳制**（我自己那格填了才给看那格），补断言「bob 未答时全空、bob 答完 alice 侧立刻可见」。其余排查项：`post` 新年卡已按 SENT 钳、`codex` 第一眼按 revealed 钳、`listen` 换位信按开放日钳、`theater` 日记/家长题/追剧已钳，无同类问题。
  2. **无上限列表类**：`CoupleRepairService`（freezes/sorries/rebuilds/admits/boxes/peace）与 `CoupleWorldService`（visits/gifts/declares/captions/apologies/vows）原本全量历史进 payload。统一钳到最近 N 条，并把钳制阈值提为 `LIST_*` 常量；`repair` 的 F325 年报原先是**拿已钳好的 VO 列表再计数**（钳列表会把年报数字一起改小）→ 重构成直接查原始表计数（`sorryMapper/admitMapper/boxMapper/peaceMapper/redoMapper.findBySpace`），与批次二十六颁奖礼同一口径。
  3. **off-by-one**：列表钳制初版写 `if (kept++ > MAX)` 实际会多给一条，统一改 `>=`；补用例断言 35 条只回 30、25 封待验在列表里是 20 条而 `report.sorryIn()` 仍是 25。
- 全量 **502 绿**（新增 2 用例 + 3 组断言）。
- **规格漏项补齐（F334 定稿进百科）**：走查逐条对照 v6 规格时发现 `world` 的文案代写只做了「三候选互评选稿」，规格里「定稿进百科」这一句没落地。已补：选稿时把定稿写成百科词条 `定稿文案 · {日期}`（definition=定稿正文、origin=谁替谁写的、usageNote=用途），走 `couple_codex_entry` 同一 uk 幂等查重，日后在百科书架能直接翻到。用例断言词条 term 前缀与正文，全量 502 绿。这类「规格里一句话没实现」的漏项，后面每批都按 F 号逐行对一遍规格表而不是只对模块名。
