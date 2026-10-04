package com.smart.chat.bootstrap.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.smart.chat.bootstrap.properties.RequestLogProperties;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 接口访问日志过滤器的行为契约。
 * 不起 Spring：直接 new 过滤器 + Mock 请求响应，用 logback ListAppender 抓真实日志行。
 * 除了「日志长什么样」，每组都同时断言「旁路采集没有改动透传字节」——
 * 这是这类改造最容易埋进去的静默数据丢失（下载变空文件、下游读不到请求体）。
 */
class RequestLogFilterTest {

    private static final RequestLogProperties ON = new RequestLogProperties(true, 4000);

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

    private List<String> lines() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private MockHttpServletResponse run(MockHttpServletRequest request, RequestLogProperties props, FilterChain chain)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new RequestLogFilter(props).doFilter(request, response, chain);
        return response;
    }

    /** 只跑一遍并把响应交回调用方 */
    private MockHttpServletResponse run(MockHttpServletRequest request, FilterChain chain) throws Exception {
        return run(request, ON, chain);
    }

    private MockHttpServletRequest jsonPost(String path, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setContentType("application/json;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    @Test
    void logsEntryLineWithInputBeforeHandlerRunsAndDoneLineWithStatusAndBody() throws Exception {
        MockHttpServletRequest request = jsonPost("/api/couple/mood", "{\"mood\":\"开心\"}");
        request.setQueryString("day=20261005");
        request.getSession().setAttribute(Sessions.SESSION_USER, "alice");
        byte[] sent = request.getContentAsByteArray();

        AtomicReference<byte[]> downstreamSaw = new AtomicReference<>();
        AtomicBoolean entryLineAlreadyWrittenAtHandlerTime = new AtomicBoolean();
        String responseBody = "{\"code\":0,\"message\":\"ok\"}";

        MockHttpServletResponse response = run(request, (req, res) -> {
            downstreamSaw.set(req.getInputStream().readAllBytes());
            // 入参必须「进来就打」：处理器刚开始时，--> 这一行就该已经在日志里了
            entryLineAlreadyWrittenAtHandlerTime.set(
                    lines().size() == 1 && lines().get(0).contains("-->"));
            res.setContentType("application/json;charset=UTF-8");
            ServletOutputStream out = res.getOutputStream();
            // 分两块写，覆盖旁路拷贝的跨块拼接
            out.write("{\"code\":0,".getBytes(StandardCharsets.UTF_8));
            out.write("\"message\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
        });

        assertArrayEquals(sent, downstreamSaw.get(), "下游必须读到与原始请求一字不差的正文");
        assertArrayEquals(responseBody.getBytes(StandardCharsets.UTF_8), response.getContentAsByteArray(),
                "旁路采集不能吃掉响应字节");
        assertTrue(entryLineAlreadyWrittenAtHandlerTime.get(), "进入行要在处理器执行前就落日志");

        List<String> lines = lines();
        assertEquals(2, lines.size(), () -> "两行才对得上，实际=" + lines);
        String entry = lines.get(0);
        String done = lines.get(1);
        assertTrue(entry.contains("--> POST /api/couple/mood"), entry);
        assertTrue(entry.contains("user=alice"), entry);
        assertTrue(entry.contains("query=day=20261005"), entry);
        assertTrue(entry.contains("{\"mood\":\"开心\"}"), entry);
        assertTrue(done.contains("<-- POST /api/couple/mood"), done);
        assertTrue(done.contains(" 200 "), done);
        assertTrue(done.contains("成功"), done);
        assertTrue(done.contains(responseBody), done);
        assertEquals(entry.substring(0, entry.indexOf(']')), done.substring(0, done.indexOf(']')),
                "两行要共用同一个 req id 才能 grep 串联");
    }

    @Test
    void businessFailureOnWriterPathIsMarkedFailedAndStillReachesClient() throws Exception {
        MockHttpServletRequest request = jsonPost("/api/couple/ceremony/coupon", "{\"text\":\"想要一张券\"}");
        request.getSession().setAttribute(Sessions.SESSION_USER, "alice");
        String body = "{\"code\":400,\"message\":\"积分不足，还差 3 分\"}";

        MockHttpServletResponse response = run(request, (req, res) -> {
            HttpServletResponse http = (HttpServletResponse) res;
            http.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            http.setContentType("application/json;charset=UTF-8");
            // 走 getWriter 且不主动 flush：包装层不把 writer 冲出去，客户端就会收到空正文
            http.getWriter().write(body);
        });

        assertArrayEquals(body.getBytes(StandardCharsets.UTF_8), response.getContentAsByteArray(),
                "getWriter 路径的正文必须被冲刷回客户端");
        List<String> lines = lines();
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains(" 400 "), lines.get(1));
        assertTrue(lines.get(1).contains("失败"), lines.get(1));
        assertFalse(lines.get(1).contains("成功"), lines.get(1));
        assertTrue(lines.get(1).contains("积分不足，还差 3 分"), lines.get(1));
    }

    @Test
    void uploadDoesNotLogBytesButStreamsThemUnchanged() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/files");
        request.setContentType("multipart/form-data; boundary=----WebKitFormBoundary");
        request.setCharacterEncoding("UTF-8");
        byte[] fileBytes = new byte[4096];
        for (int i = 0; i < fileBytes.length; i++) {
            fileBytes[i] = (byte) i;
        }
        request.setContent(fileBytes);

        AtomicReference<byte[]> downstreamSaw = new AtomicReference<>();
        MockHttpServletResponse response = run(request, (req, res) -> {
            downstreamSaw.set(req.getInputStream().readAllBytes());
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"code\":0,\"message\":\"ok\"}");
        });

        assertArrayEquals(fileBytes, downstreamSaw.get(), "上传字节要原样交给容器解析");
        List<String> lines = lines();
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("<multipart 上传正文，不记录字节>"), lines.get(0));
        assertTrue(lines.get(0).contains("user=-"), "未登录会话要打成 -");
        assertFalse(lines.get(0).chars().anyMatch(c -> c < 0x20),
                "日志行不该出现控制字符——上传字节被抄进日志就是这个样子");
        assertTrue(lines.get(1).contains("成功"), lines.get(1));
    }

    @Test
    void binaryResponseKeepsEveryByteAndLogsNoBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/files/f1/download");
        byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 1, 2, (byte) 0xFF};

        MockHttpServletResponse response = run(request, (req, res) -> {
            res.setContentType("image/png");
            res.getOutputStream().write(png);
        });

        assertArrayEquals(png, response.getContentAsByteArray(), "下载字节一个都不能少（防响应被缓冲吃掉变成空文件）");
        List<String> lines = lines();
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("<非 JSON 响应（image/png），不记录正文>"), lines.get(1));
        assertFalse(lines.get(1).contains("PNG"), "二进制正文不该进日志");
    }

    @Test
    void oversizedBodiesAreTruncatedWithTotalLengthNoted() throws Exception {
        String longText = "啊".repeat(30);
        MockHttpServletRequest request = jsonPost("/api/couple/echo", "{\"note\":\"" + longText + "\"}");
        String responseJson = "{\"code\":0,\"data\":{\"list\":" + "1".repeat(200) + "}}";

        MockHttpServletResponse response = run(request, new RequestLogProperties(true, 20), (req, res) -> {
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write(responseJson);
        });

        assertArrayEquals(responseJson.getBytes(StandardCharsets.UTF_8), response.getContentAsByteArray(),
                "截断只发生在日志里，响应本身要完整");
        List<String> lines = lines();
        // 请求体 {"note":" + 30 个「啊」 + "} 共 41 字符；响应 227 字节超出旁路缓冲（20*4+4=84 字节）
        assertTrue(lines.get(0).contains("...(截断,共 41 字符)"), lines.get(0));
        assertTrue(lines.get(0).length() < 200, "进入行要短到能一眼看完");
        assertTrue(lines.get(1).contains("...(截断,响应正文共 227 字节)"), lines.get(1));
        assertFalse(lines.get(1).contains(responseJson), lines.get(1));
    }

    @Test
    void turningItOffLogsNothingAndPassesTheRequestThrough() throws Exception {
        MockHttpServletRequest request = jsonPost("/api/health", "{}");
        AtomicBoolean handled = new AtomicBoolean();

        MockHttpServletResponse response = run(request, new RequestLogProperties(false, 4000), (req, res) -> {
            handled.set(true);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"code\":0,\"message\":\"ok\"}");
        });

        assertTrue(handled.get(), "关掉日志也不能影响请求处理");
        assertEquals(0, lines().size(), () -> "enabled=false 要一行都不打，实际=" + lines());
        assertDoesNotThrow(() -> response.getContentAsString());
    }

    @Test
    void exceptionEscapingTheChainIsLoggedAsFailureAndRethrown() {
        MockHttpServletRequest request = jsonPost("/api/couple/surprise/scratch", "{\"card\":\"s1\"}");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                run(request, (req, res) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    throw new IllegalStateException("刮卡服务炸了");
                }));

        assertEquals("刮卡服务炸了", thrown.getMessage(), "异常要继续往上抛，日志不能替容器吞掉它");
        List<String> lines = lines();
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("失败"), lines.get(1));
        assertTrue(lines.get(1).contains("异常=IllegalStateException: 刮卡服务炸了"), lines.get(1));
    }

    @Test
    void oversizedJsonBodyIsNotConsumedAndDownstreamStillSeesItInFull() throws Exception {
        String padding = "x".repeat(1024 * 1024);
        MockHttpServletRequest request = jsonPost("/api/couple/echo", "{\"note\":\"" + padding + "\"}");
        AtomicReference<Integer> downstreamLength = new AtomicReference<>();

        run(request, (req, res) -> {
            downstreamLength.set(req.getInputStream().readAllBytes().length);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"code\":0}");
        });

        assertEquals(request.getContentLength(), downstreamLength.get(),
                "超过记录上限的正文要一个字节不少地留给下游（不读就不能消费流）");
        List<String> lines = lines();
        assertTrue(lines.get(0).contains("超过 1024KB 记录上限，不读取"), lines.get(0));
        assertFalse(lines.get(0).contains("xxxxx"), "超大正文不该抄进日志");
    }

    @Test
    void controlCharactersCannotSplitTheLogLineIntoTwoRequests() throws Exception {
        MockHttpServletRequest request = jsonPost("/api/couple/mood",
                "{\"text\":\"第一行\r\n[req 999] <-- GET /伪造 200 0ms 成功\"}");
        request.setQueryString("a=1\r\nb=2");

        run(request, (req, res) -> {
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"code\":0}");
        });

        List<String> lines = lines();
        assertEquals(2, lines.size());
        assertTrue(lines.stream().noneMatch(l -> l.contains("\n") || l.contains("\r")),
                "控制字符会把一行劈成两行，凭空造出没发生过的请求：" + lines);
        assertTrue(lines.get(0).contains("body={\"text\":\"第一行·[req 999] <-- GET /伪造 200 0ms 成功\"}"), lines.get(0));
        assertTrue(lines.get(0).contains("query=a=1·b=2"), lines.get(0));
    }

    @Test
    void defaultsStayOnAndBounded() {
        RequestLogProperties unset = new RequestLogProperties(null, 0);
        assertTrue(unset.enabled(), "配置块被删掉也要默认打开：这个功能的存在意义就是每个请求都留痕");
        assertEquals(4000, unset.maxBodyChars(), "不给上限就等于让大响应整份进内存");
    }
}
