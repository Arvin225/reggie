package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.mapper.OrdersMapper;
import com.itheima.reggie.service.AddressBookService;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrdersService;
import com.itheima.reggie.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Override
    public boolean submit(Orders order) {
        //获取当前用户的id
        Long userId = BaseContext.getCurrentUser();

        /*--------------------------------------------查数据--------------------------------------------*/
        //1.查address_book表中的数据
        Long addressBookId = order.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null) {
            throw new CustomException("地址信息为空，无法提交订单");
        }

        //2.查shopping_cart表中数据
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(shoppingCartLambdaQueryWrapper);
        if (shoppingCarts == null) {
            throw new CustomException("购物车为空，无法提交订单");
        }


        /*--------------------------------------------封装数据--------------------------------------------*/
        //1.生成orderId，供order_detail表数据封装用，同时作为order的id
        long orderId = IdWorker.getId();//利用baomidou提供的id生成工具生成id
        //为order设置id
        order.setId(orderId);

        //2.遍历每项菜品或套餐，计算总费用
        AtomicInteger amount = new AtomicInteger(0); //原子Integer，用于金额计算，同时原子类保证线程安全
        List<OrderDetail> orderDetailList = shoppingCarts.stream().map((item) -> {
            //2.1 计算单项费用
            BigDecimal price = item.getAmount(); //这里其实有点问题，amount在购物车添加或删减时本应得到计算，但没有，直接用单价覆盖了，因此这里既是单价
            Integer number = item.getNumber();
            price = price.multiply(new BigDecimal(number)); //BigDecimal是Java提供的一个高精度科学计算类型，封装了各种计算工具，比如multiply
            //2.2 计算总费用
            amount.getAndAdd(price.intValue());//getAndAdd实现累加

            //3.填充order_detail表数据
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(price);//单份的金额

            return orderDetail;
        }).collect(Collectors.toList());

        //4.保存orderDetail数据
        boolean saved1 = orderDetailService.saveBatch(orderDetailList);
        if (!saved1) {
            return false;
        }

        //5.封装order的数据
        //5.1 收货信息
        order.setConsignee(addressBook.getConsignee());//收货人
        order.setPhone(addressBook.getPhone());//手机号
        //地址拼接，为空则拼接一个空的字符串，否则会填充“Null”
        String address = (addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail());
        order.setAddress(address);//收货地址
        //5.2 其他数据
        order.setUserId(userId);//用户id
        order.setNumber(String.valueOf(orderId));////订单号
        order.setOrderTime(LocalDateTime.now());//下单时间
        order.setCheckoutTime(LocalDateTime.now());//支付时间
        order.setStatus(2);//订单状态 1待付款，2待派送，3已派送，4已完成，5已取消
        order.setAmount(new BigDecimal(amount.get()));//总费用amount

        //6.保存order
        boolean saved2 = ordersService.save(order);
        if (!saved2){
            return false;
        }

        /*-------------------------------------------清空购物车-------------------------------------------*/
        return shoppingCartService.remove(shoppingCartLambdaQueryWrapper);
    }
}
