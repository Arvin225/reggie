package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.entity.OrdersDto;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 订单分页查询
     *
     * @param page
     * @param pageSize
     * @param number
     * @param beginTime
     * @param endTime
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number, String beginTime, String endTime) {
        log.info("接收到订单分页查询请求，number：{}，beginTime：{}，endTime：{}", number, beginTime, endTime);

        Page<Orders> ordersPage = new Page<>(page, pageSize);

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(number != null, Orders::getNumber, number)
                .gt(StringUtils.isNotEmpty(beginTime), Orders::getOrderTime, beginTime)
                .lt(StringUtils.isNotEmpty(endTime), Orders::getOrderTime, endTime)
                .orderByDesc(Orders::getOrderTime);

        ordersService.page(ordersPage, queryWrapper);

        return R.success(ordersPage);
    }

    /**
     * 下单
     *
     * @param order
     * @return
     */
    @PostMapping("/submit")
    public R<Object> submit(@RequestBody Orders order) {

        boolean submit = ordersService.submit(order);
        if (!submit) {
            return R.error("异常，提交失败");
        }

        return R.success("提交成功");

    }

    /**
     * 用户订单分页查询
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page<OrdersDto>> userPage(int page, int pageSize) {
        log.info("接收到订单分页查询请求...");

        //创建Orders的Page对象
        Page<Orders> ordersPage = new Page<>(page, pageSize);
        //分页查询Orders
        LambdaQueryWrapper<Orders> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Orders::getUserId, BaseContext.getCurrentUser())
                .orderByDesc(Orders::getOrderTime);
        ordersService.page(ordersPage, queryWrapper1);

        //创建OrdersDto的Page对象
        Page<OrdersDto> ordersDtoPage = new Page<>();
        //封装分页信息到此Page
        BeanUtils.copyProperties(ordersPage, ordersDtoPage, "records");

        //遍历查到的orders，为每个order查询order_detail,并一起封装到ordersDto
        List<OrdersDto> ordersDtoList = ordersPage.getRecords().stream().map((item) -> {
            Long orderId = item.getId();
            LambdaQueryWrapper<OrderDetail> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper2);

            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);
            ordersDto.setOrderDetails(orderDetailList);
            return ordersDto;
        }).collect(Collectors.toList());

        //将以上封装好的ordersDto集合封装到其Page对象中
        ordersDtoPage.setRecords(ordersDtoList);

        //响应
        return R.success(ordersDtoPage);
    }
}
