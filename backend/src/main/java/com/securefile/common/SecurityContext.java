package com.securefile.common;

/**
 * @description:
 * @author:zoujingmin
 * @date: 2026/1/26
 */


/**
 * 用户上下文工具类：用于在同一个线程内共享当前登录用户的信息
 */
public class SecurityContext {

    // 使用 ThreadLocal 存储用户 ID
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前线程的用户 ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 清理当前线程的用户信息（重要：防止内存泄漏，通常在拦截器完成后调用）
     */
    public static void remove() {
        USER_ID_HOLDER.remove();
    }
}
