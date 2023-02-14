package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.entity.SetmealDto;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private DishService dishService;

    @Override
    public boolean saveWithSetmealDish(SetmealDto setmealDto) {

        //setmeal表
        boolean saved = this.save(setmealDto);
        if (!saved) {
            return false;
        }

        //setmealDish表
        //插入前，为每条数据设置好setmealId
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.forEach((setmealDish) -> {
            setmealDish.setSetmealId(setmealDto.getId());
        });

        return setmealDishService.saveBatch(setmealDishes);
    }

    @Override
    public boolean updateWithSetmealDish(SetmealDto setmealDto) {

        //setmeal表
        boolean updated = this.updateById(setmealDto);
        if (!updated) {
            return false;
        }

        //setmeal_dish表
        //清空当前套餐下的菜品
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        boolean removed = setmealDishService.remove(queryWrapper);
        if (!removed) {
            return false;
        }
        //插入接收到的菜品，在此之前先为每条数据设置好setmealId
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.forEach((setmealDish) -> {
            setmealDish.setSetmealId(setmealDto.getId());
        });

        return setmealDishService.saveBatch(setmealDishes);
    }

    @Override
    public boolean statusUpdate(int status, List<Long> ids) {
        /*
        停启售逻辑
            停售：直接停售
            启售：判断套餐下是否有菜品停售
                    有：抛异常
                    无：启售
         */

        //启售
        if (status == 1) {
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(SetmealDish::getSetmealId, ids);
            List<SetmealDish> setmealDishes = setmealDishService.list(queryWrapper);

            List<Long> dishIds = setmealDishes.stream().map((setmealDish) -> {
                Long dishId = setmealDish.getDishId();
                return dishId;
            }).collect(Collectors.toList());

            LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishLambdaQueryWrapper.in(Dish::getId, dishIds).eq(Dish::getStatus, 0);
            int count = dishService.count(dishLambdaQueryWrapper);

            if (count > 0) {
                throw new CustomException("启售失败，选择的套餐下有已停售的菜品，请修改后重新启售");
            }

            //启售
            boolean updated = statusUpdater(status, ids);
            if (!updated) {
                return false;
            }
        }

        //停售
        return statusUpdater(status, ids);

    }

    @Override
    public boolean removeWithSetmealDish(List<Long> ids) {
        /*
        删除逻辑
            停售：删（套餐菜品关联表也要删）
            启售：不删
         */

        //查询待删除的套餐（中）是否（有）启售（的），启售则抛异常
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids).eq(Setmeal::getStatus, 1);
        int count = this.count(queryWrapper);
        if (count > 0) {
            throw new CustomException(ids.size() > 1 ? "删除失败：选择的套餐中有已启售的套餐，请先停售" : "删除失败：该套餐未停售");
        }

        //都停售了，则删除
        //删除setmeal表数据
        boolean setmealRemoved = this.removeByIds(ids);
        if (!setmealRemoved) {
            return false;
        }

        //删除setmeal_dish表中数据
        LambdaQueryWrapper<SetmealDish> setmealDishQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishQueryWrapper.in(SetmealDish::getSetmealId, ids);

        return setmealDishService.remove(setmealDishQueryWrapper);
    }

    /**
     * 状态更新器
     *
     * @param status
     * @param ids
     * @return
     */
    private boolean statusUpdater(int status, List<Long> ids) {
        List<Setmeal> setmealList = ids.stream().map((id) -> {
            Setmeal setmeal = new Setmeal();
            setmeal.setId(id);
            setmeal.setStatus(status);
            return setmeal;
        }).collect(Collectors.toList());
        boolean updatedOrNot = this.updateBatchById(setmealList);
        return updatedOrNot;
    }
}
