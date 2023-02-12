package com.itheima.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 */
@Slf4j
@ControllerAdvice(annotations = {RestController.class, Controller.class})//注明是Controller的通知
@ResponseBody //需要响应数据给前端，因此注明响应体
public class GlobalExceptionHandler {

    /**
     * SQLIntegrityConstraintViolationException异常处理器
     * @param exception
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<Object> exceptionHandler(SQLIntegrityConstraintViolationException exception) {
        //获取异常的消息
        String exceptionMessage = exception.getMessage();

        log.error(exceptionMessage);//仅输出异常消息到控制台，便于查看

        //处理唯一约束的异常
        //判断是否是“重复”的问题，是则响应给前端
        if (exceptionMessage.contains("Duplicate entry")) {
            String[] s = exceptionMessage.split(" ");
            String unique = s[2];
            return R.error(unique + "，已存在");
        }
        return R.error("未知错误");
    }


    /**
     * 自定义异常处理器
     * @param exception
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public R<Object> exceptionHandler(CustomException exception){

        log.info(exception.getMessage());

        return R.error(exception.getMessage());
    }

}
