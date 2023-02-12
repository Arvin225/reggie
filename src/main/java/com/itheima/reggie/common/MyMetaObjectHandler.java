package com.itheima.reggie.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {

        log.info("检测到插入操作，自动填充启动，自动填充创建及更新人&时间");

        metaObject.setValue("createTime", LocalDateTime.now());
        metaObject.setValue("updateTime", LocalDateTime.now());

        metaObject.setValue("createUser", BaseContext.getCurrentUser());//这里id通过ThreadLocal对象获取
        metaObject.setValue("updateUser", BaseContext.getCurrentUser());
    }

    @Override
    public void updateFill(MetaObject metaObject) {

        log.info("检测到更新操作，自动填充启动，自动填充更新人&时间");

        metaObject.setValue("updateTime", LocalDateTime.now());
        metaObject.setValue("updateUser", BaseContext.getCurrentUser());

    }
}
