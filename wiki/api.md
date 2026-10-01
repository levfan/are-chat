# REST 接口总表

> 本页回答：全部 Controller 的端点清单（方法+路径+一句话）。路径省略类前缀 `/api`；couple 系前缀见各节标题。全库共 42 个 Controller、508 个 REST 端点（批次二十后按源码注解清点）。

来源：`src/main/java` 各 `*Controller.java` 的映射注解与 javadoc。返回统一 `ApiResponse{code,message,data}`；除 `/api/auth/**` 与 `/api/health` 外均需登录会话。业务功能编号（F 号）含义见 [couple-space.md](couple-space.md)。

## 非 couple 接口

### `AuthController` `/auth`
- POST `/sms-code` 注册验证码（演示回显 devCode）｜ POST `/register` 提交注册审批申请
- GET `/register-status` 审批进度轮询 ｜ POST `/login` 登录 ｜ POST `/logout` 登出
- GET `/me` 当前用户 ｜ PUT `/password` 改密（旧密码+强度校验）｜ POST `/deactivate` 自助注销（清好友+失效会话）

### `AdminController` `/admin`（仅管理员）
- GET `/applications` 审批列表 ｜ POST `/applications/{id}/approve` `/reject` 通过/驳回
- GET `/pending-count` 待办数 ｜ GET `/users` 用户列表 ｜ POST `/users/{username}/status` 启/停 ｜ POST `/users/{username}/reset-password` 重置密码
- GET `/audit` 审计日志 ｜ POST `/announcements` 发布公告 ｜ POST `/announcements/{id}/close` 关闭 ｜ GET `/announcements` 列表

### `FriendController` `/friends`
- GET `` 好友列表 ｜ GET `/suggest` 加好友联想 ｜ POST `/requests` 发申请
- GET `/requests/incoming` `/requests/outgoing` 收/发申请 ｜ POST `/requests/{id}/accept` `/reject`
- PUT `/{id}` 改备注 ｜ DELETE `/{id}` 删除好友

### `PrivateMessageController` `/messages`
- GET `/{peer}` 会话历史 ｜ GET `/{peer}/search` 会话内搜索 ｜ GET `/{peer}/export` 导出 JSON ｜ GET `/search/global` 全局搜索
- POST `/{peer}` 发送 ｜ POST `/{peer}/read` 已读 ｜ POST `/{id}/recall` 撤回 ｜ PUT `/{id}` 2 分钟内编辑
- POST `/{id}/reactions` 表情回应 toggle ｜ POST `/{id}/star` 收藏 toggle ｜ POST `/{id}/heart` 心动时刻 toggle ｜ GET `/hearts` 心动列表
- POST/DELETE/GET `/{peer}/pin` 置顶/取消/查询 ｜ DELETE `/{peer}` 清空会话 ｜ GET `/{peer}/attachments?type=image|file` 附件墙

### `ProfileController` `/profile`
- GET `` 我的资料 ｜ GET `/{username}` 好友资料卡 ｜ PUT `` 保存资料 ｜ GET `/friends-birthdays` 好友生日列表（F42）

### 其他
- `PresenceController` GET `/presence/online` 在线名单
- `StarsController` GET `/stars` 收藏消息聚合
- `AnnouncementController` GET `/announcements/current` 生效公告 ｜ POST `/announcements/{id}/read` 已读
- `HealthController` GET `/health` 健康检查（免登录）
- `FileController` POST `/files`（multipart）上传 ｜ GET `/files/{id}/download` 下载
- WS：`/ws/chat/{name}` 聊天室与推送通道（非 REST，见 [architecture.md](architecture.md)）

## couple 接口（按 Controller）

### `CoupleController` `/couple`
- GET `/overview` 总览（未建立返回待处理邀请）｜ POST `/invites` 邀请 ｜ POST `/invites/{id}/accept` `/reject` ｜ DELETE `/invites/{id}` 撤回
- PUT `/anniversary` 在一起纪念日 ｜ POST `/dissolve` 解除空间 ｜ GET `/relationship-of/{username}` 恋爱中徽章（F44）
- GET `/promises` · POST `/promises` · POST `/promises/{id}/done` `/undone` · DELETE `/promises/{id}` 承诺卡 CRUD/标记
- POST `/checkins` 早晚安打卡 ｜ GET/POST `/question` 今日一问（取题/提交）｜ POST/GET `/answers/{day}/react|reactions` 回答互评（F48）
- GET `/questions/history` 双答存档 ｜ GET `/items` · POST `/items` · PUT/DELETE `/items/{id}` 共享清单
- GET `/anniversaries` · POST/DELETE `/anniversaries(/{id})` 纪念日 ｜ PUT `/profile` 空间个性化（宣言/主题/贴纸）
- POST/GET `/moods` 每日心情/最近曲线 ｜ GET `/timeline` 时光轴 ｜ GET `/intimacy` 心动值
- POST/GET `/letters` 写信/信箱 · POST `/letters/{id}/open` 拆信 · DELETE `/letters/{id}` 撤回
- POST/GET `/pacts` 条约 · POST `/pacts/{id}/accept` 对方盖章 · DELETE `/pacts/{id}` ｜ PUT/GET `/cities` 城市与异地卡片
- POST/GET `/funds` 基金 · POST `/funds/{id}/deposits` 存钱 · DELETE `/funds/{id}`

### `CoupleBondController` `/couple/bond`
- POST/GET `/actions` 发贴贴动作/动作流 ｜ GET `/stats` 动作统计+里程碑
- POST/GET `/mood-reactions` 回应 TA 心情/当日回应 ｜ PUT `/pet-name` 专属爱称（空串清除）

### `CoupleRitualController` `/couple/ritual`
- GET `/task` 今日任务卡（懒生成幂等）· GET `/tasks` 近 14 天 · POST `/task/done` 打卡
- GET `/tacit` 默契状态 · POST `/tacit/start` 开局 · POST `/tacit/answer` 提交（第二人即结算）· GET `/tacit/history`
- GET `/love-word` 抽情话 ｜ GET `/fortune` 今日运势（同日同签）｜ GET `/goodnight-story` 晚安故事

### `CoupleCareController` `/couple/care`
- GET `/weather` 情绪天气预报 ｜ GET `/first-aid` 情绪急救箱 ｜ GET `/mood-sync` 情绪同步率（F64）
- POST/GET `/reconciles` 递/列和好卡 · POST `/reconciles/{id}/accept` 接受
- POST/GET `/praises` 贴/列夸夸 · POST `/praises/{id}/receive` 签收 ｜ GET/PUT `/cycle` 生理期卡片/记录
- GET/POST `/comfort` 求抱抱看板/发出 · GET `/comfort/cards` 安慰话术卡 · POST `/comfort/handle` 回应（F60）
- GET `/chat-topics` 陪聊话题卡（F63）

### `CoupleMakeupController` `/couple/makeup`
- GET/POST `/reviews` 矛盾复盘列表/今日复盘（双份合成锦囊，F61）
- GET/POST `/sorry-tickets` 道歉券列表/递券（同时 2 张有效）· POST `/sorry-tickets/{id}/use` 收下（F62）

### `CoupleTalkController` `/couple/talk`
- GET/POST `/truth` 今日真心话/作答（双答互见）· GET `/truth/history` 存档（F66）
- GET/POST `/whispers` 树洞列表/匿名投递 · POST `/whispers/{id}/answer` 回答后揭晓（F67）
- GET `/telepathy` 感应板 · POST `/telepathy/start`（每天 3 轮）· POST `/telepathy/answer`（F68）
- GET/POST `/love-bank` 罐子/存情话（存入仅预告，21:00 利息，F69）

### `CoupleGrowthController` `/couple/growth`
- GET `/challenge` · POST `/challenge/check` 双人挑战赛（双完成达成，F70）
- GET/POST `/passbook` 恋爱存折每日一笔（连续里程碑，F71）
- GET/POST `/hundreds` · POST `/hundreds/{id}/checkin` `/break` 百日之约（F72）
- GET/POST `/wishes` · POST `/wishes/{id}/accept` `/fulfill` 心愿互换（F73）
- GET/POST `/read-plans` · POST `/read-plans/{id}/progress` 共读计划（F74）
- GET/POST `/travels` · POST `/travels/{id}/visit` 旅行心愿地图（F75）
- GET/POST `/watchlist` · POST `/watchlist/{id}/progress` 追剧清单（F76）
- GET `/zodiac` 星座配对（静态）（F77）｜ GET/POST `/dict` · DELETE `/dict/{id}` 恋爱词典（F78）
- GET/POST `/next-times` · POST `/next-times/{id}/nudge`（1h 冷却）`/fulfill` 下次一定（F79）

### `CoupleChronicleController` `/couple/chronicle`
- GET `` 恋爱编年史（按年聚合）（F80）｜ GET `/archaeology` 考古卡（F81）｜ GET `/quiz` 问答机（F82）
- GET `/anniversary-report` 周年报告（F85）｜ GET `/birthday-look` 生日回顾（F86）

### `CoupleKeepsakeController` `/couple/keepsake`
- GET/POST `/quotes` · DELETE `/quotes/{id}` 甜蜜语录册（F83）
- GET/POST `/tickets` · DELETE `/tickets/{id}` 电影票根（F88）
- GET/POST `/songs` · DELETE `/songs/{id}` 我们的歌单（F89）

### `CoupleTodayController` `/couple/today`
- GET `` 今日看点聚合（F95）｜ GET `/heatmap?year=` 年度热力日历（F96）

### `CoupleMemoryController` `/couple/memory`
- GET `/badges` 徽章墙 ｜ GET `/on-this-day` 那年今天
- POST/GET `/capsules` 封/列胶囊 · POST `/capsules/{id}/open` 开（F87 到期由 Job 提醒）
- POST/GET `/countdowns` · POST `/countdowns/{id}/done` · DELETE `/countdowns/{id}` 倒数日
- GET `/monthly-report` 恋爱月报（F30）｜ GET `/data-overview` 数据总览（F29）
- POST/GET `/firsts` · DELETE `/firsts/{id}` 第一次清单（F46）

### `CoupleLifeController` `/couple/life`
- POST/GET `/expenses`（月账+AA 差额）· DELETE `/expenses/{id}` 记账
- POST/GET `/chores` · POST `/chores/{id}/done`（ALTERNATE 自动轮换）· DELETE `/chores/{id}` 家务轮值
- POST/GET `/date-plans` · POST `/date-plans/{id}/done` · DELETE `/date-plans/{id}` 约会规划
- POST/GET `/habits` · POST `/habits/{id}/checkin`（幂等）`/active` · DELETE `/habits/{id}` 双人习惯
- POST/GET `/ciphers` · DELETE `/ciphers/{id}` 暗号小本本

### `CoupleGameController` `/couple/game`
- GET `/boost` 今日心动加成（F31）｜ GET `/heatmap` 12 周互动热力（F33）｜ GET `/mood-curve` 30 天心情曲线（F34）｜ GET `/traffic-light` 恋爱红绿灯（F35）

### `CoupleSurpriseController` `/couple/surprise`
- GET `/scratches` 周卡懒生成 · POST `/scratches/{id}/scratch` 刮开 · POST `/scratches/{id}/redeem` 核销（F50）
- GET/POST `/boxes` · POST `/boxes/{id}/open` 恋爱盲盒（F51）
- GET/POST `/alarms` · DELETE `/alarms/{id}` 心动闹钟（24h 内）（F52）
- GET/POST `/misses` 思念速递（5~30min）（F53）
- GET/POST `/treasures` · POST `/treasures/{id}/done` 藏宝图（F58）
- GET/POST `/confessions` · DELETE `/confessions/{id}` 告白重现（F57）

### `CoupleGardenController` `/couple/garden`
- GET `` 花园状态（首访开垦）· POST `/water` 浇水（F54）
- GET/POST `/roses` 玫瑰看板/送花（每人 3 朵）（F55）
- GET/POST `/slips` 签板/为 TA 抽幸运签（可覆盖）（F56）

### `CoupleNotifyController` `/couple/notify`
- GET `` 最近 50 条+未读数 ｜ POST `/read-all` 全部已读（F41）

### `CoupleAdminController` `/couple/admin`
- GET `/stats` 情侣空间运营看板（F45，仅管理员）

### `CoupleCommController` `/couple/comm`
- GET `/translate` 恋爱翻译器（F100）
- GET/POST `/cool-downs` · POST `/cool-downs/{id}/soften` 安静小屋（F101）
- GET/POST `/relays` · POST `/relays/{id}/catch` 情绪接力（F102）
- GET/POST `/guesses` · POST `/guesses/{id}/clue` `/guess` 比划猜词（F103）
- GET/POST `/stories` · POST `/stories/{chainId}/lines` `/finish` 故事接龙（F104）
- GET `/dict-quiz` 词典小考（F105）｜ GET `/sweet-synth` 情话合成器（F106）
- GET/POST `/apologies` · POST `/apologies/{id}/accept` 道歉三部曲（F107）
- GET/POST `/feelings` 心情词汇量（F108）｜ GET `/goodnight-radio` 晚安电台（F109）

### `CoupleDistanceController` `/couple/distance`
- GET/POST `/handhold` 隔空牵手（同天双亮算）（F110）｜ GET/POST `/miss` 双城想念（互想=双向奔赴）（F112）
- GET/POST `/routine` 作息重合表（F114）
- GET/POST `/letters` · POST `/letters/{id}/open` 下次见面信（F115）
- GET/POST `/cloud-dates` · POST `/cloud-dates/{id}/done` 云约会清单（F116）
- GET/POST `/safeties` 异地平安卡（F117）｜ GET/POST `/reunions` 见面日记（F118）
- GET `/energy` 异地能量（30 天周期）（F113）｜ GET `/report` 异地恋报告（F119）

### `CoupleSecureController` `/couple/secure`
- GET/POST `/security` · POST `/security/{id}/accept` 安全感账户（F120）
- GET `/checkup` 恋爱体检 5 项（F121）｜ GET `/decade` · POST `/decade` 十年之约（凑齐推双方）（F122）
- GET/POST `/visions` 愿景板（同词共鸣）（F123）｜ GET/POST `/oaths` · POST `/oaths/{id}/stamp` 承诺博物馆（双章展出）（F124）
- GET/POST `/trust` 信任存折（每日 1 币）（F125）｜ GET `/rings` 恋爱年轮（F126）
- GET/POST `/contracts` · POST `/contracts/{id}/checkin` 双人契约打卡（F128）
- GET/POST `/pet` · POST `/pet/care` 守护兽（惰性衰减）（F129）
- 纪念日大日子分类（F127）在 `POST /couple/anniversaries` 的 `kind` 参数

### `CouplePlayController` `/couple/play`
- GET/POST `/survey` 一百问（答一题解锁同题）（F130）
- GET/POST `/quizzes` · POST `/quizzes/{id}/answer` `/judge` 出题考TA（F131）
- GET `/heartbeat` 心动概率（F132）｜ GET `/tarot` 今日塔罗（F133）
- GET `/love-lesson` 世界情话课 · POST `/love-words` 收藏（F134）
- GET/POST `/blind` 周末盲选（双提交 stableHash 开奖）（F135）
- GET `/battle` · POST `/battle` `/battle/vote` 情话Battle（F136）｜ GET `/weather` 恋爱天气（F137）
- GET/POST `/arts` 抽象画画廊（F138）

### `CoupleDailyLifeController` `/couple/daily-life`
- GET `/theme-song` 今日主题曲（F140）｜ GET/POST `/dreams` 梦境手账（F141）
- GET/POST `/foods` · POST `/foods/{id}/checkin` 美食地图 WANT→EATEN（F142）
- GET/POST `/facts` TA 使用手册（F143）｜ GET/POST `/soses` · POST `/soses/{id}/hold` 情绪 SOS（F144）
- GET/POST `/three` 每日三问（双答 both 推送）（F145）｜ GET `/praise` 夸夸生成器（F146）
- GET/POST `/badges` · POST `/badges/{id}/issue` 自定义成就 OPEN→ISSUED（F148）
- GET `/dashboard` 恋爱仪表盘（F149）（接头暗号 F147 无表按日抽）

### `CoupleCoachController` `/couple/coach`
- GET/POST `/habits` · POST `/habits/{id}/checkin` 21 天习惯搭子（F150）
- GET/POST `/thanks` 感恩便签墙（F151）｜ GET `/feel-families` · GET/POST `/feel` 情绪颗粒度日记（F152）
- GET/POST `/week-star` 每周高光互评（周一锚）（F153）｜ GET/POST `/read-minute` 共读一分钟（F154）
- GET/POST `/delays` · POST `/delays/{id}/nag` `/done` 拖延互助所（F155）
- GET `/morning` 早安能量站（F156）｜ GET/POST `/praise-bank` 优点存折（F158）｜ GET `/year-keyword` 成长年度关键词（F159）

### `CouplePoemController` `/couple/poem`
- GET/POST `/chain` 情诗接龙（F160）｜ GET/POST `/3lines` · POST `/3lines/{id}/like` 三行情书（F161）
- GET/POST `/morning-notes` · POST `/morning-notes/{id}/read` 醒来第一条（F162）
- GET/POST `/bottles` · POST `/bottles/{id}/reply` 漂流瓶（F163）
- GET/POST `/ciphers` · POST `/ciphers/{id}/crack` 密码情书（F164）
- GET/POST `/soul` 灵魂提问（双答互见）（F165）｜ GET/POST `/journal` 贴纸手账（F166）
- GET `/quote` 恋爱语录机（F167）｜ GET `/letter-templates` 情书模板（F168）｜ GET `/stickers` 贴纸库（F169）

### `CoupleSparkController` `/couple/spark`
- GET `/love-lang/quiz` · POST/GET `/love-lang( /mine)` 爱语测评 ｜ GET `/love-lang/pair` 对照卡（F170-F171）
- GET/POST `/flashes` 心动闪光（F172）｜ GET/POST `/what-if` 「如果」问答（F173）
- GET/POST `/signals` 动作暗语本（F174）
- POST `/tap` · GET `/tap/today` 同频共振（≤500ms 命中）（F175）｜ GET `/dashboard` 默契仪表盘（F176）
- GET/POST `/heart-days` 心动日历（F177）｜ GET `/sync-rank` 同频排行榜（F178）｜ GET `/weekly` 默契周报（F179）

### `CoupleManageController` `/couple/manage`
- GET/POST `/meetings` · POST `/meetings/{id}/decision` `/close` 家庭会议纪要（F180）
- GET `/host` · POST `/host/plan` 本周主理人（非主理人 403）（F181）
- GET/POST `/skills` · POST `/skills/{id}/take` `/done` 技能交换所（F182）
- GET/POST `/month-reviews` 月度互评（双评互见）（F183）｜ GET/POST `/emergency-cards|/emergency-card` 应急卡（F184）
- GET/POST `/snapshots` 情侣存档点（每月 upsert）（F185）
- GET `/points` · POST `/points/earn` `/redeem` 家务积分市场（F186）
- GET/POST `/five-year-plans` · POST `/five-year-plans/{id}/claim` `/finish` 五年计划双轨（F187）
- GET/POST `/anniv-plans` · POST `/anniv-plans/{id}/advance` 纪念日策划案（F188）｜ GET `/weekly` 经营周报（F189）

### `CoupleMuseumController` `/couple/museum`
- GET/POST `/scenes` 纪录片分镜（三幕缺一不可）（F190）｜ GET/POST `/exhibits` 博物馆展品（F191）
- GET `/last-year` 去年今日对比镜（F192）｜ GET `/silver-line` 今日银发情话（F193）｜ GET `/words` 恋爱高频词（F194）
- GET `/achievements` 隐藏成就墙（达标自动解锁）（F195）
- GET/POST `/rules` · POST `/rules/{id}/sign` 家规宪法（提案人不能自签）（F196）
- GET/POST `/dnd` 免打扰时段（可跨零点）（F197）｜ GET `/greeting` 首页问候引擎（F198）｜ GET `/annual-book` 年度记忆书目录（F199）

### `CouplePinController` `/couple/pin`
- GET `/` 双方收藏列表（F207）｜ POST `/` 全量覆盖我的收藏（去空白去重、≤6 键、键长≤40）

### `CoupleDiningController` `/couple/dining`
- GET `/today` 今日饭桌总览（饭票/命中/裁决/话题）｜ POST `/ticket` 投今晚饭票（F210）｜ POST `/topic/mark` 话题打卡（F218）
- GET `/rates` 星评流水 ｜ POST `/rate` 登记星评（1-5 钳制）（F212）
- GET `/nogos` · POST `/nogo` · DELETE `/nogo/{id}` 踩雷库（谁提议谁划掉）（F213）
- GET `/board` 本周看板（菜单+拿手菜+搭伙车）｜ POST `/plan` 排/擦某天宫格（F214）｜ POST `/homecook` 报周拿手菜（F215）
- GET `/drink?mood=` 点单机（静态无表）（F216）
- POST `/cart` · POST `/cart/{id}/lock` · DELETE `/cart/{id}` 搭伙车（双锁成行）（F217）｜ GET `/year` 年度干饭账（F219）

### `CoupleCozyController` `/couple/cozy`（批次十八，F220-F229）
- GET `/today` 今日体温同步总览（熄灯/睡眠单/数羊/喝水/冷暖/陪伴卡/慢生活/对策本/抱抱一次拉齐）
- POST `/lightout` {atTime?} 道晚安点灯（双方当晚都点=熄灯；连击满 7 晚推 both）（F220）
- POST `/sleep` {day,stars,dream} 报昨夜睡眠单（1-5 星钳制、梦话≤70 字，本人当日可改）（F221）
- POST `/sheep` 数一只羊（60s 窗口累计满 10 下数完，超时重置；双方数完推 both 比用时）（F222）
- POST `/water` 干一杯水（TA 杯子加一格；对方 3h 未回总览带 nudge 轻提醒）（F223）
- POST `/weather` {city,feel,tempText} 互报冷暖（当日可改）（F224）｜ POST `/weather/advise` 一键叮嘱添衣（同一人一天一次）
- POST `/latenight` 递「早点睡」陪伴卡（一天一张幂等不再推）（F225）
- POST `/slow` {thing} 提本周慢生活小事（可改；凑齐两提推 both）｜ POST `/slow/check` 慢生活打卡（双方都打完推 both 回放）（F226）
- POST `/remedy` {body} 登记我的疼痛对策本（≤300 字随时改）（F227）｜ POST `/comfort` TA 不适日按 TA 对策一键执行并送达
- POST `/hug` {cnt,note} 自报抱抱（1-99 钳制，破 10/50/100/520/1000 里程碑推 both）（F228）
- GET `/monthly?month=` 月度安眠小结（聚合无表，体温同步指数 0-100）（F229）
- 除 `/monthly` 返回 MonthlyVO 外，其余全部返回 TodayVO 聚合（同 GET `/today`）

### `CoupleCeremonyController` `/couple/ceremony`（批次十九，F230-F239）
- GET `/overview` 今日仪式总览（小日子/黄历宜忌/补催/保险柜/续约/券本/双人体感/去年今日/加冕一次拉齐）
- POST `/founded` {name,startDay,repeatYear} 新建小日子（名≤60 字，默认每年重复）（F230）｜ POST `/founded/remove` {id} 删除（连带过法卡与打卡）
- POST `/ritual` {foundedId,content} 写过法任务卡（每小日子最多 3 条）（F232）｜ POST `/ritual/remove` {id} 划掉（连带其打卡）
- POST `/mark` {id} 庆祝打卡（当日幂等；该小日子全部过法打满推 both，否则推 TA）（F233）
- POST `/policy` {quote} 交本月保费=夸 TA 一句（一人一月一句可改写；双方交齐=满一月，满 3/6/12 月 payout 愿望券推 both）（F234）
- POST `/renew` {line} 续约日签字「我还是选你」（每满 100 天或周年当天，非续约日 400；双签推 both）（F235）
- POST `/coupon` {title} 发愿望券（券面≤80 字）｜ POST `/coupon/use` {id} 核销（OPEN→USED，一人说了算）（F236）
- POST `/recap` {day?,feeling} 此刻感觉（一人一天一句可改写不重推；双留推 both）（F239）
- GET `/chronicle?foundedId=` 小日子史册（一年一页，F237；黄历 F231、加冕 F238 为总览内聚合无表）
- 除 `/chronicle` 返回 ChronicleVO 外，其余全部返回 OverviewVO 聚合（同 GET `/overview`）

### `CoupleBoardController` `/couple/board`（批次二十，F240-F249）
- GET `/overview` 公司总览（任命/决议/述职/发薪/点子/签到/职级/名片/周报一次拉齐；F243 职级、F248 名片、F249 周报均聚合无表）
- POST `/role` {title} 给 TA 封职位（每人待任命最多 2 个）（F240）｜ POST `/role/appoint` {id} 被任命者本人盖章上任（非本人/已生效 400）
- POST `/vote` {title} 提交决议议案（F241）｜ POST `/vote/decide` {id,agree} 附议通过或一票否决（提案人不能裁自己的案；PASSED/VETOED 留痕推 both）
- POST `/report` {year,review,goal} 交年度述职+小目标（≤500/200 字，同年可改写；双提交才互见）（F242）
- POST `/salary` {thanks} 发本月感谢工资（一月一次；向 couple_point_ledger 插 EARN 5 分流，F186 台账复用）（F244）
- POST `/idea` {content} 投金点子（一句话≤140 字）｜ POST `/idea/adopt` {id} 采纳 TA 的点子自动生成决议转表决（自己点子 400）（F245）
- POST `/attend` 例会签到（10s 窗口内双签到=召开会议，只推一次 both）（F247）
- 写操作全部返回 OverviewVO 聚合（同 GET `/overview`）
