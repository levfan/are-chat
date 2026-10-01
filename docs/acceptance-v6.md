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
