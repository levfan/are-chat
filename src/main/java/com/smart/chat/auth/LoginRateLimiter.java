package com.smart.chat.auth;

import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 54 登录防爆破限流：同一用户名在滑动窗口内连续失败超过阈值后临时锁定。
 * 纯内存实现（重启即清零），登录成功清空该用户名的失败记录。
 */
@Component
public class LoginRateLimiter {

    /** 窗口内最多失败次数 */
    static final int MAX_FAILURES = 5;
    /** 锁定/统计窗口：5 分钟 */
    static final long WINDOW_MS = 5 * 60 * 1000L;

    private final Map<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    /** 该用户名当前是否被锁定（窗口内失败次数达到阈值）。 */
    public boolean isLocked(String username) {
        Deque<Long> deque = failures.get(username);
        if (deque == null) {
            return false;
        }
        evictExpired(deque);
        return deque.size() >= MAX_FAILURES;
    }

    /** 距离解锁还剩多少毫秒（未锁定返回 0）。 */
    public long retryAfterMs(String username) {
        Deque<Long> deque = failures.get(username);
        if (deque == null) {
            return 0;
        }
        evictExpired(deque);
        if (deque.size() < MAX_FAILURES) {
            return 0;
        }
        Long oldest = deque.peekFirst();
        return oldest == null ? 0 : Math.max(0, WINDOW_MS - (System.currentTimeMillis() - oldest));
    }

    /** 记录一次失败。 */
    public void recordFailure(String username) {
        Deque<Long> deque = failures.computeIfAbsent(username, k -> new ConcurrentLinkedDeque<>());
        evictExpired(deque);
        deque.addLast(System.currentTimeMillis());
    }

    /** 登录成功：清空该用户名的失败记录。 */
    public void reset(String username) {
        failures.remove(username);
    }

    private void evictExpired(Deque<Long> deque) {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        while (true) {
            Long first = deque.peekFirst();
            if (first == null || first >= cutoff) {
                return;
            }
            deque.pollFirst();
        }
    }
}
