package com.ticket.system.common.util;

import com.ticket.system.dto.response.UserInfoDTO;

public class ThreadLocalUtil {

    private static final ThreadLocal<UserInfoDTO> USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前线程的用户信息
     */
    public static void setUser(UserInfoDTO user) {
        USER_THREAD_LOCAL.set(user);
    }

    /**
     * 获取当前线程的用户信息
     */
    public static UserInfoDTO getUser() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        UserInfoDTO user = getUser();
        return user != null ? user.getId() : null;
    }

    /**
     * 移除当前线程的用户信息
     */
    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}