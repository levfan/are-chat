# 情侣空间功能裁剪 · 全量排序与保留 50 清单

> 生成日期：2026-10-04
> 输入：前端功能卡索引 `are-chat-web/src/components/couple/coupleCards.registry.ts` 的 **174 张卡**（F1-F399 累计交付的功能卡）
> 输出：留 **50** 张、删 **124** 张。本文档是删除动作的唯一依据，任何一张卡的去留都以本表为准。

## 一、打分口径

五个维度各 0-5 分，总分 25。分高的先留。

| 维度 | 简称 | 问的是什么 |
|---|---|---|
| 操作轻简 | 操 | 一次点击 / 一句话能不能完成？要不要填表、要不要凑两人同时？ |
| 吸引兴趣 | 钩 | 有没有"想点开看看"的理由？随机性、悬念、比拼、拆封感都算钩子 |
| 情绪价值 | 情 | 做完之后心里是暖的还是空的？能不能被 TA 感觉到 |
| 具体不虚 | 落 | 有没有落到一条真实记录 / 一件真会去做的事上？"灵魂契合度""关系体检"这类打不了分 |
| 日常频次 | 常 | 一周用得上几次？只在搬家、过年、考试周用的必然低 |

标注：**（点名必留）** = 用户明确圈定必须保留；**（积分重接）** = 保留下来并作为积分体系的新赚分入口。

## 二、排名表（174 张，按总分降序）

第 50 名与第 51 名同为 18 分，切线落在并列分内，见第三节的人工裁决。

| # | 功能卡 | 所属模块 | 操 | 钩 | 情 | 落 | 常 | 总分 | 去留 |
|---:|---|---|---:|---:|---:|---:|---:|---:|---|
| 1 | 求抱抱 · `couple-comfort` | care | 5 | 5 | 5 | 4 | 5 | **24** | 留 |
| 2 | 今晚饭桌（饭票+吃什么裁决） · `couple-dine-today` | dining | 5 | 5 | 4 | 5 | 5 | **24** | 留 |
| 3 | 家务轮盘 · `couple-fy-spin` | factory | 5 | 5 | 4 | 5 | 5 | **24** | 留 **（积分重接）** |
| 4 | 加班预报与留灯 · `couple-quest-overtime` | quest | 5 | 4 | 5 | 5 | 5 | **24** | 留 **（点名必留）** |
| 5 | 贴贴宫格 · `couple-bond` | bond | 5 | 4 | 5 | 4 | 5 | **23** | 留 |
| 6 | 今日一句话 · `couple-catch-daily` | catch | 5 | 3 | 5 | 5 | 5 | **23** | 留 |
| 7 | 安全词与暂停复盘 · `couple-catch-safeword` | catch | 5 | 4 | 5 | 5 | 4 | **23** | 留 **（点名必留）** |
| 8 | 今日真心话 · `couple-deep` | talk | 4 | 5 | 5 | 4 | 5 | **23** | 留 |
| 9 | 电量预报 · `couple-echo-battery` | echo | 5 | 4 | 5 | 4 | 5 | **23** | 留 |
| 10 | 好事簿（被爱的证据） · `couple-echo-deed` | echo | 5 | 3 | 5 | 5 | 5 | **23** | 留 **（积分重接）** |
| 11 | 对视十秒 · `couple-focus-gaze` | focus | 5 | 5 | 5 | 4 | 4 | **23** | 留 **（点名必留）** |
| 12 | 和好与道歉券 · `couple-makeup` | makeup | 4 | 5 | 5 | 5 | 4 | **23** | 留 |
| 13 | 心情日记 · `couple-mood` | mood | 5 | 3 | 5 | 5 | 5 | **23** | 留 |
| 14 | 刮刮乐与盲盒 · `couple-surprise` | surprise | 5 | 5 | 4 | 4 | 5 | **23** | 留 **（积分重接）** |
| 15 | 暗中心愿本 · `couple-catch-wish` | catch | 4 | 5 | 5 | 4 | 4 | **22** | 留 |
| 16 | 愿望券本 · `couple-cere-coupon` | ceremony | 4 | 4 | 5 | 5 | 4 | **22** | 留 **（点名必留）** |
| 17 | TOP10 互猜 · `couple-cx-top` | codex | 4 | 5 | 5 | 4 | 4 | **22** | 留 **（点名必留）** |
| 18 | 能量补给 · `couple-echo-refill` | echo | 5 | 4 | 5 | 4 | 4 | **22** | 留 |
| 19 | 饭桌不低头 · `couple-focus-meal` | focus | 5 | 4 | 4 | 5 | 4 | **22** | 留 |
| 20 | 那年今天 · `couple-on-this-day` | chronicle | 5 | 4 | 5 | 4 | 4 | **22** | 留 |
| 21 | 情绪天气与急救箱 · `couple-care` | care | 4 | 3 | 5 | 4 | 5 | **21** | 留 |
| 22 | 我们的餐厅 · `couple-dine-restaurant` | dining | 4 | 4 | 4 | 5 | 4 | **21** | 留 **（点名必留）** |
| 23 | 走神温柔哨 · `couple-focus-nudge` | focus | 5 | 4 | 4 | 4 | 4 | **21** | 留 |
| 24 | 默契考验比划猜 · `couple-fun-talk` | comm | 4 | 5 | 4 | 4 | 4 | **21** | 留 |
| 25 | 爱情花园（玫瑰+幸运签） · `couple-garden` | garden | 4 | 4 | 4 | 4 | 5 | **21** | 留 |
| 26 | 社死往事（满一年转好笑） · `couple-laugh-cringe` | laugh | 4 | 5 | 5 | 4 | 3 | **21** | 留 |
| 27 | 冷笑话结冰榜 · `couple-laugh-joke` | laugh | 4 | 5 | 4 | 4 | 4 | **21** | 留 |
| 28 | 早晚安打卡 · `couple-rituals` | ritual | 5 | 3 | 4 | 4 | 5 | **21** | 留 |
| 29 | 匿名树洞与情话罐 · `couple-whisper-box` | talk | 4 | 5 | 5 | 3 | 4 | **21** | 留 |
| 30 | 雷区探测器 · `couple-catch-mine` | catch | 4 | 4 | 4 | 4 | 4 | **20** | 留 |
| 31 | 我们的小日子 · `couple-cere-founded` | ceremony | 3 | 4 | 5 | 4 | 4 | **20** | 留 |
| 32 | 恋爱编年史（考古卡+问答机） · `couple-chronicle` | chronicle | 4 | 3 | 5 | 5 | 3 | **20** | 留 |
| 33 | 被爱日历 · `couple-echo-calendar` | echo | 5 | 3 | 4 | 4 | 4 | **20** | 删 |
| 34 | 我们的第一次 · `couple-firsts` | memory | 3 | 4 | 5 | 5 | 3 | **20** | 留 |
| 35 | 笑点存档（现场证词） · `couple-laugh-moment` | laugh | 4 | 4 | 4 | 4 | 4 | **20** | 留 |
| 36 | 周年抽奖箱 · `couple-legacy-draw` | legacy | 5 | 5 | 5 | 3 | 2 | **20** | 留 **（点名必留）** |
| 37 | 悄悄话信箱 · `couple-letters` | letter | 4 | 3 | 5 | 4 | 4 | **20** | 留 |
| 38 | 误会倒带 · `couple-ls-misrewind` | listen | 4 | 4 | 5 | 4 | 3 | **20** | 留 **（点名必留）** |
| 39 | 趣味小游戏 · `couple-play` | play | 4 | 5 | 4 | 3 | 4 | **20** | 留 |
| 40 | 生病陪护单 · `couple-quest-nurse` | quest | 4 | 3 | 5 | 5 | 3 | **20** | 留 |
| 41 | 恋爱时光轴 · `couple-timeline` | couple | 5 | 2 | 4 | 5 | 4 | **20** | 删 |
| 42 | 心愿互换板（+下次一定） · `couple-wish-board` | growth | 3 | 4 | 5 | 4 | 4 | **20** | 留 |
| 43 | 里程碑徽章墙 · `couple-badges` | memory | 5 | 2 | 4 | 4 | 4 | **19** | 留 |
| 44 | 时光胶囊 · `couple-capsules` | capsule | 3 | 4 | 5 | 5 | 2 | **19** | 留 |
| 45 | 倒数日 · `couple-countdowns` | countdown | 4 | 2 | 3 | 5 | 5 | **19** | 留 |
| 46 | 今日体温同步（同熄灯+抱抱计量） · `couple-cozy-today` | cozy | 4 | 3 | 4 | 4 | 4 | **19** | 留 |
| 47 | 鼓励语罐 · `couple-echo-juice` | echo | 4 | 3 | 4 | 4 | 4 | **19** | 留 |
| 48 | 心动时刻 · `couple-heart-moments` | heart | 4 | 3 | 4 | 4 | 4 | **19** | 留 |
| 49 | 年度热力日历 · `couple-heatmap` | today | 5 | 3 | 4 | 4 | 3 | **19** | 留 |
| 50 | 不插电半小时 · `couple-focus-unplug` | focus | 5 | 3 | 3 | 4 | 3 | **18** | 删 |
| 51 | 恋爱加成清单 · `couple-game` | game | 4 | 4 | 4 | 3 | 3 | **18** | 留 **（点名必留）** |
| 52 | 回忆收藏册（语录/票根/歌单） · `couple-keepsake` | keepsake | 3 | 3 | 4 | 5 | 3 | **18** | 留 |
| 53 | 快乐突袭与中弹 · `couple-laugh-attack` | laugh | 4 | 4 | 4 | 3 | 3 | **18** | 删 |
| 54 | 小胜利账本与成就墙 · `couple-quest-win` | quest | 4 | 3 | 4 | 4 | 3 | **18** | 删 |
| 55 | 爱情保险柜与续约 · `couple-cere-vault` | ceremony | 3 | 3 | 4 | 4 | 3 | **17** | 留 |
| 56 | 双人挑战赛 · `couple-challenge` | growth | 3 | 2 | 4 | 4 | 4 | **17** | 删 |
| 57 | 年度干饭账 · `couple-dine-year` | dining | 5 | 3 | 3 | 4 | 2 | **17** | 删 |
| 58 | 攒一句话 · `couple-focus-queue` | focus | 4 | 3 | 4 | 3 | 3 | **17** | 删 |
| 59 | 情绪接力棒 · `couple-mood-relay` | comm | 4 | 3 | 4 | 3 | 3 | **17** | 删 |
| 60 | 出关战报与盖章 · `couple-quest-battle` | quest | 3 | 4 | 4 | 4 | 2 | **17** | 删 |
| 61 | 低谷通行证 · `couple-quest-valley` | quest | 4 | 3 | 5 | 3 | 2 | **17** | 删 |
| 62 | 周年报告 · `couple-anniv-report` | chronicle | 5 | 3 | 4 | 3 | 1 | **16** | 删 |
| 63 | 周期照顾卡与不适SOS · `couple-body-care` | body | 3 | 2 | 4 | 4 | 3 | **16** | 删 |
| 64 | 今日体征互报 · `couple-body-metric` | body | 3 | 2 | 4 | 4 | 3 | **16** | 删 |
| 65 | 甜蜜任务与运势 · `couple-daily` | ritual | 4 | 3 | 3 | 3 | 3 | **16** | 删 |
| 66 | 本周饭桌 · `couple-dine-week` | dining | 3 | 3 | 3 | 4 | 3 | **16** | 删 |
| 67 | 夸夸回执 · `couple-echo-receipt` | echo | 3 | 3 | 4 | 3 | 3 | **16** | 删 |
| 68 | 专注打卡 · `couple-focus-night` | focus | 4 | 2 | 3 | 4 | 3 | **16** | 删 |
| 69 | 心愿基金 · `couple-funds` | fund | 3 | 2 | 4 | 4 | 3 | **16** | 删 |
| 70 | 每日一逗（值班判分） · `couple-laugh-daily` | laugh | 3 | 4 | 3 | 3 | 3 | **16** | 删 |
| 71 | 笑点预判默契考 · `couple-laugh-guess` | laugh | 3 | 4 | 3 | 3 | 3 | **16** | 删 |
| 72 | 我们的一年（年度盘点） · `couple-legacy-review` | legacy | 4 | 3 | 4 | 3 | 2 | **16** | 删 |
| 73 | 双向约定卡 · `couple-promises` | couple | 3 | 2 | 4 | 4 | 3 | **16** | 删 |
| 74 | 恋爱月报 · `couple-report` | report | 5 | 2 | 3 | 3 | 3 | **16** | 删 |
| 75 | 社会信用（保证与见证） · `couple-world-credit` | world | 3 | 3 | 4 | 4 | 2 | **16** | 删 |
| 76 | 送礼互助池 · `couple-world-gift` | world | 3 | 3 | 4 | 4 | 2 | **16** | 删 |
| 77 | 群聊记者 · `couple-world-group` | world | 4 | 3 | 3 | 3 | 3 | **16** | 删 |
| 78 | 「说到哪了」话头存档 · `couple-catch-thread` | catch | 4 | 2 | 3 | 3 | 3 | **15** | 删 |
| 79 | 今日体感与年度加冕 · `couple-cere-feel` | ceremony | 4 | 2 | 3 | 3 | 3 | **15** | 删 |
| 80 | 写给低落的自己 · `couple-echo-self` | echo | 3 | 3 | 4 | 3 | 2 | **15** | 删 |
| 81 | 感谢慢递 · `couple-echo-slow` | echo | 3 | 3 | 4 | 3 | 2 | **15** | 删 |
| 82 | 服药与久坐 · `couple-fy-care` | factory | 3 | 2 | 3 | 4 | 3 | **15** | 删 |
| 83 | 空间等级与年度称号 · `couple-legacy-level` | legacy | 5 | 2 | 3 | 3 | 2 | **15** | 删 |
| 84 | 新家第一晚 · `couple-quest-night` | quest | 4 | 3 | 4 | 3 | 1 | **15** | 删 |
| 85 | 考试周静音舱 · `couple-quest-pod` | quest | 3 | 3 | 4 | 3 | 2 | **15** | 删 |
| 86 | 关卡预告 · `couple-quest-upcoming` | quest | 3 | 3 | 3 | 4 | 2 | **15** | 删 |
| 87 | 心动软陪伴 · `couple-soft` | soft | 4 | 3 | 3 | 2 | 3 | **15** | 删 |
| 88 | 今日节气 · `couple-alm-today` | almanac | 4 | 2 | 2 | 3 | 3 | **14** | 删 |
| 89 | 发薪日 · `couple-bd-pay` | board | 3 | 3 | 3 | 3 | 2 | **14** | 删 |
| 90 | 呼噜与震感报告 · `couple-body-snore` | body | 3 | 3 | 3 | 3 | 2 | **14** | 删 |
| 91 | 反话词典 · `couple-catch-say` | catch | 3 | 3 | 3 | 3 | 2 | **14** | 删 |
| 92 | 敏感日历 · `couple-catch-sensitive` | catch | 3 | 2 | 3 | 4 | 2 | **14** | 删 |
| 93 | 话题许愿池 · `couple-catch-topic` | catch | 3 | 3 | 3 | 3 | 2 | **14** | 删 |
| 94 | 百科词条 · `couple-cx-codex` | codex | 3 | 2 | 3 | 4 | 2 | **14** | 删 |
| 95 | 恋爱词典（+星座配对） · `couple-dict` | growth | 3 | 2 | 3 | 4 | 2 | **14** | 删 |
| 96 | 跑腿与叫醒 · `couple-fy-errand` | factory | 3 | 2 | 3 | 3 | 3 | **14** | 删 |
| 97 | 采买与冰箱 · `couple-fy-shop` | factory | 3 | 2 | 2 | 4 | 3 | **14** | 删 |
| 98 | 大笑处方与服用回执 · `couple-laugh-rx` | laugh | 3 | 3 | 3 | 3 | 2 | **14** | 删 |
| 99 | 生活共享账本 · `couple-life` | life | 2 | 2 | 3 | 4 | 3 | **14** | 删 |
| 100 | 底线与认错榜与纪念碑 · `couple-repair-ledger` | repair | 3 | 3 | 3 | 3 | 2 | **14** | 删 |
| 101 | 今日身份签 · `couple-theater-role` | theater | 4 | 3 | 3 | 2 | 2 | **14** | 删 |
| 102 | 文案代写 · `couple-world-caption` | world | 3 | 3 | 3 | 3 | 2 | **14** | 删 |
| 103 | 节日家档 · `couple-alm-festival` | almanac | 3 | 2 | 3 | 3 | 2 | **13** | 删 |
| 104 | 戒东西互助营与运动链 · `couple-body-camp` | body | 2 | 2 | 3 | 4 | 2 | **13** | 删 |
| 105 | 身体账本四页 · `couple-body-ledger` | body | 3 | 2 | 3 | 3 | 2 | **13** | 删 |
| 106 | 小日子黄历 · `couple-cere-almanac` | ceremony | 4 | 2 | 2 | 3 | 2 | **13** | 删 |
| 107 | 双城卡片 · `couple-city-card` | distance | 3 | 2 | 3 | 3 | 2 | **13** | 删 |
| 108 | 默契综艺 · `couple-cx-quiz` | codex | 2 | 3 | 3 | 3 | 2 | **13** | 删 |
| 109 | 异地恋雷达 · `couple-distance` | distance | 2 | 2 | 4 | 3 | 2 | **13** | 删 |
| 110 | 高光重放 · `couple-echo-highlight` | echo | 3 | 2 | 3 | 3 | 2 | **13** | 删 |
| 111 | 数字排毒半天 · `couple-focus-detox` | focus | 3 | 2 | 3 | 3 | 2 | **13** | 删 |
| 112 | 专属时段 · `couple-focus-slot` | focus | 3 | 2 | 3 | 3 | 2 | **13** | 删 |
| 113 | 欢乐周报 · `couple-laugh-week` | laugh | 5 | 2 | 2 | 2 | 2 | **13** | 删 |
| 114 | 里程碑倒推 · `couple-legacy-milestone` | legacy | 4 | 2 | 3 | 2 | 2 | **13** | 删 |
| 115 | 年度十问 · `couple-legacy-ten` | legacy | 2 | 3 | 4 | 3 | 1 | **13** | 删 |
| 116 | 呵护台 · `couple-ls-care` | listen | 3 | 2 | 3 | 3 | 2 | **13** | 删 |
| 117 | 倾听时段 · `couple-ls-slot` | listen | 3 | 2 | 3 | 3 | 2 | **13** | 删 |
| 118 | 发声与语气 · `couple-ls-voice` | listen | 3 | 2 | 3 | 3 | 2 | **13** | 删 |
| 119 | 恋爱条约 · `couple-pacts` | couple | 3 | 2 | 3 | 3 | 2 | **13** | 删 |
| 120 | 人生大事与改天拍卖 · `couple-post-bucket` | post | 2 | 3 | 3 | 3 | 2 | **13** | 删 |
| 121 | 五年后的新年卡 · `couple-post-oath` | post | 3 | 3 | 4 | 2 | 1 | **13** | 删 |
| 122 | 许愿井与解梦局 · `couple-post-well` | post | 3 | 3 | 3 | 2 | 2 | **13** | 删 |
| 123 | 冷冻解冻规程 · `couple-repair-freeze` | repair | 2 | 2 | 4 | 3 | 2 | **13** | 删 |
| 124 | 和好倒计时与修复礼盒 · `couple-repair-makeup` | repair | 3 | 2 | 3 | 3 | 2 | **13** | 删 |
| 125 | 道歉质检 · `couple-repair-sorry` | repair | 2 | 2 | 4 | 3 | 2 | **13** | 删 |
| 126 | 共享清单 · `couple-shared` | couple | 3 | 1 | 2 | 4 | 3 | **13** | 删 |
| 127 | 时空电话亭与黑话 · `couple-theater-booth` | theater | 3 | 3 | 3 | 2 | 2 | **13** | 删 |
| 128 | 长假愿望 · `couple-alm-holiday` | almanac | 3 | 2 | 3 | 2 | 2 | **12** | 删 |
| 129 | 择吉日 · `couple-alm-lucky` | almanac | 3 | 2 | 2 | 3 | 2 | **12** | 删 |
| 130 | 组织架构 · `couple-bd-org` | board | 3 | 2 | 3 | 2 | 2 | **12** | 删 |
| 131 | 聆听方式协议 · `couple-catch-protocol` | catch | 3 | 2 | 3 | 2 | 2 | **12** | 删 |
| 132 | 月度安眠小结 · `couple-cozy-monthly` | cozy | 5 | 1 | 2 | 2 | 2 | **12** | 删 |
| 133 | 考据卷宗 · `couple-cx-dossier` | codex | 2 | 2 | 3 | 3 | 2 | **12** | 删 |
| 134 | 深度陪伴 · `couple-daily-life` | daily-life | 2 | 2 | 3 | 3 | 2 | **12** | 删 |
| 135 | 专注周报 · `couple-focus-weekly` | focus | 5 | 1 | 2 | 2 | 2 | **12** | 删 |
| 136 | 年度笑榜 · `couple-laugh-year` | laugh | 5 | 2 | 2 | 2 | 1 | **12** | 删 |
| 137 | 生活经营所 · `couple-manage` | manage | 2 | 2 | 3 | 3 | 2 | **12** | 删 |
| 138 | 情诗与文字浪漫 · `couple-poem` | poem | 2 | 3 | 3 | 2 | 2 | **12** | 删 |
| 139 | 搬家区块分工 · `couple-quest-move` | quest | 2 | 2 | 3 | 4 | 1 | **12** | 删 |
| 140 | 重来卡与信任重建 · `couple-repair-rebuild` | repair | 2 | 2 | 3 | 3 | 2 | **12** | 删 |
| 141 | 确定感与安全感 · `couple-secure` | secure | 2 | 2 | 4 | 2 | 2 | **12** | 删 |
| 142 | 官宣日 · `couple-world-declare` | world | 3 | 2 | 3 | 3 | 1 | **12** | 删 |
| 143 | 亲戚称呼册 · `couple-world-relative` | world | 3 | 3 | 2 | 3 | 1 | **12** | 删 |
| 144 | 拜访攻略 · `couple-world-visit` | world | 2 | 2 | 3 | 4 | 1 | **12** | 删 |
| 145 | 年运与小结 · `couple-alm-year` | almanac | 4 | 2 | 2 | 1 | 2 | **11** | 删 |
| 146 | 董事会 · `couple-bd-board` | board | 2 | 2 | 3 | 2 | 2 | **11** | 删 |
| 147 | 例会与周报 · `couple-bd-weekly` | board | 4 | 1 | 2 | 2 | 2 | **11** | 删 |
| 148 | 成长搭子 · `couple-coach` | coach | 2 | 2 | 3 | 2 | 2 | **11** | 删 |
| 149 | 回音壁年报 · `couple-echo-year` | echo | 5 | 1 | 2 | 2 | 1 | **11** | 删 |
| 150 | 账本与月检 · `couple-fy-books` | factory | 2 | 1 | 2 | 4 | 2 | **11** | 删 |
| 151 | 传世清单 · `couple-legacy-list` | legacy | 2 | 2 | 3 | 3 | 1 | **11** | 删 |
| 152 | 换位信与早想说 · `couple-ls-letter` | listen | 2 | 2 | 3 | 2 | 2 | **11** | 删 |
| 153 | 时光博物馆 · `couple-museum` | museum | 2 | 2 | 3 | 2 | 2 | **11** | 删 |
| 154 | 愿望台账与未来信用卡 · `couple-post-ledger` | post | 2 | 2 | 3 | 2 | 2 | **11** | 删 |
| 155 | 下次关口预约 · `couple-quest-report` | quest | 3 | 1 | 2 | 3 | 2 | **11** | 删 |
| 156 | 冲突类型年报 · `couple-repair-report` | repair | 5 | 1 | 2 | 2 | 1 | **11** | 删 |
| 157 | 默契亲密仪表盘 · `couple-spark` | spark | 2 | 2 | 3 | 2 | 2 | **11** | 删 |
| 158 | 互换日记与师徒日 · `couple-theater-swap` | theater | 2 | 3 | 3 | 2 | 1 | **11** | 删 |
| 159 | 代 TA 赔礼 · `couple-world-apology` | world | 2 | 2 | 3 | 3 | 1 | **11** | 删 |
| 160 | 朋友视角问卷 · `couple-world-view` | world | 2 | 3 | 3 | 2 | 1 | **11** | 删 |
| 161 | 聆听者年报 · `couple-catch-year` | catch | 5 | 1 | 2 | 1 | 1 | **10** | 删 |
| 162 | 注意力年报 · `couple-focus-year` | focus | 5 | 1 | 2 | 1 | 1 | **10** | 删 |
| 163 | 幽默风格图鉴 · `couple-laugh-style` | laugh | 3 | 2 | 2 | 2 | 1 | **10** | 删 |
| 164 | 续约发布会 · `couple-legacy-speech` | legacy | 2 | 2 | 3 | 2 | 1 | **10** | 删 |
| 165 | 共读追剧 · `couple-read-watch` | growth | 2 | 1 | 2 | 3 | 2 | **10** | 删 |
| 166 | 恋爱汇率与年末结算 · `couple-legacy-fx` | legacy | 2 | 2 | 2 | 2 | 1 | **9** | 删 |
| 167 | 想象中的家与退休 · `couple-post-home` | post | 2 | 2 | 3 | 1 | 1 | **9** | 删 |
| 168 | 每日奥斯卡与家长题 · `couple-theater-act` | theater | 2 | 2 | 2 | 2 | 1 | **9** | 删 |
| 169 | 追剧客服颁奖礼 · `couple-theater-house` | theater | 2 | 2 | 2 | 2 | 1 | **9** | 删 |
| 170 | 进城接待方案 · `couple-world-city` | world | 2 | 1 | 2 | 3 | 1 | **9** | 删 |
| 171 | 年度述职 · `couple-bd-report` | board | 2 | 1 | 2 | 2 | 1 | **8** | 删 |
| 172 | 灵魂与人格 · `couple-cx-soul` | codex | 2 | 2 | 2 | 1 | 1 | **8** | 删 |
| 173 | 记忆库年审 · `couple-legacy-audit` | legacy | 2 | 1 | 2 | 2 | 1 | **8** | 删 |
| 174 | 情侣品牌 · `couple-legacy-brand` | legacy | 2 | 2 | 2 | 1 | 1 | **8** | 删 |

## 三、切线上的人工裁决（4 删 1 留）

纯按分数会有四处重复/冗余被留在场内，一处低分但机制上承重的被切掉。以下五条是本表里唯一凌驾于分数之上的判断，理由写死在此：

| 卡 | 分 | 分数本来的结论 | 实际 | 理由 |
|---|---:|---|---|---|
| `couple-timeline` 恋爱时光轴 | 20 | 留 | **删** | 与 `couple-chronicle` 恋爱编年史同为"按时间回望"，编年史还带考古卡与问答机，留一个就够 |
| `couple-echo-calendar` 被爱日历 | 20 | 留 | **删** | 好事簿本身就是一张按日列表，再加一张日历是同一份数据的第二种画法；年度视角已有 `couple-heatmap` |
| `couple-focus-unplug` 不插电半小时 | 18 | 留 | **删** | 与"对视十秒""饭桌不低头"是同一张三列双点打卡卡，留两张已经说得清"放下手机" |
| `couple-quest-win` 小胜利账本 | 18 | 留 | **删** | "记一笔好事"这个动作已由 `couple-echo-deed` 好事簿承担，且好事簿才是积分入口，账本再留一份会让两个人不知道往哪记 |
| `couple-cere-vault` 爱情保险柜与续约 | 17 | 删 | **留** | 每月各夸一句 → 双齐算当月保费 → 满 3/6/12 月掉愿望券。它是"愿望券本"的上游供给，也是唯一一个持续产出券的循环，拆了券本就没有进水口 |

## 四、保留的 50 张（重组为 5 个页签）

原来的 11 个页签里有 6 个会被删空，剩下的也只剩两三张。顺带收敛成 5 个页签，让裁剪后的空间是"小而满"而不是"空架子"。

### 🫶 今天 — `today`（12 张）

每天进来点一下就走，全部是一步操作

| 排名 | 功能卡 | 总分 |
|---:|---|---:|
| 5 | 贴贴宫格 · `couple-bond` | 23 |
| 28 | 早晚安打卡 · `couple-rituals` | 21 |
| 13 | 心情日记 · `couple-mood` | 23 |
| 6 | 今日一句话 · `couple-catch-daily` | 23 |
| 8 | 今日真心话 · `couple-deep` | 23 |
| 9 | 电量预报 · `couple-echo-battery` | 23 |
| 2 | 今晚饭桌（饭票+吃什么裁决） · `couple-dine-today` | 24 |
| 4 | 加班预报与留灯 · `couple-quest-overtime` | 24 |
| 11 | 对视十秒 · `couple-focus-gaze` | 23 |
| 19 | 饭桌不低头 · `couple-focus-meal` | 22 |
| 23 | 走神温柔哨 · `couple-focus-nudge` | 21 |
| 46 | 今日体温同步（同熄灯+抱抱计量） · `couple-cozy-today` | 19 |

### 🌈 抱抱 — `care`（10 张）

情绪出事时来这里，含修复与回血

| 排名 | 功能卡 | 总分 |
|---:|---|---:|
| 21 | 情绪天气与急救箱 · `couple-care` | 21 |
| 1 | 求抱抱 · `couple-comfort` | 24 |
| 12 | 和好与道歉券 · `couple-makeup` | 23 |
| 7 | 安全词与暂停复盘 · `couple-catch-safeword` | 23 |
| 30 | 雷区探测器 · `couple-catch-mine` | 20 |
| 38 | 误会倒带 · `couple-ls-misrewind` | 20 |
| 40 | 生病陪护单 · `couple-quest-nurse` | 20 |
| 10 | 好事簿（被爱的证据） · `couple-echo-deed` | 23 |
| 47 | 鼓励语罐 · `couple-echo-juice` | 19 |
| 18 | 能量补给 · `couple-echo-refill` | 22 |

### 🎲 玩一玩 — `fun`（10 张）

随机性、比拼、拆封感，负责"想打开"

| 排名 | 功能卡 | 总分 |
|---:|---|---:|
| 39 | 趣味小游戏 · `couple-play` | 20 |
| 24 | 默契考验比划猜 · `couple-fun-talk` | 21 |
| 17 | TOP10 互猜 · `couple-cx-top` | 22 |
| 27 | 冷笑话结冰榜 · `couple-laugh-joke` | 21 |
| 26 | 社死往事（满一年转好笑） · `couple-laugh-cringe` | 21 |
| 35 | 笑点存档（现场证词） · `couple-laugh-moment` | 20 |
| 15 | 暗中心愿本 · `couple-catch-wish` | 22 |
| 3 | 家务轮盘 · `couple-fy-spin` | 24 |
| 14 | 刮刮乐与盲盒 · `couple-surprise` | 23 |
| 25 | 爱情花园（玫瑰+幸运签） · `couple-garden` | 21 |

### 🏠 过日子 — `life`（10 张）

吃穿用度、小日子、约定与仪式

| 排名 | 功能卡 | 总分 |
|---:|---|---:|
| 22 | 我们的餐厅 · `couple-dine-restaurant` | 21 |
| 31 | 我们的小日子 · `couple-cere-founded` | 20 |
| 55 | 爱情保险柜与续约 · `couple-cere-vault` | 17 |
| 16 | 愿望券本 · `couple-cere-coupon` | 22 |
| 45 | 倒数日 · `couple-countdowns` | 19 |
| 42 | 心愿互换板（+下次一定） · `couple-wish-board` | 20 |
| 37 | 悄悄话信箱 · `couple-letters` | 20 |
| 29 | 匿名树洞与情话罐 · `couple-whisper-box` | 21 |
| 44 | 时光胶囊 · `couple-capsules` | 19 |
| 36 | 周年抽奖箱 · `couple-legacy-draw` | 20 |

### 📖 回忆 — `memory`（8 张）

已经攒下的东西，被动回看

| 排名 | 功能卡 | 总分 |
|---:|---|---:|
| 52 | 回忆收藏册（语录/票根/歌单） · `couple-keepsake` | 18 |
| 34 | 我们的第一次 · `couple-firsts` | 20 |
| 20 | 那年今天 · `couple-on-this-day` | 22 |
| 32 | 恋爱编年史（考古卡+问答机） · `couple-chronicle` | 20 |
| 48 | 心动时刻 · `couple-heart-moments` | 19 |
| 43 | 里程碑徽章墙 · `couple-badges` | 19 |
| 49 | 年度热力日历 · `couple-heatmap` | 19 |
| 51 | 恋爱加成清单 · `couple-game` | 18 |

## 五、删除的 124 张（按模块归堆，执行用）

整模块清零的（模块内一张不剩）优先整文件删除；只删一部分的需要在同一 Controller / 同一组件里动刀，是风险集中点，单独标出。

| 模块 | 删掉的卡 | 该模块保留数 | 删法 |
|---|---|---:|---|
| `world` | `couple-world-credit` 社会信用（保证与见证）<br>`couple-world-gift` 送礼互助池<br>`couple-world-group` 群聊记者<br>`couple-world-caption` 文案代写<br>`couple-world-declare` 官宣日<br>`couple-world-relative` 亲戚称呼册<br>`couple-world-visit` 拜访攻略<br>`couple-world-apology` 代 TA 赔礼<br>`couple-world-view` 朋友视角问卷<br>`couple-world-city` 进城接待方案 | 0 | **整模块删** |
| `legacy` | `couple-legacy-review` 我们的一年（年度盘点）<br>`couple-legacy-level` 空间等级与年度称号<br>`couple-legacy-milestone` 里程碑倒推<br>`couple-legacy-ten` 年度十问<br>`couple-legacy-list` 传世清单<br>`couple-legacy-speech` 续约发布会<br>`couple-legacy-fx` 恋爱汇率与年末结算<br>`couple-legacy-audit` 记忆库年审<br>`couple-legacy-brand` 情侣品牌 | 1 | ⚠ 模块内定向删 |
| `quest` | `couple-quest-win` 小胜利账本与成就墙<br>`couple-quest-battle` 出关战报与盖章<br>`couple-quest-valley` 低谷通行证<br>`couple-quest-night` 新家第一晚<br>`couple-quest-pod` 考试周静音舱<br>`couple-quest-upcoming` 关卡预告<br>`couple-quest-move` 搬家区块分工<br>`couple-quest-report` 下次关口预约 | 2 | ⚠ 模块内定向删 |
| `focus` | `couple-focus-unplug` 不插电半小时<br>`couple-focus-queue` 攒一句话<br>`couple-focus-night` 专注打卡<br>`couple-focus-detox` 数字排毒半天<br>`couple-focus-slot` 专属时段<br>`couple-focus-weekly` 专注周报<br>`couple-focus-year` 注意力年报 | 3 | ⚠ 模块内定向删 |
| `laugh` | `couple-laugh-attack` 快乐突袭与中弹<br>`couple-laugh-daily` 每日一逗（值班判分）<br>`couple-laugh-guess` 笑点预判默契考<br>`couple-laugh-rx` 大笑处方与服用回执<br>`couple-laugh-week` 欢乐周报<br>`couple-laugh-year` 年度笑榜<br>`couple-laugh-style` 幽默风格图鉴 | 3 | ⚠ 模块内定向删 |
| `echo` | `couple-echo-calendar` 被爱日历<br>`couple-echo-receipt` 夸夸回执<br>`couple-echo-self` 写给低落的自己<br>`couple-echo-slow` 感谢慢递<br>`couple-echo-highlight` 高光重放<br>`couple-echo-year` 回音壁年报 | 4 | ⚠ 模块内定向删 |
| `catch` | `couple-catch-thread` 「说到哪了」话头存档<br>`couple-catch-say` 反话词典<br>`couple-catch-sensitive` 敏感日历<br>`couple-catch-topic` 话题许愿池<br>`couple-catch-protocol` 聆听方式协议<br>`couple-catch-year` 聆听者年报 | 4 | ⚠ 模块内定向删 |
| `repair` | `couple-repair-ledger` 底线与认错榜与纪念碑<br>`couple-repair-freeze` 冷冻解冻规程<br>`couple-repair-makeup` 和好倒计时与修复礼盒<br>`couple-repair-sorry` 道歉质检<br>`couple-repair-rebuild` 重来卡与信任重建<br>`couple-repair-report` 冲突类型年报 | 0 | **整模块删** |
| `body` | `couple-body-care` 周期照顾卡与不适SOS<br>`couple-body-metric` 今日体征互报<br>`couple-body-snore` 呼噜与震感报告<br>`couple-body-camp` 戒东西互助营与运动链<br>`couple-body-ledger` 身体账本四页 | 0 | **整模块删** |
| `almanac` | `couple-alm-today` 今日节气<br>`couple-alm-festival` 节日家档<br>`couple-alm-holiday` 长假愿望<br>`couple-alm-lucky` 择吉日<br>`couple-alm-year` 年运与小结 | 0 | **整模块删** |
| `board` | `couple-bd-pay` 发薪日<br>`couple-bd-org` 组织架构<br>`couple-bd-board` 董事会<br>`couple-bd-weekly` 例会与周报<br>`couple-bd-report` 年度述职 | 0 | **整模块删** |
| `theater` | `couple-theater-role` 今日身份签<br>`couple-theater-booth` 时空电话亭与黑话<br>`couple-theater-swap` 互换日记与师徒日<br>`couple-theater-act` 每日奥斯卡与家长题<br>`couple-theater-house` 追剧客服颁奖礼 | 0 | **整模块删** |
| `post` | `couple-post-bucket` 人生大事与改天拍卖<br>`couple-post-oath` 五年后的新年卡<br>`couple-post-well` 许愿井与解梦局<br>`couple-post-ledger` 愿望台账与未来信用卡<br>`couple-post-home` 想象中的家与退休 | 0 | **整模块删** |
| `couple` | `couple-timeline` 恋爱时光轴<br>`couple-promises` 双向约定卡<br>`couple-pacts` 恋爱条约<br>`couple-shared` 共享清单 | 0 | **整模块删** |
| `factory` | `couple-fy-care` 服药与久坐<br>`couple-fy-errand` 跑腿与叫醒<br>`couple-fy-shop` 采买与冰箱<br>`couple-fy-books` 账本与月检 | 1 | ⚠ 模块内定向删 |
| `codex` | `couple-cx-codex` 百科词条<br>`couple-cx-quiz` 默契综艺<br>`couple-cx-dossier` 考据卷宗<br>`couple-cx-soul` 灵魂与人格 | 1 | ⚠ 模块内定向删 |
| `listen` | `couple-ls-care` 呵护台<br>`couple-ls-slot` 倾听时段<br>`couple-ls-voice` 发声与语气<br>`couple-ls-letter` 换位信与早想说 | 1 | ⚠ 模块内定向删 |
| `growth` | `couple-challenge` 双人挑战赛<br>`couple-dict` 恋爱词典（+星座配对）<br>`couple-read-watch` 共读追剧 | 1 | ⚠ 模块内定向删 |
| `dining` | `couple-dine-year` 年度干饭账<br>`couple-dine-week` 本周饭桌 | 2 | ⚠ 模块内定向删 |
| `ceremony` | `couple-cere-feel` 今日体感与年度加冕<br>`couple-cere-almanac` 小日子黄历 | 3 | ⚠ 模块内定向删 |
| `distance` | `couple-city-card` 双城卡片<br>`couple-distance` 异地恋雷达 | 0 | **整模块删** |
| `comm` | `couple-mood-relay` 情绪接力棒 | 1 | ⚠ 模块内定向删 |
| `chronicle` | `couple-anniv-report` 周年报告 | 2 | ⚠ 模块内定向删 |
| `ritual` | `couple-daily` 甜蜜任务与运势 | 1 | ⚠ 模块内定向删 |
| `fund` | `couple-funds` 心愿基金 | 0 | **整模块删** |
| `report` | `couple-report` 恋爱月报 | 0 | **整模块删** |
| `soft` | `couple-soft` 心动软陪伴 | 0 | **整模块删** |
| `life` | `couple-life` 生活共享账本 | 0 | **整模块删** |
| `cozy` | `couple-cozy-monthly` 月度安眠小结 | 1 | ⚠ 模块内定向删 |
| `daily-life` | `couple-daily-life` 深度陪伴 | 0 | **整模块删** |
| `manage` | `couple-manage` 生活经营所 | 0 | **整模块删** |
| `poem` | `couple-poem` 情诗与文字浪漫 | 0 | **整模块删** |
| `secure` | `couple-secure` 确定感与安全感 | 0 | **整模块删** |
| `coach` | `couple-coach` 成长搭子 | 0 | **整模块删** |
| `museum` | `couple-museum` 时光博物馆 | 0 | **整模块删** |
| `spark` | `couple-spark` 默契亲密仪表盘 | 0 | **整模块删** |

> 逐模块逐表的删除影响清单（Controller/Service/Entity/Mapper/Bank/测试/表名/api 函数/WS 事件）在 `couple-trim-impact.md`，由代码扫描生成，与本节的人工排名互为校验。

## 六、数据库处理

- **只新增一个 `V50__drop_unused_couple_features.sql`**，不改已入库的 V1-V49（规范硬约束：已入库脚本内容不得再修改）。
- 写法用 `DROP TABLE IF EXISTS`，MariaDB 10.11 与 H2 `MODE=MySQL` 双认；不加任一库独有的子句。幂等是硬要求，因为新库存量走 `baseline-on-migrate`，V1→V50 会全量重放。
- `schema.sql` 同步删掉对应表定义（它是全量结构文档，不被运行时执行，但必须与 V 链一致）。
- **不做数据迁移、不做备份表**：本次是产品裁剪，被删功能的数据随之作废。生产库上执行前另行手动备份，脚本本身不带备份逻辑。
- 表清单必须来自代码扫描而不是本文档的手写枚举——`grep -c "^CREATE TABLE"` 会少数表，最终以保留清单里每张卡的实体 `@TableName` 反查为准，差集才是要 drop 的。
- 校验手段：`mvn test` 里的 `@SpringBootTest` 会打 H2 跑全量 Flyway，V50 在空库上从 V1 一路执行到 V50 必须无错。

## 七、心动值与积分经济

结论（已回代码核实）：**积分台账 `couple_point_ledger` 原样保留，赚分入口重接到用户点名的三张卡上；心动值不删，但改成由积分派生**。

核实到的现状：

- `CoupleService.intimacy()`（`CoupleService.java:846`）是**读时算**，无独立表。计分式 = 互道早安×1 + 互道晚安×2 + 一问双答天数×2 + 兑现承诺×5 + 清单完成×3 + 心情条数×1，再映射 7 级恋爱等级（怦然心动→相守一生）。
  其中「兑现承诺」(`couple-promises`) 与「清单完成」(`couple-shared`) **两张卡本次都删**，留着就是两个永远为 0 的死项撑着的分数。
  处理：删掉这两个乘数项，加一项「台账累计 EARN」，前端 header 与等级阶梯不动。心动值因此重新有了活的进水口，而不是靠已被删掉的功能回忆当年。
- `couple_point_ledger` 由 V27（生活经营所）建表，但它**不属于将被删的模块**——它是积分体系的物理载体，V50 必须明确跳过它。
- 现有 EARN 写入点 5 处，本次全部随功能消失：`CoupleRepairService:241` 复温 +8、`CoupleWorldService:674` 说到做到 +10、`CoupleBoardService:229` 发薪日 +5、`CoupleFactoryService:456` 代拿快递 +2、`CoupleManageService:393` 家务记分。SPEND 写入点 1 处：`CoupleManageService:410` 兑换奖励。
- 现有读侧 4 处：`CoupleBoardService`（升职公示栏/成员/周报，删）、`CoupleManageService`（积分市场，删）、`CoupleLegacyService:514`（**周年抽奖箱奖池，留**）、`CoupleLegacyService:658`（空间等级，卡已删）。
  ⇒ 裁完后台账只剩抽奖箱一个读者，没有任何写者。**这就是重接不是可选项、而是必做项的原因**：不重接，周年抽奖箱的奖池会永远走"回落到静态愿望位"那条分支。
- `couple-game` 恋爱加成清单（`CoupleGameService.todayBoost`，F31）读的是打卡/一问/动作/任务/心情/信箱/夸夸七项当日行为，**与积分台账无耦合**，因此它不是积分出口，保持原样即可，不需要为它改代码。

### 赚分（EARN）——三个入口

| 卡 | 动作 | 分值 | 归谁 | 为什么这么定 |
|---|---|---:|---|---|
| `couple-echo-deed` 好事簿 | 记一笔「TA 为我做的事」 | +2 | **被记的那位（TA）** | 做了好事的人拿分，才对得上"对 TA 好能攒下来"；记的人已经得到"被爱证据"这份回报，不重复计分 |
| `couple-echo-deed` 好事簿 | 对方给这条加星 | +1 | 被记的那位 | 星是确认，小额加成，同日同人同内容已有 400 去重，加星按行幂等 |
| `couple-fy-spin` 家务轮盘 | 天选之人干完自己那格打勾 | +3 | 干的人 | 家务是本模块最容易"没人干"的事，给即时反馈 |
| `couple-fy-spin` 家务轮盘 | 本周全部格子清空 | +2 | 双方各一份 | 沿用现有 `factory-spin-clear` 双推事件，把庆祝升级成分数 |
| `couple-surprise` 刮刮乐 | 券面兑现（核销） | +5 | **送券的人** | 读代码才定准：券是 TA 送我刮、**送券人**才有权限点「已兑现」（`redeemScratch` 里的 `fromUser` 校验），所以分归送券人；刮开只算"看到"不计分 |

原分散在被删模块里的写入口随模块一并消失：修复车间复温 +8、社会信用 +10、公司发薪日 +5、代拿快递 +2。这些是本次唯一需要"搬家"的分数逻辑——搬进上表三处，而不是留着半截。

### 花分（SPEND）——一个出口

| 卡 | 动作 | 分值 | 说明 |
|---|---|---:|---|
| `couple-cere-coupon` 愿望券本 | 发一张券给 TA | 按券面分（缺省 10）扣发券人 | 积分的用途就是"给 TA 造一个愿望"；核销时不再扣第二次 |

原 `couple-manage` 里的家务积分市场（6 项兑换奖励）整块删除，不再保留第二个出口。若后续想让积分多一处花法，加在愿望券本的券面模板上，不要重新引入市场。

### 分数能换来什么（保留的读侧）

- `couple-legacy-draw` 周年抽奖箱：奖池仍按原规格吃**本年台账里 EARN 的条目**，一条没攒过时回落到静态愿望位——这条链路是积分最有情绪价值的兑现方式，必须留。
- `couple-game` 恋爱加成清单：作为积分的"看得见"入口（详见下方待核项）。
- 空间等级与年度称号（`couple-legacy-level`）已删，不再做等级派生。

### 三个待核项的核实结果

1. **心动值**：`CoupleService.intimacy()`（`CoupleService.java:846`）读时算、无独立表，六个乘数项里「兑现承诺」「清单完成」两张卡本次都删。
   处理：删掉这两个死项，并入「台账累计 EARN」，等级阶梯与前端 header 不动。**不删心动值这个显示，删的是它已经断供的算法。**
2. **刮刮乐"核销"的语义**：`CoupleSurpriseService.redeemScratch` 只有 `fromUser`（送券人）能点，`owner` 只能刮。
   ⇒ 上表的归属人已按实现改正为送券人。**这是本节唯一一处"设计猜错、以代码为准"的地方**，原样保留记录以免下次有人按旧文案改回去。
3. **恋爱加成清单**：`CoupleGameService.todayBoost` 读的是打卡/一问/动作/任务/心情/信箱/夸夸七项当日行为，与台账无耦合。
   ⇒ 它不是积分出口，保持原样，不为它加代码。

三处赚分与一处花分都要有单测**锁副作用**（断言台账真插了一行、分值与归属人对），而不是只断言返回值里的余额——只看返回值的断言在 mock 下坏代码也能过。

## 八、执行顺序

1. 先出逐卡影响清单（代码扫描），与本文档第五节的归堆互为校验；
2. 后端按模块删：整模块清零的先删（风险最低、收益最大），模块内定向删的排后；每批 `mvn -q compile`；
3. 前端同步删卡与 api/types/store 分支，registry 只留 50 张并改为 5 页签；`pnpm build` + `pnpm test`；
4. V50 drop 脚本 + `schema.sql` 同步，`mvn test` 验证空库 V1→V50 全链路；
5. 积分重接（第七节）单独成批，带单测；
6. 最后同步两个 map skill 与 wiki，升 rc 版本。

跨仓改动一律分批 commit、数据库脚本独立成 commit；**不 push**，等用户过一遍再推。


## 九、执行进度账（跨会话续接用，每轮更新）

判定「某模块还剩几卡」的唯一依据是 `couple-trim-ranking.md` 第二节的去留列；下表记录代码是否已落到那个状态。

| 模块 | 目标留卡 | 后端 | 前端 |
|---|---:|---|---|
| 整模块清零（老黄历/我们公司/身体通知/成长系/深度陪伴/异地恋/生活经营/博物馆/文字浪漫/明日邮局/修复车间/确定感/默契亲密/扮演剧场/两家与朋友/生活账本） | 0 | ✅ 已删 | 8 个独立 api 组的 ✅ 已删；其余 8 个走 coupleApi 子前缀的 ⬜ 待删 |
| 传世系统 | 1 | ✅ | ⬜ |
| 我们百科 | 1 | ✅ | ⬜ |
| 倾听与发声 | 1 | ✅ | ⬜ |
| 二人制造厂 | 1 | ✅（含积分入口） | ⬜ |
| 回音壁 | 4 | ✅（含积分入口） | ⬜ |
| 人生关卡 | 2 | ✅ | ⬜ |
| 注意力保护区 | 3 | ✅ | ⬜ |
| 聆听者 | 4 | ✅ | ⬜ |
| 欢笑银行 | 3 | ⬜ | ⬜ |
| 小日子仪式感 | 3 | ✅（含积分 SPEND 出口） | ⬜ |
| 两个人的饭桌 | 2 | ✅ | ⬜ |
| 体温同步 | 1 | ✅ | ⬜ |
| 沟通增强 | 1 | ✅ | ⬜ |
| 共同养成 | 1 | ⬜ | ⬜ |
| 回忆资产/徽章/时光轴 | 5 | ⬜ | ⬜ |
|  CoupleController 核心（心情/信箱/纪念日/约定/条约/清单/城市/基金/时光轴/心动值） | 2 | ⬜ | ⬜ |
| 明日邮局/扮演剧场/身体通知/修复车间/两家与朋友/老黄历 | 0 | ✅ | ✅（api 组独立的 8 个） |

**未做完全局的事**：
- 积分 SPEND 出口（愿望券本发券扣分）与心动值改算（删掉 promise/item 两个死乘数、并入台账累计 EARN）；
- ~~V50 drop 脚本~~ **已出并验证**（43ead2c）：drop 173 张、保留 `couple_point_ledger`，
  由 `.tmp-audit/gen-v50.mjs` 从「297 张 couple_* − 现存实体 @TableName」推导，
  `schema.sql` 同步 310→137 且带算术断言。
  **但后端还剩若干模块未裁完，所以 V50 必须重跑一次生成**（`node .tmp-audit/gen-v50.mjs && node .tmp-audit/sync-schema.mjs`），
  否则剩余功能表不会被覆盖到；重跑是幂等的，drop 列表只会变长。
- 两仓 map skill 与 wiki 同步、前端页签从 11 收敛到 5、版本号升 rc；
- 前端 132 卡 → 50 卡的定向删（与后端逐模块对齐着做）。

**复用手法**（每个部分裁的模块都走这七步，脚本在 `.tmp-audit/`）：
1. `cut-java.mjs Service --drop-m=... --drop-rec=...` 按名删成员（会连带吞 javadoc 与注解行）；
2. 手改聚合 VO；
3. `fix_*_build.py` 用花括号配对替换 build()；
4. `cut-java.mjs Controller --drop-m=...`；
5. `clean-fields.py` 重建字段与构造器；
6. `mvn -q -o clean compile` 拿真退出码，按报错回 1；
7. `orphans.mjs delete` 扫孤儿实体（迭代到 0 命中）→ 改测试 → `mvn -o test`。

**四个必须知道的工具陷阱**（都真踩过，都会静默改变行为，不会被编译或测试拦住）：

- **按名删成员必须只锚声明行**。早期版本把调用点也当锚点，`if (secretWishes(space, owner).size() >= PER_OWNER_MAX)`
  这种行会命中 `secretWishes`，于是从这一行开始向后按花括号配对删除，
  **把无关且要保留的方法里的守卫子句整块吃掉**（饭桌的 `hitRedlines`、聆听者的 `secretWishes` 各中一次）。
  现判据：名字之前不得出现 `(`，整行须以 `{` 结尾，且行首不是 if/for/while/return 等控制词。
  兜底动作是**删完立刻 `git diff` 核对保留代码没少行**，别只信"编译过了"。
- **`mvn -q compile` 会被删掉的源文件骗过**。删除实体/Mapper 后不 clean 直接 compile 可能仍报 0，
  因为增量编译器复用了 target 里的旧 class。凡是删了文件的批次，一律 `mvn -q -o clean compile` 取真退出码。
- **孤儿扫描的引用者包含静态内容库**。功能删完后，`Couple*Bank` 里的 `kindLabel/modeLabel` 这类
  只服务已删功能的映射方法，会因为引用实体常量而让实体"看起来还活着"，表就漏进了 V50 drop 清单。
  每轮删完要回扫一遍 Bank 的死方法再跑孤儿。
- **跨行签名的成员删不掉，会留下悬空调用**。判据要求「整行以 `{` 结尾」，而
  `public static String yearSummary(String year, int moments, ...,
 long seed) {` 首行以逗号结尾，
  于是 `yearSummary` 躲过删除、它调用的 `yearTitle` 却被删了 —— 编译在**下一轮全量**才炸出来。
  同类问题在 Controller 的 `year(...)` 上出现过一次。所以每删一轮都要跑全量 `mvn -o test`，
  不能只跑被改模块那一个测试类。
- **测试里的多行 stub 不能按行删**。直接过滤含死标识符的行会截断 `lenient().when(x.find(...))` 这类跨行语句，
  留下括号不闭合的碎片（我把 Focus 的测试删坏过一次，只能 git 恢复重写）。
  现用 `cut-stmt.mjs`：按语句走到 `)`/`}` 平衡为止，且**花括号总数不变才写盘**，否则拒绝并保留原文件。
