package com.smart.chat.config;

import com.alibaba.fastjson2.JSON;
import com.smart.chat.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：替代老项目里失踪的 com.CommonUtils.UserFilter。
 * 未登录访问受保护接口时返回 401 + 整活文案。
 */
public class LoginInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER = "CurrentUser";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var session = request.getSession(false);
        Object user = session == null ? null : session.getAttribute(SESSION_USER);
        if (user != null) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(ApiResponse.error(401,
                "此路是我开，要想此路过——雁过拔毛，人过留名，先去登录！")));
        return false;
    }
}
