package com.securefile.interceptor;

import com.securefile.common.JwtUtils;
import com.securefile.common.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT认证拦截器
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取Token
        String token = request.getHeader("Authorization");
        
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        
        // 验证Token
        if (!JwtUtils.validate(token)) {
            response.setStatus(401);
            return false;
        }
        
        // 提取用户ID并存入ThreadLocal
        Long userId = JwtUtils.getUserId(token);
        SecurityContext.setUserId(userId);
        
        log.debug("用户认证成功: userId={}", userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理ThreadLocal，防止内存泄漏
        SecurityContext.remove();
    }
}
