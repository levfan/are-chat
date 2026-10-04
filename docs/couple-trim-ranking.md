# 情侣空间功能裁剪 · 全量排序与保留 10 清单

> 生成日期：2026-10-04
> 输入：前端功能卡索引 `are-chat-web/src/components/couple/coupleCards.registry.ts` 的 **174 张卡**（F1-F399 累计交付的功能卡）
> 输出：留 **10** 张、删 **164** 张。本文档是删除动作的唯一依据，任何一张卡的去留都以本表为准。

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
| 1 | 求抱抱 · `couple-comfort` | care | 5 | 5 | 5 | 4 | 5 | **24** | **留（10）** |
| 2 | 今晚饭桌（饭票+吃什么裁决） · `couple-dine-today` | dining | 5 | 5 | 4 | 5 | 5 | **24** | **留（10）** |
| 3 | 家务轮盘 · `couple-fy-spin` | factory | 5 | 5 | 4 | 5 | 5 | **24** | **留（10）** |
| 4 | 加班预报与留灯 · `couple-quest-overtime` | quest | 5 | 4 | 5 | 5 | 5 | **24** | **留（10）** |
| 5 | 贴贴宫格 · `couple-bond` | bond | 5 | 4 | 5 | 4 | 5 | **23** | **留（10）** |
| 6 | 今日一句话 · `couple-catch-daily` | catch | 5 | 3 | 5 | 5 | 5 | **23** | 删 |
| 7 | 安全词与暂停复盘 · `couple-catch-safeword` | catch | 5 | 4 | 5 | 5 | 4 | **23** | **留（10）** |
| 8 | 今日真心话 · `couple-deep` | talk | 4 | 5 | 5 | 4 | 5 | **23** | 删 |
| 9 | 电量预报 · `couple-echo-battery` | echo | 5 | 4 | 5 | 4 | 5 | **23** | 删 |
| 10 | 好事簿（被爱的证据） · `couple-echo-deed` | echo | 5 | 3 | 5 | 5 | 5 | **23** | **留（10）** |
| 11 | 对视十秒 · `couple-focus-gaze` | focus | 5 | 5 | 5 | 4 | 4 | **23** | 删 |
| 12 | 和好与道歉券 · `couple-makeup` | makeup | 4 | 5 | 5 | 5 | 4 | **23** | 删 |
| 13 | 心情日记 · `couple-mood` | mood | 5 | 3 | 5 | 5 | 5 | **23** | **留（10）** |
| 14 | 刮刮乐与盲盒 · `couple-surprise` | surprise | 5 | 5 | 4 | 4 | 5 | **23** | **留（10）** |
| 15 | 暗中心愿本 · `couple-catch-wish` | catch | 4 | 5 | 5 | 4 | 4 | **22** | 删 |
| 16 | 愿望券本 · `couple-cere-coupon` | ceremony | 4 | 4 | 5 | 5 | 4 | **22** | **留（10）** |
| 17 | TOP10 互猜 · `couple-cx-top` | codex | 4 | 5 | 5 | 4 | 4 | **22** | 删 |
| 18 | 能量补给 · `couple-echo-refill` | echo | 5 | 4 | 5 | 4 | 4 | **22** | 删 |
| 19 | 饭桌不低头 · `couple-focus-meal` | focus | 5 | 4 | 4 | 5 | 4 | **22** | 删 |
| 20 | 那年今天 · `couple-on-this-day` | chronicle | 5 | 4 | 5 | 4 | 4 | **22** | 删 |
| 21 | 情绪天气与急救箱 · `couple-care` | care | 4 | 3 | 5 | 4 | 5 | **21** | 删 |
| 22 | 我们的餐厅 · `couple-dine-restaurant` | dining | 4 | 4 | 4 | 5 | 4 | **21** | 删 |
| 23 | 走神温柔哨 · `couple-focus-nudge` | focus | 5 | 4 | 4 | 4 | 4 | **21** | 删 |
| 24 | 默契考验比划猜 · `couple-fun-talk` | comm | 4 | 5 | 4 | 4 | 4 | **21** | 删 |
| 25 | 爱情花园（玫瑰+幸运签） · `couple-garden` | garden | 4 | 4 | 4 | 4 | 5 | **21** | 删 |
| 26 | 社死往事（满一年转好笑） · `couple-laugh-cringe` | laugh | 4 | 5 | 5 | 4 | 3 | **21** | 删 |
| 27 | 冷笑话结冰榜 · `couple-laugh-joke` | laugh | 4 | 5 | 4 | 4 | 4 | **21** | 删 |
| 28 | 早晚安打卡 · `couple-rituals` | ritual | 5 | 3 | 4 | 4 | 5 | **21** | 删 |
| 29 | 匿名树洞与情话罐 · `couple-whisper-box` | talk | 4 | 5 | 5 | 3 | 4 | **21** | 删 |
| 30 | 雷区探测器 · `couple-catch-mine` | catch | 4 | 4 | 4 | 4 | 4 | **20** | 删 |
| 31 | 我们的小日子 · `couple-cere-founded` | ceremony | 3 | 4 | 5 | 4 | 4 | **20** | 删 |
| 32 | 恋爱编年史（考古卡+问答机） · `couple-chronicle` | chronicle | 4 | 3 | 5 | 5 | 3 | **20** | 删 |
| 33 | 被爱日历 · `couple-echo-calendar` | echo | 5 | 3 | 4 | 4 | 4 | **20** | 删 |
| 34 | 我们的第一次 · `couple-firsts` | memory | 3 | 4 | 5 | 5 | 3 | **20** | 删 |
| 35 | 笑点存档（现场证词） · `couple-laugh-moment` | laugh | 4 | 4 | 4 | 4 | 4 | **20** | 删 |
| 36 | 周年抽奖箱 · `couple-legacy-draw` | legacy | 5 | 5 | 5 | 3 | 2 | **20** | 删 |
| 37 | 悄悄话信箱 · `couple-letters` | letter | 4 | 3 | 5 | 4 | 4 | **20** | 删 |
| 38 | 误会倒带 · `couple-ls-misrewind` | listen | 4 | 4 | 5 | 4 | 3 | **20** | 删 |
| 39 | 趣味小游戏 · `couple-play` | play | 4 | 5 | 4 | 3 | 4 | **20** | 删 |
| 40 | 生病陪护单 · `couple-quest-nurse` | quest | 4 | 3 | 5 | 5 | 3 | **20** | 删 |
| 41 | 恋爱时光轴 · `couple-timeline` | couple | 5 | 2 | 4 | 5 | 4 | **20** | 删 |
| 42 | 心愿互换板（+下次一定） · `couple-wish-board` | growth | 3 | 4 | 5 | 4 | 4 | **20** | 删 |
| 43 | 里程碑徽章墙 · `couple-badges` | memory | 5 | 2 | 4 | 4 | 4 | **19** | 删 |
| 44 | 时光胶囊 · `couple-capsules` | capsule | 3 | 4 | 5 | 5 | 2 | **19** | 删 |
| 45 | 倒数日 · `couple-countdowns` | countdown | 4 | 2 | 3 | 5 | 5 | **19** | 删 |
| 46 | 今日体温同步（同熄灯+抱抱计量） · `couple-cozy-today` | cozy | 4 | 3 | 4 | 4 | 4 | **19** | 删 |
| 47 | 鼓励语罐 · `couple-echo-juice` | echo | 4 | 3 | 4 | 4 | 4 | **19** | 删 |
| 48 | 心动时刻 · `couple-heart-moments` | heart | 4 | 3 | 4 | 4 | 4 | **19** | 删 |
| 49 | 年度热力日历 · `couple-heatmap` | today | 5 | 3 | 4 | 4 | 3 | **19** | 删 |
| 50 | 不插电半小时 · `couple-focus-unplug` | focus | 5 | 3 | 3 | 4 | 3 | **18** | 删 |
| 51 | 恋爱加成清单 · `couple-game` | game | 4 | 4 | 4 | 3 | 3 | **18** | 删 |
| 52 | 回忆收藏册（语录/票根/歌单） · `couple-keepsake` | keepsake | 3 | 3 | 4 | 5 | 3 | **18** | 删 |
| 53 | 快乐突袭与中弹 · `couple-laugh-attack` | laugh | 4 | 4 | 4 | 3 | 3 | **18** | 删 |
| 54 | 小胜利账本与成就墙 · `couple-quest-win` | quest | 4 | 3 | 4 | 4 | 3 | **18** | 删 |
| 55 | 爱情保险柜与续约 · `couple-cere-vault` | ceremony | 3 | 3 | 4 | 4 | 3 | **17** | 删 |
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

## 三、本轮口径变更：从「留 50」收紧到「留 10」

用户 2026-10-04 的原始要求是：**整个系统太臃肿，从情侣空间开始裁；留下的要「操作简单」或「操作吸引兴趣」、注重情绪价值、但不虚无缥缈也不难实现；排序后只保留最核心最重要的 10 个功能，其余全部删除。**

第二节的 174 行分数表沿用不动（它就是排序依据），但「去留」列已整体改写：**留 10 / 删 164**。原先「留 50」方案的第四节（5 页签 50 张）与第五节（124 张归堆）随之作废，由下面的第四、第五节替换。

### 3.1 切线为什么切在第 10 名

前 16 名里 23 分及以上有 14 张（并列一堆），22 分有 5 张。纯按分数无法切到 10，因此本节的裁决规则按优先级排列，**每条都写清"谁因此落选"**，可复核：

| 序 | 规则 | 依据 |
|---|---|---|
| R1 | **同分先落选者 = 与已留卡重复同一个动作** | 「每天点一下说个状态」这一族里有 心情日记 / 贴贴宫格 / 今日一句话 / 电量预报 / 早晚安打卡 五张。只留 心情日记（唯一有情绪标签+随笔、且被心动值直接消费的一张）和 贴贴宫格（唯一是"给 TA 一个动作"而不是"报告自己"的一张）。`couple-catch-daily` 今日一句话(23)、`couple-echo-battery` 电量预报(23)、`couple-rituals` 早晚安打卡(21) 全部落选 |
| R2 | **同分先留者 = 承担积分经济闭环的卡** | 好事簿(EARN+2)、家务轮盘(EARN+3)、刮刮乐(EARN+5) 是三个赚分入口，愿望券本是唯一的花分出口。这四张即使分数被别的 23 分卡追平也必须留，否则积分台账会变成"只写不读"或"只读不写"的死表 |
| R3 | **情绪弧必须完整：日常→求助→冲突→关怀→惊喜→积累** | 冲突场景只留一张代表。`couple-catch-safeword` 安全词与暂停复盘(23) 胜过 `couple-makeup` 和好与道歉券(23)：道歉券的本质是"欠 TA 一次道歉"，与安全词的"喊停然后复盘"功能重叠，而安全词多带一份"吵架时不至于是灾难"的安全感，落点更硬（有使用记录与复盘补写两张表） |
| R4 | **落选的高分卡逐张写明去向** | `couple-deep` 今日真心话(23)：与 心情日记 同为"每日一条自我表达"，且它需要双方必答才能出结果（操作不是单人一步），违反"操作简单"；`couple-focus-gaze` 对视十秒(23)：需要两人同时在场的仪式，日常频次只有 4，且注意力保护区整族已在本轮清零；`couple-catch-wish` 暗中心愿本(22)、`couple-cx-top` TOP10 互猜(22)、`couple-echo-refill` 能量补给(22)、`couple-focus-meal` 饭桌不低头(22)、`couple-on-this-day` 那年今天(22) 按 R1/R5 落选 |
| R5 | **不做"回望型"功能** | 回忆/年报/盘点/热力/编年史这类"被动看历史"的卡，情绪价值靠数据量堆，裁剪后数据源本身就被砍掉，留下就是空壳。`couple-on-this-day`、`couple-keepsake`、`couple-chronicle`、`couple-heatmap`、`couple-firsts`、`couple-badges`、`couple-capsules`、`couple-legacy-draw` 全部清零 |
| R6 | **「点名必留」让位于本轮指令** | 上一版方案里用户点名的 9 张（加班预报与留灯、安全词、愿望券本、TOP10 互猜、对视十秒、我们的餐厅、周年抽奖箱、误会倒带、恋爱加成清单、心愿互换板）本轮只留下 加班预报与留灯、安全词、愿望券本 三张。这不是遗忘，是 R1/R3/R5 的直接结果：**"只留 10 个"和"9 张点名必留"不能同时成立**，本轮指令更新，按新指令执行 |

### 3.2 上一版人工裁决五条的失效情况

上一版（留 50）在切线上做了 4 删 1 留的人工干预，本轮结果如下——写出来是为了避免下次有人拿旧结论反驳新表：

| 卡 | 上一版结论 | 本轮 | 说明 |
|---|---|---|---|
| `couple-timeline` 恋爱时光轴 | 删 | 删 | 一致 |
| `couple-echo-calendar` 被爱日历 | 删 | 删 | 一致 |
| `couple-focus-unplug` 不插电半小时 | 删 | 删 | 一致 |
| `couple-quest-win` 小胜利账本 | 删 | 删 | 一致 |
| `couple-cere-vault` 爱情保险柜与续约 | 留（作为券本上游） | **删** | 它是"每月各夸一句→满 3/6/12 月掉愿望券"的券源。本轮只保留 10 张卡，**券本的进水口改由积分承担**（发券扣积分），不再需要第二条产券链 |

## 四、最终保留的 10 张卡

一屏能放完，全部是**单人一步或两步可完成**的操作，两两之间不重复动作；积分闭环（3 个赚分入口 + 1 个花分出口）完整保留。

| # | 功能卡 | 模块 | 分 | 一次操作是什么 | 为什么是它 |
|---:|---|---|---:|---|---|
| 1 | 求抱抱 · `couple-comfort` | Care | 24 | 点一个感受（难过/委屈/累/焦虑/emo）发出去；TA 那边点一张话术卡回一句 | 全场唯一"我在低位，需要被接住"的出口。操作 5 步长 0，情绪价值最高，且带 23:00 深夜陪伴兜底 |
| 2 | 今晚饭桌（饭票+吃什么裁决） · `couple-dine-today` | Dining | 24 | 每人投一票菜，页面直接给今天吃什么 | 每天真实要吵的那件事。一步投票、结果双方看到同一个答案，是"操作简单+具体不虚"的双满分 |
| 3 | 家务轮盘 · `couple-fy-spin` | Factory | 24 | 点一下转盘，本周谁干什么当场定，干完打勾 | 用随机性消掉"凭什么是我"的争执，现实摩擦最大的一环；同时是积分 EARN 入口 |
| 4 | 加班预报与留灯 · `couple-quest-overtime` | Quest | 24 | 说一句"我今天要加班到几点"；TA 那边点一下留一盏灯 | 异地/晚归最戳人的一句"我等你"。灯卡只有对方能留，这个不对称为情绪价值服务 |
| 5 | 贴贴宫格 · `couple-bond` | Bond | 23 | 从宫格里选一个动作（抱抱/亲亲/举高高…）发给 TA | 每日亲密的底座，带动作流与里程碑；同时给心动值供"双向往来"的数 |
| 6 | 心情日记 · `couple-mood` | Core | 23 | 选一个心情表情，可附一句 | 频次 5、落点 5：它是整个空间"TA 今天怎么样"的唯一底色数据源，被心动值与深夜陪伴消费 |
| 7 | 安全词与暂停复盘 · `couple-catch-safeword` | Catch | 23 | 约定一个词；吵架时点它喊停；事后补一句复盘 | 冲突是情侣空间唯一"必须有"的负面场景。有使用记录（一天一次）和复盘补写，不是口号 |
| 8 | 好事簿（被爱的证据） · `couple-echo-deed` | Echo | 23 | 记一行"TA 为我做的事"，可给这条加星 | 被爱的证据越攒越多，是唯一一张"回顾价值随使用自动增长"的卡；积分 EARN 主入口 |
| 9 | 刮刮乐与盲盒 · `couple-surprise` | Surprise | 23 | 刮一张券 / 到日开一次盒 | 拆封感 = 钩子满分；券由 TA 送、由送券人核销，核销即 EARN+5，把"惊喜"接进积分 |
| 10 | 愿望券本 · `couple-cere-coupon` | Ceremony | 22 | 花积分发一张券给 TA；TA 用掉时点核销 | 积分唯一出口，"给你造一个愿望"。没有它整条积分经济只有进水口，攒的分永远花不掉 |

**页签从 11 个收敛到 3 个**（见第八节）：🫶 今天（心情日记/贴贴宫格/求抱抱/安全词）、🍚 过日子（今晚饭桌/家务轮盘/加班预报与留灯/愿望券本）、🎁 小惊喜（刮刮乐与盲盒/好事簿）。

## 五、删除的 164 张（执行归堆）

现存代码里的 110 张卡全部来自第二节表格，其中 **100 张本轮删除**。按"整模块清零"与"模块内定向删"分开列，前者可整文件删（风险最低），后者要在同一 Controller/组件内动刀（风险集中点）。

### 5.1 整模块清零（17 个后端模块，连 Controller/Service/Bank/实体/测试/组件一起删）

| 模块 | Controller | 现存卡数 | 删掉的卡 |
|---|---|---:|---|
| 欢笑银行 | `CoupleLaughController` | 10 | moment / daily / joke / cringe / attack / guess / rx / style / week / year |
| 注意力保护区 | `CoupleFocusController` | 10 | night / slot / queue / meal / gaze / unplug / nudge / weekly / detox / year |
| 传世系统 | `CoupleLegacyController` | 10 | ten / audit / speech / milestone / fx / brand / review / list / draw / level |
| 我们百科 | `CoupleCodexController` | 5 | cx-codex / cx-quiz / cx-top / cx-dossier / cx-soul |
| 倾听与发声 | `CoupleListenController` | 5 | ls-slot / ls-voice / ls-misrewind / ls-letter / ls-care |
| 回忆资产 | `CoupleMemoryController` | 5 | capsules / countdowns / badges / on-this-day / firsts |
| 体温同步 | `CoupleCozyController` | 2 | cozy-today / cozy-monthly |
| 沟通增强 | `CoupleCommController` | 1 | fun-talk |
| 趣味小游戏 | `CouplePlayController` | 1 | play |
| 共同养成 | `CoupleGrowthController` | 1 | wish-board |
| 二人制造厂之外的游戏位 | `CoupleGameController` | 1 | game |
| 爱情花园 | `CoupleGardenController` | 1 | garden |
| 回忆收藏册 | `CoupleKeepsakeController` | 1 | keepsake |
| 和好与道歉券 | `CoupleMakeupController` | 1 | makeup |
| 真心话/树洞/感应/情话罐 | `CoupleTalkController` | 2 | deep / whisper-box |
| 恋爱编年史 | `CoupleChronicleController` | 1 | chronicle |
| 今日看点+年度热力 | `CoupleTodayController` | 1 | heatmap |

另外，**后端 Controller 已经删干净、但前端 api/types/组件还留着死枝**的 8 个模块（老黄历/我们公司/身体通知/修复车间/两家与朋友/明日邮局/扮演剧场/深度陪伴，以及更早的 确定感/默契亲密/文字浪漫/成长系/生活经营/时光博物馆/生活账本/异地恋/小仪式/共享空间/条约/清单/城市/基金），本轮一并把前端残留删掉——它们对应的 `coupleApi.*` 方法仍在 `src/api/couple.ts` 里（poem 21 个、secure 20 个、coach 20 个、life 19 个、distance 18 个、daily-life 18 个、spark 16 个、growth 28 个、comm 23 个 等方法都指向已不存在的后端路由，点了就是 404）。

### 5.2 模块内定向删（9 个模块）

| 模块 | 留 | 删掉的卡 | 备注 |
|---|---|---|---|
| 关怀 Care | comfort | `couple-care`（情绪天气与急救箱） | `CoupleCareController` 里 comfort/chat-topics/mood-sync 三组端点迁到新 `CoupleComfortController`，`CoupleCareService`（weather/first-aid/reconcile/praise/cycle）整文件删除；`CoupleCareTalkJob` 只保留 23:00 深夜陪伴 |
| 聆听者 Catch | catch-safeword | wish / mine / sensitive / thread / say / protocol / topic / **daily** / year | 只留 `CoupleCatchSafeword` + `CoupleCatchSafewordUse` 两张表 |
| 小日子 Ceremony | cere-coupon | cere-founded / cere-vault / cere-feel / cere-almanac | 券本不再依赖保险柜产券，改为纯积分兑换（见第七节） |
| 饭桌 Dining | dine-today | dine-week / dine-restaurant / dine-year | 只留 `CoupleDineTicket`；`couple_dine_rate`/`couple_dine_nogo` 随之删除 |
| 回音壁 Echo | echo-deed | juice / refill / slow / highlight / receipt / **battery** / self / calendar / year | 只留 `CoupleEchoDeed` |
| 二人制造厂 Factory | fy-spin | fy-shop / fy-errand / fy-care / fy-books | 只留 `CoupleSpinTask`；`CoupleFactoryService` 里的积分 EARN 记账保留 |
| 人生关卡 Quest | quest-overtime | upcoming / battle / nurse / pod / move / night / valley / win / report | 只留加班预报与灯卡两张表 |
| 惊喜 Surprise | surprise（卡名=刮刮乐与盲盒） | 卡内 心动闹钟 / 思念速递 / 藏宝图 / 告白重现 四个子功能 | 按卡名诚实收口：`CoupleSurpriseJob` 只保留生日彩蛋，其余三个定时分支连表一起删 |
| 核心 Core | mood | letters / heart-moments / promises(约定) / items(清单) / pacts(条约) / funds(基金) / cities(双城) / timeline(时光轴) / checkins(早晚安) / question+answers(每日一问) | `CoupleController`/`CoupleService` 只保留空间建立、纪念日设置、心情、心动值、资料卡、通知、收藏。`couple_anniversary`/`couple_space`/`couple_invite`/`couple_mood`/`couple_mood_reaction`/`couple_action`/`couple_point_ledger`/`couple_user_pin`/`couple_notify*` 为地基表，**不进 drop 清单** |

## 六、数据库处理

- 已入库脚本（V1-V50）内容一律不改；本轮新增 **`V51__drop_couple_features_except_top10.sql`**，写法仍是 `DROP TABLE IF EXISTS`，MariaDB 10.11 与 H2 `MODE=MySQL` 双认、幂等（新库 `baseline-on-migrate` 会从 V1 全量重放到 V51）。
- drop 清单**由代码扫描生成，不手写**：以保留侧实体的 `@TableName` 反查为准，「迁移历史里建过的 couple_* 全集 − 现存实体表」的差集才是要 drop 的；`schema.sql` 同步删除对应表定义（它是全量结构文档，不被运行时执行，但必须与 V 链一致）。
- **不做数据迁移、不做备份表**：产品裁剪，被裁功能的数据随之作废；生产库执行前另行手动备份，脚本本身不带备份逻辑。
- 校验口径：`mvn test` 里的 `@SpringBootTest` 会打 H2 跑全量 Flyway，V51 在空库上从 V1 一路执行到 V51 必须无错；并用集合等式自证「建表数 − drop 数 = 存活实体数」，等式不成立就拒绝写盘。

## 七、心动值与积分经济

### 7.1 积分台账：闭环原样保留

现状（已回代码核实，写入点均在保留卡上）：

| 方向 | 卡 | 位置 | 分值 |
|---|---|---|---:|
| EARN | 好事簿 | `CoupleEchoService:257` | +2（被记的那位） |
| EARN | 家务轮盘 | `CoupleFactoryService:150` | +3（打勾的人） |
| EARN | 刮刮乐 | `CoupleSurpriseService:132` | +5（送券人核销） |
| SPEND | 愿望券本 | `CoupleCeremonyService:264` | 按券面分（缺省 10）扣发券人 |

`couple_point_ledger` 表与这四个写入点**全部保留**，本轮不需要重接积分入口（上一版已经重接过）。唯一消失的读者是 `CoupleLegacyService:84` 的周年抽奖箱奖池——抽奖箱已删。因此台账的读者改为只剩心动值，见 7.2。

### 7.2 心动值：改成只吃保留卡的活数据

`CoupleService.intimacy()` 当前算式（`CoupleService.java:849`）是
`互道早安×1 + 互道晚安×2 + 一问双答天数×2 + 心情条数×1 + 台账累计EARN×1`。
其中 早安/晚安（`couple-rituals` 已删）与 每日一问双答（`couple-question` 已删）本轮都变成永远为 0 的死项，必须换掉：

```
score = 心情条数×1 + 贴贴双向往来天数×2 + 今日留灯天数×3 + 好事簿条数×2 + 安全词被复盘天数×2 + 台账累计EARN×1
```

七个项全部来自保留的 10 张卡，`IntimacyBreakdown` 的字段随之改为这六项（前端 header 展示同步改）。
**7 级恋爱等级阶梯与阈值不动**（0/50/150/300/500/800/1300），避免出现"裁完就全员掉级"的观感倒退。

### 7.3 券本进水口的替代

`couple-cere-vault`（爱情保险柜）原每月各夸一句掉券的链路删除后，愿望券本改为**纯积分购买**：发一张券 = 扣发券人积分（本来就是 SPEND 出口），不再保留第二条免费产券路径。这样积分只有一条赚法（三张卡）和一条花法（券本），读者也从"抽奖箱"收敛到"心动值 + 券本余额"。

## 八、执行顺序

1. ~~重写本文档去留列与第四~七节~~ ✅ 已完成；
2. **后端整模块清零**（5.1 的 17 个 Controller 连 Service/实体/Bank/测试一起删）→ `mvn -q -o clean compile` 取真退出码；
3. **后端模块内定向删**（5.2 的 9 个模块，按 `.tmp-audit/` 七步手法）→ 每模块一次 clean compile；
4. **心动值改算**（7.2）+ 单测锁副作用 → `mvn -o test`；
5. **V51 drop 脚本 + schema.sql 同步**，跑集合等式与空库全链路；
6. **前端**：删组件与卡片区块 → registry 收敛到 10 张 3 页签 → 以「后端存活端点清单」为唯一依据剪 `api/couple.ts`（现存 565 个方法）与 `types/index.ts`、`stores/couple.ts` 死枝 → `pnpm build` + `pnpm test`；前后端 interface/record 逐字段机械比对；
7. **收尾**：同步 `are-chat-map` / `are-chat-web-map` 两个 skill 与 wiki，按 db/后端/测试/前端/文档分项 commit。

跨仓改动一律分批 commit、数据库脚本独立成 commit；**本轮不 push**，等用户过一遍再推。

## 九、执行进度账（本轮已收官）

**结论：裁剪已全部落地，两仓构建与测试双绿，未 push。**

### 9.1 收口后的规模（全部为实测，不是估算）

| 维度 | 裁剪前 | 现在 | 判据来源 |
|---|---:|---:|---|
| 后端 couple 包 java 文件 | 292 | **74** | `find src/main/java/com/smart/chat/couple -name '*.java' \| wc -l` 实测（本表早前记的 78 是删干净前的草稿数） |
| 后端 Controller | 30 | **13** | 源码扫描 |
| 后端 HTTP 映射 | 793（v7 快照） | **57** | `@*Mapping` 抽取 |
| couple_* 表 | 297 建过 | **19 存活**（V50 drop 193 + V51 drop 85） | 实体 `@TableName` 反查 |
| 前端功能卡 | 110（registry） | **10** | registry 计数 |
| 前端页签 | 11（含 5 组子页签） | **3**（today/life/gift，无子页签） | CoupleView |
| 前端情侣组件 | 40 | **13**（含 registry/Collapsible） | ls 计数 |
| `api/couple.ts` 方法 | 565（2096 行） | **57（218 行）** | 按后端存活端点逐条比对 |
| `types/index.ts` | 403 个类型 / 4132 行 | **200 个 / 1974 行**（情侣死类型删 203） | 分词引用扫描 |
| `stores/couple.ts` | 3453 行 | **370 行** | wc |
| 后端情侣 WS 事件 | ~472 | **37** | Service/Job 源码抽取 |

### 9.2 验收台账（每步的实际退出码）

| 步骤 | 命令 | 结果 |
|---|---|---|
| 裁剪前基线 | `mvn -o test` | 272 用例 0 失败，MVN_EXIT=0 |
| 裁剪前基线 | `pnpm test` / `pnpm build` | 182 用例 / build，双 EXIT=0 |
| 第十六轮（整模块清零 17 个） | `mvn -o test` | 184 用例 0 失败，EXIT=0 |
| 第十七轮（9 模块定向删） | `mvn -q -o clean compile` | EXIT=0 |
| 心动值改算 | `mvn -o test` | 158 用例 0 失败，EXIT=0 |
| V51 + schema 同步 | `mvn -o test`（H2 空库全量重放） | Successfully applied 51 migrations，now at v51，EXIT=0 |
| 结构对账 | `verify-v51.mjs` | 297 − 278 = 19，四条集合等式闭合 |
| 前端裁剪后 | `pnpm build` / `pnpm test` | EXIT=0 / 74 用例全过（含一条 mock 覆盖守卫） |
| 收尾清码 | `pnpm build` / `pnpm test` | EXIT=0 / EXIT=0（CoupleView 死代码与 coupleTheme.ts 删除后复跑） |
| 前后端契约 | `contract-check.mjs` 24 对 | 逐字段（名+顺序）零不一致 |
| WS 事件对账 | 后端 `pushCouple*` 抽取 40 项 − 2 个私信域 − 1 个实参字面量 = 37，与 `are-chat-web/wiki/ws-events.md` 表内 37 项做集合差 | 双向「只在一边」均为空 |

守卫可证伪性（都是先证明它会红才当证据用）：
- 保留清单命中数断言：把一张卡名改错 → 命中 9 → 抛错；
- `gen-v51` 的 `alive ∩ drop = ∅` 与 `V50∪V51 = 应删全集` 两条集合等式；
- `sync-schema-v51` 改成先算后写，算术不闭合即拒写盘；
- `prune-api` 首轮跑出「保留 0 / 删除 565」正是因为归一化顺序错，被"保留数应等于端点数"这条对账暴露；
- `prune-api-imports` / `prune-types` 都带"随机取一个判为活的类型反查"的自证，且踩过一次真坑：脚本里的 `\b` 被批量编辑退化成退格符，判据静默全空——现在一律改用标识符分词集合；
- **新增**：`gen-wiki-api-layer.mjs` 的「说明列不得抽到代码」守卫——注入一段 `http.postJson<...>` 假文案后脚本 EXIT=1 并逐行点名，先证明能红；同一脚本另带「方法数必须 57」「每个方法必须抽到 URL」两条。
- **新增**：`couple.spec.ts` 的「mock 工厂必须覆盖 api/couple.ts 的每个方法」守卫——删掉 `comfortCards`（`beforeEach` 完全不引用的方法）→ `Tests 2 failed \| 22 passed`，报 `mock 工厂缺：coupleApi.comfortCards`；反向证据也记着：删 `sendAction` 时**没有**这条守卫会 23/23 全绿，说明「逐个列出 mock」本身不是守卫，用例真点到的路径才是。

### 9.3 保留的 19 张表与 10 张卡的对应

地基 5：`couple_space` `couple_invite` `couple_anniversary` `couple_notify` `couple_user_pin`
心动值供数 1：`couple_point_ledger`（三赚一花全保留）
卡 14：mood(+mood_reaction) / action / comfort / catch_safeword(+use) / dine_ticket / echo_deed / ceremony_coupon / spin_task / quest_overtime / scratch / mystery_box

### 9.4 有意没做的事（下次别当缺陷顺手"修"）

1. **前端未做真后端逐按钮巡检**：本轮只跑了 vitest（74 用例）与 vue-tsc，没有起后端+浏览器逐卡点击。按 v7 的教训，只有真后端巡检能照出运行时 500 与"点了没反应"，**这项待做**。
2. **`docs/couple-features-v3~v7.md` 未同步删减**：那些是历史规格，保留原样作为决策留痕；只在 `docs/couple-features.md` 顶部加了一条「历史记录，非现役清单」的指路横幅，正文一字未改。现役口径以本文档第四节 + 两仓 `*-map` skill 为准。
3. **wiki 已与代码逐条重核**：后端 `Home/architecture/api/couple-space/database/modules/scheduled-jobs`、前端 `Home/architecture/pages-routing/state-management/api-layer/ws-events/testing/dev-guide` 全部按现役代码改过（api-layer 由 `gen-wiki-api-layer.mjs` 从源码生成，不手抄）；`docs/acceptance-v5~v7.md` 是带时间戳的验收记录，按其自身口径不动。
4. **心动值阶梯阈值未重标定**（仍 0/50/150/300/500/800/1300）：换成六项活数据源后同样日子的分会变，故意不动，避免"裁剪即掉级"的观感；若要重标定请单独提一次。
5. **两仓均未 push**：本轮按任务既定口径只做到本地 commit，等人工过一遍。
