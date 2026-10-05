package com.smart.chat.bootstrap.config;

import com.smart.chat.bootstrap.properties.RequestLogProperties;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 接口访问日志：{@code /api/**} 上每个请求打两行，用同一个 req id 串联。
 *
 * <pre>
 * → [BEGIN] 000003 POST /api/couple/mood | user=alice | query=- | body={"mood":"happy"}
 * ✓ [SUCCESS] 000003 POST /api/couple/mood 200 37ms | user=alice | body={"mood":"happy"} | resp={"code":0,...}
 * ⚠ [CLIENT_ERR] 000004 GET /api/auth/me 401 12ms | user=- | body=- | resp={"code":401,...}
 * ✗ [SERVER_ERR] 000005 POST /api/couple/echo 500 203ms | user=alice | body={...} | resp={"code":500,...}
 * </pre>
 *
 * 完成行自带入参与响应，一行就能判读一次请求；进入行只在「请求还没走完」时才有独立价值
 * （handler 卡死或进程中途挂掉，它是唯一留下过的入参证据），所以保留但不重复状态信息。
 * 分级口径：{@code <400} 记 SUCCESS，{@code 400-499} 记 CLIENT_ERR，{@code >=500} 与逃逸异常记 SERVER_ERR。
 *
 * 为什么是 Filter 而不是 AOP 或 HandlerInterceptor：被 {@link LoginInterceptor} 拒掉的 401、
 * 路径不存在的 404、以及请求体不是合法 JSON（参数还没绑定就抛异常）这三类恰恰最需要看入参，
 * 而拦截器的 {@code afterCompletion} 在 preHandle 返 false 时不触发、{@code postHandle} 也拿不到
 * {@code @ResponseBody} 的返回值；AOP 切 Controller 还要额外引入 aspectj 依赖并同样漏掉前两类。
 *
 * 不覆盖的三类（有意为之，理由见 docs/adr/0006）：
 * multipart 上传（不把附件字节读进内存）、非 JSON 的二进制响应（文件下载）、
 * WebSocket {@code /ws/chat/{name}}（{@code @ServerEndpoint} 不走 Servlet 过滤链）。
 */
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    /** 请求序号，进程内自增，36 进制短编码，够人眼区分即可 */
    private static final AtomicLong COUNTER = new AtomicLong();

    /**
     * 进日志前读 JSON 请求体的字节上限：读就得整份读（流只能消费一次），
     * 没有这个上限就等于让客户端用一个大 POST 决定我们堆里放多大数组（nginx 侧放行到 25MB）。
     */
    private static final int MAX_EAGER_BODY_BYTES = 1024 * 1024;

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}]+");

    private final RequestLogProperties props;

    public RequestLogFilter(RequestLogProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!props.enabled() || !log.isInfoEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        long start = System.nanoTime();
        String id = String.format("%06x", COUNTER.incrementAndGet());
        String method = request.getMethod();
        String path = safeLogText(request.getRequestURI());
        String query = safeLogText(orDash(request.getQueryString()));

        String contentType = request.getContentType();
        HttpServletRequest loggedRequest = request;
        String requestBody;
        if (isJson(contentType)) {
            long declared = request.getContentLengthLong();
            if (declared > MAX_EAGER_BODY_BYTES) {
                // 读就得整份读（流只能消费一次），所以超上限时干脆不碰流，让请求原样透传
                requestBody = "<JSON 请求体 " + declared + " 字节，超过 "
                        + (MAX_EAGER_BODY_BYTES / 1024) + "KB 记录上限，不读取>";
            } else if (declared < 0) {
                requestBody = "<分块传输的 JSON 请求体，长度无法预先确定，不读取>";
            } else {
                // 流只能读一次：整份读出来打完日志，再包一层让下游原样重放
                CachedBodyRequest cached = new CachedBodyRequest(request, CachedBodyRequest.readFully(request));
                loggedRequest = cached;
                requestBody = safeLogText(truncate(cached.bodyAsText()));
            }
        } else {
            requestBody = describeRequestBody(contentType);
        }

        log.info("→ [BEGIN] {} {} {} | user={} | query={} | body={}", id, method, path,
                userOf(loggedRequest), query, requestBody);

        LoggedResponse loggedResponse = new LoggedResponse(response, props.maxBodyChars());
        try {
            chain.doFilter(loggedRequest, loggedResponse);
        } catch (ServletException | IOException | RuntimeException e) {
            loggedResponse.flushWriter();
            // 逃逸出链路的异常一律按服务端问题定级：此刻容器状态码可能还是 200，不能拿它当判据
            log.info("✗ [SERVER_ERR] {} {} {} {} {}ms | user={} | body={} | resp={} | 异常={}", id, method, path,
                    response.getStatus(), costMs(start), userOf(loggedRequest), requestBody,
                    safeLogText(responseBodyOf(loggedResponse)),
                    safeLogText(e.getClass().getSimpleName() + ": " + e.getMessage()));
            throw e;
        }
        loggedResponse.flushWriter();
        int status = response.getStatus();
        log.info("{} {} {} {} {} {}ms | user={} | body={} | resp={}", verdict(status), id, method, path,
                status, costMs(start), userOf(loggedRequest), requestBody,
                safeLogText(responseBodyOf(loggedResponse)));
    }

    /** 三档判读：<400 成功，4xx 是调用方用错（参数/归属/权限），5xx 是服务端自己的问题 */
    private static String verdict(int status) {
        if (status >= 500) {
            return "✗ [SERVER_ERR]";
        }
        return status >= 400 ? "⚠ [CLIENT_ERR]" : "✓ [SUCCESS]";
    }

    /** 请求体这一栏：上传与非 JSON 只说明类型，GET 这类没有 Content-Type 的打 - */
    private static String describeRequestBody(String contentType) {
        if (contentType == null) {
            return "-";
        }
        if (contentType.toLowerCase().startsWith("multipart/")) {
            return "<multipart 上传正文，不记录字节>";
        }
        return "<非 JSON 正文（" + contentType + "），不记录>";
    }

    private String responseBodyOf(LoggedResponse response) {
        if (!response.isCapturing()) {
            String type = response.getContentType();
            return type == null ? "-" : "<非 JSON 响应（" + type + "），不记录正文>";
        }
        String text = stripSplitTail(response.bodyText());
        if (text.isEmpty()) {
            return "-";
        }
        if (response.totalBytes() > response.cachedBytes()) {
            // 旁路缓冲按字节封顶，正文本身不完整，报总字节数比报字符数诚实
            return capOf(text) + "...(截断,响应正文共 " + response.totalBytes() + " 字节)";
        }
        return truncate(text);
    }

    private static String userOf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object user = session == null ? null : session.getAttribute(Sessions.SESSION_USER);
        return user == null ? "-" : user.toString();
    }

    private String truncate(String text) {
        String body = stripSplitTail(text);
        if (body.isEmpty()) {
            return "-";
        }
        return body.length() <= props.maxBodyChars()
                ? body
                : capOf(body) + "...(截断,共 " + body.length() + " 字符)";
    }

    /** 日志注入防线：客户端带来的控制字符（CRLF 最常见）会把一行劈成两行，伪造出根本不存在的请求 */
    private static String safeLogText(String text) {
        return CONTROL_CHARS.matcher(text).replaceAll("·");
    }

    /** 裁到单行正文长度上限但不加标记，标记由调用方按自己知道的总量补 */
    private String capOf(String text) {
        int cap = props.maxBodyChars();
        return text.length() <= cap ? text : text.substring(0, cap);
    }

    /** 缓冲是按字节封顶的，最后一个字符可能被劈成半个 UTF-8 序列，解码出一个替换符，去掉它比留乱码尾巴诚实 */
    private static String stripSplitTail(String text) {
        int end = text.length();
        if (end > 0 && text.charAt(end - 1) == 0xFFFD) {
            return text.substring(0, end - 1);
        }
        return text;
    }

    private static boolean isJson(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("json");
    }

    private static String orDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private static long costMs(long startNanos) {
        return Math.max(0, (System.nanoTime() - startNanos) / 1_000_000);
    }
}
