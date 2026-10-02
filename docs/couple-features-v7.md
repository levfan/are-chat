# 情侣空间 v7 迭代规格（F350-F399，50 功能，五个批次）

> 红线：不做照片/视频上传；重情绪价值；迁移幂等、H2/MariaDB 双兼容；utf8mb4_bin 用户名列 NOT NULL 无 DEFAULT；每批后端 ≥10 单测、前端 ≥3 用例。
> 版本目标：v7 全部交付后 → 1.6.0-alpha 递增，收官 1.6.0-rc.1。命名前缀防撞：新 VO/表/方法先 grep。
> 去重声明：与 F1-F349 的差异点在每行「备注」列；实现前必须先读 are-chat-map 第三节确认不复刻。
> 五批总命题：**把「被爱」变成可翻阅的证据（回音壁）→ 把「注意力」变成可储蓄的礼物（注意力保护区）→ 把「陪伴」铺到人生关卡上（人生关卡）→ 把「随口一说」变成算数（聆听者）→ 把「大笑」变成应急储备金（欢笑银行）。**

## 批次三十一 F350-F359 回音壁（被爱的证据库） `/api/couple/echo` V44 `couple_echo_*`

情绪命题：低落时人不信「被爱」，只信「证据」。把证据存进墙，难过时一键取。

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F350 | 好事簿 | 记「TA 为我做的一件好事」（≤80 字+日期），存进对方的「你被爱的证据」页；对方可点「这条救过我」加星 | couple_echo_deed(id,space,from_user 记录人,about_user 证据主角,content,day,starred) | F54 夸夸墙是「说的话」，此是「做的事」；F151 感恩便签是泛感谢，此定向 TA |
| F351 | 能量补给 | 低落时一键「给我一点能量」：随机翻 3 条我的被爱证据+双方鼓励语各 1 条+1 条高光重放，并可一键喊 TA（echo-refill 事件推 TA） | 无表，读时聚合+单次推送 | F60 求抱抱是按钮+话术卡，本功能翻证据库，联动 F350/F352/F355 |
| F352 | 鼓励语罐 | 每人预存 ≤5 条「给 TA 的打气话」（≤60 字），补给时随机取 | couple_echo_juice uk(space,from_user,idx) | F54 夸夸墙是即时贴，此是预存弹药 |
| F353 | 被爱日历 | 每天一格：有新证据/加星/补给领取则点亮，按年聚合 | 无表，读时聚合 | F96 热力日历聚合心情/存折，本项聚合 echo 域 |
| F354 | 感谢慢递 | 想谢 TA 的话封存 7 天后送达（echo-thanks-arrived），在途每人 ≤3 | couple_echo_slow uk(space,from_user) 每行一条带 open_day | F162 醒来第一条是次日，F84 胶囊是远期自选，此固定 7 天 |
| F355 | 高光重放 | 收藏「我们最好的瞬间」三行（什么时候+我们做了什么+我的感觉），每人 ≤12 条，补给时随机重放 | couple_echo_highlight uk(space,from_user,idx) | F36 心动时刻是时间轴流水，此是精选收藏且联动补给 |
| F356 | 夸夸回执 | 对 F54 夸夸墙里夸我的句子点「收到」，对方见「已送达」，回执句进能量库 | couple_echo_receipt uk(space,quote_id,from_user)，跨模块只读 couple_praise | F54 的闭环补全，不新增夸夸入口 |
| F357 | 电量预报 | 每天自报社交电量 1-5 格+「今天想被怎样对待」一句；≤2 格时对方总览提示「今晚轻轻的」 | couple_echo_battery uk(space,day,user) | F267 语气翻译是当下状态，此是当日预告；F51 情绪天气是心情分 |
| F358 | 写给低落的自己 | 给「下次 emo 的自己」写信（≤300 字），只在本人点开补给时可读；一封在途，读完可再写 | couple_echo_self_letter uk(space,from_user) 单行 status | F84 胶囊是给未来的双方，此是给当下低落的自己 |
| F359 | 回音壁年报 | 聚合证据数/星标数/补给次数/慢递送达数/夸夸回执率 → Bank 文案 | 无表 | 与 F85/F99/F346 的年报区分域 |

## 批次三十二 F360-F369 注意力保护区（把不被手机抢走的注意力还给彼此） `/api/couple/focus` V45 `couple_focus_*`

情绪命题：注意力是当代最贵的礼物；「我在看你」比「我爱你」稀缺。

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F360 | 专注打卡 | 每晚自报「今晚放下手机陪了 TA 多少分钟」（0-180），双报当夜点亮 | couple_focus_night uk(space,day) minutes_a/minutes_b+note 各一 | F220 熄灯是睡前仪式，此报专注时长 |
| F361 | 专属时段 | 每周预约 2 小时「只属于我们」（写做什么），对方确认生效，开始前 1h 推提醒 | couple_focus_slot uk(space,week) | F260 想被听是 10 分钟倾诉，此是共处时光 |
| F362 | 攒一句话 | 对方专注/勿扰时留言排队（≤80 字），TA 结束后一键收全部并回执已读 | couple_focus_queue(space,to_user,content,read_at) 每人在途 ≤5 | F198 免打扰是屏蔽，F265 早想说是 7 天放行，此即时攒发 |
| F363 | 饭桌不低头 | 吃饭手机倒扣 20 分钟一键打卡，双打=同桌成功，连击记录 | couple_focus_meal uk(space,day) | F210 饭票管「吃什么」，此管「怎么吃」 |
| F364 | 对视十秒 | 每天一次对视打卡，双打点亮；月度点亮数聚合 | couple_focus_gaze uk(space,day) | F177 同频共振是按键手感，此是眼神仪式 |
| F365 | 不插电半小时 | 睡前 30 分钟无手机共同打卡，周连击 | couple_focus_unplug uk(space,day) | 与 F360 区分：F360 报时长，此是纯打卡仪式 |
| F366 | 走神温柔哨 | 一天 2 张额度递「回来啦」卡，防唠叨限流 | couple_focus_nudge uk(space,day,from_user) | F276 久坐互拍是健康，此是注意力 |
| F367 | 专注周报 | 周一锚：专注分钟/同桌次数/对视/不插电连击 → Bank 文案 | 无表 | 与 F179 默契周报区分域 |
| F368 | 数字排毒半天 | 周末发起半日无手机挑战，对方应战，双达成收「清净半天」纪念 | couple_focus_detox uk(space,day) | F361 是预约，此是即时对抗 |
| F369 | 注意力年报 | 年度聚合「为彼此放下的手机小时数」+最专注的一天 | 无表 | |

## 批次三十三 F370-F379 人生关卡（TA 的大日子，我在场） `/api/couple/quest` V46 `couple_quest_*`

情绪命题：「你的人生大事，我不缺席」做成系统，而不是靠记性。

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F370 | 关卡预告 | 宣布 Boss 战（类型：面试/汇报/答辩/谈判/体检/其它+日期+一句怯场），TA 自动收准备提醒 | couple_quest_battle uk(space,from_user,day,name≤30) 在途每人 ≤3 | F291 人生大事是拆步长线，此是单场战斗 |
| F371 | 出关战报 | 战后报 WIN/LOSE/SURVIVE+一句感受；TA 按结果盖庆功章/抱抱章/幸亏章 | couple_quest_report uk(battle) 一战一报 | F148 自定义成就是颁奖，此是单场收尾 |
| F372 | 加班预报 | 今晚加班到几点预报 → TA 收「别等饭」，可留一张「到家灯给你留着」卡 | couple_quest_overtime uk(space,day,user)+lamp 字段 | F268 BUSY 是当下，此是当晚预报+留灯卡 |
| F373 | 生病陪护单 | TA 生病开单：喝水/吃药由陪护人代记打卡+病中留言，痊愈日关单庆典 | couple_quest_nurse uk(space,patient,open_day) 在途每人 ≤1 | F318 服药链是日常慢病，此是急性期陪护 |
| F374 | 考试周静音舱 | TA 入舱至某日；我只发加油卡（每日 ≤1 白名单通道），出舱日提醒我补一封长信 | couple_quest_pod uk(space,from_user,until_day)+cheer 计数 | F198 免打扰是全局时段，此是单人舱+白名单仪式（不真屏蔽私信） |
| F375 | 搬家互助 | 打包 8 区块分工认领+纸箱计数+「新家第一晚」庆祝打卡 | couple_quest_move uk(space,item) | F328 体检陪同是单日到场，此是多日分工 |
| F376 | 低谷通行证 | TA 宣布「最近状态不好」（7-30 天）：对方每日一张「不说话也行」卡，TA 主动定回升日收尾 | couple_quest_valley uk(space,from_user,open_day,until_day) | F61 矛盾复盘是冲突后，此是情绪低谷期 |
| F377 | 小胜利账本 | 每天记一件做成的小事（≤40 字），周日互颁「小赢奖」（选对方本周最佳一条） | couple_quest_win uk(space,day,user)+award_day | F159 优点存折是特质，此是当日成就 |
| F378 | 关卡成就墙 | 年度聚合：Boss 战数/通关率/陪护天数/静音舱数 → Bank 称号 | 无表 | |
| F379 | 下次关卡预约 | 把未来 60 天已知关口挂双人时间轴，对方点「我会到场」 | couple_quest_upcoming uk(space,day,title≤30) | F94 倒数日是私人倒数，此是双人应援位 |

## 批次三十四 F380-F389 聆听者（随口说的都算数） `/api/couple/catch` V47 `couple_catch_*`

情绪命题：爱是「你随口一说，我一直记得」。

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F380 | 暗中心愿本 | 偷偷记 TA 随口提过想要的（内容+出处日期+场景），对 TA 保密；兑现登记后才揭晓「你 X 月 X 日说过」 | couple_catch_wish uk(space,owner,content≤60) revealed_at 空=保密 | F73 心愿互换是公开许愿，F331 送礼池是互派任务，此是暗中捕捉 |
| F381 | 雷区探测器 | 提前挂出易吵话题（话题+我的雷点+安全说法），对方盖「已知晓」章 | couple_catch_mine uk(space,from_user,topic≤30) | F326 底线卡是原则声明，此是话题预警+安全做法 |
| F382 | 安全词 | 双方各约一个暂停词；使用时记录（哪天+事后一句复盘），月度统计 | couple_catch_safeword uk(space,from_user) + use log uk(space,day,user) | F268 休战旗是吵架中机制，此是预防约定与事后复盘 |
| F383 | 敏感日历 | 给 TA 的敏感日提前标注（周期第一天/考核日/忌日等+当天想被怎样对待），前 1 天提醒我 | couple_catch_sensitive uk(space,owner,day,kind) | F312 周期共览是标记当天，此是提前预告+照护方案 |
| F384 | 「说到哪了」 | 被打断的话题存档（话题一句+进度一句），续完销档，在途每人 ≤5 | couple_catch_thread uk(space,from_user,status) | F63 话题卡管开场，此管断点续聊 |
| F385 | 真话翻译机 | 本人申报口是心非词条（我说=实际意思），对方只见结果不可改 | couple_catch_say uk(space,from_user,say≤20) | F305 黑话是圈内梗，此是自我申报的反话 |
| F386 | 聆听方式协议 | 各写「我难过时要的是」五选一（讲道理/陪骂/抱抱不说话/递吃的/别理我）+补充说明 | couple_catch_protocol uk(space,from_user) | F171 爱语测评管接收爱，此管接收安慰 |
| F387 | 话题许愿池 | 「希望我们多聊 XX」，对方接单，一周内聊完+一句感想 | couple_catch_topic uk(space,from_user,title≤30) | F63 是系统出题，此是人出题 |
| F388 | 今日一句话 | 每天给对方留一句想说的话（≤40 字）；没留则显示昨天那句+「今天还没说」 | couple_catch_daily uk(space,day,user) | F265 早想说是封存队列，此是当日直说 |
| F389 | 聆听者年报 | 捕捉兑现数/安全词使用/雷区避雷数/话题完成率聚合 | 无表 | |

## 批次三十五 F390-F399 欢笑银行（幽默是关系的复利） `/api/couple/laugh` V48 `couple_laugh_*`

情绪命题：一起大笑过的时刻，是关系的应急储备金。

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F390 | 笑点存档 | 笑到肚子疼的时刻（谁干的+现场还原 ≤100+好笑度 1-5），对方可补「现场证词」 | couple_laugh_moment uk(space,day,from_user,title≤30) | F83 语录是话，此是事件 |
| F391 | 每日一逗 | 每天一方负责逗笑对方（周轮换），对方判 笑/没笑/强撑，判完互见 | couple_laugh_daily uk(space,day) owner+verdict | F158 早安能量是单向输出，此双人判分 |
| F392 | 冷笑话结冰榜 | 互发冷笑话，对方判「结冰」，年度结冰最多者获「冷场之王」 | couple_laugh_joke(space,from_user,content≤80,verdict) | F391 是每日仪式，此是自由对轰 |
| F393 | 尴尬回收站 | 社死时刻提交（≤100 字），对方盖「抱抱你」章；**365 天后读时结算转「好笑的事」** | couple_laugh_cringe(space,from_user,day,content) healed 读时算 | 时间治愈机制，全新 |
| F394 | 快乐突袭 | 突发一串夸奖/一个梗/一段回忆杀，对方「中弹」盖章，月度中弹榜；一天一突袭 | couple_laugh_attack uk(space,from_user,day) | F90 贴贴是 IM 动作，此是空间内突袭 |
| F395 | 笑点默契考 | 同一梗两人各自预判对方笑不笑，双判一致=默契+1 | couple_laugh_guess uk(space,joke,from_user) | F281 默契综艺考词条，此考笑点 |
| F396 | 大笑处方 | 对方低落时开处方（指定翻看某条笑点/尴尬/突袭），对方「已服用」回执 | couple_laugh_rx uk(space,from_user,day) 指向 moment/cringe/attack | F351 翻被爱证据，此翻笑点 |
| F397 | 幽默风格图鉴 | 自评+互评幽默类型（谐音梗/冷幽默/自嘲/动作派/模仿派），差异出相处建议 | couple_laugh_style uk(space,about_user,rater) | F171 爱语测评同构不同域 |
| F398 | 欢乐周报 | 周一锚：笑点存档/一逗成功率/结冰数/中弹数 → Bank 文案 | 无表 | |
| F399 | 年度欢笑榜 | 年度聚合：最好笑一条/笑声贡献王/结冰王/中弹王 → 「我们的喜剧奖」 | 无表 | 收官功能 |

## 实施约定（各批一致）

- 挂载页签：echo→care/rescue 末尾；focus→growth 末尾；quest→promises 末尾；catch→letters/send 末尾；laugh→rituals/fun 末尾。
- 前端命名：api 分组 echoApi/focusApi/questApi/catchApi/laughApi（方法名 echo*/focus*/quest*/catch*/laugh* 前缀）；类型前缀 CoupleEcho*/CoupleFocus*/CoupleQuest*/CoupleCatch*/CoupleLaugh*（先 grep 防撞）；testid 前缀 couple-echo-* 等；组件 CoupleEcho.vue 等；全部组件自持数据（无 store、无 WS case，推送仅进 notify）。
- 后端分层：照抄第三节模板；静态内容进 Bank（CoupleEchoBank 等）只增不改；新 WS 事件统一 echo-*/focus-*/quest-*/catch-*/laugh-* 前缀；心动值台账 EARN 时机：复温类高难动作才记分，新增功能默认不记分（防通胀）。
- 提交节奏：每批 = db 独立 commit + feat 后端 + test 后端 + feat 前端 + test 前端 + docs 双仓；前端每批升 1.6.0-alpha.n，收官 1.6.0-rc.1。
