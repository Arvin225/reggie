package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;

    public boolean updateWithFlavor(DishDto dishDto) {
        //1.更新dish表
        boolean updated = this.updateById(dishDto);//这里可以直接传入dishDto，因为DishDto继承自Dish，是其子类，可以向上转型
        if (!updated) {
            return false;
        }

        //2.更新dish_flavor表：先删除该dish的所有flavor数据，再插入新的flavor数据，获取传来的dishDto中dishFlavor的部分，进行遍历，挨个设置好dish_id，最后批量插入设置好的数据
        //2.1 删除
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(queryWrapper);
        //2.2 插入
        //2.2.1 设置dish_id
        //获取DishFlavor对象列表
        List<DishFlavor> flavors = dishDto.getFlavors();
        //遍历DishFlavor对象列表，设置dish_id
        flavors.forEach((flavor) -> {
            flavor.setDishId(dishDto.getId());
        });

        //2.2.2 插入（这里批量插入）
        boolean savedOrNot = dishFlavorService.saveBatch(flavors);
        return savedOrNot;
    }

    @Override
    public boolean saveWithFlavor(DishDto dishDto) {

        //1. 存dish表
        boolean saved = this.save(dishDto);
        if (!saved) {
            return false;
        }

        //2.存dish_flavor表
        //2.1 获取传入的DishFlavor对象列表
        List<DishFlavor> flavors = dishDto.getFlavors();
        //2.2 为每个flavor设置好dish_id
        flavors.forEach((flavor) -> {
            flavor.setDishId(dishDto.getId());
        });
        //2.3 批量保存（插入）
        boolean savedOrNot = dishFlavorService.saveBatch(flavors);

        return savedOrNot;
    }

    @Override
    public boolean removeWithFlavorByIds(List<Long> ids) {

        //获取要删除的dish们
        List<Dish> dishes = this.listByIds(ids);
        //遍历，判断是否有已启售或有关联套餐的
        dishes.forEach((dish -> {
            //获取状态
            Integer status = dish.getStatus();
            //已启售则抛异常
            if (status == 1) {
                throw new CustomException("删除失败，有正在售卖或关联了套餐的菜品，请检查");
            }

            //获取归属套餐的数量
            LambdaQueryWrapper<SetmealDish> setmealDishQueryWrapper = new LambdaQueryWrapper<>();
            setmealDishQueryWrapper.eq(SetmealDish::getDishId, dish.getId());
            int count = setmealDishService.count(setmealDishQueryWrapper);
            //有归属套餐则抛异常
            if (count > 0) {
                throw new CustomException("删除失败，有正在售卖或关联了套餐的菜品，请检查");
            }

        }));

        //删除dish表中数据
        boolean removed = this.removeByIds(ids);
        if (!removed) {
            return false;
        }

        //删除dish_flavor表中数据
        LambdaQueryWrapper<DishFlavor> dishFlavorQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorQueryWrapper.in(DishFlavor::getDishId, ids);
        boolean removedOrNot = dishFlavorService.remove(dishFlavorQueryWrapper);

        return removedOrNot;
    }

    @Override
    public boolean statusUpdate(int status, List<Long> ids) {

        /*
        停启售逻辑
            启售：直接启售
            停售：判断是否有关联套餐，无则直接停售
                有则判断关联套餐中是否有启售的，有则响应失败，无则停售
         */

        //停售
        if (status == 0) {
            //判断是否有关联套餐，无则直接停售
            LambdaQueryWrapper<SetmealDish> setmealDishQueryWrapper = new LambdaQueryWrapper<>();
            setmealDishQueryWrapper.in(SetmealDish::getDishId, ids);
            List<SetmealDish> setmealDishList = setmealDishService.list(setmealDishQueryWrapper);
            //没有关联套餐，直接停售
            if (setmealDishList.isEmpty()) {
                //改变状态：停售
                return statusUpdater(status, ids);

            }

            //有则判断关联套餐中是否有启售的，有则抛异常，无则停售
            //获取各个关联套餐的ID
            List<Long> setmealIds = setmealDishList.stream().map((setmealDish -> {
                Long setmealId = setmealDish.getSetmealId();
                return setmealId;
            })).collect(Collectors.toList());
            //根据以上获取到的关联套餐的ID，查询setmeal表中状态为1的关联套餐的数量
            LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
            setmealLambdaQueryWrapper.in(Setmeal::getId, setmealIds).eq(Setmeal::getStatus, 1);
            int count = setmealService.count(setmealLambdaQueryWrapper);
            //数量大于0即有已启售的关联套餐，此时抛出异常
            if (count > 0) {
                throw new CustomException("停/启售失败，有已启售的相关联的套餐");
            }

            //关联套餐均为停售状态，改变状态，停售
            return statusUpdater(status, ids);
        }

        //启售
        //改变状态：启售
        return statusUpdater(status, ids);

    }

    /**
     * 状态更新器
     * @param status
     * @param ids
     * @return
     */
    private boolean statusUpdater(int status, List<Long> ids) {
        List<Dish> dishList = ids.stream().map((id) -> {
            Dish dish = new Dish();
            dish.setId(id);
            dish.setStatus(status);
            return dish;
        }).collect(Collectors.toList());
        boolean updatedOrNot = this.updateBatchById(dishList);
        return updatedOrNot;
    }
}
