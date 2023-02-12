package com.itheima.reggie.common;

/**
 * 用于登录用户的存取
 */
public class BaseContext {

    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentUser(Long id){
        threadLocal.set(id);
    }

    public static Long getCurrentUser(){
        return threadLocal.get();
    }

}