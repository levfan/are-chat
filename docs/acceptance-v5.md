# v5 迭代验收记录（F200-F249，滚动）

> 口径：每批「实现→测试全绿→代码走查→提交推送」四步留痕；发现即修的记「当场修复」，跨批遗留记「待办」。
> 红线：不做照片/视频上传；情侣功能重情绪价值；迁移幂等；测试基线每批后端 ≥10、前端 ≥3。

## 批次十六（F200-F209 体验重构与验收）

- F206 头部搜索 + F207 常用收藏 + F208 首访气泡 + 五页签拆子页签：前端重构已落地（are-chat-web 2ec3215/895276e），`pnpm test` 91 全绿。
- **当场修复**：F207 后端红测——`CouplePinService.savePins` 语义与测试冲突（mock 回读恒 null），改为返回本次保存的 keys；独立 fix commit 3bac1b2。教训沉淀进规范：提交前必须真实跑全量，不引用上一次的「绿」。
- **当场修复**：CoupleSpark 根 testid `couple-dashboard` 与总览卡撞名 → `couple-spark-dash`。
- 待办：F205 卡片折叠组件（CoupleCollapsible）未实现，排批次二十前端后补。

## 批次十七（F210-F219 饭桌）

- 后端：V30 七表 + dining 18 文件 + 20 用例全绿；**当场修复**：`V30.locked_by` varchar(50) 装不下 userA,userB CSV，未提交前加宽至 120 并同步 schema.sql。
- 前端：CoupleDining 四分区 + diningApi 16 方法 + 5 用例，96 全绿（4a30f02/08c536c）。验收抽查：board 写接口整板替换、canLock 口径、registry key=卡根 testid 遵从现状。

## 批次十八（F220-F229 体温同步）

- 后端：V31 九表 + cozy 21 文件 + 19 用例全绿。**开发中自纠**：canLock 恒真式删除；yearReport 残留双赋值改 findByYear；数羊双完结推送条件写反（后完成者漏推）已删条件；streak 回看上限 400→60 次查询；慢生活「改单也推 planned」加 isNew 守卫。
- 前端：CoupleCozy 十卡 + 月度指数进度条 + cozyApi 14 方法 + 5 用例，101 全绿（90c44f3/5bb7822）。

## 批次十九（F230-F239 小日子·仪式感）

- 后端：V32 七表 + ceremony 17 文件 + 16 用例，全量 370 绿。**当场修复**：RecapVO 误传实体改 getFromUser；mark 全勾判定条件（单条才算全勾）改 done==size；all-done 改推 both ceremony-all-done；nextRenewAnchor 起点当天会误报 0 天已修；almanac 对坏日期数据加 try-skip。
- 已知边界：续约日按「整 100 天/周年当日」精确判定，无宽限日（产品口径：错过就等下一档，前端把 daysToNext 亮出来）。
- 前端：进行中（CoupleCeremony 六分区，timeline 时光流），完成后验收记本文件。

## 批次二十（F240-F249 我们公司）

- 后端：V33 六表 + board 16 文件 + 11 用例，全量 381 绿。**当场修复**：attendState 一次写坏后立即改回；测试断言文案（「最多封 2 个职位」）与发薪日跨月构造修正。
- 联动核验：paySalary 同时写 couple_board_salary 与 F186 couple_point_ledger（EARN+5），职级 F243/名片 F248 均读同一台账，与 /api/couple/manage 积分市场共用余额口径，无第二账本。
- 待办：前端 CoupleBoard（shared 经营所）批次十九前端之后串行派单。

## 版本与基线快照

- 后端 `mvn test`：354 → 370（批次十九）→ 381（批次二十），全绿。
- 前端 `pnpm test`：91 → 96（批次十七）→ 101（批次十八），全绿；registry 卡 47→53。
- 版本节奏：批次十六~十七 已交付；十八~十九交付后 → 1.4.2-alpha；二十交付并验收通过 → 1.5.0-rc（独立 chore commit）。
