# are-chat（are-chat 前后端分离即时通讯）

系统名称 **are-chat**：由 2019 年的 Spring 4.3 + JSP + Hibernate 4 单体 WAR 改造而来的前后端分离即时通讯项目：

| 项目 | 说明 | 技术栈 |
|------|------|--------|
| `./`（本目录） | 后端服务 | JDK 25 · Spring Boot 4.1.1 · fastjson2 2.0.65 · MyBatis-Plus 3.5.17 · WebSocket |
| `../are-chat-web/` | 前端界面 | Vue 3.5 · Vite 6.4 · TypeScript 5.9 · Pinia 4 · Vue Router 4 · Element Plus 2.14 |

> 目录/包命名遵循约定：后端 `are-chat`，前端 `are-chat-web`，包名 `com.smart.chat`（系统名 are-chat，不出现重复包段）。
> 本 README 放在后端项目内；前端项目同级目录 `../are-chat-web/`。

## 目录结构

```
./（后端，本目录）
├── pom.xml                              # Boot 4.1.1 parent，java 25，fastjson2
├── Dockerfile / docker-compose.yml      # 一体化镜像构建与部署（见下文）
├── docker/                              # nginx.conf / start.sh / maven-settings.xml
├── DEPLOY.md                            # 完整部署指南
└── src/main/java/com/smart/chat/
    ├── SmartChatApplication.java        # 启动类
    ├── common/                          # ApiResponse / BusinessException / 全局异常 / Sessions
    ├── config/                          # fastjson2 转换器、CORS+登录拦截、WS/MyBatis-Plus 配置
    ├── auth/                            # 手机号注册 / 密码登录 / 会话（AppUser + PBKDF2 + 验证码）
    ├── im/                              # 好友/私聊/资料：实体+Mapper+Service+Controller+推送（核心）
    ├── room/                            # /ws/chat/{昵称} IM 实时推送通道（心跳/typing/presence）
    └── upload/                          # /api/files 上传/下载（IM 图片消息的存储通道）

../are-chat-web/（前端）
├── vite.config.ts                       # vitest + 自动导入 + /api /ws 代理
├── e2e/                                 # Playwright 端到端测试（chromium：登录/IM 全链路）
├── tests/unit/                          # Vitest 单元测试（http/auth/im/format/视图）
└── src/
    ├── api/                             # http 封装 + auth/files + im（好友/私信/资料）
    ├── stores/                          # Pinia：auth、im（好友私聊事件流）
    ├── components/im/                   # ImAvatar / EmojiPicker / MessageBubble / ProfileDialog
    ├── router/                          # /login + 布局内 chat/friends
    ├── layouts/  views/                 # IM 侧边栏布局与三个页面（登录/消息/好友）
    └── types/  utils/  constants.ts
```

## 后端（本目录）

### 运行

```bash
# 默认：H2 内存库，开箱即用（MODE=MySQL 兼容老库语法）
mvn spring-boot:run

# 连接老项目 MySQL（表结构与旧库一致）
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
#   环境变量：MYSQL_HOST / MYSQL_PORT / MYSQL_DB / MYSQL_USERNAME / MYSQL_PASSWORD
```

### 主要接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/sms-code` | 获取注册验证码（演示环境无短信网关，`devCode` 直接回显） |
| POST | `/api/auth/register` | 手机号注册 `{phone, username, password, code}`，成功即登录 |
| POST | `/api/auth/login` | 登录 `{account, password}`，account 为手机号或用户名（HttpSession，`CurrentUser`） |
| GET  | `/api/auth/me` | 当前用户（用户名 / 昵称 / 脱敏手机号 / 本次登录时间） |
| POST | `/api/files` | 上传（SHA-256 去重，IM 图片消息用，命中返回 `deduplicated=true`） |
| GET  | `/api/files/{id}/download` | 下载（RFC 5987 中文文件名，图片消息展示用） |
| GET  | `/api/friends` | 好友列表（未读数 + 最后一条消息 + 在线状态） |
| GET  | `/api/friends/suggest?q=` | 加好友输入联想：候选用户名 + 与我的关系（75） |
| POST | `/api/friends/requests` | 发好友申请（自查重：已是好友/重复申请/对方已申请） |
| GET  | `/api/friends/requests/incoming` / `outgoing` | 收到 / 发出的申请 |
| POST | `/api/friends/requests/{id}/accept` / `reject` | 同意 / 拒绝（同意建双向好友 + 系统消息） |
| PUT  | `/api/friends/{id}` | 好友备注 / 置顶 / 免打扰 |
| DELETE | `/api/friends/{id}` | 删除好友（双向移除） |
| GET  | `/api/messages/{peer}?before=&limit=` | 私聊历史（游标倒序分页，返回正序） |
| POST | `/api/messages/{peer}` | 发私信 `{content, type: text/image/poke/card/location, replyToId?}`（在线实时推送） |
| GET  | `/api/messages/{peer}/search?q=` | 会话内关键字搜索（未撤回，最近 50 条，正序） |
| POST | `/api/messages/{peer}/read` | 标记会话已读（清零未读） |
| POST | `/api/messages/{id}/recall` | 撤回（2 分钟内，双向同步；text/image 可撤回） |
| GET/PUT | `/api/profile` | 个人资料（emoji 头像 + 昵称 + 个性签名） |
| GET  | `/api/profile/{username}` | 好友资料卡（仅好友可看，403 拒绝非好友） |
| GET  | `/api/stats/me` | 会话统计（好友/收发/收藏/最活跃好友） |
| GET  | `/api/messages/{peer}/export` | 导出当前会话全部消息（JSON） |
| GET  | `/api/presence/online` | 全站在线用户列表与人数 |
| GET  | `/api/health` | 健康检查（免登录：运行时长/在线人数/版本） |
| WS   | `/ws/chat/{昵称}` | IM 实时事件推送通道 |

WS 协议（fastjson2 JSON）：
- `heart`/`heart-ack` 心跳（兼容裸字符串 `heartBeat`）、`system` 系统提示
- IM 推送：`dm{msgId,from,to,content,msgType,replyToId,created}` 私聊（含图片与引用）、`typing{from,to,typing}` 正在输入、`recall{msgId}` 撤回、`presence{username,online}` 上下线、`friend-request` / `friend-accepted` / `friend-deleted` 好友事件（服务端类型禁止客户端伪造）

## IM 功能一览（核心 2 + 四轮共 56 个功能点）

核心：
1. **加好友**：申请 → 对方同意/拒绝 → 建立双向好友关系（防重复、防加自己、名单校验）
2. **选好友聊天**：会话列表点开即聊，私聊消息落库持久化，支持分页拉历史

第一轮 10 项：
3. **最近会话列表**：置顶优先，按最后一条消息时间排序，带消息预览
4. **未读红点**：按好友计数，进入会话自动标记已读清零
5. **在线状态**：好友列表绿点 + 上下线 `presence` 实时广播
6. **消息撤回**：2 分钟内可撤回，双方同步显示"撤回了一条消息"
7. **对方正在输入**：输入实时推送（`typing`），4 秒无新输入自动消失
8. **拍一拍**：趣味戳一戳消息，居中灰字展示
9. **个人资料**：8 档低饱和头像底色（用户名首字作头像）+ 昵称 + 个性签名，侧边栏一键编辑
10. **emoji 表情面板**：32 个常用表情快捷插入
11. **好友备注**：备注名全站生效（会话/聊天窗/好友页），最长 32 字
12. **系统消息入聊天流**：成为好友时自动发一条系统消息，带来新友未读提醒

第二轮 10 项：
13. **图片消息**：聊天窗直接选图发送（复用 SHA-256 去重上传），气泡缩略图点击放大预览
14. **粘贴图片即发**：剪贴板里的图片 Ctrl+V 直接发送
15. **消息引用回复**：hover 气泡点「引用」，输入框上方显示回复条，气泡内渲染引用块
16. **会话内消息搜索**：聊天窗放大镜搜索历史消息（只搜未撤回，最近 50 条）
17. **会话免打扰**：免打扰会话未读不计入侧边栏/标题红点，列表红点变灰、带 🔕 标记
18. **新消息提示音**：WebAudio 现场合成（无需音频资源），免打扰会话不响
19. **浏览器标题未读计数**：切到别的标签页也能看到 `(3) are-chat`
20. **最近在线时间**：WS 上下线刷新 last_seen_at，显示「x 分钟前在线」
21. **消息时间分隔线**：今天 / 昨天 / M月D日 自动分组
22. **发送失败一键重试**：失败消息本地标记 ⚠ 发送失败，点重试原样重发

### 第三轮 20 项（43–62，本次新增）

43. **在线状态徽标与快捷切换**：好友列表/聊天窗显示「忙碌/离开」徽标，左侧栏一键切换自己的状态
44. **好友分组标签**：好友可设置分组（好友页维护），会话列表按分组筛选
45. **桌面通知**：页面不可见时新消息弹系统通知，点击通知聚焦并跳转会话（设置内可开关）
46. **WebSocket 断线自动重连**：指数退避重连 + 连接状态指示灯，重连成功自动拉平数据
47. **消息链接识别**：文本消息中的 URL 自动渲染为安全链接（rel=noopener）
48. **引用消息点击定位**：点引用块跳转到原消息并高亮
49. **双击气泡快捷 👍**：双击消息气泡直接 toggle 👍 回应
50. **只看未读 + 全部已读**：会话列表过滤未读会话，一键清零全部未读
51. **输入字数统计**：2000 上限实时计数，接近上限变红
52. **发送快捷键策略**：Enter 发送 / Ctrl+Enter 发送可切换（设置内持久化）
53. **登录会话信息**：/api/auth/me 返回本次登录时间，个人中心可见
54. **登录防爆破限流**：同一用户名 5 次失败锁 5 分钟（429 提示）
55. **会话统计**：好友数/收发消息/收藏/最活跃好友（左侧栏「我的统计」）
56. **聊天记录 JSON 导出**：服务端全量导出当前会话（含回应/编辑/撤回状态）
57. **全站在线人数**：/api/presence/online + 左侧栏实时显示在线人数
58. **多标签页登录同步**：一个标签页退出登录，其它标签页自动跳回登录页
59. **系统健康检查**：/api/health 免登录，登录页显示服务状态点与在线人数
60. **键盘快捷键**：Esc 关闭搜索/取消回复/退出编辑，Ctrl+F 聚焦会话内搜索
61. **搜索关键词高亮**：搜索结果命中关键词高亮显示
62. **好友申请附言**：申请好友可附言，对方收到的申请展示留言

> 另：第三轮同时把既有工具补齐接线——39 主题色、40 聊天字号、42 favicon 未读角标（原为未启用的死代码，现接入设置面板与标题栏）。

### 第四轮 14 项（63–76：颜值交互 + 手机号注册体系）

63. **6 套皮肤主题**：晴空蓝 / 樱花粉 / 暗夜紫 / 森林绿 / 落日橙 / 海洋青，主色与我的气泡渐变整体换肤（Element Plus 主色用 `color-mix` 跟随）
64. **聊天背景自定义**：5 种背景（默认 / 静谧夜空 / 樱花漫舞 / 薄荷微光 / 素笺纸纹），即时切换并持久化
65. **点赞飘心**：双击气泡点赞、或点任意表情回应时，气泡旁飘出爱心动画
66. **关键词全屏特效**：发送「生日快乐 / 新年快乐 / 下雪 / 爱你 / 撒花」等关键词触发 canvas 粒子特效（撒花、烟花、飘雪、气球）
67. **拍一拍自定义后缀**：设置「拍一拍后缀」（如"的小脑袋"），拍一拍消息展示自定义文案并带抖动动画
68. **气泡入场动画**：新消息气泡滑入、居中提示缩放淡入、撤回淡出
69. **图片灯箱**：点击图片全屏查看，支持缩放（滚轮/按钮）、旋转、重置、下载原图，Esc 关闭
70. **新消息浮动卡片**：非当前会话来消息时右上角滑入卡片（≤3 条自动堆叠、5 秒消失），点击直达会话
71. **在线呼吸光环**：在线好友头像带呼吸光环，忙碌/离开显示对应颜色小圆点
72. **好友名片卡片**：会话菜单「分享好友名片」，以卡片消息发送（头像 + 昵称 + 签名 + @用户名）
73. **位置分享卡片**：会话菜单「分享位置」，从预设地点选择后发送地图风格卡片
74. **hover 快捷表情条**：鼠标悬浮消息即在气泡旁弹出一键表情回应条
75. **加好友输入联想**：输入用户名时实时给出候选（带「可添加 / 已是好友 / 已申请 / 待你处理」关系标注与脱敏手机号），点选即回填；候选来自已注册用户，用户名大小写不敏感
76. **手机号注册体系**：手机号 + 验证码 + 用户名 + 密码注册，注册即登录；密码 PBKDF2-HMAC-SHA256 加盐存储；登录支持手机号或用户名（见下文「登录与注册」）

> 本轮的取舍：只做「看得见、点得动」的实用花活，不加性能/运维类特性；名片与位置复用既有 `private_message` 表（`msg_type` 扩展为 `card` / `location`），拍一拍复用 `poke` 类型并允许自定义内容。

### 测试

```bash
$env:JAVA_HOME='<jdk25 路径>'; mvn test    # 或 mvn test
```

覆盖：服务层 Mockito 单测、FriendService/PrivateMessageService 真实与模拟测试、
真实 H2 库测试（schema.sql 建表 + 事务回滚）、
`@WebMvcTest` 控制器切片（含 401 拦截）、
文件去重（并发落盘竞态/遍历路径拒绝/SHA-256 已知向量）、WS 注册表与协议桥（含 typing/防伪造/presence）、
图片与引用/搜索/免打扰服务测试、
真实容器 WebSocket 心跳与 typing 集成测试、应用冒烟测试，共 64 个用例。
注意：surefire 已配置 `-XX:+EnableDynamicAgentLoading`（JDK 25 默认禁用动态 agent，Mockito 需要）。

## 前端（../are-chat-web）

```bash
pnpm install
pnpm dev          # http://localhost:5173（/api 与 /ws 代理到 8080）
pnpm test         # Vitest 单元测试（jsdom）
pnpm build        # vite build + vue-tsc 类型检查
pnpm test:e2e     # Playwright（chromium；首次需 pnpm exec playwright install chromium）
```

登录与注册（第 76 项）：

- **手机号注册**：填手机号 → 「获取验证码」→ 用户名 + 密码 → 注册即登录。演示环境没有短信网关，验证码由后端生成后在响应里回显（`devCode`），前端直接展示并回填，同时写入后端日志。
- **规则**：手机号 11 位（`1[3-9]xxxxxxxxx`，全局唯一）；用户名 3~20 位小写字母/数字/下划线（全局唯一，输入自动转小写）；密码 6~64 位且必须同时含字母和数字；验证码 6 位、5 分钟有效、同号 60 秒内不可重发、最多试错 5 次。
- **登录**：账号可以是**手机号或用户名**，配合密码登录；同一账号连续失败 5 次锁定 5 分钟（429）。
- **只有 `app_user` 里注册过的账号才是合法用户**：加好友、发消息、输入联想都以它为准（固定白名单已移除）。
- **演示账号**：库里没有任何用户时，启动会自动播种 `alice` / `bob` / `carol`（手机号 `13800000001~3`，密码统一 `arechat123`），方便直接体验；之后所有账号都必须走手机号注册。

## 新旧对照

- Spring 4.3 + web.xml + JSP（war）→ Boot 4.1.1 内嵌 Tomcat 11（jar，JDK 25）
- javax.* → jakarta.*
- fastjson 1.2 / json-lib / jackson 混用 → fastjson2 统一（Boot 4 `ServerHttpMessageConvertersCustomizer` 接管 JSON）
- c3p0 + Hibernate 4 + QueryDSL 老版本 → HikariCP + MyBatis-Plus 3.5.17（BaseMapper + LambdaQueryWrapper）
- JSP + jQuery + ACE 模板 → Vue 3.5 + Vite 6 + Pinia 4 + Element Plus
- UserFilter 登录拦截（源码失踪）→ LoginInterceptor + /api/auth 会话登录（手机号注册 + 密码）
- WebSocket 三版本并存 → 统一 `/ws/chat/{昵称}` JSON 协议（IM 推送通道）
- 同名即跳过的上传 → SHA-256 内容寻址去重（IM 图片消息存储）
- 无测试 → JUnit 5（64 个用例）+ Vitest + Playwright

## Docker 一键部署（前后端单镜像）

部署文件全部在**本目录**：`Dockerfile` · `docker-compose.yml` · `docker/` · `.dockerignore`。多阶段构建：Maven+JDK25 打后端 jar → Node26+pnpm 打前端 dist（经 `additional_contexts` 引入同级目录 `../are-chat-web`）→ 运行层为 nginx（静态资源 + `/api`、`/ws` 反代）+ JRE 25（Spring Boot 只监听 127.0.0.1）。

```bash
# 在本目录（are-chat）执行
docker compose up -d --build      # 构建并启动
docker compose logs -f app        # 看日志
```

访问 `http://localhost/`（`APP_PORT` 环境变量可改宿主机端口）。上传文件落在命名卷 `uploads`（容器内 `/data/uploads`）。连接老 MySQL：在 `docker-compose.yml` 中取消 `SPRING_PROFILES_ACTIVE=mysql` 与 `MYSQL_*` 注释并填入信息。

**完整部署指南（配置项 / 数据备份 / 离线部署 / 故障排查）见 [DEPLOY.md](DEPLOY.md)。**

## 环境备注（工作区级，即 `../` 下）

- Maven：`-s ../.m2-settings.xml`（本地仓库指向 `../.m2-repo/`，阿里云镜像）
- pnpm：store 指向 `../.pnpm-store/`；前端项目 `pnpm-workspace.yaml` 放行 esbuild 构建脚本
- Playwright 浏览器：`PLAYWRIGHT_BROWSERS_PATH=D:\00.personal\4.code\.playwright-browsers`
