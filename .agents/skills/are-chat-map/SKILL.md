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
- 规模快照（v7 批次三十五后）：57 个 Controller / 793 个映射方法 / 310 张表（`couple_*` 297，V1-V48）/ 608 用例基线 / 情侣 WS 事件约 472 个；逐端点与逐表清单见 `wiki/api.md`、`wiki/database.md`

### 目录与关键文件

```
are-chat/                             # 单模块 Maven（无多 module），坐标见 pom.xml
├── pom.xml                           # Java 25 / Spring Boot 4.1.1 / MyBatis-Plus 3.5.17 / Flyway
├── src/main/java/com/smart/chat/     # 11 个包，见第二节
│   ├── SmartChatApplication.java     # 主启动类
│   └── ...
├── src/main/resources/
│   ├── application.yml               # 端口 8080、数据源、Flyway、上传 20MB 限制（改配置先读这里的注释）
│   ├── application-mysql.yml         # MySQL 变体数据源
│   ├── db/V*.sql                     # Flyway 增量脚本 = 运行时唯一建表路径（当前链至 V48）
│   └── schema.sql                    # 全量结构文档（基线 187 表；不被运行时执行，改表必须同步）
├── src/test/java/                    # Service 单测（Mockito）+ 少量 SpringBootTest 集成（H2 跑 Flyway）
├── src/test/resources/application.yml# 测试库 H2 MODE=MySQL
├── wiki/                             # Repo wiki：Home/architecture/modules/couple-space/database/api/scheduled-jobs/dev-guide
├── docs/                             # couple-features-v1~v6.md 功能规格 + acceptance-v5/v6.md 验收留痕
├── deploy/  Dockerfile  are-chat-1.0.0.tar   # 私有化部署产物与脚本
├── uploads/                          # 本地文件存储根（FileStorageProperties.base-dir=./uploads，随 cwd）
└── AGENTS.md                         # 仓库级 agent 约束（优先级高于个人记忆，如绿后必推）
```

### 端口与联调速查

| 项 | 值/说明 |
|---|---|
| 后端 HTTP | `server.port=8080`；REST 统一前缀 `/api`；健康检查 `/api/health`；HTTPS 由 nginx 终结，`forward-headers-strategy=framework` 使 Cookie 自动带 Secure |
| WebSocket 聊天室 | `@ServerEndpoint("/ws/chat/{name}")`（room/ChatEndpoint；@ServerEndpoint 实例由容器创建，经 ChatWebSocketBridge 桥接静态 Spring 引用） |
| 前端 dev | 5173；vite 代理 `/api`→8080、`/ws`→8080（ws:true） |
| 开发库（默认） | MariaDB `117.72.73.149:3307/smart_collections`；`DB_CONNECT_URL/DB_CONNECT_USER/DB_CONNECT_PASSWORD/DB_CONNECT_DRIVER` 环境变量可整体覆盖（真实口令不落仓库文档） |
| 测试库 | H2 内存 `MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE`，与生产跑同一批 V 脚本 |
| 登录态 | HttpSession Cookie；后端 `Sessions.requireUser(session)`，除 `/api/auth/**`、`/api/health` 外全部需会话 |
| Flyway | `baseline-on-migrate` + `baseline-version=0`：存量老库自动打 0 基线后从 V1 全量执行——所以每个 V 脚本都必须幂等；`clean-disabled=true` |

### 请求与推送链路（一图）

```
Vue 组件 → api/<域>Api → http.ts(get/postJson/putJson/delete，withCredentials)
  → Controller(/api/**) → Sessions.requireUser → Service(requireSpace / partnerOf)
  → Mapper(BaseMapperCompat default 方法 + LambdaQueryWrapper) → MariaDB(生产)/H2(测试)
Service → ImPushService.pushCoupleEvent(Both) ─┬→ WS 帧 {type:'couple', event, detail}
                                                └→ couple_notify 落库（CoupleNotifyRecorder，F41 通知中心）
前端 im store 收 WS → 派发 `arechat:couple` 自定义事件 → couple store handleCoupleEvent 按 event 刷新 + 通知铃铛
```

### 命令速查

- 编译门禁：`mvn -q compile`（提交前必跑）
- 全量测试：`mvn test`（当前基线 409 用例）；单类：`mvn test -Dtest=CoupleListenServiceTest`
- 运行：`mvn spring-boot:run`（8080）；打包 `mvn -q -B package` 后按 Dockerfile/deploy 部署
- 数表：`grep -c "^CREATE TABLE" src/main/resources/schema.sql`

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
- `CoupleDiningController` `/api/couple/dining`：F210-F219 两个人的饭桌——今晚饭票（每人每天一票 upsert，撞菜推 both dine-hit；**TicketVO.redlines 只读 couple_body_redline 拼标 F316 忌口项，只标不拦**）、吃什么裁决（当日票池去重按空间+日 stableHash，两人刷新结果一致）、吃过星评（1-5 钳制）、踩雷库（同名 400，谁提议谁划掉）、本周菜单（day 为周内日期，week=该日周一，留空擦格）、拿手菜（周 upsert 推 TA）、点单机（5 心情→饮品静态兜底，无表）、外卖搭伙车（双方各锁才 LOCKED 推 both，仅本人可删未锁菜）、饭桌话题卡（24 条按日 stableHash，标记幂等）、年度干饭账（星评 top5/踩雷数/票数/菜单数聚合，无新表）
- `CoupleCozyController` `/api/couple/cozy`：F220-F229 体温同步——晚安同熄灯（每人每天一次幂等，双方都点算当夜，连击恰满 7 推 both cozy-lightout-week，连击回看最多 60 天）、睡眠报告单（睡龄 1-5 钳制，当日本人可改不再推）、数羊房（60s 无按键重开一轮，满 10 下 done，后数完者推 both 用时对比）、喝水接力（一杯一格推 TA；partner 有喝且我 0 杯且 TA 最近一杯超 3h → nudge）、冷暖互报（城市必填当日可改；叮嘱添衣每天一条 cozy-advise 带 Bank 话术）、熬夜守护卡（一天一张幂等）、周末慢生活（周 upsert，双方都提 cozy-slow-planned、都打卡 cozy-slow-done 回放）、疼痛对策本（每人一本 for_user 唯一，comfort 一键执行送达 cozy-comfort）、抱抱计量器（自报 1-99 钳制，跨 10/50/100/520/1000 里程碑推 both）、月度安眠小结（bothLit 夜数×2 封顶 40+睡眠单×2 封顶 30+数羊×3 封顶 15+水杯÷2 封顶 15 = 体温同步指数≤100，无表）
- `CoupleCeremonyController` `/api/couple/ceremony`：F230-F239 小日子·仪式感——建国纪念日（自定义小日子 name≤60/startDay/repeatYear，删除连带过法卡与打卡）、节日老黄历（小日子+couple_anniversary+couple_countdown 统一倒数≤15 条，今日宜/忌 Bank stableHash，无表）、过法任务卡（每日子≤3 条超出 400，划掉连带打卡）、庆祝打卡（ritual+day 幂等打勾，单条推 TA ceremony-mark、当日该日子全部勾满推 both ceremony-all-done）、爱情保险柜（每人每月夸一句可改写，双方齐=当月保费；满 3/6/12 月按 ref policy-N 幂等 payout 愿望券推 both）、续约仪式（锚点=space.anniversary 或 created，距锚点整 100 天或恰逢周年当天才可签，每人每锚一句可改，双方签齐推 both；非续约日 400 带剩余天数）、愿望券本（手动发券推 TA、OPEN→USED 核销推 TA、已核销再核销 400）、小日子史册（按届一年一页聚合打卡与体感，无表）、年度加冕（仅 0520/1231/0101 当天出 crown：当年打卡数 top3 小日子，0101 统计去年，无表）、当日体感（每人每天一句可改，双齐推 both、先写推 TA）；overview 一次聚合全部
- `CoupleAlmanacController` `/api/couple/almanac`：F250-F259 夫妻老黄历（批次二十一）——节气跟风（termOfToday 近似±1天窗口，放空日挡打卡，双跟风推 both term-check-both）、节气过法（每节气≤2 条 400，当日打卡 term-ritual-done 推 TA、全勾推 both term-ritual-all）、择吉日（未来日+大事唯一，Bank 宜忌点评，发起推 TA、对方盖章 both term-lucky-confirmed、不能自盖章）、农历生日换算（couple_anniversary 加 calendar_type/lunar_md 列，CoupleTermBank 标准 1900-2100 压缩表 lunarToSolar/solarToLunar，总览给未来换算日）、节日家档（8 节日键 NEWYEAR/CHUXI/VALENTINE/L520/QIXI/MIDAUTUMN/NATIONAL/ANNIVM，一人一年一案 upsert，双案 both term-festival-both）、节气手账（24 节气一人一笔 upsert 不重推）、生肖年运（无表，读 UserProfile.birthday 年定生肖+stableHash 抽年运）、长假愿望（内置法定假 nextHoliday 60 天窗口，首写推 TA、补写 both term-wish-append）、反仪式感日（每年≤3 天，当日挡跟风/过法打卡 400 带「偷得浮生」卡）、一年日子小结（无表，/yearly 聚合跟风/手账/过法/吉日/家档/放空计数+节气长卷）；GET /today 聚合，写接口全部返回整份 TodayVO
- `CoupleListenController` `/api/couple/listen`：F260-F269 倾听与发声（批次二十二）——想被听时段（申请/对方确认/聊完/1-5 双评齐推 both slot-rated，在途仅 1 个，说的人不能自确认）、替我说（一人一份在途草稿可覆盖，被代笔人定稿 ADOPTED 推 both，不能自定稿）、误会倒带（同日同主题双方各写「我当时以为/我猜你其实想」，双份齐推 misrewind-done，改写不重推）、卡壳一问（周一锚一周一题，只能答对方的题，双答齐推 both stuck-both）、换位信（以对方口吻写信封存 openDay>写信日，到日由对方拆、作者拆自己 400，同日双封推 both swap-letter-pair）、早想说队列（每 7 天一放行，today() 读时惰性结算每日最多 SENT 一句推 TA hold-sent）、三行打卡（印象/谢/夸各≤80 一人一天一条可改写，连续恰跨 21 天推 both three-line-21）、语气翻译官（TIRED/BUSY/SAD/OKAY 四选一当日可改，新报推 TA tone-marked）、休战旗（默认 30 分钟钳 10-120，在途仅一面，到点双方各表态继续/算了，双决定收旗推 both truce-resumed/truce-off，超 24h 未齐自动收旗）、称呼日（Bank 日抛爱称按 space+day 稳定抽，双方各「用过了」推 both nameday-hit）；GET /today 聚合，写接口返回整份 TodayVO
- `CouplePostController` `/api/couple/post`：F290-F299 明日邮局（批次二十五）——五年后新年卡（每人每年一张 SEALED，deliver_day=五年后元旦，读时惰性放行推 both post-oath-opened，SENT 后 400 改写）、人生大事进度（name 唯一+拆步≤12+TA 可补进展章，全步完成推 both post-bucket-done，发起人独可放弃留档 GONE）、总得有一天拍卖（上拍在途≤5 对、他人 7 天内认领+未来排期，自接 400，逾期读时 EXPIRED 推 TA，TAKEN 才能完成）、想象中的家（yyyy 版本年本人 upsert 新建推 TA，四字段各≤100）、退休计划（30/40/50 档双写，双齐推 both）、许愿井周问（周一锚 52 周题卷按周序轮换，双答推 both，当年列表回看）、时光胶囊接龙（给对方 1/2/3 年三档，本人在途≤3，到点仅收信人可拆且拆最早笔，自拆 400）、解梦局（一人一天一梦，对方点评一案一断，做梦人盖「灵/胡说」章，未点评不能盖章）、周年愿望台账（yyyy 一年一愿可改，往年中奖者本人盖 KEPT/PIGEON，当年未到期 400，盖章后锁改）、未来信用卡（立旗 due_day 必须未来，本人兑现推 both，读时逾期结算 BROKEN 降额，额度六档梗 CouplePostBank.creditTier）；GET /box 聚合，写接口返回整份 PostVO
- `CoupleTheaterController` `/api/couple/theater`：F300-F309 扮演剧场（批次二十六）——今日身份签（空间+日 stableHash 从 14 个身份池抽「身份+相处指南」，日终各打 1-5 演技分，双打分推 both theater-role-rated，本人重复打分不覆盖）、一日互换日记（每人每天一页「作为 TA」≤300 字，当日本人可改写不重推，双齐推 both theater-diary-both 且互见对方那页）、师徒日（周锚周一，stableHash 定师父，徒每日侍奉打卡（同日幂等、一周≤7），师父独可评语定级：GRADUATED 需满 3 次侍奉、每周只定一次，推 both theater-master-grade）、时空电话亭（FUTURE 一年后 open_day 封存、PAST 当场接通；读时 settle 到点置 SENT 推 both theater-booth-connected + Bank「信号不好」彩蛋话术）、黑话大全（term 同空间唯一 40 字，释义/出处各≤200；抽查只能考对方「自己收的梗不能考自己」，作答覆盖会把 judged 清空，收录人才能判卷 RIGHT/WRONG 推 both）、每日奥斯卡（uk(space,day,from_user)，提名对象固定为对方，本人当日可改不重推）、如果我是你爸妈（20 题家长卷按 space+day stableHash 存卷，双答推 both theater-family-both，未双答只见自己）、双角色追剧（uk(space,work,from_user) 各认领一角，日记逐条追加≤200、总量 600 超限 400，双方都剧终才合成剧本推 both theater-movie-script 并放出 TA 的角色日记）、今日客服（下单→非下单人才能接单（30 分钟内 on_time=1）→顾客评分 1-5→仅 1-2 星可由客服申诉一次，状态机 OPEN/ANSWERED/RATED/APPEALED）；F309 冷知识颁奖礼无表读时聚合（提名数/词条数/抽查数/答对数/双页日记天数/准时单数）；GET /today 聚合，写接口返回整份 TheaterVO
- `CoupleBodyController` `/api/couple/body`：F310-F319 身体通知系统（批次二十七）——体征互报（uk(space,day,user)，体温/体重/睡眠文本＋自设体温线/睡眠下限，**只按本人自设线判异常不做医学判断**，首报超线推 TA body-metric-alert、改写不重推、全空 400）、呼噜自报（uk(space,day) 双人各一档 NONE/TINY/MID/HEAVY 存 level_a/level_b，对方补 shake_a/shake_b 震感点评，Bank 按档位出「你家打雷了吗」话术）、周期共览（uk(space,day,user) 本人标 BEFORE/MENSTRUATING/AFTER/OWULARE+不适，TA 只能给对方那天递照顾卡「TA 那天没标，卡递过去也没人接」）、戒烟戒糖互助营（uk(space,owner,name) 营期 7-100 天，破戒按日幂等 CSV 累加，安慰词只能由陪绑方说「自己夸不算」，本人结营：满天数 DONE／提前 GONE，营龄逢 7 的倍数出里程碑话术）、运动链（uk(space,day,kind) 五项 PUSHUP/SQUAT/PLANK/RUN/STRETCH，两人各报计数，**30 分钟窗口内双报才算接上链**推 both body-fit-link，已接上后改数不再重推）、不适 SOS（在途一条，自己接不住自己，对方从 Bank 五张「我能做」选项卡挑一句回执 body-sos-held）、忌口红线本（uk(space,item) 分 ALLERGY/AVOID，谁登记谁才能划；读 couple_dine_ticket 当日饭票做子串撞标 → 总览给「N 票含『香菜』，做之前先撤了它」）、体检陪同（uk(space,day,owner) PLAN→对方虚拟陪同到场（自己到不算，幂等不再重推）→本人写检后一句话 REPORTED 推 both）、情绪药友（uk(space,week,user) 自愿三档 STEADY/HARD/NONE 按周 upsert，对方可回陪伴话术，**全链路非医嘱口径**）、早睡军令状（uk(space,week) 各签自己 HH:mm 熄灯线，双签齐推 both body-oath-sign；违约率读 couple_cozy_lightout 本周 at_time 与线字符串比对，读时算不入库）；GET /overview 聚合，写接口返回整份 BodyVO
- `CoupleRepairController` `/api/couple/repair`：F320-F329 修复车间（批次二十八）——冷冻解冻规程（uk(space,start_day) 3-24h 自选、全局仅一单在冻，**未到 until_at 不能签**「签了也不算数」，三问由挂冷冻的人答、双签+三问齐才 THAWED 并给两人各掉一只修复礼盒，**同时向答完三问的冷冻提出人记一笔心动台账 EARN「复温成功：三问答完了」8 分**（复温是全套里最难的一步，要给回报）、道歉质检（六要素 FACT/FEEL/BLAME/SORRY/FIX/ASK 自评≥3 项，只有对方能验货，打回必填一句差在哪，PASSED 进陈列室推 both、BACK 只能本人重写）、重来卡（uk(space,quarter) 每季一张，领卡→重放记一句改说了什么→打满意度 1-5 且全季只打一次）、信任重建（uk(space,name) 档位 14/30/60，任务卡≤10 条各≤60 字，signed_days 存 `yyyy-MM-dd:A|B` 记号、**双人才算一天签到**，签满自动 DONE，任一人可写周复盘，开计划人独可中止），和好了倒计时（uk(space,day) 10-60 分钟，**暂停/继续权只在对方**，读时 settle 到点自动 OFFERED 递 Bank 台阶卡（暂停中不递），宣布和好掉礼盒）、冲突类型年报（无表，按当年聚合冷冻/解冻/验货/认错/感动/礼盒/纪念碑 + 平均冷冻时长，出 Bank 称号与 summary）、底线声明卡（uk(space,user,slot) 每人 3 格≤60 字，首立推 TA、改写不重推；踩线只能由踩的那个人补红线记录，计数累加）、我错了榜（uk(space,day,user) 一天一次防刷、「最感人」只能被认错方标，自己给自己发奖 400）、修复礼盒（解冻/和好掉落，任务本人完成才推 both）、和平纪念碑（uk(space,day,user) 一天一句，本人可补「现在回看」注解不重推）；GET /workshop 聚合，写接口返回整份 RepairVO
- `CoupleWorldController` `/api/couple/world`：F330-F339 两家与朋友（批次二十九）——拜访攻略（uk(space,day,from_user) 前置任务卡≤8 条各≤60 字、host_side 只分 MINE/YOURS，**双确认必须对方点**「自己写的攻略自己确认不算」，写攻略的人交战报置 DONE 推 both）、送礼互助池（idea 同空间唯一，含预算与雷点，只能接对方的单、只有接单人能宣布买好推 both）、朋友视角问卷（uk(space,slot) 三题按空间稳定从 Bank 取，逐题线下问友回填，三题齐了才出「他观卡」话术）、官宣日（uk(space,month) 一月一张纯文字卡，推 both 成官宣编年）、文案代写（uk(space,day,user,slot) 每人每日三候选，同格改写不重推，**选稿权只在求稿的对方**，定稿清其余候选并推 both，**同时把定稿写成百科词条「定稿文案 · 日期」（F334「定稿进百科」落地，term 查重幂等）**、进城接待方案（uk(space,city) 手册 upsert，行程≤8 条必填、小包清单≤12 项可留空）、亲戚称呼册（uk(space,term) 出题人不能自答，答案去空格精确比对，错题 wrong_count 累加并记 last_wrong_day 进「考前强化」，列表按错题数倒序）、社会信用（uk(space,owner,content) 保证到期日必须未来；对方见证 witnessed；**到期且已见证 → 读时惰性结算 KEPT 推 both，并向立保证人记心动台账 EARN「说到做到：…」10 分**，未见证的到期不自动解、只能由对方举报塌房 BROKEN+一句事实），群聊记者（uk(space,day) 双人各一条 line_a/line_b，本人可改写不重推，「笑了对方那条」双人都笑推 both world-group-both）、代 TA 赔礼（写给 TA 亲友的信 OPEN→对方审阅：通过 SENT 推 both / 打回 BACK 必填改哪儿→本人重写回到 OPEN 并清空审阅，只有信主能改）；GET /world 聚合（含 packTemplate/他观三题恒定三行），写接口返回整份 WorldVO
- `CoupleLegacyController` `/api/couple/legacy`：F340-F349 传世系统（批次三十，v6 收官批）——年度十问（uk(space,year,user)，**十答用换行符存 answers 列（1500 宽刚好 10×140+9）**，逐格填写、本人可改写该格，答满 10 格才推 TA legacy-ten-done；总览恒定下发今年+去年两期供跨年 diff）、记忆库年审（uk(space,year,user)，最想留/最想删各 ≤3 条各 ≤60 字，同年本人改卷不增行，读回忆资产口径只做意见不真删）、续约发布会（uk(space,year,user) 发言稿 ≤600，**重发即作废对方评分**；评分卡 1-5 只能由对方打且一年一次，Bank 按分档出话术）、恋爱汇率（uk(space,user) 1 亲亲=1-20 抱抱、1 抱抱=1-20 句夸夸，**两人都报过才允许年末结算**，`settled_year` 同年只结一次）、情侣品牌（uk(space) 单行，拟名 ≤30/slogan ≤60/简介 ≤300，**发布权在对方确认**，任何改动把 published 清零重走确认）、我们的一年（uk(space,year) 一键组文，正文数字全部来自真实表：台账笔数/十问答数/年审份数/发言与已评分数/清单条数，本人可重生覆盖）、传世清单（uk(space,item)，类型 PLACE/PASSWORD/THING/WORD，**封存需对方加签**，本人签不了自己的）、周年抽奖箱（uk(space,year) 每人一年一次；**奖池按规格吃「当年攒的迷你愿望」=本年 `couple_point_ledger` 里 `EARN` 的条目（去重、按 `PRIZE_MAX` 截断），本年一条都没攒过才回落 Bank 静态愿望位**；**周年提醒走读时惰性结算 notified 标记，不新建 Job**）、F343 里程碑倒推与 F349 空间等级均无表读时算（近 30 天台账速率倒推达成日，goal 越界回落 300；等级=台账数+传世系行数合计按门槛表定档）；GET /vault?goal= 聚合（**auditCandidates 把回忆资产系现有条目前 12 项列成「语录：/票根：/第一次：」候选，F341 年审让人能挑不用凭空想**），写接口返回整份 LegacyVO
- `CoupleCodexController` `/api/couple/codex`：F280-F289 我们百科（批次二十四）——词条共建（term 同空间唯一 upsert 推 TA，首建人可删）、默契综艺（≥5 词条才能开一期、一天一期，stableHash 抽 5 词填空，双交齐同答计数推 both codex-quiz-done 默契 x/5，本人重复交卷 400）、喜好 TOP10（8 类目本人榜 upsert + 猜对方的榜一次可改；对方榜+我的猜齐即 revealed，差异进「重新认识清单」rematch）、外号考据（nickname 唯一 upsert 新建推 TA 改写静默）、友情测验（题面唯一，被考人才能答，答错 last_try_day 起 7 天冷却，答对锁死）、去过的地方（name 唯一 upsert，rating 1-5 钳制，year 可空）、第一眼对视（双盲各≤3 次提交，同刻推 both codex-firstlook-match，双方满 3 次强制互见 codex-firstlook-force，互见后 400）、习惯图鉴（observer+habit 唯一，target 独判 REAL/WRONG 推回观察员）、口味变迁（thing+user upsert 新建推 TA）、人格双报（8 题 1/2，TYPE_AXES E/I S/N T/F J/P 两题一轴平票取第一题，一年一报重测覆盖当年，双报推 both 差异「四维里 x/4 轴相同」）；GET /overview 聚合，写接口返回整份 OverviewVO
- `CoupleFactoryController` `/api/couple/factory`：F270-F279 二人制造厂（批次二十三）——家务轮盘（一周一转 ≤8 项、stableHash 交替分配、对方认账双签、天选之人干完打勾、全清推 both factory-spin-clear、近 3 周欠账栏）、采买清单（加/删[登记者]/买回推登记人，月榜按 doneAt 计数出「生活委员」并列可双委员）、冰箱库存（同名 upsert 复活、赏味期≤3 天进 expiring 提示、用完 OUT）、代拿快递（自接禁止、接单侠送达向 couple_point_ledger 插 EARN2「代拿快递感谢章」推 both）、叫醒服务（周词 upsert 可改、对方每天可递一张叫醒卡，一天一卡）、服药提醒链（TA 点提醒了每日一次、本人点吃了链 +1 断日重开、停服可复活）、久坐互拍（一人一天一拍、双方≤1h 记同起行 paired=1 只推一次 both）、垫付本（金额分正整数、欠款一方清账推 both 无债卡）、战利品互猜（周单一报可改、对方一猜、买家 0-5 打分推 both）、家安月检（六项编码全勾、双人才算检完、近 3 月缺检聚合提醒）；GET /board 聚合，写接口返回整份 BoardVO
- `CoupleBoardController` `/api/couple/board`：F240-F249 我们公司——头衔任命（给 TA 封职位，本人待任命最多 2 个超出 400；被任命者本人盖章 board-appointed，非本人 400「任命章要本人盖」）、董事会决议（提案推 TA；仅非提案人可裁「自己的议案不能自己裁」，PENDING→PASSED/VETOED 一票否决留痕 decided_at/veto_by，双推 board-passed/board-vetoed）、年度股东大会（year 默认当年、4 位数字校验，review≤500/goal≤200，同年 upsert 可改不重推；对方述职仅双提交后可见）、升职公示栏（无表：按 couple_point_ledger EARN 累计定档 实习生0/正式职员20/小组主管60/部门经理150/公司总监300/合伙人600）、发薪日（一人一月一次重复 400，thanks≤200，发薪即向台账插 EARN「发薪日感谢工资」5 分；双发推 both）、金点子箱（content≤140；对方才能采纳「自己的点子要对方来采纳」，采纳生成 PENDING 决议并回填 vote_id 推 both）、会议签到（每人每天一行，10s 窗口内双签置双方 convened=1 推 both board-convened，已开会不再更新）、公司名片（无表拼文本：两人职衔+通过/否决数+当月最早发薪日+Bank 收尾话术）、公司周报（周一锚毫秒过滤本周议案/点子/赚分）；overview 一次聚合全部

- `CoupleEchoController` `/api/couple/echo`：F350-F359 回音壁（批次三十一，v7 首批）——好事簿（**单记录人口径**：from_user 写下「TA 为我做的事」即 TA 爱我的证据；同日同人同内容 400、加星只归记录人且幂等）、鼓励语罐（每人 ≤5 格，取**最小空槽**复用、第 6 条 400、只能清自己罐里的）、能量补给（**每人每天一次**（uk space+from_user+day，重复 400），随机翻自己 3 条证据+双方鼓励语与高光各 1 条，顺带开读在途自留信，推双方 echo-refilled）、感谢慢递（欲谢的话封存 7 天、**在途每人 ≤3**、到日读时惰性结算推双方 echo-thanks-arrived）、高光重放（三行卡 moment/did/feel，每人 ≤12、本人可整理）、夸夸回执（**跨模块只读 couple_praise** 校验归属，uk(space,quote_id,from_user) 幂等，推夸的人 echo-receipt-given）、电量预报（level 1-5 钳制、每人每天一格可改写，对方 ≤2 格读时给「今晚轻轻的」）、写给低落的自己（**一人同时一封在途**，READ 只有本人开读不给对方推；**SEALED 态聚合接口不下发正文**——锁要在服务端锁，前端不渲染不等于没泄漏）；GET /vault 总览、GET /calendar?year=、GET /year?year= 为读接口，其余 12 个 POST 写接口（/deed /deed/star /juice /juice/remove /refill /slow /highlight /highlight/remove /receipt /battery /self /self/read）全部返回整份 EchoVO 聚合；读时聚合无表项=F353 被爱日历、F359 年报；共 15 个映射

- `CoupleFocusController` `/api/couple/focus`：F360-F369 注意力保护区（批次三十二）——专注夜报（uk(space,day) 一行两列 `minutes_a/minutes_b`，0-180 钳制、note ≤40 超出 400；**双报当夜点亮**推 both `focus-night-lit`，单报推 both `focus-night-reported`）、专属时段（uk(space,week) 每周一格，`day` 必须落在本周否则 400，hours 1-6 钳制默认 2，**提议人不能自己盖章**「自己写的时段不能自己确认」，对方确认推 both `focus-slot-confirmed` 且重复确认幂等不重推）、攒一句话（≤80 字、在途每人 ≤5 超出 400，写入推 TA `focus-queue-added`；`today()` 读时惰性签收**不推事件**，`queueRead` 才推 both `focus-queue-read`，count 只算本次真置成已读的）、饭桌不低头/对视十秒/不插电半小时（各点自己那列，**只有本次真的 0→1 才 update 并在双点时推 both**，重复点击静默；不插电周连击 `streak` 读时算只扫当年）、走神温柔哨（每人每天 ≤2 张超出 400、note ≤40，只推收卡人 `focus-nudge-sent`，seed 走 `CoupleRitualBank.stableHash`）、数字排毒半天（kind 只收 AM/PM 否则 400，一天一格，首个应战者写进 `confirmed_by`，双报推 both `focus-detox-done`、首次发起推 `focus-detox-started`，重复应战不重推）；GET /today 总览与 12 个写接口一律返回整份 TodayVO 聚合，GET /weekly（周一锚）、GET /year?year= 除外；共 13 个映射

- `CoupleQuestController` `/api/couple/quest`：F370-F379 人生关卡（批次三十三）——关卡预告（day 必须今天或以后、kind 白名单 INTERVIEW/REPORT/DEFEND/TALK/CHECKUP/OTHER、name ≤30 且同人同日同名 400、**在途每人 ≤3**（countPrep 读 status=PREP），只有挂单人能撤，已报战报的不许撤）、出关战报（**一战一报** uk(battle_id)、只有打这关的人能交、result 三选一否则 400，交完 battle 转 DONE 从而离开在途列表）、盖章（**自己不能给自己盖**，章名按战果映射 WIN→🏆庆功章 / SURVIVE→🍀幸亏章 / LOSE→🫂抱抱章，重复盖幂等返回不重推）、加班预报（每人每天一行 upsert、untilHour 13-23 钳制、note ≤40；**灯卡只有对方能留**且必须已预报，自己留 400）、生病陪护单（**只有对方能为 TA 开单**——生病的人自己顾不上记，patient 恒为 partnerOf(操作人)，在途每人 ≤1；代记 WATER/MED **只有陪护人**且一天每种只记一次；病中留言只有陪护人能写；**痊愈只能病人自己宣布**，陪护人替 TA 关 400，关单推双方并带陪护天数/喝水/吃药计数）、静音舱（untilDay 必须晚于今天、一人一个在途舱；加油卡**只有舱外的人能递且每天一张**（cheers 存 MMdd CSV，列宽 varchar(320) 按 60 条留余量）；本人出舱后提醒对方补长信，**长信勾只有对方能打**且幂等）、搬家互助（区块位 1-8 越界 400、name ≤20；**一格只有一个人认领**，被对方认领先点 400，本人再点是取消；纸箱与完成勾**只认认领人**，0-99 钳制，重复完成不重推；新家第一晚双人列口径，**只有本次真的 0→1 才 update 并在双点时推 both**）、低谷通行证（span 7-30 天否则 400「太短像赌气，太长像放弃」、在途每人 ≤1；「不说话也行」卡**只有对方能递**且一天一张（careDays MMdd CSV）；**回升只能本人宣布**，别人替 TA 说好 400）、小胜利账本（每人每天一条 upsert，**改写不重推 quest-win**，不许预支未来日；**小赢奖只能颁对方的记录且一人一周一颁**，按 findByDayRange 本周区间查 awardedBy）、关口预约（day 限今天起 60 天内、title ≤30、同人同日同名 400；**到场只有非挂单人能点**且幂等，撤单只有挂单人）；GET /board 总览与 27 个写接口一律返回整份 QuestVO 聚合，GET /wall?year= 除外；共 29 个映射。**F378 成就墙计数一律直查原始表按 yearOf(day) 过滤**（不用已 limit 的列表 VO 回算，沿用批次二十六/二十八的教训：钳列表会把年报数字一起改小），列表钳制阈值 LIST_BATTLE=12/REPORT=20/NURSE=6/WIN=21/UPCOMING=20/POD=8

- `CoupleCatchController` `/api/couple/catch`：F380-F389 聆听者（批次三十四）——暗中心愿本（**记下时一个事件都不推**，保密全靠读时按 `secret()` 过滤而不是靠前端：owner 侧只给已揭晓的行、recorder 侧只给自己记的未揭晓行；uk(space,owner,content) 查重、每人被记 ≤12、出处日不许是将来；`fulfill()` 同写 fulfilled+revealed_at「兑现即揭晓」，只有记账人能勾且幂等，勾了才推 owner `catch-wish-fulfilled`）、雷区（每人 ≤6 颗、话题查重；**知晓章只能对方盖**且 `ack()` 返回本次是否真变、重复盖不重推；避雷**必须先有知晓**且挂雷人自己绕开不算战绩）、安全词（每人一格 upsert；**没约定就喊不了暂停**；使用记录 uk(space,day,user) 一天一次；复盘只能由喊停的人补自己那天，补完推双方）、敏感日历（只能为 TA 标且不许标过去、kind 白名单 PERIOD/CHECK/MEMORY/OTHER、同日同型查重；代标的人才能撤，**敏感日主人自己不能撤**）；话头存档（查重只看在途 OPEN——(space,from_user,topic) 不是唯一键，续完后允许再存同一话题，也因此**不能用 selectOne 查**否则抛 TooManyResults；销档只由存话头的人，重复销幂等不重推）、真话翻译（本人申报、必须写翻译结果、每人 ≤10 条、**对方不能改也不能删**）、聆听协议（五选一白名单、upsert；总览给三态 hint：没写齐→「还差你/TA」，写齐但不同→差异话术，写齐且相同→「两份说明书都交了」）、话题许愿池（自己许的题不能自己接、聊完只由接单人且感想必填、`takenAt` 起超 7 天置 overdue 并换话术）、今日一句话（每人每天一句，**改写不重推**；对方今天没说就回看 TA 昨天那句并挂 hint）；GET /board 与 19 个 POST 返回整份 CatchVO，GET /year?year= 除外；共 21 个映射；F389 年报直查原始表按 day/yearOfMillis 归年，**不用已 limit 的列表回算**

- `CoupleLaughController` `/api/couple/laugh`：F390-F399 欢笑银行（批次三十五）——笑点存档（day ≤今天否则 400「还没发生」、title ≤30、scene/culprit/witness 各按上限、funLevel 1-5 钳制、**每人每天 ≤3 条**、uk(space,day,from,title) 查重；**现场证词只有对方能补且一条只补一次**，补过再补 400）、每日一逗（**值班人按本周一锚的周序号奇偶在 userA/userB 间轮换**，非值班交节目 400「轮不到你」，uk(space,day) 一天一格，**判过分就不许再改节目**；判分只归非值班人、白名单 HAPPY/FLAT/FAKE、重复判幂等返回不重推）、冷笑话结冰榜（≤80 字、uk(space,from,content) 内容查重「不许重播」、每人每天 ≤3 条；**结冰只由对方判且判过不许翻案**——这条与一逗的「重复幂等」不同，是显式 400）、尴尬回收站（day ≤今天否则「社死不能预约」、uk(space,day,from) 一天一条；**抱抱章只归对方**且 `heal()` 返回本次是否真变、重复盖不重推；**满一年转好笑完全是读时算**：`turnedFunny(today)` 不写库不推送）、快乐突袭（kind 白名单 PRAISE/MEME/MEMORY、uk(space,from,day) 一天一发；**中弹章只认对方**且幂等）、笑点默契考（一条梗每人一票 uk(space,joke,from)，可改自己那一票不重推；**双人一致才算默契**，只看是否相同、不对照实际结冰结果）、大笑处方（targetKind 白名单且**指向的行必须属于本空间**否则 400，uk(space,from,day) 每天一张；**服用回执只归收方**且幂等）、幽默风格图鉴（aboutUser 限两人之一、style 五选一、rater=自己，自评与互评各一行可改写；hint 三态：没填满→补齐提示、一致→同频、不一致→差异建议）；GET /bank 总览与 13 个 POST 返回整份 LaughVO，GET /week（周一锚）、GET /year?year= 除外；共 17 个映射。**年报按「事件发生日」归年**（社死按社死日而非盖章日），400 天前那条进不了今年榜单。

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
- `CoupleListenBank`：倾听与发声内容库（语气四标签 TIRED/BUSY/SAD/OKAY+翻译条/休战收场卡 3 条/日抛称呼 16 个/三行 21 天纪念文案/互评语线/早想说放行话术）
- `CoupleFactoryBank`：二人制造厂内容库（轮盘开场 4 条/生活委员头衔/临期与漏检话术/快递感谢章 2 分/同起与清账卡/家安六项编码 GAS·WATER·ELEC·WINDOW·LOCK·FIRSTAID/战利品打分评论 3 档）
- `CouplePostBank`：明日邮局内容库（许愿井 12 题按周序轮换/未来卡额度六档梗/接龙封存与新年卡放行话术）
- `CoupleTheaterBank`：扮演剧场内容库（身份池 14 组「身份+相处指南」/电话亭信号彩蛋 4 条/家长题 20 道/颁奖词 4 条/双视角剧本收尾 3 条/申诉判词 3 条/颁奖礼奖项 8 个）+ stableHash 按空间+日稳定取身份与家长题
- `CoupleBodyBank`：身体通知系统内容库（体征异常提醒 3 条模板 {u}{w} 填充/呼噜震感三档话术/照顾卡 4 张/SOS「我能做」选项 5 张/虚拟陪同 3 条/药友陪伴话术三池（稳-难-没记）/军令状违约与双签话术）+ phaseLabel/fitLabel 类静态换算，无任何医疗建议
- `CoupleRepairBank`：修复车间内容库（解冻三问题干/复温话术 3 条/验货通过与退回各 2 条/重来卡与台阶卡 5 张/暂停话术/礼盒任务 5 条与完成回执 2 条/纪念碑 2 条/年报称号两池+summary）+ pointLabel 六要素中文名，全程不评判谁对谁错
- `CoupleWorldBank`：两家与朋友内容库（拜访建议池 7 条分 BRING/TALK/MINE/他观六题按空间轮转/战报与官宣与选稿与送达话术/陪同小包模板 6 项/亲戚称谓样卷 6 条/保证见证-解除-塌房三态话术/赔礼信模板 3 封）+ prepKind 前缀归类，口径是「把慌拆成能准备的任务卡」
- `CoupleLegacyBank`：传世系统内容库（年度十问固定题面 10 条跨年可比/发言评分卡五档话术/里程碑加速建议 4 条/汇率结算与品牌发布与封存话术/抽奖奖池 8 项/空间等级称号门槛表 10 档/盘点收尾句 3 条）
- `CoupleCodexBank`：我们百科内容库（默契题评语 5 条/TOP 八类目 FOOD·MOVIE·SONG·COLOR·PLACE_EAT·SHOW·SEAT·SNACK 与重新认识话术/第一眼互见话术同刻与差异各 2 条/人格 8 题四维卷与 TYPE_AXES E-I·S-N·T-F·J-P/差异解读 4 条）

- `CoupleEchoBank`：回音壁内容库（好事簿记下/加星话术、补给随机话术与推 TA 话术、慢递送达话术、回执话术、低电量「今晚轻轻的」提示、年报称号与 summary）+ 按 seed 稳定取值，无副作用

- `CoupleFocusBank`：注意力保护区内容库（单报/双报/等 TA 报的夜报话术、时段预约与确认文案、攒话与收讫回执、同桌/对视/不插电双点话术、温柔哨「回来啦」按 seed 稳定随机、排毒发起与达成、AM/PM 中文标签、周报与年报 summary）+ 按 seed 稳定取值，无副作用

- `CoupleCatchBank`：聆听者内容库（kind/mode 中文标签、心愿记下与兑现揭晓话术（带出处日+场合）、雷区挂出/知晓/避雷、安全词约定/喊停/复盘、敏感日标注与前一天提醒、话头存档与续完、反话对照、聆听协议三态 hint（含「两份说明书都交了」与差异话术）、话题池许愿/接单/聊完（准时与超时两种）、今日一句话与「今天还没说就回看昨天那句」hint、年报称号五档与 summary）+ 语料池 CHEERS 式YEAR_TAILS 按 seed 稳定取值，无副作用

- `CoupleLaughBank`：欢笑银行内容库（kind/verdict/style/targetKind 四套中文标签、笑点与证词、一逗上台与判分语气（强撑/没笑/真笑各一句）、冷笑话抛出与结冰计数、社死提交/抱抱/满一年转档、突袭与中弹、默契一致与不一致、处方与服用、风格同频与差异建议、周报收尾句池 4 条与年度称号五档）+ 按 seed 稳定取值，无副作用

- `CoupleQuestBank`：人生关卡内容库（关卡 kind/result 中文标签与**战果→章名映射**、预告/战报/盖章话术、加班与留灯卡、陪护开单/代记/关单计数、静音舱入舱/出舱/长信、加油卡语料池 8 条与「不说话也行」卡语料池 8 条（按 `stableHash(space|域|id|MMdd)` 稳定取值）、搬家认领/完成/第一晚、低谷开卡与回升计数、小胜利与小赢奖、到场应援、成就墙称号（按 battles/attends 五档定级）与 summary）+ 无副作用

定时任务 `CoupleReminderJob`（Asia/Shanghai）：09:00 约定逾期提醒；09:30 纪念日倒数（7/1/0 天）；09:45 倒数日提醒（7/3/1/0 天）；10:00 情绪急救箱（连续 2 天低落提醒对方）。
定时任务 `CoupleSurpriseJob`（Asia/Shanghai）：每分钟送达心动闹钟与思念速递（alarm-fired/miss-delivered）；09:15 告白重现；09:20 生日彩蛋（读 im 包 UserProfile.birthday）；10:15 花园缺水巡检（garden-withered）。
定时任务 `CoupleCareTalkJob`（Asia/Shanghai）：21:00 情话储蓄罐利息（每人随机取一句未投递情话送达，love-bank-interest）；23:00 深夜陪伴（当天负面心情且未被求抱抱接住时提醒对方，night-care）。

## 四、数据层规范（硬性）

- 凡改表结构或初始化数据，必须产出 Flyway 增量脚本并同步 schema.sql——触发条件、命名、幂等/双兼容写法、种子数据等完整规范见 `.agents/skills/db-migration/SKILL.md`
- 现有迁移：V1 couple 基础表 → V2 存量基线 → V3 心情 → V4 信箱 → V5 条约/城市/基金 → V6 贴贴动作/心情回应/爱称 → V7 任务卡/默契 → V8 和好卡/夸夸/生理期 → V9 胶囊/倒数日 → V10 记账/家务/约会/习惯/暗号 → V11 空间个性化 → V12 私信心动时刻 → V13 通知中心/生日 → V14 第一次清单/一问互评 → V15 惊喜与期待（刮刮乐/盲盒/闹钟/思念/花园/玫瑰/幸运签/告白/藏宝图）→ V16 懂我与被接住（求抱抱/矛盾复盘/道歉券/真心话/树洞/心灵感应/情话储蓄罐）→ V17 共同养成（挑战赛/恋爱存折/百日之约/心愿互换/共读/旅行心愿/追剧/词典/下次一定）→ V18 回忆资产（语录册/电影票根/我们的歌单；F80-F82/F85-F87 为现有数据聚合与放宽常量，无新表）→ V19 沟通增强（安静小屋/情绪接力/比划猜/故事接龙/道歉三部曲/心情词汇）→ V20 异地恋（牵手/想念/作息/见面信/云约会/平安卡/见面日记）→ V21 确定感（安全感账户/十年之约/愿景板/承诺博物馆/信任存折/双人契约/守护兽 + couple_anniversary.kind 列）→ V22 趣味游戏（一百问/出题考TA/情话课/周末盲选/情话Battle/参赛句子/抽象画廊）→ V23 深度陪伴（梦境/美食地图/TA手册/情绪SOS/每日三问/自定义成就）→ V24 成长系（习惯搭子 couple_habit_streak/感恩便签/情绪颗粒度/每周高光/共读一分钟/拖延互助/优点存折）→ V25 文字浪漫（情诗接龙/三行情书/醒来第一条/漂流瓶/密码情书/灵魂提问/贴纸手账）→ V26 默契亲密（爱语测评/心动闪光/如果问答/动作暗语/同频共振/心动日历）→ V27 生活经营（家庭会议/主理人/技能交换/月度互评/应急卡/存档点/家务积分/五年计划/策划案）→ V28 时光博物馆（纪录片分镜/博物馆展品/隐藏成就/家规宪法/免打扰设置）→ V29 常用收藏（couple_user_pin 每人一行 pins 逗号分隔） → V30 两个人的饭桌（饭票/星评/踩雷/本周菜单/拿手菜/搭伙车/话题标记 7 表） → V31 体温同步（熄灯/睡眠单/数羊/喝水/冷暖/熬夜卡/慢生活/对策本/抱抱 9 表） → V32 小日子仪式感（建国纪念日/过法卡/打卡/保险柜保费/续约签字/愿望券/当日体感 7 表） → V33 我们公司（头衔/决议/述职/发薪/金点子/签到 6 表） → V34 夫妻老黄历（节气跟风/过法/吉日/节日家档/手账/长假愿望/放空日 7 表 + couple_anniversary 加 calendar_type/lunar_md 列） → V35 倾听与发声（时段/代笔/倒带/卡壳问/换位信/早想说/三行/语气/休战旗/称呼日 10 表） → V36 二人制造厂（轮盘任务/采买/冰箱/快递/叫醒/服药/久坐/垫付/战利品/月检 10 表） → V37 我们百科（词条/默契综艺/TOP榜/互猜/外号考据/友情测验/足迹/第一眼/习惯图鉴/口味变迁/人格年报 11 表） → V38 明日邮局（新年卡/大事+步骤/拍卖/梦想家/退休计划/井答/胶囊接龙/解梦/愿望台账/未来卡 11 表） → V39 扮演剧场（身份签/互换日记/师徒日/电话亭/黑话/奥斯卡/家长题/双角色追剧/客服工单 9 表） → V40 身体通知系统（体征/呼噜/周期/互助营/运动链/不适SOS/忌口红线/体检陪同/情绪药友/早睡军令状 10 表，全部 couple_body_ 前缀） → V41 修复车间（冷冻单/道歉质检/重来卡/重建计划/冷战倒计时/底线卡/认错榜/修复礼盒/纪念碑 9 表，全部 couple_repair_·couple_sorry_review·couple_rebuild_plan·couple_bottom_line·couple_admit_log·couple_peace_line 前缀，F325 年报无表） → V42 两家与朋友（拜访攻略/送礼池/他观问卷/官宣卡/文案候选/接待手册/称呼册/社会信用/群聊素材/代 TA 赔礼 10 表，全部 couple_world_ 前缀） → V43 传世系统（年度十问/记忆库年审/续约发言/恋爱汇率/情侣品牌/年度盘点/传世清单/周年抽奖 8 表，全部 couple_legacy_ 前缀；F343/F349 无表） → V44 回音壁（好事簿/鼓励语罐/补给日志/感谢慢递/高光重放/夸夸回执/电量预报/写给低落的自己 8 表，全部 couple_echo_ 前缀；F353 日历、F359 年报无表） → V45 注意力保护区（专注夜报/专属时段/攒一句话/饭桌/对视/不插电/温柔哨/排毒半天 8 表，全部 couple_focus_ 前缀；F367 周报、F369 年报无表） → V46 人生关卡（关卡预告/出关战报/加班预报/陪护单/代记打卡/静音舱/搬家区块/新家第一晚/低谷通行证/小胜利账本/关口预约 11 表，全部 couple_quest_ 前缀；F378 成就墙无表；care_mark 只有 created 没有 updated_at） → V47 聆听者（暗中心愿/雷区/安全词+使用记录/敏感日历/话头存档/反话词条/聆听协议/话题池/今日一句话 10 表，全部 couple_catch_ 前缀；F389 年报无表；**F384 规格的 uk(space,from_user,status) 与「在途每人 ≤5」自相矛盾，改按 idx_catch_thread 普通索引实现**） → V48 欢笑银行（笑点存档/每日一逗/冷笑话/社死往事/快乐突袭/笑点预判/大笑处方/幽默风格 8 表，全部 couple_laugh_ 前缀；F398 周报与 F399 年度榜无表）
- 索引/唯一键名是**全库唯一**（H2 索引不随表隔离，重名报 42S11「Index already exists」）：新增 `uk_*/idx_*` 前先用脚本对全部 `db/V*.sql` 查重，模块前缀（如 `uk_body_*`）是最省事的办法
- **`Integer`/`Long` 位字段禁止配 `isXxx()` 布尔 helper**（硬性）：Lombok 已给字段生成 `getXxx()`，再手写 `isXxx()` 就是同一属性两个不同类型的 getter，MyBatis `Reflector` 会按方法枚举顺序**随机**抛 `ReflectionException: ambiguous type for property`——实测让 `GET /api/couple/world/world` 与 `GET /api/couple/legacy/vault` 整页 500（两家与朋友、传世系统两批 20 个功能对每个用户都不可用），而单测全用 mock 完全照不出来。位判断一律命名 `xxxFlag()`（`laughAFlag()`/`doneFlag()`…）；只有真 `private boolean` 字段才允许 `isXxx()`。回归保护见 `src/test/java/com/smart/chat/EntityReflectionGuardTest`——**必须逐字段 `reflector.getGetInvoker(name).invoke(instance)` 真读一遍属性**：歧义 getter 被 MyBatis 包成 `AmbiguousMethodInvoker`，只实例化实体（旧写法）从不触发取值，守卫会恒绿成假守卫；新增实体后跑它，并确认它真的能变红
- **日期进 CSV 列一律用 `MMdd` 或周几 1-7，不要写 ISO**：`couple_theater_master_day.serves(40)` 存 ISO 三天就满（一周要记七天）、`couple_body_quit.broke_days(160)` 存 ISO 只够 14 条。已统一为周几（师徒侍奉）/`MMdd`（破戒，超 25 条 400）/`MMdd:A|B`（重建双签）；新批次写 CSV 前先按「条数上限 × 记号长度」对一遍列宽
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
