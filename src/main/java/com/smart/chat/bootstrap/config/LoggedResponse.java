package com.smart.chat.bootstrap.config;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * 响应体旁路采集包装。
 * 不用 Spring 的 ContentCachingResponseWrapper：它在 Spring 7.0.9 里只有「整份缓冲」一种行为
 * （没有 cacheLimit 构造函数），文件下载这种二进制响应会被完整吃进内存。
 * 这里改成首次写入时按响应的 Content-Type 决定要不要缓存：
 * JSON 才 tee 一份到有上限的缓冲区，其余类型直接透传、零缓冲；两种情况下客户端拿到的字节都原样写出。
 */
class LoggedResponse extends HttpServletResponseWrapper {

    /** UTF-8 一个字符最多 4 字节，按字符上限换算成字节上限，日志截断才能落在字符边界上 */
    private static final int BYTES_PER_CHAR = 4;

    private final int maxLogChars;
    private final int bufferLimit;
    private boolean decided;
    private byte[] cache;
    private int cached;
    private long totalBytes;
    private TeeStream stream;
    private PrintWriter writer;

    LoggedResponse(HttpServletResponse response, int maxLogChars) {
        super(response);
        this.maxLogChars = maxLogChars;
        this.bufferLimit = (long) maxLogChars * BYTES_PER_CHAR > Integer.MAX_VALUE - 8
                ? Integer.MAX_VALUE - 8
                : maxLogChars * BYTES_PER_CHAR + 4;
        this.cache = new byte[Math.min(bufferLimit, 8192)];
    }

    /**
     * 首次写正文时定型：此时 Content-Type 已由消息转换器/拦截器写好。
     * 判定点不能放在 setHeader/setContentType 上——CORS 的 Vary、会话的 Set-Cookie 都会先于
     * Content-Type 到达，那样会把判定永久锁成「非 JSON 不采集」（端到端探针实测到过）。
     */
    private void ensureDecided() {
        if (decided) {
            return;
        }
        decided = true;
        String type = getContentType();
        if (type == null || !type.toLowerCase().contains("json")) {
            cache = null;
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (stream == null) {
            ensureDecided();
            stream = new TeeStream(super.getOutputStream());
        }
        return stream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            String enc = getCharacterEncoding();
            // 走同一个旁路流：不覆盖 getWriter 的话，拦截器写出去的 401 正文根本进不了采集
            writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), enc == null ? "UTF-8" : enc));
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        // getWriter 的正文先进 PrintWriter 缓冲，容器 commit 时只会调 flushBuffer，
        // 不先把 writer 冲出去就会让 401 这类响应变成空正文
        flushWriter();
        super.flushBuffer();
    }

    /** 链路走完后由过滤器调用：把 writer 缓冲里的字节真正写回客户端，并让它进入旁路采集 */
    void flushWriter() {
        if (writer != null) {
            writer.flush();
        }
    }

    /** 本响应是否在做旁路采集（非 JSON 响应不采，一个字节都不缓冲） */
    boolean isCapturing() {
        return cache != null;
    }

    /** 已采集到的字节数，与 totalBytes 对照即可判断正文是否被缓冲上限截断 */
    int cachedBytes() {
        return cached;
    }

    /** 是否采集到了正文 */
    boolean hasBody() {
        return cached > 0;
    }

    /** 采集到的正文原文（可能已在字节上限处停止拷贝，由调用方对照 totalBytes 判断截断） */
    String bodyText() {
        return new String(cache == null ? new byte[0] : cache, 0, cached, java.nio.charset.StandardCharsets.UTF_8);
    }

    int bodyCharLimit() {
        return maxLogChars;
    }

    long totalBytes() {
        return totalBytes;
    }

    /** 透传 + 旁路拷贝：写客户端这一步永远先做，缓冲满了也只是不再拷，绝不影响响应 */
    private final class TeeStream extends ServletOutputStream {

        private final ServletOutputStream delegate;

        TeeStream(ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
            totalBytes++;
            if (cache != null && cached < bufferLimit) {
                ensureCapacity(cached + 1);
                cache[cached++] = (byte) b;
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            totalBytes += len;
            if (cache != null && cached < bufferLimit) {
                int take = Math.min(len, bufferLimit - cached);
                ensureCapacity(cached + take);
                System.arraycopy(b, off, cache, cached, take);
                cached += take;
            }
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener listener) {
            delegate.setWriteListener(listener);
        }
    }

    private void ensureCapacity(int needed) {
        if (needed <= cache.length) {
            return;
        }
        int target = Math.min(bufferLimit, Math.max(needed, cache.length * 2));
        byte[] bigger = new byte[target];
        System.arraycopy(cache, 0, bigger, 0, cached);
        cache = bigger;
    }
}
