package com.smart.chat.bootstrap.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 接口访问日志的端到端探针：真 Tomcat + 真 HTTP 往返，验证 Mock 请求验证不到的两件事——
 * ① 采集时机：fastjson 转换器与拦截器都是「先定 Content-Type 再拿流」，这个顺序只在容器里成立才算数，
 *    判错时机响应体就会静默变成「非 JSON 不记录」；
 * ② 旁路采集没有影响真响应字节（客户端仍然拿到完整 JSON）。
 * 两条写回路径都覆盖：@RestController 走 getOutputStream，LoginInterceptor 的 401 走 getWriter。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestLogEndToEndTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(RequestLogFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    /** 只取本次请求产生的日志行（上下文与别的测试共用一个 JVM） */
    private List<String> linesFor(String path) {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.contains(" " + path))
                .toList();
    }

    /**
     * 等日志行到位再断言。真 HTTP 往返下「响应字节回到客户端」与「Filter 写完完成行」
     * 两件事没有先后保证：单跑这个类 6 次全绿，整套跑因负载升高会偶发少一行。
     * 这里只是把它真正等到位，判据没有放松——仍然要求行数到位后逐条比对内容；
     * 等不到就把最后一次快照交给 assertEquals，报错信息照原样列出实际日志行。
     */
    private List<String> awaitLinesFor(String path, int expectedLines) {
        long deadline = System.currentTimeMillis() + 3000;
        List<String> lines = linesFor(path);
        while (lines.size() < expectedLines && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            lines = linesFor(path);
        }
        return lines;
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void controllerResponseIsCapturedThroughTheRealMessageConverter() throws Exception {
        appender.list.clear();
        HttpResponse<String> response = get("/api/auth/me");

        assertEquals(401, response.statusCode(), "未登录取当前用户应当 401");
        assertTrue(response.body().contains("\"code\":401"), "客户端要拿到完整 JSON，实际=" + response.body());

        List<String> lines = awaitLinesFor("/api/auth/me", 2);
        assertEquals(2, lines.size(), () -> "进入与完成各一行，实际=" + lines);
        String done = lines.get(1);
        assertTrue(lines.get(0).contains("GET /api/auth/me user=- query=- body=-"), lines.get(0));
        assertTrue(done.contains(" 401 "), done);
        assertTrue(done.contains("失败"), done);
        // 这一条就是采集时机的判据：时机判错会退化成「非 JSON 响应，不记录正文」
        assertTrue(done.contains("\"message\":\"还没有登录哦\""), done);
        assertTrue(done.matches(".*\\d+ms.*"), "要带上耗时：" + done);
    }

    @Test
    void loginInterceptorRejectionIsLoggedWithWriterCaptured() throws Exception {
        appender.list.clear();
        HttpResponse<String> response = post("/api/persons", "{\"name\":\"探针\"}");

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("雁过拔毛"), "拦截器写的 401 正文要完整回到客户端，实际=" + response.body());

        List<String> lines = awaitLinesFor("/api/persons", 2);
        assertEquals(2, lines.size(), () -> "被拦截器拒掉的请求也要留两行，实际=" + lines);
        assertTrue(lines.get(0).contains("--> POST /api/persons"), lines.get(0));
        assertTrue(lines.get(0).contains("{\"name\":\"探针\"}"), lines.get(0));
        assertTrue(lines.get(1).contains("失败"), lines.get(1));
        assertTrue(lines.get(1).contains("雁过拔毛"), lines.get(1));
    }

    @Test
    void loginAttemptLogsItsInputVerbatimAndMarksFailure() throws Exception {
        appender.list.clear();
        String body = "{\"account\":\"nosuchuser-zzz\",\"password\":\"Pa55w0rd!\"}";
        HttpResponse<String> response = post("/api/auth/login", body);

        assertEquals(401, response.statusCode(), "不存在的账号要 401，实际=" + response.statusCode());
        assertNotNull(response.headers().firstValue("Set-Cookie").orElse(null),
                "会话 Cookie 仍要正常下发（包装层不能改动响应头）");

        List<String> lines = awaitLinesFor("/api/auth/login", 2);
        assertEquals(2, lines.size(), () -> "实际=" + lines);
        // 按用户拍定的口径：入参全量原样记录，不做脱敏
        assertTrue(lines.get(0).contains("body=" + body), lines.get(0));
        assertTrue(lines.get(0).contains("user=-"), lines.get(0));
        assertTrue(lines.get(1).contains("失败"), lines.get(1));
        assertTrue(lines.get(1).contains("\"code\":401"), lines.get(1));

        appender.list.clear();
        HttpResponse<String> health = get("/api/health");
        assertEquals(200, health.statusCode());
        List<String> healthLines = awaitLinesFor("/api/health", 2);
        assertEquals(2, healthLines.size(), () -> "实际=" + healthLines);
        assertTrue(healthLines.get(1).contains("成功"), healthLines.get(1));
    }

    /** 登录成功后的请求，两行都要带上用户名（这是排查「谁把接口打挂了」的主线索） */
    @Test
    void authenticatedRequestCarriesUsernameOnBothLines() throws Exception {
        // 管理员账号由 AdminBootstrapper 引导（admin / admin123456，测试配置同值）
        HttpResponse<String> login = post("/api/auth/login", "{\"account\":\"admin\",\"password\":\"admin123456\"}");
        assertEquals(200, login.statusCode(), "管理员登录要成功，实际=" + login.body());
        String cookie = login.headers().firstValue("Set-Cookie").orElse("").split(";")[0];

        appender.list.clear();
        HttpResponse<String> me = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/me"))
                        .header("Cookie", cookie).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, me.statusCode(), "带 Cookie 取当前用户应当成功，实际=" + me.statusCode() + " " + me.body());
        List<String> lines = awaitLinesFor("/api/auth/me", 2);
        assertEquals(2, lines.size(), () -> "实际=" + lines);
        assertTrue(lines.get(0).contains("user=admin"), lines.get(0));
        assertTrue(lines.get(1).contains("user=admin"), lines.get(1));
        assertTrue(lines.get(1).contains("成功"), lines.get(1));
    }
}
