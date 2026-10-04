package com.smart.chat.sharedkernel.web;

import jakarta.servlet.http.HttpSession;

/**
 * 从会话中取当前登录用户的小工具。
 */
public final class Sessions {

    /**
     * 登录态在 HttpSession 里的属性名。放在共享内核而不是拦截器里：
     * 「谁是当前登录用户」是全部上下文共用的语汇，谁都不该为了拿这个名字去 import 装配层。
     * 值保持 "CurrentUser" 不变——它是既有会话的运行时契约。
     */
    public static final String SESSION_USER = "CurrentUser";

    private Sessions() {
    }

    public static String requireUser(HttpSession session) {
        Object user = session.getAttribute(SESSION_USER);
        if (user == null) {
            throw new BusinessException(401, "还没有登录哦");
        }
        return user.toString();
    }

    /** 已由 LoginInterceptor 保证登录的取值（拦截器 401，这里不再重复判空） */
    public static String username(HttpSession session) {
        return requireUser(session);
    }
}
