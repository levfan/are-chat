package com.smart.chat.bootstrap.config;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 提前读完 JSON 请求体的请求包装。
 * 接口日志要在「进入」这一行就把入参打出来，而 Servlet 的输入流只能读一次，
 * 所以这里整份读进内存、打日志，再交给下游重放；下游拿到的字节与原始请求完全一致。
 */
class CachedBodyRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    CachedBodyRequest(HttpServletRequest request, byte[] body) {
        super(request);
        this.body = body;
    }

    static byte[] readFully(HttpServletRequest request) throws IOException {
        return request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
        return newByteArrayInputStreamServletInputStream(body);
    }

    @Override
    public BufferedReader getReader() {
        Charset charset = charsetOf(this);
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), charset));
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    /** 请求体原文（用于打日志） */
    String bodyAsText() {
        return new String(body, charsetOf(this));
    }

    int bodyLength() {
        return body.length;
    }

    private static Charset charsetOf(HttpServletRequest request) {
        String enc = request.getCharacterEncoding();
        try {
            return enc == null ? StandardCharsets.UTF_8 : Charset.forName(enc);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static ServletInputStream newByteArrayInputStreamServletInputStream(byte[] body) {
        return new BodyInputStream(body);
    }

    /** 把 byte[] 包成 ServletInputStream：容器/Spring 只认这个类型 */
    private static final class BodyInputStream extends ServletInputStream {

        private final ByteArrayInputStream delegate;

        BodyInputStream(byte[] body) {
            this.delegate = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return delegate.read(b, off, len);
        }

        @Override
        public int available() {
            return delegate.available();
        }

        @Override
        public boolean isFinished() {
            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener listener) {
            throw new UnsupportedOperationException("接口日志用的是同步读，不需要异步 ReadListener");
        }
    }
}
