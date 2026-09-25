package com.smart.chat.common;

import com.smart.chat.config.LoginInterceptor;
import jakarta.servlet.http.HttpSession;

/**
 * 从会话中取当前登录用户的小工具。
 */
public final class Sessions {

    private Sessions() {
    }

    public static String requireUser(HttpSession session) {
        Object user = session.getAttribute(LoginInterceptor.SESSION_USER);
        if (user == null) {
            throw new BusinessException(401, "还没有登录哦");
        }
        return user.toString();
    }
}
