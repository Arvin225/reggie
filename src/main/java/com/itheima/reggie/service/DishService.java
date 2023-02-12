package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DishService extends IService<Dish> {

    /**
     * 菜品更新（同时更新口味表）
     * @param dishDto
     * @return
     */
    @Transactional
    boolean updateWithFlavor(DishDto dishDto);

    /**
     * 菜品新增（同时新增口味数据）
     * @param dishDto
     * @return
     */
    @Transactional
    boolean saveWithFlavor(DishDto dishDto);

    /**
     * 菜品（批量）删除（同时删除口味数据）
     * @param ids
     * @return
     */
    @Transactional
    boolean removeWithFlavorByIds(List<Long> ids);

    /**
     * 菜品停/启售
     * @param status
     * @param ids
     * @return
     */
    boolean statusUpdate(int status, List<Long> ids);
}
