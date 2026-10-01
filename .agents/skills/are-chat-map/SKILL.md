---
name: are-chat-map
description: are-chat 后端项目地图（Spring Boot 4 + MyBatis-Plus + Flyway + MariaDB）。提供模块地图、情侣空间分层模板、数据层规范与交付门禁（第六节）。凡在 are-chat 中开发新功能、修复缺陷、评审改动，开工前必须先加载本 skill，避免重新通读项目。
whenToUse: are-chat 后端开工前加载；新增/删除模块、表、接口、定时任务后回来更新本文件
---

# are-chat 后端项目地图

> 本文件是给 AI agent 看的项目速查地图。维护义务见第六节「收尾」。

## 一、技术栈与运行

- Java 25 + Spring Boot 4.1.1（Web/MVC，无独立前台，会话用 `HttpSession`）
- ORM：MyBatis-Plus 3.5.17（BaseMapper 统一继承 `com.smart.chat.im.BaseMapperCompat`）
- 数据库：生产 MariaDB 10.11 / 测试 H2 MODE=MySQL；结构由 Flyway 管理（`spring.flyway.locations=classpath:db`，禁用 spring.sql.init）
- 鉴权：登录态在 HttpSession；`com.smart.chat.common.Sessions.requireUser(session)` 取当前用户名
- 统一返回：`ApiResponse.ok(data)` / 业务异常 `BusinessException(code, message)`
- 构建：`mvn -q compile`；测试 `mvn test`（何时必跑见第六节）

## 二、包结构（com.smart.chat）

| 包 | 职责 | 关键类 |
|---|---|---|
| `auth` | 注册审批/登录/管理员 | AppUserService, AdminService, AuthController |
| `im` | 好友/私信/在线状态/资料/推送 | FriendService, PrivateMessageService, ImPushService, PresenceService, UserProfile, BaseMapperCompat |
| `couple` | 情侣空间（核心业务，见下节） | CoupleService + 各功能 Service/Controller |
| `room` | 聊天室 WebSocket | ChatSessionRegistry, ChatWebSocketBridge |
| `system` | 系统配置/公告/审计 | Announcement 相关 |
| `upload` | 文件上传 | FileStorageService（本地存储） |
| `tools` | 健康检查/工具 | — |
| `notify` | 管理员推送 | AdminNotifyService |
| `common` | ApiResponse/BusinessException/Sessions | — |
| `config` | 配置类 | FileStorageProperties 等 |

推送机制（重要）：`ImPushService.pushCoupleEvent(event, actor, toUser, detail)` 给单人推 WS 事件（type=couple），`pushCoupleEventBoth(...)` 推双方；每次情侣事件推送同时落库 `couple_notify`（F41 通知中心，`CoupleNotifyRecorder` 启动时经 `ImPushService.setNotifySink` 挂接，im 包不反向依赖 couple 包）；`isOnline(username)` 查在线。

## 三、情侣空间模块全景（couple 包）

数据约定：所有表主键为 36 位 UUID 字符串；时间统一毫秒 bigint（字段名 `created`/`updated_at` 等）；用户名列 `utf8mb4_bin` 区分大小写。 CoupleSpace 双方固定为 `userA`/`userB`（字典序小者为 A），`partnerOf(me)` 取对方。

分层模式（新功能照抄）：
1. 实体：`@Data @TableName` + `@TableId(IdType.INPUT)` + 静态 `of()` 工厂 + 常量（如 STATUS_*, XXX_MAX）
2. Mapper：`@Mapper interface extends BaseMapperCompat<T>`，常用查询写成 default 方法
3. Service：构造注入（final 字段 + 构造器），VO 用嵌套 `record`，`requireSpace(me)` 取有效空间（无效抛 404）
4. Controller：`@RestController @RequestMapping("/api/couple/...")`，请求体用 record，每个方法一句 javadoc

现有 Controller 与路由前缀：
- `CoupleController` `/api/couple`：总览/邀请/纪念日/解除/约定/打卡/一问(+F48 互评 reactions)/清单/纪念日历/心情/时光轴/心动值/信箱/一问历史/条约/城市/基金/个性化(profile: 宣言/主题/贴纸)/恋爱状态徽章(relationship-of)
- `CoupleBondController` `/api/couple/bond`：贴贴动作（sendAction/动作流/统计/里程碑）、心情回应、专属爱称
- `CoupleRitualController` `/api/couple/ritual`：甜蜜任务卡、默契大考验、情话抽卡、恋爱运势、晚安故事
- `CoupleCareController` `/api/couple/care`：情绪天气预报、情绪急救箱、和好卡、夸夸墙、生理期关怀 + F60 求抱抱（看板/发出/话术卡/回应）+ F63 陪聊话题卡 + F64 情绪同步率
- `CoupleMakeupController` `/api/couple/makeup`：F61 矛盾复盘（双方各一份，齐了合成和好锦囊）+ F62 道歉券（每人同时 2 张有效，对方收下）
- `CoupleTalkController` `/api/couple/talk`：F66 真心话（空间+天稳定同题，双方必答+存档）、F67 匿名树洞（匿名投递/在途 1 个/回答后揭晓）、F68 心灵感应（题库选项作答，每天 3 轮，双答自动结算）、F69 情话储蓄罐（存入只发预告，21:00 利息送达）
- `CoupleGrowthController` `/api/couple/growth`：F70 双人挑战赛（空间+天稳定一题，双完成达成）、F71 恋爱存折（每天一笔小事+连续天数里程碑）、F72 百日之约（单活跃约定+每日打卡满 100 天自动达成+可中止）、F73 心愿互换（许愿/接单/实现）、F74 共读计划（各自报进度，双方到终点完结）、F75 旅行心愿地图（钉地点/打卡去过）、F76 追剧清单（共同集数，到总集数完结）、F77 星座配对（静态）、F78 恋爱词典（专属词汇）、F79 下次一定（登记/1h 冷却催办/兑现）
- `CoupleChronicleController` `/api/couple/chronicle`：F80 恋爱编年史（firsts/纪念日/胶囊/兑现约定/旅行打卡/真心话按年聚合倒序）、F81 考古卡（真心话/存折/语录/心情随笔随机挖卡，30 天前优先）、F82 恋爱问答机（真实数据出 2-3 道选择题：第一次日期/在一起日期/在一起天数，正确索引随题返回前端判分）、F85 周年报告（最近周年以来 6 项统计+情绪化 summary）、F86 生日回顾（TA 生日 MM-dd 的历史事件聚合，无生日 404）
- `CoupleKeepsakeController` `/api/couple/keepsake`：F83 甜蜜语录收藏册（收藏/场景/删）、F88 电影票根（片名/观看日/1-5 星缺省满分/感想/撕掉）、F89 我们的歌单（歌名/歌手/为什么/移除）；全部双方可整理、WS 事件 quote-kept/ticket-added/song-added
- `CoupleMemoryJob`：F87 每天 09:05 扫描当日到期 SEALED 胶囊 → pushCoupleEventBoth "capsule-due"（CoupleCapsuleMapper.findByOpenDay）
- `CoupleTodayController` `/api/couple/today`：F95 今日看点（挑战/真心话/心情/存折/百日打卡状态 + 最近到期胶囊聚合）、F96 年度热力日历（心情+存折+挑战+真心话+百日打卡按天计数分级 0-3，`/heatmap?year=`）
- `CoupleSurpriseJob.birthdayCards`：F92 在生日贺卡基础上增加生日前 3 天 "birthday-eve" 预告推送
- `CoupleMemoryController` `/api/couple/memory`：徽章墙（里程碑+成就）、那年今天、时光胶囊、倒数日、恋爱月报/数据总览、第一次清单（F46）
- `CoupleLifeController` `/api/couple/life`：甜蜜记账本、家务轮值、约会规划、双人习惯、暗号小本本
- `CoupleGameController` `/api/couple/game`：恋爱加成、互动热力图、心情曲线、恋爱红绿灯
- `CoupleSurpriseController` `/api/couple/surprise`：惊喜与期待——刮刮乐（周卡懒生成/刮开/核销）、恋爱盲盒（装盒/到日开箱）、心动闹钟（24h 内定时送达）、思念速递（5~30min 随机延迟）、藏宝图任务、告白重现（每年今天重播）
- `CoupleGardenController` `/api/couple/garden`：爱情花园（浇水养成 0-6 阶段/缺水会蔫/复活）、每日玫瑰（每人 3 朵+花语）、幸运签（每天为 TA 抽一支可覆盖）
- `CoupleNotifyController` `/api/couple/notify`：空间动态通知中心（F41 列表/全部已读）
- `CoupleAdminController` `/api/couple/admin`：情侣空间运营看板（F45 仅管理员）
- `ProfileController` `/api/profile`：资料卡含生日（F42 本人填写 + friends-birthdays 好友生日列表）
- `CoupleCommController` `/api/couple/comm`：F100-F109 沟通增强——安静小屋（冷静角）、情绪接力（双方接力续写）、比划猜词（静态词库）、故事接龙、道歉三部曲、心情词汇量
- `CoupleDistanceController` `/api/couple/distance`：F110-F119 异地恋——隔空牵手（doubleHold 同拍才算）、双城想念计量（双向奔赴）、作息重合表、下次见面信（写/拆信，事件 reunion-letter-*）、云约会清单、异地平安卡、见面日记、异地恋能量（30 天周期充能）、异地恋报告
- `CoupleSecureController` `/api/couple/secure`：F120-F129 确定感与安全感——安全感账户（存/收下）、恋爱体检（5 项聚合）、十年之约（凑齐推双方）、愿景板（同词共鸣）、承诺博物馆（双章展出）、信任存折（每日 1 币）、恋爱年轮（按年聚合）、纪念日大日子分类（CoupleService.AnniversaryCreateRequest.kind）、双人契约打卡、守护兽（惰性心情衰减）
- `CouplePlayController` `/api/couple/play`：F130-F139 趣味游戏——一百问（答一题解锁同题）、出题考TA（判分权在出题人，作答判分前对对方隐藏）、心动概率、今日塔罗、世界情话课（静态库今日一课+收藏）、周末盲选（周卡双方提交 stableHash 配对开奖）、情话Battle（OPEN→FULL→DONE 互投结算）、恋爱天气、抽象画（seed 前端生成 SVG）
- `CoupleDailyLifeController` `/api/couple/daily-life`：F140-F149 深度陪伴——今日主题曲、梦境手账、美食地图（WANT→EATEN 打卡评分）、TA 使用手册（TASTE/NOGO/FAV/QUIRK）、情绪 SOS（SENT→HELD 抱住接住，在途 1 条）、每日三问（双答触发 both 推送）、夸夸生成器+接头暗号（无表按日抽）、自定义成就（OPEN→ISSUED 颁发）、恋爱仪表盘（F149 聚合：待办=三问/SOS/想吃/挑战中成就，回忆=梦境/主题曲/暗号/SOS）
- `CoupleCoachController` `/api/couple/coach`：F150-F159 成长系——21天习惯搭子（couple_habit_streak，避开 V10 couple_habit；每日 1 打卡满目标自动 DONE）、感恩便签墙、情绪颗粒度日记（40 词 5 族每日 1 记可改）、每周高光互评（周一为周锚，双提名推 both）、共读一分钟（无表短文按日抽+每日感想）、拖延互助所（催办 1h 冷却、立事人宣布完成）、早安能量站（无表）、优点存折、成长年度关键词（聚合出「坚持力/感恩力/觉察力」）
- `CouplePoemController` `/api/couple/poem`：F160-F169 文字浪漫——情诗接龙（每人每天一句，今日执笔人按 space+day hash）、三行情书（对方点赞）、醒来第一条（睡前封存次日 deliver_day 送达+已读回执）、心情漂流瓶（FLOATING→REPLIED 对方回信）、数字密码情书（前端编码器，对方解码上报）、灵魂提问盲盒（24 问按日抽，双答才互见）、贴纸手账（每日 1 页可改+贴纸白名单）、恋爱语录机（模板填天数无表）、情书模板 8 封/贴纸库 16 枚（无表）
- `CoupleSparkController` `/api/couple/spark`：F170-F179 默契亲密——爱语测评（12 题静态卷 A/B 计分重测覆盖）与对照卡（双结果门槛+相处建议）、心动闪光速记、「如果」问答（20 问按日抽，双答互见，先答者为默契之星）、动作暗语本、同频共振（10s 窗口双方先后按键，差值<=500ms 算命中推 both）、默契仪表盘（按键 bestMs+双答天数+心动邮戳+暗语 加权 0-100）、心动日历（等级 1-3 钳制 upsert）、同频排行榜（bestMs 升序 top10）、默契周报（周一锚聚合）
- `CoupleManageController` `/api/couple/manage`：F180-F189 生活经营——家庭会议纪要（周一为周锚，议题→决议→关闭推 both）、本周主理人（按周一日号单双周轮换 userA/userB，非主理人排计划 403）、技能交换所（OPEN→TAKEN→DONE，不能自揭摊）、月度互评（星级 1-5 钳制，双评互见）、家庭应急卡（各填一份互见，至少一项）、情侣存档点（每月 upsert，感情温度 1-100 钳制）、家务积分市场（EARN 默认 5 分钳 1-200，兑换校验本人余额，奖励 6 项静态）、五年计划双轨（MINE/OURS，OURS 认领一人一半，只进不退）、纪念日策划案（未来 400 天窗口，IDEA→LOCKED→DONE 只进不退）、经营周报（周一锚聚合会议/积分/主理人无新表）
- `CoupleMuseumController` `/api/couple/museum`：F190-F199 时光博物馆——纪录片分镜（三幕缺一不可）、博物馆展品（文字展品档案+可选藏品日）、去年今日对比镜（按自然年聚合感恩/手账/闪光计数，无表）、银发情话机（12 句静态按 space+day 稳定抽，无表）、恋爱高频词（手账/便签/闪光/如果文本 CJK bigram 词云，停用词过滤，count>=2 top12，无表）、隐藏彩蛋成就（6 枚定义在 Bank，读时评估达标自动 insert+推 both，一人解锁全空间可见）、家规宪法（条款/修正案 RULE/AMENDMENT，提案人不能自签，对方签字推 both，重签幂等）、免打扰时段（每人一份 HH:mm 起止不可相同，covers 支持跨零点，前端弹窗过滤用）、首页问候引擎（6 时段模板+days together+quietNow，无表）、年度记忆书目录（当年 12 章计数，空月「空白页」占位，无表）
- `CouplePinController` `/api/couple/pin`：F207 常用收藏——GET 双方 pin 列表 / POST 全量覆盖（去空白去重、≤6 键、键长≤40，upsert couple_user_pin）
- `CoupleDiningController` `/api/couple/dining`：F210-F219 两个人的饭桌——今晚饭票（每人每天一票 upsert，撞菜推 both dine-hit）、吃什么裁决（当日票池去重按空间+日 stableHash，两人刷新结果一致）、吃过星评（1-5 钳制）、踩雷库（同名 400，谁提议谁划掉）、本周菜单（day 为周内日期，week=该日周一，留空擦格）、拿手菜（周 upsert 推 TA）、点单机（5 心情→饮品静态兜底，无表）、外卖搭伙车（双方各锁才 LOCKED 推 both，仅本人可删未锁菜）、饭桌话题卡（24 条按日 stableHash，标记幂等）、年度干饭账（星评 top5/踩雷数/票数/菜单数聚合，无新表）
- `CoupleCozyController` `/api/couple/cozy`：F220-F229 体温同步——晚安同熄灯（每人每天一次幂等，双方都点算当夜，连击恰满 7 推 both cozy-lightout-week，连击回看最多 60 天）、睡眠报告单（睡龄 1-5 钳制，当日本人可改不再推）、数羊房（60s 无按键重开一轮，满 10 下 done，后数完者推 both 用时对比）、喝水接力（一杯一格推 TA；partner 有喝且我 0 杯且 TA 最近一杯超 3h → nudge）、冷暖互报（城市必填当日可改；叮嘱添衣每天一条 cozy-advise 带 Bank 话术）、熬夜守护卡（一天一张幂等）、周末慢生活（周 upsert，双方都提 cozy-slow-planned、都打卡 cozy-slow-done 回放）、疼痛对策本（每人一本 for_user 唯一，comfort 一键执行送达 cozy-comfort）、抱抱计量器（自报 1-99 钳制，跨 10/50/100/520/1000 里程碑推 both）、月度安眠小结（bothLit 夜数×2 封顶 40+睡眠单×2 封顶 30+数羊×3 封顶 15+水杯÷2 封顶 15 = 体温同步指数≤100，无表）
- `CoupleCeremonyController` `/api/couple/ceremony`：F230-F239 小日子·仪式感——建国纪念日（自定义小日子 name≤60/startDay/repeatYear，删除连带过法卡与打卡）、节日老黄历（小日子+couple_anniversary+couple_countdown 统一倒数≤15 条，今日宜/忌 Bank stableHash，无表）、过法任务卡（每日子≤3 条超出 400，划掉连带打卡）、庆祝打卡（ritual+day 幂等打勾，单条推 TA ceremony-mark、当日该日子全部勾满推 both ceremony-all-done）、爱情保险柜（每人每月夸一句可改写，双方齐=当月保费；满 3/6/12 月按 ref policy-N 幂等 payout 愿望券推 both）、续约仪式（锚点=space.anniversary 或 created，距锚点整 100 天或恰逢周年当天才可签，每人每锚一句可改，双方签齐推 both；非续约日 400 带剩余天数）、愿望券本（手动发券推 TA、OPEN→USED 核销推 TA、已核销再核销 400）、小日子史册（按届一年一页聚合打卡与体感，无表）、年度加冕（仅 0520/1231/0101 当天出 crown：当年打卡数 top3 小日子，0101 统计去年，无表）、当日体感（每人每天一句可改，双齐推 both、先写推 TA）；overview 一次聚合全部
- `CoupleAlmanacController` `/api/couple/almanac`：F250-F259 夫妻老黄历（批次二十一）——节气跟风（termOfToday 近似±1天窗口，放空日挡打卡，双跟风推 both term-check-both）、节气过法（每节气≤2 条 400，当日打卡 term-ritual-done 推 TA、全勾推 both term-ritual-all）、择吉日（未来日+大事唯一，Bank 宜忌点评，发起推 TA、对方盖章 both term-lucky-confirmed、不能自盖章）、农历生日换算（couple_anniversary 加 calendar_type/lunar_md 列，CoupleTermBank 标准 1900-2100 压缩表 lunarToSolar/solarToLunar，总览给未来换算日）、节日家档（8 节日键 NEWYEAR/CHUXI/VALENTINE/L520/QIXI/MIDAUTUMN/NATIONAL/ANNIVM，一人一年一案 upsert，双案 both term-festival-both）、节气手账（24 节气一人一笔 upsert 不重推）、生肖年运（无表，读 UserProfile.birthday 年定生肖+stableHash 抽年运）、长假愿望（内置法定假 nextHoliday 60 天窗口，首写推 TA、补写 both term-wish-append）、反仪式感日（每年≤3 天，当日挡跟风/过法打卡 400 带「偷得浮生」卡）、一年日子小结（无表，/yearly 聚合跟风/手账/过法/吉日/家档/放空计数+节气长卷）；GET /today 聚合，写接口全部返回整份 TodayVO
- `CoupleBoardController` `/api/couple/board`：F240-F249 我们公司——头衔任命（给 TA 封职位，本人待任命最多 2 个超出 400；被任命者本人盖章 board-appointed，非本人 400「任命章要本人盖」）、董事会决议（提案推 TA；仅非提案人可裁「自己的议案不能自己裁」，PENDING→PASSED/VETOED 一票否决留痕 decided_at/veto_by，双推 board-passed/board-vetoed）、年度股东大会（year 默认当年、4 位数字校验，review≤500/goal≤200，同年 upsert 可改不重推；对方述职仅双提交后可见）、升职公示栏（无表：按 couple_point_ledger EARN 累计定档 实习生0/正式职员20/小组主管60/部门经理150/公司总监300/合伙人600）、发薪日（一人一月一次重复 400，thanks≤200，发薪即向台账插 EARN「发薪日感谢工资」5 分；双发推 both）、金点子箱（content≤140；对方才能采纳「自己的点子要对方来采纳」，采纳生成 PENDING 决议并回填 vote_id 推 both）、会议签到（每人每天一行，10s 窗口内双签置双方 convened=1 推 both board-convened，已开会不再更新）、公司名片（无表拼文本：两人职衔+通过/否决数+当月最早发薪日+Bank 收尾话术）、公司周报（周一锚毫秒过滤本周议案/点子/赚分）；overview 一次聚合全部

内容库（静态，只增不改顺序）：
- `CoupleQuestions`：今日一问题库（105 题 11 主题，按 epochDay 轮换）
- `CoupleRitualBank`：甜蜜任务/默契题/情话/运势/晚安故事库 + `stableHash`（FNV-1a，按天+空间稳定取值）
- `CoupleSurpriseBank`：惊喜内容库（刮刮乐券面 24 种/盲盒任务灵感 16 条/花语 8 种/幸运签 20 支）+ stableHash 按周稳定抽券
- `CoupleTalkBank`：懂我与被接住内容库（安慰话术卡按感受 5 类×5 条/陪聊话题 20 条/真心话题 30 道/心灵感应选题 24 道+选项/深夜关怀文案）+ stableHash 按天稳定取真心话题
- `CoupleGrowthBank`：共同养成内容库（每日挑战 40 条按空间+天稳定一题/存款里程碑 5 档/星座 12 座元素相性+配对评语 12 条）+ stableHash
- `CoupleCommBank`：沟通增强内容库（比划猜词/故事接龙开头/情绪词汇）
- `CouplePlayBank`：趣味游戏内容库（一百问题库 100 题/塔罗大阿尔卡那 22 张/恋爱天气 5 种/世界情话课 16 课/心动概率文案 5 档）+ stableHash 按天稳定
- `CoupleDailyLifeBank`：深度陪伴内容库（主题曲 30 首/夸夸 100 条/接头暗号 20 句）+ stableHash 按天稳定（夸夸取 3 条 hash>>16 分段）
- `CoupleCoachBank`：成长系内容库（情绪词 5 族 40 词/原创共读短文 20 段/早安能量 20 组）+ stableHash 按天稳定取段落与早安
- `CouplePoemBank`：文字浪漫内容库（灵魂 24 问/语录模板 8 条/情书模板 8 封/手账贴纸 16 枚）+ stableHash 按天稳定取灵魂一问
- `CoupleSparkBank`：默契亲密内容库（五爱语档案+12 题测评卷/「如果」脑洞题 20 问）+ stableHash 按天稳定取「如果」一题
- `CoupleManageBank`：生活经营内容库（家务积分兑换奖励 6 项：电影之夜选片权/免洗碗金牌/周末爱心早餐/游戏不限时/二十分钟抱抱/任意愿望卡）
- `CoupleMuseumBank`：时光博物馆内容库（银发情话 12 句按天稳定抽/隐藏成就定义 6 枚 THANKS_10·FLASH_5·JOURNAL_7·SIGNAL_3·WHATIF_10·SYNC_HIT_1/首页问候 6 时段模板）
- `CoupleTermBank`：夫妻老黄历内容库（24 节气近似公历表 termOfToday/nextTerm、标准 1900-2100 农历压缩表 lunarToSolar/solarToLunar/springFestival/zodiac、8 节日键与当年公历日 festivalOn、内置法定假 nextHoliday 60 天窗口、择吉日宜/忌各 12 条点评、跟风晒话术 5 条、年运 12 条、放空日挡打卡卡 3 条）

定时任务 `CoupleReminderJob`（Asia/Shanghai）：09:00 约定逾期提醒；09:30 纪念日倒数（7/1/0 天）；09:45 倒数日提醒（7/3/1/0 天）；10:00 情绪急救箱（连续 2 天低落提醒对方）。
定时任务 `CoupleSurpriseJob`（Asia/Shanghai）：每分钟送达心动闹钟与思念速递（alarm-fired/miss-delivered）；09:15 告白重现；09:20 生日彩蛋（读 im 包 UserProfile.birthday）；10:15 花园缺水巡检（garden-withered）。
定时任务 `CoupleCareTalkJob`（Asia/Shanghai）：21:00 情话储蓄罐利息（每人随机取一句未投递情话送达，love-bank-interest）；23:00 深夜陪伴（当天负面心情且未被求抱抱接住时提醒对方，night-care）。

## 四、数据层规范（硬性）

- 凡改表结构或初始化数据，必须产出 Flyway 增量脚本并同步 schema.sql——触发条件、命名、幂等/双兼容写法、种子数据等完整规范见 `.agents/skills/db-migration/SKILL.md`
- 现有迁移：V1 couple 基础表 → V2 存量基线 → V3 心情 → V4 信箱 → V5 条约/城市/基金 → V6 贴贴动作/心情回应/爱称 → V7 任务卡/默契 → V8 和好卡/夸夸/生理期 → V9 胶囊/倒数日 → V10 记账/家务/约会/习惯/暗号 → V11 空间个性化 → V12 私信心动时刻 → V13 通知中心/生日 → V14 第一次清单/一问互评 → V15 惊喜与期待（刮刮乐/盲盒/闹钟/思念/花园/玫瑰/幸运签/告白/藏宝图）→ V16 懂我与被接住（求抱抱/矛盾复盘/道歉券/真心话/树洞/心灵感应/情话储蓄罐）→ V17 共同养成（挑战赛/恋爱存折/百日之约/心愿互换/共读/旅行心愿/追剧/词典/下次一定）→ V18 回忆资产（语录册/电影票根/我们的歌单；F80-F82/F85-F87 为现有数据聚合与放宽常量，无新表）→ V19 沟通增强（安静小屋/情绪接力/比划猜/故事接龙/道歉三部曲/心情词汇）→ V20 异地恋（牵手/想念/作息/见面信/云约会/平安卡/见面日记）→ V21 确定感（安全感账户/十年之约/愿景板/承诺博物馆/信任存折/双人契约/守护兽 + couple_anniversary.kind 列）→ V22 趣味游戏（一百问/出题考TA/情话课/周末盲选/情话Battle/参赛句子/抽象画廊）→ V23 深度陪伴（梦境/美食地图/TA手册/情绪SOS/每日三问/自定义成就）→ V24 成长系（习惯搭子 couple_habit_streak/感恩便签/情绪颗粒度/每周高光/共读一分钟/拖延互助/优点存折）→ V25 文字浪漫（情诗接龙/三行情书/醒来第一条/漂流瓶/密码情书/灵魂提问/贴纸手账）→ V26 默契亲密（爱语测评/心动闪光/如果问答/动作暗语/同频共振/心动日历）→ V27 生活经营（家庭会议/主理人/技能交换/月度互评/应急卡/存档点/家务积分/五年计划/策划案）→ V28 时光博物馆（纪录片分镜/博物馆展品/隐藏成就/家规宪法/免打扰设置）→ V29 常用收藏（couple_user_pin 每人一行 pins 逗号分隔） → V30 两个人的饭桌（饭票/星评/踩雷/本周菜单/拿手菜/搭伙车/话题标记 7 表） → V31 体温同步（熄灯/睡眠单/数羊/喝水/冷暖/熬夜卡/慢生活/对策本/抱抱 9 表） → V32 小日子仪式感（建国纪念日/过法卡/打卡/保险柜保费/续约签字/愿望券/当日体感 7 表） → V33 我们公司（头衔/决议/述职/发薪/金点子/签到 6 表） → V34 夫妻老黄历（节气跟风/过法/吉日/节日家档/手账/长假愿望/放空日 7 表 + couple_anniversary 加 calendar_type/lunar_md 列）
- H2 兼容注意：`CHARACTER SET utf8mb4 COLLATE utf8mb4_bin` 列必须 `NOT NULL`，可空用户名列用普通 `varchar(50) DEFAULT NULL`（V17/V22 踩过坑）

## 五、测试

- 测试在 `src/test/java`（H2 自动配置），已有 auth/im/room/upload 各模块测试；新 Service 的核心算法（判定/统计/轮换）建议补单测
- 测试资源：`src/test/resources/application.yml`

## 六、交付门禁（硬性流程，给 agent 的快速上手路径）

规范全集在各专项 skill 里（db-migration / lombok-data / git-commit），本节只做流程串联与红线登记，不复述细节：

1. **开工**：必读本 skill；任务触及表结构/初始化数据 → db-migration；新建 Java 数据类 → lombok-data；提交推送 → git-commit
2. **编码**：照抄第三节分层模板（加"XX卡"类功能可参考 CoupleLetter/CoupleCapsule 全链路）；静态内容库只增不改顺序（第四节）；用户可见文案要可爱、口语化、带 emoji（模仿现有推送文案）；新增 WS 事件须同步前端 `stores/couple.ts` 注册 case + api/types（见 are-chat-web-map skill）
3. **自检**：git-commit skill 的「架构师 Code Review 五项」清单全过
4. **构建**：提交前必跑 `mvn -q compile`；改了表跑 `mvn test` 验证 Flyway 脚本 H2 可执行；涉测试改动 `mvn test` 全绿
5. **提交**：按改动性质分组，一类一 commit；数据库脚本（V*.sql+schema.sql）永远独立成 commit；信息 `type(scope): 中文描述`
6. **推送**：commit → `git pull --no-rebase` → push；失败保留本地 commit 并报告，不 force push
7. **收尾**：新增/删除模块、表、接口、定时任务 → 更新本 skill 对应小节，与功能同批提交（commit type `docs`）

**产品红线（用户长期约束，各批次均适用）**：情侣空间功能注重情绪价值；**不做照片/视频上传类功能**（服务器部署要求高）；迁移脚本必须幂等且 H2/MariaDB 双兼容（见第四节）。
