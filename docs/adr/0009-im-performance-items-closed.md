# ADR-0009：im/room 两项性能议题与 `ddd/cp2` 残枝的结案（用户授权代拍板）

- 状态：已采纳（2026-10-05）
- 相关：`docs/adr/0008-tactical-refactor-covers-all-contexts.md`、`docs/adr/0004-source-scan-architecture-guard.md`、`docs/adr/0006-request-log-filter.md`、`docs/ddd/06-ddd-standard.md`、`docs/acceptance-v7.md`（性能专项一节）

## 背景

DDD 战术收口交付时我留了两件"不属于本轮、等你拍板"的事：

1. v7 就挂着的 **im/room 性能议题**——缺陷账记的两半：联系人列表 `listFriends` 每好友 2 趟查询、各批聚合 `build()` 里的重算项（当时记的是「关卡的年度成就墙一次 9 个查询，27 个写接口全走这条路，即每个写接口约 23 次查询」）；
2. 本地分支 **`ddd/cp2`**——被我否决的旧方案残留，未 push。

用户的回复是「你帮我拍板就行」，即授权我自行裁决并执行。

**拍板前先实测，结果第一条就被推翻**：`listFriends` 的 2N 早在 2026-10-03 的 `8a91786`
（`perf(im): 联系人列表从每人 2 趟查询改成常量 5 趟`）治掉了，我却在 `docs/ddd/06-ddd-standard.md`
的「有意没做」表里把它当未做项复述了一遍——那一条是**过期口径**，本页顺带纠偏。
教训与既有口径一致：判断性结论写下来之前先读代码，别转述上一轮的账本。

## 决策

### 1. 联系人列表：已治，本轮只补它没收的第二半（做）

`GET /api/friends` 现在与好友数无关（常量 ~7 趟）。**同一个缺陷族里还有一条活的**：
`GET /api/friends/suggest` 的 `relation(me, name)` 在候选循环里每个候选跑三条查询
（`findByOwnerAndFriend` 一条边 + `findPendingBetween` 两个方向的待处理单），上限 10 个候选就是 **31 趟**。
这条是 `8a91786` 没收的后半——同一处代码、同一类循环内逐条查、同一个文件。

裁决：**改**，且只用端口上已有的批量方法，**不新增端口面**：

| 关系 | 改造前 | 改造后 |
|---|---|---|
| `friend` | `findByOwnerAndFriend(me, name)` × 每候选 | `findAllByOwner(me)` 的边集合一次取全 |
| `pending-out` | `findPendingBetween(me, name)` × 每候选 | `listOutgoing(me)` 的 `toUser` 集合 |
| `pending-in` | `findPendingBetween(name, me)` × 每候选 | `listIncoming(me)` 的 `fromUser` 集合 |

四态优先级（好友 > 我发出的 > 等我处理的 > 可添加）与判断顺序原样保留；
无候选时一趟仓储都不打（沿用 `findMessagesAtCreated` 空集合早退、避免 `IN ()` 的既有口径）。
`findByOwnerAndFriend`/`findPendingBetween` 本身留在端口上——`apply` 的两条闸门是**单对**判定，
不是列表，没有 N+1。

### 2. `build()` 重算项：按实测结案，不改契约、不挪懒读、不加预算守卫（不做）

v7 记的那件事**主语已经不在仓库里**，实测（源码调用点计数，含同类私有方法递归）：

- 全仓 `成就墙` 与 `/wall` **0 命中**，`CoupleQuestService.build()` 现在只有 **1 个**仓储调用点；
  `CoupleQuestBank` 的注释写着「随功能裁剪，其余关卡话术已下线」。
- v7 点名的七张卡里，`世界`/`注意力`/`欢笑`/`传世` 四个名字在现存 `couple/application` 下**已无对应类**
  （现存 16 个 Service：Bond/Catch/Ceremony/Comfort/Dining/Echo/Factory/Memory/Notify/Pin/Quest/
  Question/Streak/Service/Surprise/Wish），`回音壁`/`聆听` 的 `build()` 各 2 个调用点。
- 现存最重的写路径是 `CoupleStreakService#makeup` = **12 个调用点**（其中还含积分台账 `append`、
  补签日 `append` 这类真写入），其余 Service 最重公开方法在 4–9 之间。

结论：这是**常量**开销，不随数据量增长，与 `8a91786` 治掉的"每好友两趟全表扫"不是一类问题。
把它改小的唯一手段仍是要改契约（把重算子聚合挪出写响应 + 前端配合懒读），
代价是 27 个写接口形态一起动、收益是把 12 趟常量查询压成几趟——**在当前规模下不划算，结案**。

同时明确**不加**「每请求查询数预算」这类守卫：现有防线已经覆盖真正的风险面——
端口上只暴露批量方法（逐条查没有现成的枪可用），加上
`listFriendsNeverFallsBackToPerPeerQueries` / `suggestNeverFallsBackToPerCandidateQueries`
两条 `verifyNoMoreInteractions` 用例守着「把逐条查加回循环就红」。
若将来某张卡的调用点随数据量增长（出现按成员/按条数的循环），那才是重新立案的理由，
复核方法见最后一节。

### 3. `ddd/cp2`：删除（做）

分支三个 commit（`fd1b71b`/`5ded973`/`5f98b54`）建的三张表，main 已全部覆盖且覆盖得更厚：

| cp2 产物 | main 现状 |
|---|---|
| `Deed` + `DeedRepository` + 适配器 | 同名，另有 `DeedRepositoryAdapterTest`；`starBy` 返回 boolean 锁幂等 |
| `SpinTask` + `SpinRound` 值对象 | `SpinTask.draw(spaceId, week, rawItems, userA, userB, dice, alreadySpun)`——`SpinRound` 的清洗、上下限、去重、奇偶交替三条口径逐条在内，文案一字不差 |
| `OvertimeForecast` | 改名 `QuestOvertime` + `QuestOvertimeRepository` + 适配器 + 两个测试 |

因此它没有任何独占内容，裁决删除：`git branch -D ddd/cp2`（已执行，tip 为 `5f98b54`）。
它从未 push，回收窗口是 git reflog 的默认 90 天；真要找回用 `git branch cp2-restored 5f98b54`。

### 4. 顺手收掉的两把旧枪（做）

`PrivateMessageMapper.findLatestBetween` 与 `countUnread` 是 2N 写法的本体，`8a91786` 之后全仓零调用点，
只剩两处注释提名字。删除它们，并把端口 javadoc 里「方法名与 Mapper 保持一致所以可被 grep 复核」
这句已经不成立的话，改成指名真正在守口径的那条用例。

## 落地结果

| 决策 | 证据 |
|---|---|
| suggest 批量化 | `FriendServiceTest` 14 → **18** 用例 0 失败；messaging 契约面与 HEAD 逐条一致（路由 32、文案 73、事件 0，**0 消失 0 新增**）；变异证明见下 |
| 变异证明 | 把循环改回逐候选查询后 `suggestNeverFallsBackToPerCandidateQueries` 转红：`NoInteractionsWanted ... found this interaction on mock 'friendRepository'`（`FriendService.java:188` 三次未验证调用），`Tests run: 1, Failures: 1, Errors: 0`，MVN_EXIT=1；**是断言失败不是编译失败**。还原后 18/18 绿 |
| 死方法删除 | `mvn -o test -Dtest=FriendServiceTest,PrivateMessageRepositoryAdapterTest,FriendListBatchQueryTest,ArchitectureGuardTest` → 33 用例 0 失败，BUILD SUCCESS；守卫 6/6 仍绿 |
| 全量回归 | `mvn -o clean test` 在隔离 worktree（`.worktrees/verify-*`，detached 到具体 commit）跑两遍：`e9961db` 与本页之外的并行会话 `6c2eeb1` 各一次，均为 **`Tests run: 568, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`**（DDD 收口基线 563 → 本轮 +4 → 并行会话的日志用例 +1）。取隔离 worktree 而非主工作区，是因为同一时间有另一个会话在往 main 提交接口日志改动，主工作区的树不是任何一个 commit 的实况 |
| `build()` 结案 | 本页第 2 节的调用点实测；不改代码，因此无回归风险 |
| 分支删除 | `git branch -D ddd/cp2` → `Deleted branch ddd/cp2 (was 5f98b54)`；`git branch -vv` 只剩 main |

## 复核方法（下一个人怎么重跑这两个数）

1. **suggest 的趟数**：读 `FriendService.suggest` 的方法体，数 `xxxRepository.` 出现处——
   应在循环**外**（三趟批量 + 空候选早退）。若在循环内，`suggestNeverFallsBackToPerCandidateQueries` 必红。
2. **写路径调用点**：按「公开方法 → 同类私有方法递归 → `*Repository`/`*Mapper` 调用点」计数
   （一次性脚本，不属于仓库）。判据是**是否随数据量增长**：常量十几趟属于卡片聚合载荷的成本，
   不立案；出现按好友/按成员/按条数的循环才是 N+1，按 `8a91786` + 本页第 1 节的批量手法处理，
   并补一条 `verifyNoMoreInteractions` 防退化用例。

## 后果

- 「有意没做」表里那条**联系人列表 N+1 是错的**，已改为「已治，第二半 suggest 一并收口」；
  `docs/acceptance-v7.md` 的性能待办就地标注结案，不改写它的历史正文。
- im/room 侧不再有需要用户拍板的悬置项：能治的治了，不划算的按实测结案并留下复核方法。
- 这条 ADR 本身是「先实测再复述」的第二次教训记录（第一次见 ADR-0008 背景节）：
  上一轮账本里的数字**必须重新测一遍**才允许当作本轮依据，包括我自己在内。
