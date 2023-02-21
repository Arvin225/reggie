package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.entity.SetmealDto;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 套餐信息分页查询
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page<SetmealDto>> page(int page, int pageSize, String name) {
        Page<SetmealDto> setmealDtoPage = new Page<>();

        //分页查询setmeal表，取出setmeal们，挨个拷贝到传输对象，再依据其categoryId查询category表，获得分类名，并设置进传输对象
        Page<Setmeal> setmealPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, Setmeal::getName, name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(setmealPage, queryWrapper);

        //将除记录之外的数据封装至setmealDtoPage,因为记录还要处理成dto才能封装
        BeanUtils.copyProperties(setmealPage, setmealDtoPage, "records");

        List<Setmeal> setmealList = setmealPage.getRecords();
        List<SetmealDto> setmealDtoList = setmealList.stream().map((setmeal) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(setmeal, setmealDto);
            Category category = categoryService.getById(setmeal.getCategoryId());
            setmealDto.setCategoryName(category.getName());
            return setmealDto;
        }).collect(Collectors.toList());

        //将setmealDto装进setmealDtoPage
        setmealDtoPage.setRecords(setmealDtoList);

        //响应
        return R.success(setmealDtoPage);
    }

    /**
     * 套餐信息回显
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable Long id) {
        log.info("接收到套餐信息回显请求，id：{}", id);

        //setmeal
        Setmeal setmeal = setmealService.getById(id);

        //categoryName
        Category category = categoryService.getById(setmeal.getCategoryId());
        String categoryName = category.getName();

        //setmealDishList
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishList = setmealDishService.list(setmealDishLambdaQueryWrapper);

        /*List<Long> dishIds = setmealDishList.stream().map((setmealDish -> {
            Long dishId = setmealDish.getDishId();
            return dishId;
        })).collect(Collectors.toList());

        List<Dish> dishList = dishService.listByIds(dishIds);*/

        //setmealDto
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);
        setmealDto.setCategoryName(categoryName);
        setmealDto.setSetmealDishes(setmealDishList);

        return R.success(setmealDto);
    }

    /**
     * 套餐新增
     *
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<Object> add(@RequestBody SetmealDto setmealDto) {
        log.info("接收到套餐新增请求，套餐名称：{}", setmealDto.getName());

        boolean saved = setmealService.saveWithSetmealDish(setmealDto);
        if (!saved) {
            return R.error("异常，新增失败");
        }
        return R.success("新增成功");
    }

    /**
     * 套餐修改
     *
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<Object> update(@RequestBody SetmealDto setmealDto) {
        log.info("接收到套餐修改请求，套餐id：{}", setmealDto.getId());

        boolean updated = setmealService.updateWithSetmealDish(setmealDto);
        if (!updated) {
            return R.error("异常，修改失败");
        }

        return R.success("修改成功");
    }

    /**
     * 套餐停/启售
     *
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<Object> status(@PathVariable int status, @RequestParam("ids") List<Long> ids) {
        log.info("接收到套餐停/启售请求，id：{}", ids);

        boolean updated = setmealService.statusUpdate(status, ids);
        if (!updated) {
            return R.error("异常，停/启售失败");
        }

        return R.success("停/启售成功");
    }

    /**
     * 套餐删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<Object> delete(@RequestParam("ids") List<Long> ids) {
        log.info("接收到套餐删除请求，id：{}", ids);

        boolean removed = setmealService.removeWithSetmealDish(ids);
        if (!removed) {
            return R.error("异常，删除失败");
        }

        return R.success("删除成功");
    }

    /**
     * 套餐分类查询
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        log.info("接收到套餐分类查询请求：{}", setmeal.toString());

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getCategoryId, setmeal.getCategoryId()).
                eq(Setmeal::getStatus, setmeal.getStatus());
        List<Setmeal> setmealList = setmealService.list(queryWrapper);

        if (setmealList.isEmpty()){
            R.error("无数据");
        }

        return R.success(setmealList);
    }

}
