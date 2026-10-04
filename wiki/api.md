# REST 接口总表

> 本页回答：全部 Controller 的端点清单（方法+路径+一句话）。路径省略类前缀 `/api`；couple 系前缀见各节标题。**当前口径（2026-10-05 v8 后）**：非 couple 侧 10 个 Controller / 56 个映射（未动，实测自源码注解），couple 侧 **17 个 Controller / 70 个端点**（2026-10-04 裁剪后是 13/57，v8 加 4 个 Controller 13 个端点；全库 52 个 Controller / 698 端点是裁剪前的历史口径，见 docs/couple-trim-ranking.md）。

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

> 2026-10-04 情侣空间裁剪后为 13 个 Controller / 57 个端点；**2026-10-05 v8 第一批补到 17 个 Controller / 70 个端点**
> （新增 streak 2 + question 3 + wish 7 + memory 1）。
> 由脚本从源码映射注解与 javadoc 生成，不是手抄；保留的 10 张卡与地基的对应关系见 [couple-space.md](couple-space.md)。

### `CoupleAdminController`（1）
- GET `/admin/stats` 情侣空间运营统计（仅管理员）。

### `CoupleBondController`（6）
- POST `/bond/actions` 发送一个贴贴动作（戳一戳/抱抱/亲亲/捏捏脸/蹭蹭/挠痒痒/在想你）。
- GET `/bond/actions` 最近动作流（新→旧，默认 50 条）。
- GET `/bond/stats` 贴贴统计：各类动作累计/双方占比/最近时间 + 今日双方动作数。
- POST `/bond/mood-reactions` 回应 TA 某天的心情（默认今天）：抱抱/亲亲/加油/摸摸头。
- GET `/bond/mood-reactions` 某天（默认今天）双方给彼此心情的回应。
- PUT `/bond/pet-name` 给 TA 设置专属爱称（空串清除）。

### `CoupleCareController`（6）
- GET `/care/comfort` 求抱抱看板：我今天的状态 + TA 待回应的求抱抱 + 最近记录。
- POST `/care/comfort` 发出求抱抱（每人每天一条，重复提交视为更新感受）。
- GET `/care/comfort/cards` TA 的安慰话术卡：按感受随机 3 张。
- POST `/care/comfort/handle` 回应 TA 的求抱抱（把抱抱和那句话送过去）。
- GET `/care/chat-topics` 低落时抽 3 张话题卡，解决「不知道聊什么」。
- GET `/care/mood-sync` 双方心情同频程度：一致占比 / 今天是否同步 / 连续同步天数。

### `CoupleCatchController`（4）
- GET `/catch/board` 安全词看板。
- POST `/catch/safeword` 约定/改写自己的安全词。
- POST `/catch/safeword/use` 喊一次暂停（一天一人只记一次）。
- POST `/catch/safeword/reflect` 事后补一句复盘（只有喊停本人能补）。

### `CoupleCeremonyController`（3）
- GET `/ceremony/overview` 券本总览。
- POST `/ceremony/coupon` 发一张愿望券（扣发券人积分）。
- POST `/ceremony/coupon/use` 核销一张愿望券。

### `CoupleController`（15）
- GET `/overview` 总览：未建立时返回待处理邀请（指引建立）；建立后返回空间与今天双方的心情。
- POST `/invites`
- POST `/invites/{id}/accept`
- POST `/invites/{id}/reject`
- DELETE `/invites/{id}`
- PUT `/anniversary` 在一起纪念日（用于计算在一起天数，双方都可改）。
- PUT `/profile` 空间个性化：我们的宣言 / 空间主题 / 贴纸墙佩戴。
- GET `/relationship-of/{username}` F44 恋爱中徽章：查某人是否在恋爱中 + 在一起天数（好友资料卡展示）。
- POST `/dissolve`
- GET `/anniversaries`
- POST `/anniversaries`
- DELETE `/anniversaries/{id}`
- POST `/moods` 记录/修改今天的心情（每人每天一条，重复提交视为修改）。
- GET `/moods` 双方最近 N 天的心情（1-90，默认 14），按日期新→旧。
- GET `/intimacy`

### `CoupleDiningController`（2）
- GET `/dining/today` 今日饭桌：双方饭票 + 撞菜 + 吃什么裁决。
- POST `/dining/ticket` 投今晚饭票（每人每天一票，重复投=改票）。

### `CoupleEchoController`（3）
- GET `/echo/vault` 好事簿看板。
- POST `/echo/deed` 记一件「TA 为我做的事」。
- POST `/echo/deed/star` 给这条证据加星。

### `CoupleFactoryController`（4）
- GET `/factory/board` 本周车间总览（轮盘分工 + 前几周欠账）。
- POST `/factory/spin` 一转定分工，一周一转。
- POST `/factory/spin/confirm` 对方认账这一格。
- POST `/factory/spin/done` 干的人自己打勾。

### `CoupleNotifyController`（2）
- GET `/notify` 我的最近 50 条通知 + 未读数。
- POST `/notify/read-all` 全部标记已读。

### `CouplePinController`（2）
- GET `/pin` 双方收藏清单。
- POST `/pin` 全量覆盖我的收藏。

### `CoupleQuestController`（3）
- GET `/quest/board` 加班看板。
- POST `/quest/overtime` 预报今晚忙到几点。
- POST `/quest/overtime/lamp` 给对方留一张到家灯卡。

### `CoupleSurpriseController`（6）
- GET `/surprise/scratches` 我的刮刮乐（自动补发本周的卡）。
- POST `/surprise/scratches/{id}/scratch` 刮开我的券。
- POST `/surprise/scratches/{id}/redeem` 送券人核销（承诺闭环，兑现即 +5 分归送券人）。
- GET `/surprise/boxes`
- POST `/surprise/boxes` 装一个盲盒（最早明天开箱）。
- POST `/surprise/boxes/{id}/open` 开盲盒（到开箱日才能拆）。

### `CoupleStreakController`（2）
- GET `/streak/board` 打卡看板：今天日期 / 当前连续 / 历史最长 / 累计确认天数 / 今天是否已打 / 昨天是否断 / 七档 `tiers`（含 `unlocked` 与派生的 `unlockedDay`）/ 下一档 key 与还差几天 / 近 21 格 `strip` / 补签价格·本月剩余额度·余额 / `canMakeup`。
- POST `/streak/makeup` 补签某一天（body `{day:yyyy-MM-dd}`），落一行 `MAKEUP` 并扣 20 分，返回整份看板。闸门四选一失败即 400：超出 7 天窗口 / 补今天 / 该日已打 / 本月满 3 次 / 余额不足。

### `CoupleQuestionController`（3）
- GET `/question/today` 今日一问：题号 + 题干 + 我的回答 + （双方都答完才有的）TA 的答案。
- POST `/question/answer` 回答或改写今天的答案（body `{answer}`，≤300 字），每人每天一行。
- GET `/question/history?days=` 回看最近 N 天（1-90，默认 14），没答的一侧返回 null。

### `CoupleWishController`（7）
- GET `/wish/board` 愿望清单，**按请求者视角脱敏**：`open` / `prepared`（只有标记人看得到）/ `fulfilled` 三组。
- POST `/wish/add` 添加愿望（body `{title,note,ownerUsername}`；owner 只能是本人或另一半，可空默认给自己许）。
- POST `/wish/prepare` 偷偷标记「已准备」（body `{id}`，只有对方能标自己许的愿；**不推任何 WS 事件**）。
- POST `/wish/unprepare` 撤销「已准备」（只有当初点的人能撤）。
- POST `/wish/fulfill` 许愿人确认实现（只有本人能点；这时才公开并推 `wish-fulfilled`）。
- POST `/wish/note` 改补充说明（只有记录人能改）。
- POST `/wish/remove` 删除（只有记录人能删，已实现的删不掉）。

### `CoupleMemoryController`（1）
- GET `/memory/page` 百日隐藏回顾页：一句话总结 + 时间轴。**服务端也挡一道**——历史最长连续不足 100 天返回 400「还差 N 天」，不靠前端藏页签。
