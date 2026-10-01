# 情侣空间 v6 迭代规格（F250-F349，100 功能，十个批次）

> 红线：不做照片/视频上传；重情绪价值；迁移幂等、H2/MariaDB 双兼容；utf8mb4_bin 用户名列 NOT NULL 无 DEFAULT；每批后端 ≥10 单测、前端 ≥3 用例。
> 版本目标：v6 全部交付后 → 1.6.0-alpha 阶梯。命名前缀防撞：新 VO/表/方法先 grep。
> 去重声明：与 F1-F249 的差异点在每行「备注」列；实现前必须先读 are-chat-map 第三节确认不复刻。

## 批次二十一 F250-F259 夫妻老黄历（中式岁时仪式） `/api/couple/almanac` V34

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F250 | 节气跟风机 | 24 节气到来当日一键记「今天跟上了」+晒一句话，双方互见 | couple_term_check uk(space,term,year,from_user) | 区别于 F231 黄历倒数（那是小日子列表） |
| F251 | 节气过法 | 给任一节气定一条固定过法（每节气≤2），当日打卡推 TA | couple_term_ritual uk(space,term)+限2 | 规则同 F232 过法卡但按节气维度 |
| F252 | 择吉日 | 给一件大事挑吉日：候选吉日 Bank 点评宜忌，双确认存「我们的日子」 | couple_lucky_day uk(space,day) | 大事=纪念日/领证/搬家类文本 |
| F253 | 农历生日换算 | couple_anniversary 增日历类型列，农历生日自动换算未来十年公历并提醒 | ALTER 加 calendar_type（普通列 DEFAULT 'SOLAR'） | F42 只支持公历 |
| F254 | 节日家档 | 8 大节日（元旦/除夕/情人节/520/七夕/中秋/国庆/周年月）每年怎么过：双提交成对照页 | couple_festival_plan uk(space,festival,year) | |
| F255 | 一节气一件事 | 24 节气手账：每节气一句话记当日小事，年末成一年日历 24 页 | couple_term_note uk(space,term,year,user) | 无表则无法分人，独立流水 |
| F256 | 生肖年运 | 双人星座/生肖相性年运（静态 Bank+stableHash），每年一键领取可重抽一次 | 无表 | F77 星座配对应答日维度娱乐 |
| F257 | 下个长假倒数 | 自动倒数下一个法定长假+「干什么」认领卡，一人写另一人可补 | couple_holiday_wish uk(space,holiday) | F94 倒数日是用户自建，此为内置法定假 |
| F258 | 反仪式感日 | 每年挑 3 天声明「这天什么都不做」，系统挡打卡推「偷得浮生」卡 | couple_normal_day uk(space,year,day) 3 位 | 防仪式感疲劳 |
| F259 | 一年日子小结 | 聚合 F250-F255 打卡率/吉日/手账成年度「我们的日子」长卷 | 无表 | |

## 批次二十二 F260-F269 倾听与发声（沟通） `/api/couple/listen` V35

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F260 | 想被听时段 | 申请 10 分钟「只听我说」时段，对方确认（10s 双签窗口复用），事后互评被听感 | couple_listen_slot uk(space,slot) | 区别于 F101 安静小屋（那是冷静） |
| F261 | 替我说 | 害羞的话写成「TA 口吻」草稿，TA 可改写后定稿存档 | couple_proxy_word | |
| F262 | 误会倒带卡 | 一次争执各写「我当时以为/你其实想」，双份齐并排回放 | couple_misrewind uk(space,topic) | F61 矛盾复盘是和解流程，此重认知差异 |
| F263 | 本周答不上来的问题 | 每周一问「最近什么难住你了」，双答互见，问题成库 | couple_stuck_q uk(space,week) | |
| F264 | 换位信 | 「我是你」写一封信给对方读，双交换定时拆 | couple_swap_letter | F162 醒来第一条是封存送达，此是身份互换 |
| F265 | 早想说 | 封存队列：想说的话先存，每 7 天自动放行一句送达 | couple_hold_word open_day | |
| F266 | 三行打卡 | 每日「今日印象/谢一件/夸一句」三行，连续 21 天解锁纪念 | couple_three_line uk(space,day,user) | F161 三行情书是情书体，此为日记 |
| F267 | 语气翻译官 | 觉得自己话太冷时自选语气（累/忙/没事），对方看到翻译条 | couple_tone_note uk(space,day,user) | |
| F268 | 休战旗 | 吵架先举旗方冻结 30 分钟，到点双方各选「继续/算了」 | couple_truce uk(space,active) | F320 冷冻期是长周期版 |
| F269 | 称呼日 | 今日专属爱称 Bank 抽取+使用说明，双方各「用过一次」完成当日 | couple_name_use uk(space,day) | F6 爱称是常驻，此为日抛娱乐 |

## 批次二十三 F270-F279 二人制造厂（生活协作 2.0） `/api/couple/factory` V36

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F270 | 家务轮盘 | 转盘分配本周家务（前端动画+结果双签落库），赖账进「欠账栏」 | couple_spin_task | F8 轮值是排班，此为游戏化分派 |
| F271 | 采买清单 | 超市清单共编，「我买了」推 TA，月终点名最多的人获「生活委员」 | couple_shop_item | F14 共享清单是愿望类 |
| F272 | 冰箱库存 | 食材文字库存（名/量/到期），临期推「今晚吃掉它」 | couple_stock uk(space,item) | 无图，纯文本 |
| F273 | 代拿快递 | 一单=「帮 TA 拿快递」打卡，攒感谢章，章可兑换 F186 积分奖励位 | couple_parcel | |
| F274 | 叫醒服务 | 定一句叫醒词+生效日，Job 到点把卡递给 TA（TA 手动递） | couple_wake_word | |
| F275 | 服药提醒链 | 登记在服药物（文本时段），到点 TA 点「提醒了」本人点「吃了」成链 | couple_medicine | F26 暗号本无关 |
| F276 | 久坐互拍 | 「站起来」一键推 TA，1 小时内双起算「同起」积 1 | couple_standup | |
| F277 | 垫付本 | 大项支出记「谁垫付/还了没」，清账推 both「无债一身轻」卡 | couple_advance | F5 记账是日常流水，此为互欠结算 |
| F278 | 逛超市战利品 | 本周各自采购 5 件互猜「为什么买」，猜对计分 | couple_grocery | |
| F279 | 家安月检 | 每月水电气锁门窗清单，双人才算检完；漏月推提醒 | couple_home_check uk(space,month) | |

## 批次二十四 F280-F289 我们百科（默契资产化） `/api/couple/codex` V37

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F280 | 词条共建 | 「我们的词」定义/出处/现行用法，双人共编（改后推 TA） | couple_codex_entry | F78 恋爱词典是造词，此为考据 |
| F281 | 默契综艺 | 从词条自动出 5 题填空考，双方同答算默契率，历届对比 | couple_quiz_show | |
| F282 | 喜好 TOP10 | 各类目「TA 的十大」互猜，揭榜后差异项成「重新认识清单」 | couple_top_list+couple_top_guess | |
| F283 | 外号考据 | 每个爱称的诞生故事（谁起/场合/首次使用），存入百科 | couple_petname_story | F6 爱称是功能，此为档案 |
| F284 | 友情测验 | 定期发「TA 好友名字/父母生日」类题，错则 7 天后可补考 | couple_exam | |
| F285 | 去过的地方 | 地名+年份+发生了什么+一句评价，年末「足迹年表」 | couple_place | F75 旅行心愿是未来，此为已去过 |
| F286 | 第一眼对视 | 双方盲提交「你注意到我是哪一刻」，一致或各满 3 次自动互见 | couple_first_look | F46 firsts 是事件列表，此为双盲 |
| F287 | 习惯图鉴 | 记录观察到的 TA 小习惯+频率标签，TA 可标「确实/冤枉」 | couple_habit_map | |
| F288 | 口味变迁 | 「以前不爱现在爱」记录，5 年成口味演化图（文本时间线） | couple_taste_shift | |
| F289 | 人格双报 | 8 题速测（静态卷），双方类型+差异解读卡，每年可重测 | couple_type_report | F172 爱语测评五爱语；此为性格向 |

## 批次二十五 F290-F299 明日邮局（梦想与未来） `/api/couple/post` V38

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F290 | 五年后新年卡 | 每年除夕写一张，5 年后当日自动送达（年年接续成邮筒） | couple_year_oath uk(space,year) | F84 胶囊是自选远期，此为年度仪式 |
| F291 | 人生大事进度 | 大事（买房/婚礼/生子…）拆步+进度%，TA 可补「进展章」 | couple_bucket+couple_bucket_step | F10 愿景板是一词共鸣 |
| F292 | 总得有一天拍卖 | 「改天一定」上拍，对方 7 天内在货架认领排期，逾期自动下架 | couple_someday | F79 下次一定是一件事，此为排期制 |
| F293 | 想象中的家 | 字段化（房间/窗帘/阳台/味道）填梦想家，每年双对照「距我家 +x%」 | couple_dream_home uk(space,year) | |
| F294 | 退休计划双写 | 30/40/50 岁各写「我们在干嘛」，双写完成标分歧点高亮 | couple_retire_plan uk(space,age,user) | |
| F295 | 许愿井周问 | 每周「如果我们无所不能，先做什么」双写，攒满一年回看 | couple_well_qa uk(space,week) | |
| F296 | 时光胶囊接龙 | 各为 TA 写未来 3 年，每年开一笔（错开），开完解锁下一笔 | couple_relay_capsule | |
| F297 | 解梦局 | TA 记梦投稿「解梦官」（对方一本正经胡说），点评入库成集 | couple_dream_case | F141 梦境手账是自己记，此为互评 |
| F298 | 周年愿望台账 | 每周年愿望登记，次年核对实现度打「圆上了/鸽了」章 | couple_anniv_wish | |
| F299 | 未来信用卡 | 「承诺未来小事」获积分提额度，兑现涨/逾期降，额度梗文案 | couple_future_credit | F71 存折是记录当下，此为预支 |

## 批次二十六 F300-F309 今日我是别人（扮演剧场） `/api/couple/theater` V39

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F300 | 今日身份签 | 双方抽同一池身份（老板/顾客/猫…），按 Bank「相处指南」相处一天，日终互评 | couple_role_day uk(space,day) | |
| F301 | 一日互换日记 | 互换日各写一页「作为 TA」，双成交换时间点互见 | couple_swap_diary | F264 是信，此为日记体 |
| F302 | 师徒日 | 本周随机师徒：徒每日 3 次「侍奉」打卡，师给评语，期满定级 | couple_master_day | |
| F303 | 时空电话亭 | 给一年前/后的自己留言，到周年回放给 TA 听（读），错答「信号不好」彩蛋 | couple_booth_note | |
| F304 | 黑话大全 | 只有两人懂的梗+出处+用法，定期抽查「谁先忘」 | couple_private_ref | |
| F305 | 奥斯卡 | 互提「今日最佳演技」一句话证据，年末最佳男女主颁奖 | couple_act_award | |
| F306 | 如果我是你爸妈 | 脑洞问答：想象对方家长拷问，双方作答互见（20 题 Bank 按日抽） | couple_if_family uk(space,day) | F173 如果问答池不含家庭向 |
| F307 | 双角色追剧 | 同一部剧各认领一角写角色日记，剧终合成「双视角剧本」 | couple_role_movie | F76 追剧清单是进度 |
| F308 | 今日客服 | TA 下一单（合理小事），30 分钟内响应，服务评分可差评申诉 | couple_service_ticket | |
| F309 | 冷知识颁奖礼 | 年末从全年数据评「最佳反问/最稳打脸」等趣味奖（Bank 提名模板） | 无表 | |

## 批次二十七 F310-F319 身体通知系统（健康关照 2.0） `/api/couple/body` V40

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F310 | 体征互报 | 体温/体重/睡眠时长文本互报，异常日（自设阈值）自动推 TA | couple_body_metric uk(space,day,user) | F221 睡眠单是感受星级，此为数值 |
| F311 | 呼噜自报 | 晨起自报打鼾档位 4 级，TA 可补「震感」点评 | couple_snore uk(space,day) | |
| F312 | 周期共览 | 生理周期两人共同日历视图+双人不适照顾链（本人标记，TA 递卡） | couple_cycle_log | F8x 生理期关怀是单方关怀，此为共览 |
| F313 | 戒烟戒糖互助营 | 双设目标：破戒记录+陪绑方安慰词，营期天数里程碑 | couple_quit_camp | |
| F314 | 运动链 | 结对俯卧撑/深蹲计数 PK，断链宽限 30 分钟，周榜 | couple_fit_chain | F223 喝水接力是喝水，此为运动 |
| F315 | 身体不适 SOS | 一键「不舒服」+症状+从何时，TA 收「能做什么」选项卡（Bank 话术） | couple_body_sos | F144 情绪 SOS 是情绪，此为躯体 |
| F316 | 忌口红线本 | 各自过敏/忌口清单；饭桌 F210 饭票池自动标红含忌口菜 | couple_diet_redline | 与 dining 联动：读同一表拼标 |
| F317 | 体检陪同 | 约体检+「虚拟陪同」到场打卡+检后一句话报告互见 | couple_checkup | |
| F318 | 情绪药友 | 自愿登记「最近药怎么样」周报式互助（非医嘱，陪伴话术 Bank） | couple_mood_med | |
| F319 | 早睡军令状 | 双签本周熄灯线（时点），违约率公示接 F220 熄灯数据 | couple_sleep_oath uk(space,week) | |

## 批次二十八 F320-F329 修复车间（安全感与修复） `/api/couple/repair` V41

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F320 | 冷冻解冻规程 | 吵架挂「冷冻中」（3-24h 自选），解冻需双人签+三问流程走完才解冻 | couple_freeze uk(space,active) | F268 休战旗是 30 分钟短停 |
| F321 | 道歉质检 | 道歉信按六要素自评+对方验货，不合格退回重写，合格进陈列室 | couple_sorry_review | F62 道歉券是凭证，此为质量 |
| F322 | 重来卡 | 每季 1 张：「那段对话重放一遍」，用后记重放满意度 | couple_redo_card uk(space,quarter) | |
| F323 | 信任重建 30 天 | 大事后开重建计划：每日任务卡+双方签+周复盘，断签可续 | couple_rebuild_plan | |
| F324 | 和好了倒计时 | 冷战发倒计时（10-60 分钟），对方可暂停/提前，到点自动递台阶卡 | couple_makeup_count | |
| F325 | 冲突类型年报 | 全年争吵主题/时长/和好速度聚合，发「最佳和解奖」 | 无表 | 读 F61/F320-F324 数据 |
| F326 | 底线声明卡 | 各写 3 条底线（文本）+生效日，被踩后需「红线记录」说明 | couple_bottom_line uk(space,user,slot) | |
| F327 | 我错了榜 | 年度「认错次数+最感人认错方式」榜（认错需一句具体说明） | couple_admit_log | |
| F328 | 修复礼盒 | 和好达成掉礼盒，开出补偿小任务（Bank），完成推 both | couple_repair_box | |
| F329 | 和平纪念碑 | 每次大和好保存一句「这段吵架最代表性的话」，周年回看 | couple_peace_line | |

## 批次二十九 F330-F339 两家与朋友（情侣看世界） `/api/couple/world` V42

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F330 | 拜访攻略 | 回谁家前置任务卡（带什么/聊什么/雷区）双确认，回访后「战报」 | couple_visit_plan | |
| F331 | 送礼互助池 | 收集「TA 亲友可能喜欢」，可接单代买；节前排雷 | couple_gift_pool | F73 心愿互换是给彼此 |
| F332 | 朋友视角问卷 | 3 题「外人怎么看我们」，TA 线下问友回填，出「他观」卡 | couple_friend_view | |
| F333 | 官宣日 | 每月可发一张官宣卡（纯文字+日期），推 TA 进通知，成「官宣编年」 | couple_declare uk(space,month) | |
| F334 | 文案代写 | TA 朋友圈文案求写：各交 3 候选互评选稿，定稿进百科 | couple_caption | |
| F335 | 进城接待方案 | TA 来访城市：行程/交通/「陪同小包」清单模板共建，存接待手册 | couple_city_plan | F112 见面倒数是情绪，此为攻略 |
| F336 | 亲戚称呼册 | TA 亲戚称谓关系测验，错题进「考前强化」 | couple_relatives_q | |
| F337 | 社会信用 | 公开声明「我保证不做…」+TA 见证+到期解除或「塌房」记录 | couple_vow_credit | F125 信任存折是积累，此为承诺 |
| F338 | 群聊记者 | 「今天群里最好笑的是我们…」每日一条互递素材 | couple_group_report uk(space,day) | |
| F339 | 代 TA 赔礼 | 与 TA 亲友有误会：代写赔礼信，TA 审阅通过才算送达 | couple_proxy_apology | F261 替我说是表白向 |

## 批次三十 F340-F349 传世系统（年度与资产化收官） `/api/couple/legacy` V43

| F | 功能 | 说明 | 数据 | 备注 |
|---|---|---|---|---|
| F340 | 年度十问 | 每年 12-31 固定 10 问双答，跨年 diff 视图看变化 | couple_year_ten uk(space,year,user) | |
| F341 | 记忆库年审 | 年检「最想删/最想留」各限 3 条，归档审计意见 | couple_vault_audit | 读回忆资产系各表 |
| F342 | 续约发布会 | 年度「发布会」各发言稿，对方按 Bank 评分卡打分 | couple_renew_speech uk(space,year,user) | F235 续约是 100 天短句 |
| F343 | 里程碑倒推 | 输入目标（1000 次熄灯），按当前速率算预计达成日+加速建议 | 无表 | |
| F344 | 恋爱汇率 | 「1 个亲亲=5 个抱抱」双改汇率，年末趣味结算 | couple_love_fx uk(space,user) | |
| F345 | 情侣品牌 | 关系命名+slogan+「产品简介」，发布后显示在空间头部 | couple_brand uk(space) | F11 个性化是主题皮肤 |
| F346 | 我们的一年 | 一键年度盘点：聚合十批数据成文字年报（非模板套话，含真数字） | couple_year_review uk(space,year) | F85/F99 月报周年报是单域 |
| F347 | 传世清单 | 「想留给你」条目（含地点/口令类），双签封存 | couple_legacy | F84 胶囊是信，此为清单 |
| F348 | 周年抽奖箱 | 奖池=当年攒的迷你愿望（积分位），周年 Job 提醒双方抽奖 | couple_anniv_draw | |
| F349 | 空间等级 | 全空间互动计数定级 1-99 +年度称号（Bank 门槛），读时计算无缓存 | 无表 | |

## 实施节奏

- 每批：V 迁移(+schema.sql 同步) → 后端包（实体/Mapper/Bank/Service/Controller+单测）→ commit 分组（db/feat/test）→ 前端 agent（组件自持数据模式+api 分组+types 前缀防撞+≥3 用例+registry+skill）。
- 批次内聚合读接口一律 overview/today 一次拉齐，写接口返回整份聚合 VO。
- 事件名域前缀：term-/listen-/spin-/codex-/post-/role-/body-/repair-/world-/legacy-。
