# 情侣空间 50 项新功能总览（F50-F99）

> 本轮迭代以「情绪价值」为主线：让两个人有更多理由说早安、有台阶下、有东西一起养、有回忆可翻、有小确幸可捡。
> 全部功能不涉及照片/视频上传；题库/文案类内容均为内置静态库，无第三方依赖。
> 后端：`are-chat` Spring Boot（Flyway V15-V18）；前端：`are-chat-web` Vue3（情侣空间 11 个页签）。

## 批次一：惊喜与期待（F50-F59）— `gift` / `surprise` 页签

| # | 功能 | 说明 | 入口 |
|---|------|------|------|
| F50 | 恋爱刮刮乐 | 每天一张，刮开有小奖励/小心思 | `CoupleSurprise` |
| F51 | 恋爱盲盒 | 存一枚盲盒，对方随机时机开出 | `CoupleSurprise` |
| F52 | 心动闹钟 | 约定时刻互相叫醒的甜蜜闹钟（job 推送） | `CoupleSweetAlarm` + job |
| F53 | 思念速递 | 想 TA 的时候一键投递思念卡 | `CoupleMissExpress` |
| F54 | 爱情花园 | 一起浇水养成的小花园，缺水会枯（job 巡检） | `CoupleGarden` |
| F55 | 每日玫瑰 | 每天送出一朵不掉线的玫瑰 | `CoupleRose` |
| F56 | 幸运签 | 每天一支恋爱运势小签 | `CoupleFortuneSlip` |
| F57 | 告白重现 | 把当年的告白再放一遍 | `CoupleConfession` |
| F58 | 恋爱藏宝图 | 藏宝+寻宝的小游戏 | `CoupleTreasure` |
| F59 | 生日贺卡 | 生日当天双方各收到一张专属贺卡（job） | `CoupleSurpriseJob` |

## 批次二：懂我与被接住（F60-F69）— `care` / `rituals` / `letters` 页签

| # | 功能 | 说明 | 入口 |
|---|------|------|------|
| F60 | 求抱抱 | 选感受→系统给话术卡→对方精准接住 | `CoupleComfort` |
| F61 | 矛盾复盘 | 和好锦囊：双方各自复盘才生成合页 | `CoupleMakeup` |
| F62 | 道歉券 | 在途最多 2 张，用券道歉不丢人 | `CoupleMakeup` |
| F63 | 陪聊话题卡 | 没话找话时的 20 个甜蜜话题 | `CoupleComfort` |
| F64 | 情绪同步率 | 每天双方心情匹配度 + 连续同步 | `CoupleComfort` |
| F65 | 深夜关怀 | 当天有负面情绪且未被接住 → 21/23 点推送 | `CoupleCareTalkJob` |
| F66 | 今日真心话 | 双方同题必答，答完拼在一起看 | `CoupleTruth` |
| F67 | 匿名树洞 | 匿名投递，回答后才揭晓是谁 | `CoupleWhisperBox` |
| F68 | 心灵感应 | 同题默契测试，每天 3 轮 | `CoupleTruth` |
| F69 | 情话储蓄罐 | 存情话只发预告，21:00 「利息」送达 | `CoupleWhisperBox` + job |

## 批次三：共同养成（F70-F79）— `growth` 页签（新增）

| # | 功能 | 说明 | 入口 |
|---|------|------|------|
| F70 | 双人挑战赛 | 每天同一道小挑战，双完成即达成 | `CoupleChallenge` |
| F71 | 恋爱存折 | 每天存一件小事，攒连续天数与里程碑 | `CoupleChallenge` |
| F72 | 百日之约 | 单活跃约定，满 100 天双方打卡自动达成 | `CoupleChallenge` |
| F73 | 心愿互换 | 许愿/接单/实现，接单不能是自己 | `CoupleWishBoard` |
| F74 | 共读计划 | 各自报进度，双方都到终点即完结 | `CoupleReadWatch` |
| F75 | 旅行心愿地图 | 钉下想去的地方，去过打卡留念 | `CoupleWishBoard` |
| F76 | 追剧清单 | 共同集数进度，追到完结自动庆祝 | `CoupleReadWatch` |
| F77 | 星座配对 | 12 星座元素相性（仅供情趣） | `CoupleDict` |
| F78 | 恋爱词典 | 收录只有你们懂的词 | `CoupleDict` |
| F79 | 下次一定 | 随口的承诺落单可催办（1h 冷却），兑现销账 | `CoupleWishBoard` |

## 批次四：回忆资产（F80-F89）— `timeline` / `badges` / `letters` 页签

| # | 功能 | 说明 | 入口 |
|---|------|------|------|
| F80 | 恋爱编年史 | 第一次/纪念日/胶囊/兑现/旅行/真心话按年成史 | `CoupleChronicle` |
| F81 | 记忆考古卡 | 随机挖一张 30 天前的旧记录 | `CoupleChronicle` |
| F82 | 恋爱问答机 | 用你们真实的日子出选择题 | `CoupleChronicle` |
| F83 | 甜蜜语录收藏册 | 甜话会过期，收藏不会 | `CoupleKeepsake` |
| F84 | 远期胶囊预设 | 胶囊可封 1/3/5/10 年 | `CoupleCapsule` |
| F85 | 周年报告 | 最近一个周年的「这一年我们」 | `CoupleAnniversaryReport` |
| F86 | 生日回顾 | TA 生日那天，历史上的我们 | `CoupleAnniversaryReport` |
| F87 | 胶囊到期提醒 | 每天 09:05 提醒到期胶囊（job） | `CoupleMemoryJob` |
| F88 | 恋爱电影票根 | 散场不散，票根为证 | `CoupleKeepsake` |
| F89 | 我们的歌单 | 每首歌都藏着一段我们的故事 | `CoupleKeepsake` |

## 批次五：体验与其它菜单（F90-F99）

| # | 功能 | 说明 | 入口 |
|---|------|------|------|
| F90 | 私聊快捷贴贴 | 聊天工具条一键亲亲/抱抱/摸摸头 | `ChatView` |
| F91 | 消息彩蛋指令 | 发送 `/抱抱` 等暗号触发全屏特效 | `ChatView` + `effects.ts` |
| F92 | 生日提前预告 | 生日前 3 天提醒双方准备惊喜（job） | `CoupleSurpriseJob` |
| F93 | 节日登录页文案 | 特殊日子登录页说应景的话 | `LoginView` |
| F94 | 通知分类筛选 | 空间动态按任务/情绪/回忆/系统筛选 | `CoupleView` |
| F95 | 今日看点聚合卡 | 今天值得做的甜蜜小事一眼看清 | `CoupleTodayBoard` |
| F96 | 年度热力日历 | 一整年的互动画成一片星空 | `CoupleHeatmap` |
| F97 | 窄屏与暗色适配 | 新组件媒体查询 + CSS 变量全覆盖 | 各组件 |
| F98 | 新手引导 | 首次进入空间的三步漫游 | `CoupleView` |
| F99 | 功能总览文档 | 本文档 + 回归测试 | `docs/` |

## 数据迁移

- `V15__couple_surprise.sql`：惊喜与期待（刮刮乐/盲盒/闹钟/思念/花园/玫瑰/幸运签/告白/藏宝图）
- `V16__couple_care_talk.sql`：懂我与被接住（求抱抱/矛盾复盘/道歉券/真心话/树洞/心灵感应/情话储蓄罐）
- `V17__couple_growth.sql`：共同养成（挑战赛/存折/百日之约/心愿互换/共读/旅行心愿/追剧/词典/下次一定）
- `V18__couple_keepsake.sql`：回忆资产（语录册/票根/歌单；其余为现有数据聚合或常量放宽）

## 质量基线

- 后端：`mvn test` 155 个用例全绿（本轮五批共新增约 47 个 Mockito 单测）
- 前端：`pnpm build`（vue-tsc）通过；`pnpm test`（Vitest）69 个用例全绿（本轮新增 14 个）
- 推送事件：全部走 `ImPushService.pushCoupleEvent(Both)`，前端 `stores/couple.ts` 统一分发
