# ADR-0006：接口访问日志用 Servlet Filter + 自研响应旁路，不在 Controller 层埋点

- 状态：已采纳（2026-10-05）
- 相关：`bootstrap/config/RequestLogFilter`、`bootstrap/config/LoggedResponse`、`bootstrap/config/CachedBodyRequest`、`bootstrap/config/RequestLogConfig`、`bootstrap/properties/RequestLogProperties`、`wiki/dev-guide.md`「接口访问日志」

## 背景

需求是「每个后端请求都留痕：进来打入参，完成打成功或失败」。落到这个仓库上有四个既成事实决定了可选空间：

1. 全部 REST 入口在 `/api/**`（25 个 Controller / 137 个映射），登录态在 `HttpSession`，鉴权由 `bootstrap/config/LoginInterceptor` 承担；错误统一由 `sharedkernel/web/GlobalExceptionHandler` 翻成 `ApiResponse`。
2. `pom.xml` 没有 AOP/AspectJ 依赖，且本机 Maven 走离线（`mvn -o`），新增依赖等于必须联网取包——与 ADR-0004 同一口径。
3. 已实测确认：全仓没有任何 Controller 直接 `return ApiResponse.error(...)`（`grep -rn "ApiResponse.error" */api/` 在 main 侧为空），业务失败一律经异常处理器落成 4xx/5xx。**因此 HTTP 状态码是可信的成功/失败判据**，不会出现「业务失败但日志记成成功」。
4. 有两个端点天生不能整份读写字节：`POST /api/files`（multipart，上限 20MB）与 `GET /api/files/{id}/download`（`ResponseEntity<Resource>` 二进制流）。

## 决策

### 1. 用 Filter，不用 AOP 或 HandlerInterceptor

- **AOP 切 Controller**：要新增 aspectj 依赖；且切面在参数绑定成功之后才进得去，恰好漏掉最想看的三类——被拦截器拒掉的 401、路径不存在的 404、请求体不是合法 JSON（`HttpMessageNotReadableException`，此时参数还没绑定出来）。
- **HandlerInterceptor**：`preHandle` 返 false 时本拦截器的 `afterCompletion` 不触发（401 看不见完成行），`postHandle` 也拿不到 `@ResponseBody` 的返回值。
- **Filter**：零新依赖，覆盖整条 Servlet 链（含拦截器拒绝、异常处理器兜底、404），耗时统计口径也最接近客户端感知。

注册用 `FilterRegistrationBean` 限定 `/api/*`，`order = HIGHEST_PRECEDENCE + 10`（排在字符集过滤器之后，先把编码定好再读请求体，中文入参才不会解错）。装配单独成 `RequestLogConfig` 且**不实现 `WebMvcConfigurer`**：`@WebMvcTest` 切片会纳入 `WebMvcConfigurer` 但不纳入普通 `@Configuration`，否则四个既有切片测试会因为拿不到 `RequestLogProperties` 而起不来（同 `MybatisPlusConfig` 的既有口径）。

### 2. 入参在「进入」这一行就打印：整份读完再重放

`ContentCachingRequestWrapper` 只在下游读完之后才有内容，等于把入参推迟到完成行——而需求要的是「请求一进来就留下入参」（进程在 handler 里挂掉时这一条是唯一证据）。因此用 `CachedBodyRequest` 把 JSON 请求体一次性读进内存、打完日志、再包一层让下游原样重放。

配套两条边界（都是自审阶段按 git-commit 清单第 3 条「入参要钳制防内存放大」补的，各有一条能变红的测试）：

- 只读 `Content-Length ≤ 1MB` 的 JSON。读就必须整份读，超上限时干脆不碰流并打出说明，请求原样透传；分块传输（长度未知）同样不读。nginx 侧放行到 25MB，没有这个上限就等于让客户端决定我们堆里放多大数组。
- multipart 一律不读正文，只打 `<multipart 上传正文，不记录字节>`。

### 3. 响应旁路自己实现，不用 `ContentCachingResponseWrapper`

实测本机 Spring Framework 7.0.9 的 `ContentCachingResponseWrapper` **只有整份缓冲一种行为**（`javap` 显示只有单参构造，`ContentCachingRequestWrapper` 才有 `(request, int cacheLimit)`）。用它就得把下载的文件整张吃进内存，还多一根 `copyBodyToResponse()` 的漏针。

`LoggedResponse` 改成首次写正文时按响应的 `Content-Type` 判定：JSON 才 tee 一份到有字节上限的缓冲（`maxBodyChars × 4 + 4`），非 JSON 直接透传、零缓冲；两种情况下客户端拿到的字节都原样写出。`getWriter()` 也接到同一个旁路流上（`LoginInterceptor` 的 401 就是走 writer），并由过滤器在链路收尾调 `flushWriter()` 把缓冲冲出去。

**判定位置是被实测纠正过的**：最初把 `decide()` 挂在 `setContentType/setHeader/addHeader` 上，端到端探针一跑，三条 JSON 响应全部退化成 `<非 JSON 响应…不记录正文>`——因为 CORS 的 `Vary`、会话的 `Set-Cookie` 会先于 `Content-Type` 到达，把判定永久锁死。现在只在 `getOutputStream()/getWriter()` 首次触发时定型。这条只有真 Tomcat 能照出来，`MockHttpServletResponse` 全绿也照不出来，故 `RequestLogEndToEndTest` 常驻。

### 4. 不脱敏（用户拍定，与项目 review 清单冲突，显式记账）

用户 2026-10-05 明确要求「全量原样记录，不打码」「全量记录响应体」。这与 `.agents/skills/git-commit/SKILL.md` 架构师清单第 2 条「无敏感信息进日志」直接冲突，按「用户指令优先于技能规范」执行，并在此登记后果：

- 日志里会出现明文口令与会话内容：请求发什么就记什么，`RequestLogEndToEndTest` 里断言进日志的入参原文即 `{"account":"nosuchuser-zzz","password":"Pa55w0rd!"}`；日志文件因此是一份可读的凭据副本。
- 止损手段：`arechat.request-log.enabled=false` 一个开关即可全关；日志不进 git（`uploads/`、控制台输出由部署侧的容器日志驱动管留存）。
- 若将来要收紧，改动面很小：`RequestLogFilter.truncate()` 出口处按字段名替换即可，不必再动采集链路。

### 5. 有意不覆盖的三类

- **WebSocket `/ws/chat/{name}`**：`@ServerEndpoint` 由容器实例化，不走 Servlet 过滤链，本改造天然看不见。要做得在 `ChatEndpoint` 收发处各自埋点，是另一件事。
- **二进制响应**：只打状态码与耗时，正文打 `<非 JSON 响应（image/png），不记录正文>`。
- **multipart 请求体**：见上。

`/actuator/**` 也不在 `/api/*` 范围内（管理端点自身有日志与鉴权口径）。

## 后果

- 正面：137 个 REST 映射零改动获得统一留痕；两行共用 `[req N]` 前缀可直接 grep 串起来；成功/失败判据与容器状态码同源，不会自造一套。
- 负面：每请求多两次包装对象与一次全量 JSON 读（≤1MB）；日志体积随列表类接口增长，靠 `max-body-chars`（默认 4000）与开关控制。
- 可证伪性：六个变异各自打红对应测试（去掉 `flushWriter` → writer 路径变空正文；去掉透传 → 下载字节变 0；硬编码「成功」→ 400 记成成功；去掉重放包装 → 下游读不到请求体；去掉净化 → CRLF 劈出伪造行；去掉 1MB 上限 → 1MB 正文整条抄进日志），验证记录见 `docs/ddd/03-phase-plan.md` 同批台账与本次交付报告。
