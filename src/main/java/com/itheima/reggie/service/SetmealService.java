package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 套餐新增，同时操作setmeal_dish表
     *
     * @param setmealDto
     * @return
     */
    @Transactional
    boolean saveWithSetmealDish(SetmealDto setmealDto);

    /**
     * 套餐修改，同时操作setmeal_dish表
     *
     * @param setmealDto
     * @return
     */
    @Transactional
    boolean updateWithSetmealDish(SetmealDto setmealDto);

    /**
     * 套餐售卖状态更新
     * @param status
     * @param ids
     * @return
     */
    boolean statusUpdate(int status, List<Long> ids);

    /**
     * 套餐删除（同时删除与菜品的关联）
     * @param ids
     * @return
     */
    @Transactional
    boolean removeWithSetmealDish(List<Long> ids);
}
