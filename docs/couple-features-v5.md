# 情侣空间 5.0 · 50 项迭代总览（F200-F249）

> 延续 v4（F150-F199）的分层与提交节奏。本轮两条主线：**先把页面做「轻」**（用户反馈页面臃肿、层级深——批次十六专做信息架构重构与全系统验收），**再把日子过「暖」**（吃饭、体温、小日子、我们公司四大主题，全部纯文字/打卡/状态机，情绪价值优先）。
> 红线不变：**不涉及照片/视频上传**（服务器部署要求高）；迁移幂等；utf8mb4_bin 列必须 NOT NULL（可空列不带 charset）；前端不改默认页签 `promises`；新 VO 一律 `Couple+域前缀` 并先 grep types/api 防撞名。

## 批次十六：体验重构与全系统验收（F200-F209）— 前端 IA 为主 + 收藏落库 `/api/couple/pin`（V29）

| # | 功能 | 说明 | 落点 |
|---|------|------|------|
| F200 | shared 页签拆分 | 8 卡组拆子页签「🧾 过日子」（城市/异地/倒数/生活/共享/日常/基金）+「🏪 经营所」（CoupleManage 十卡），消除超长滚动 | CoupleView 子 el-tabs |
| F201 | care 页签拆分 | 5 卡组拆「🚑 情绪急救」（Care/Comfort/Makeup）+「✨ 默契亲密」（Soft/Spark） | CoupleView 子 el-tabs |
| F202 | rituals 页签拆分 | 5 卡组拆「🌙 每日仪式」（Rituals/Daily/Truth）+「🎲  playful 时间」（FunTalk/Play） | CoupleView 子 el-tabs |
| F203 | letters 页签拆分 | 5 卡组拆「💌 寄给你」（Letter/Whisper/Capsule/Poem）+「🗃️ 收藏册」（Keepsake） | CoupleView 子 el-tabs |
| F204 | timeline 页签拆分 | 6 卡组拆「⏳ 时光流」（OnThisDay/Firsts/HeartMoments/Timeline/Chronicle）+「🏛️ 博物馆」（Museum） | CoupleView 子 el-tabs |
| F205 | 卡片折叠 | 每张功能卡标题行带折叠钮，状态记 localStorage（默认全展开→自动收起空卡） | 新 `CoupleCollapsible` 包装组件，热门卡默认收起 |
| F206 | 空间功能搜索 | 头部搜索框：按卡片名拼音/关键词命中→切页签+展开+滚动定位（纯前端索引表） | CoupleView + 常量索引 |
| F207 | 常用收藏 | 每人 pin ≤6 张卡，各页签顶部「⭐ 我的常用」横排；表 couple_user_pin（uk space+user，order_json） | `/api/couple/pin` + 各页签顶部 |
| F208 | 页签首访气泡 | 每个页签第一次进入弹一枚一句话引导气泡（localStorage 记忆，不弹窗不打断） | CoupleView |
| F209 | 全系统验收回归 | 按 F1-F199 逐项功能走查（后端单测+前端用例+真实交互），产出 bug 清单并即时修复，修复单独 `fix` commit；其它菜单（聊天/通讯录/我的）臃肿点一并记录优化 | 滚动执行，docs/acceptance-v5.md |

## 批次十七：两个人的饭桌（F210-F219）— shared「过日子」组 `/api/couple/dining`（V30）

| # | 功能 | 说明 | 落点 |
|---|------|------|------|
| F210 | 今晚饭票 | 各提名一道菜（菜名+一句理由），同日两票都含同一菜=「命中」自动推 both | CoupleDining（couple_dine_ticket） |
| F211 | 吃什么裁决 | 从双方当日饭票按空间+日稳定 hash 裁决一道，终结选择困难（无表） | CoupleDining |
| F212 | 吃过星评 | 吃完登记：吃了啥+1-5 星+一句点评，攒成我们的餐厅档案 | CoupleDining（couple_dine_rate） |
| F213 | 踩雷 depot | 黑名单小店：店名+避雷理由，谁提议谁有权划掉 | CoupleDining（couple_dine_nogo） |
| F214 | 本周菜单 | 周一锚 7 天格子各排一顿正餐，对方可见可改 | CoupleDining（couple_dine_weekplan） |
| F215 | 家常菜搭档 | 周末各报一道拿手菜+一句「配饭指数」，凑一桌 | CoupleDining（couple_dine_homecook） |
| F216 | 点单机 | 今日心情 5 选 1→推荐一杯饮品+土味文案（静态库，无表） | CoupleDining（CoupleDiningBank） |
| F217 | 外卖搭伙车 | 同一辆车各加菜品（品名+份数），凑齐喊「锁车下单」 | CoupleDining（couple_dine_cart，周锚可清空） |
| F218 | 饭桌话题卡 | 每日一道「吃饭聊这个」话题（静态库，无表），吃完可标记「聊过了」 | CoupleDining |
| F219 | 年度干饭账 | 今年吃过星评 top/踩雷数/菜单完成度 summary（无表） | CoupleDining |

## 批次十八：体温同步·作息与健康（F220-F229）— care「体温同步」组 `/api/couple/cozy`（V31）

| # | 功能 | 说明 | 落点 |
|---|------|------|------|
| F220 | 晚安同熄灯 | 双方都发晚安=当日「熄灯」，连击满 7 天推 both 里程碑 | CoupleCozy（couple_cozy_lightout） |
| F221 | 睡眠报告单 | 晨间各报昨夜自评（睡龄 1-5+一句梦话），互见 | CoupleCozy（couple_cozy_sleep） |
| F222 | 数羊房 | 60s 内双方各点满 10 下一起「数完一群羊」，看默契用时 | CoupleCozy（couple_cozy_sheep） |
| F223 | 喝水接力 | 我喝一杯=给 TA 的杯子加一格，对方 3h 未回应亮一句轻提醒 | CoupleCozy（couple_cozy_water） |
| F224 | 冷暖互报 | 自报城市+气温体感（纯文字），对方一键「叮嘱添衣」送达 | CoupleCozy（couple_cozy_weather） |
| F225 | 熬夜守护 | 23:30 后对方仍在互动时，可手动递一张「早点睡」陪伴卡（一天 1 张，幂等） | CoupleCozy（couple_cozy_latenight） |
| F226 | 周末慢生活 | 周五各提 1 件「什么都不赶」的小事，周日打卡回放 | CoupleCozy（couple_cozy_slow） |
| F227 | 疼痛对策本 | 各自登记「我胃疼时的正确做法」清单，对方不适日一键送达+关怀推送 | CoupleCozy（couple_cozy_remedy） |
| F228 | 抱抱计量器 | 见面拥抱自报计数，累计里程碑点亮（呼应 F60 求抱抱） | CoupleCozy（couple_cozy_hug） |
| F229 | 月度安眠小结 | 聚合熄灯连击/睡眠自评/数羊出「本月体温同步指数」（无表） | CoupleCozy |

## 批次十九：小日子·仪式感（F230-F239）— timeline「时光流」组 `/api/couple/ceremony`（V32）

| # | 功能 | 说明 | 落点 |
|---|------|------|------|
| F230 | 建国纪念日 | 自定义「我们的小日子」（名称/日期/每年重复），与官方纪念日区分 | CoupleCeremony（couple_ceremony_founded） |
| F231 | 节日老黄历 | 小日子+纪念日+倒数日统一倒数列表，今日宜/忌一句俏皮话（无表聚合） | CoupleCeremony |
| F232 | 过法任务卡 | 每个小日子写死 1-3 条「庆祝方式」（文字动作，如一起吃火锅） | CoupleCeremony（couple_ceremony_ritual） |
| F233 | 庆祝打卡 | 当日逐条打勾，隔日未齐补催「去年的今天你们…」 | CoupleCeremony（couple_ceremony_mark） |
| F234 | 爱情保险柜 | 每月交「保费」=互夸各 1 句；满 3/6/12 月 payout=一张愿望券 | CoupleCeremony（couple_ceremony_policy） |
| F235 | 续约仪式 | 每满 100 天/周年，双方重签一句「我还是选你」，攒续约长卷 | CoupleCeremony（couple_ceremony_renew） |
| F236 | 愿望券本 | 手动发/核销愿望券（OPEN→USED），券面文字自拟 | CoupleCeremony（couple_ceremony_coupon） |
| F237 | 小日子史册 | 按节日聚合历年庆祝记录与感言，一年一页 | CoupleCeremony（无表聚合） |
| F238 | 年度加冕 | 520/跨年生成「今年最热闹的三个小日子」（无表） | CoupleCeremony |
| F239 | 当日体感 | 仪式当天各留一句「此刻感觉」，次年今日对比展示（呼应 F192） | CoupleCeremony（couple_ceremony_recap） |

## 批次二十：我们公司（F240-F249）— shared「经营所」组 `/api/couple/board`（V33）

| # | 功能 | 说明 | 落点 |
|---|------|------|------|
| F240 | 头衔任命 | 各报两个在家的「职位」（财政部长/首席大厨…），对方点「任命」生效 | CoupleBoard（couple_board_role） |
| F241 | 董事会决议 | 大事提案→附议→通过/否决（一票否决），全程留痕 | CoupleBoard（couple_board_vote） |
| F242 | 年度股东大会 | 年末各交「本年述职+明年一个小目标」，双提交互见 | CoupleBoard（couple_board_report） |
| F243 | 升职公示栏 | 家务积分（F186）达档位自动「晋升」称号并公示（无表） | CoupleBoard |
| F244 | 发薪日 | 每月固定日各发一句「本月感谢工资」+5 积分入账（走 F186 台账） | CoupleBoard（couple_board_salary） |
| F245 | 金点子箱 | 经营 improvement 提案（一句话），被采纳即转 F241 决议 | CoupleBoard（couple_board_idea） |
| F246 | 请假交接 | 趣味「职务假期」：某项职责托管给 TA 一段时间，到期说声感谢交接回 | CoupleBoard（couple_board_leave） |
| F247 | 会议签到 | 决议表决日双方按键签到，双签到才开会（复用 10s 窗口模式） | CoupleBoard（couple_board_attend） |
| F248 | 公司名片 | 职位+积分职级+决议数+发薪日自动拼一张「我们公司」文字名片（无表） | CoupleBoard |
| F249 | 经营周报·公司版 | 本周决议/点子/晋升/积分 summary（无表，独立于 F189） | CoupleBoard |

## 里程碑与提交节奏

- 每批次四步提交：`db`（迁移+schema.sql 同批）→ `feat` 后端（Entity/Mapper/Service/Controller/Bank）→ `test` 后端 → `feat` 前端（组件+api+types+挂载）+ `test` 前端；skill 同步随各批 feat 一起提；wiki 若受影响随批更新对应页
- 批次十六特殊：F200-F208 纯前端重构一个 feat commit + test commit；F207 收藏另走 db→后端→前端全链路；F209 验收回归滚动执行，问题按 `fix(scope)` 独立小 commit，走查记录落 `docs/acceptance-v5.md`
- 版本节奏：F199 全落地后 1.4.0-rc；批次十六~十七完成 → 1.4.1-alpha；十八~十九 → 1.4.2-alpha；二十完成并验收通过 → 1.5.0-rc
- 测试基线：每批后端用例新增 ≥10、前端 ≥3；`mvn test` / `pnpm test` 全绿方可提交
