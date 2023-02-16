package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 购物车查询
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        log.info("接收到用户：{} 的购物车查询请求", BaseContext.getCurrentUser());

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentUser());
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(queryWrapper);
        /*if (shoppingCartList.isEmpty()){
            return R.error("无数据");
        }*/

        return R.success(shoppingCartList);
    }

    /**
     * 购物车菜品或套餐份数加一
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<Object> add(@RequestBody ShoppingCart shoppingCart) {
        log.info("接收到添加购物车请求，添加的菜品或套餐信息：{}", shoppingCart.toString());

        //判空
        if (shoppingCart.getDishId() == null && shoppingCart.getSetmealId() == null) {
            return R.error("错误，请检查参数是否有效");
        }

        //设置当前用户的id、创建时间
        shoppingCart.setUserId(BaseContext.getCurrentUser());
        shoppingCart.setCreateTime(LocalDateTime.now());

        //查表
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentUser());
        if (shoppingCart.getSetmealId() != null) {
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        } else {
            queryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        }
        ShoppingCart getShoppingCart = shoppingCartService.getOne(queryWrapper);

        //存在，更新number
        if (getShoppingCart != null) {
            Integer number = getShoppingCart.getNumber();
            number += 1;
            getShoppingCart.setNumber(number);
            shoppingCartService.updateById(getShoppingCart);
            return R.success("");
        }

        //不存在，将number设为1，再插入
        shoppingCart.setNumber(1);
        shoppingCartService.save(shoppingCart);

        return R.success("");
    }

    /**
     * 购物车菜品或套餐份数减一
     * @param shoppingCart
     * @return
     */
    @PostMapping("/sub")
    public R<Object> sub(@RequestBody ShoppingCart shoppingCart) {
        log.info("接收到移除购物车请求，移除的套餐或菜品信息：{}", shoppingCart.toString());

        //判空
        if (shoppingCart.getDishId() == null && shoppingCart.getSetmealId() == null) {
            return R.error("异常，请检查参数是否有效");
        }

        shoppingCart.setUserId(BaseContext.getCurrentUser());

        /*移除逻辑
            判断number的值
                等于1则删除
                大于1则更新（number-=1）
         */

        //查询number的值，为1则直接删除，大于1则number-1并更新
        //查
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, shoppingCart.getUserId());
        //根据传入的是菜品还是套餐，构造不同的条件
        if (shoppingCart.getSetmealId() != null) {
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        } else if (shoppingCart.getDishId() != null) {
            queryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        }
        ShoppingCart getShoppingCart = shoppingCartService.getOne(queryWrapper);
        Integer number = getShoppingCart.getNumber();

        if (number < 1) {
            return R.error("异常，份数小于1");
        }

        //只剩一份，删除
        if (number == 1) {
            shoppingCartService.remove(queryWrapper);
            return R.success("");
        }

        //还有许多份，份数减一再更新
        LambdaUpdateWrapper<ShoppingCart> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentUser());
        updateWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentUser());
        if (shoppingCart.getSetmealId() != null) {
            updateWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        } else if (shoppingCart.getDishId() != null) {
            updateWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        }
        number -= 1;
        getShoppingCart.setNumber(number);
        shoppingCartService.updateById(getShoppingCart);

        return R.success("");
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<Object> clean() {
        log.info("接收到 用户{} 的购物车清空请求", BaseContext.getCurrentUser());

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentUser());
        boolean removed = shoppingCartService.remove(queryWrapper);

        if (!removed){
            return R.error("异常，清空失败");
        }

        return R.success("清空购物车成功");
    }

}
