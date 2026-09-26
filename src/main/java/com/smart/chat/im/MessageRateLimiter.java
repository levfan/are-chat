package com.smart.chat.im;

import com.smart.chat.common.BusinessException;
import com.smart.chat.config.ImProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 87 发送频率限制（防刷屏）：滑动窗口计数，单用户每分钟最多 send-limit-per-minute 条（默认 30）。
 * 超限抛 429，提示还需等待的秒数；窗口随时间自然回收。
 */
@Component
public class MessageRateLimiter {

    private static final long WINDOW_MS = 60_000L;

    private final ImProperties properties;
    private final Map<String, Deque<Long>> sentAt = new ConcurrentHashMap<>();

    public MessageRateLimiter(ImProperties properties) {
        this.properties = properties;
    }

    /** 发送前调用：超限抛 429 */
    public void check(String username) {
        long now = System.currentTimeMillis();
        Deque<Long> window = sentAt.computeIfAbsent(username, k -> new ArrayDeque<>());
        synchronized (window) {
            evict(window, now);
            if (window.size() >= properties.sendLimitPerMinute()) {
                Long oldest = window.peekFirst();
                long waitSeconds = oldest == null ? 0 : (oldest + WINDOW_MS - now) / 1000 + 1;
                throw new BusinessException(429, "发送太快了，休息 " + Math.max(1, waitSeconds) + " 秒再发吧");
            }
            window.addLast(now);
        }
    }

    /** 当前窗口已发送条数（观测用） */
    public int usedInWindow(String username) {
        Deque<Long> window = sentAt.get(username);
        if (window == null) {
            return 0;
        }
        synchronized (window) {
            evict(window, System.currentTimeMillis());
            return window.size();
        }
    }

    private void evict(Deque<Long> window, long now) {
        while (!window.isEmpty() && window.peekFirst() + WINDOW_MS <= now) {
            window.pollFirst();
        }
    }
}
