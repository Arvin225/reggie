package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Orders;
import org.springframework.transaction.annotation.Transactional;

public interface OrdersService extends IService<Orders> {

    /**
     * 订单提交
     * @param order
     * @return
     */
    @Transactional
    boolean submit(Orders order);
}
