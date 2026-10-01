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
