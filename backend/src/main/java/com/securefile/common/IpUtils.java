package com.securefile.common;

import javax.servlet.http.HttpServletRequest;

/**
 * IP 地址工具类
 * 用于获取客户端真实 IP 地址
 */
public class IpUtils {

    /**
     * 获取客户端真实IP
     * 优先级：X-Client-Public-IP > X-Forwarded-For > X-Real-IP > RemoteAddr
     * 
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // 优先使用前端传递的公网 IP（通过 ipify.org 等服务获取）
        String ip = request.getHeader("X-Client-Public-IP");
        
        // 代理服务器传递的真实客户端 IP
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
            // X-Forwarded-For 可能包含多个 IP，取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        }
        
        // Nginx 代理传递的真实客户端 IP
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        
        // 直接连接的客户端 IP
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        return ip != null ? ip : "unknown";
    }
}
