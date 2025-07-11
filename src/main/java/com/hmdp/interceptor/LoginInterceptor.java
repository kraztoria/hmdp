package com.hmdp.interceptor;

import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录校验拦截器 如果访问某些资源没有登录就进行拦截 并返回 401 状态码
 *
 * @author Kraztoria rotroutine@163.com
 * @since 2025/7/11 14:57
 */
@Service
public class LoginInterceptor implements HandlerInterceptor {
    // 在 controller 方法执行之前执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (UserHolder.getUser() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        return true;
    }
}
