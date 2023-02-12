package com.itheima.reggie.common;

/**
 * 自定义异常类，用于特殊情况的处理
 */
public class CustomException extends RuntimeException{
    public String message;
    public CustomException(String message){
        this.message = message;
    }

    public String getMessage(){
        return message;
    }

}
