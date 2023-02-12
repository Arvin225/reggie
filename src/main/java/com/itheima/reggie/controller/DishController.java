package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishDto;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    public DishService dishService;

    @Autowired
    public CategoryService categoryService;

    @Autowired
    public DishFlavorService dishFlavorService;


    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        log.info("收到菜品分页查询请求，当前页码：{}，显示条数：{}，是否模糊查询：{}", page, pageSize, name != null ? "是" : "否");

        //1.创建响应的Page对象
        Page<DishDto> dishDtoPage = new Page<>();

        //2.数据封装
        //2.1 除records外数据的封装，如total、size，主要通过拷贝分页查询dish表得到的Page对象的属性进行封装
        Page<Dish> dishPage = new Page<>(page, pageSize);

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, Dish::getName, name);//模糊查询
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getCreateTime);//排序

        dishService.page(dishPage, queryWrapper);
        //拷贝dishPage中除records外的所有属性到dishDtoPage，因为封装的不再是Dish对象，而是DishDto对象
        BeanUtils.copyProperties(dishPage, dishDtoPage, "records");

        //2.2 封装records
        //遍历查出来的dish表中的数据，将每一条数据封装进DishDto，同时查询category表封装CategoryName属性,最后装进list集合
        List<DishDto> records = dishPage.getRecords().stream().map((dish) -> {
            DishDto dishDto = new DishDto();

            //拷贝dish所有属性到dishDto，实现对dish的封装
            BeanUtils.copyProperties(dish, dishDto);

            //查询当前dish对应的categoryName
            Long categoryId = dish.getCategoryId();
            Category category = categoryService.getById(categoryId);

            //查询到则封装进dishDto
            if (category != null) {
                String categoryName = category.getName();
                //设置categoryName
                dishDto.setCategoryName(categoryName);
            }

            return dishDto;

        }).collect(Collectors.toList());

        //将以上装好DishDto数据的集合赋给ishDtoPage的records属性
        dishDtoPage.setRecords(records);

        //3.响应
        return R.success(dishDtoPage);
    }

    /**
     * 菜品信息回显
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> list(@PathVariable Long id) {
        log.info("接收到菜品回显请求，菜品id：{}", id);

        //1.创建数据封装对象
        DishDto dishDto = new DishDto();

        //2.封装菜品信息
        //2.1 查询菜品信息，未查到则响应错误
        Dish dish = dishService.getById(id);
        if (dish == null) {
            return R.error("异常，未找到该菜品信息");
        }

        //2.2 封装菜品信息，拷贝查询到的菜品至数据封装对象
        BeanUtils.copyProperties(dish, dishDto);

        //3.封装对应口味信息：查询菜品对应的口味信息，查到了则封装进数据封装对象
        //3.1 创建条件构造器，构造条件
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, id);
        queryWrapper.orderByDesc(DishFlavor::getUpdateTime);
        //3.2 查询
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        if (!flavors.isEmpty()) {
            //3.3封装对应口味信息
            dishDto.setFlavors(flavors);
        }

        //4.响应
        return R.success(dishDto);
    }

    /**
     * 菜品信息更新
     *
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<Object> update(@RequestBody DishDto dishDto) {
        log.info("接收到菜品信息更新请求，菜品id：{}", dishDto.getId());

        boolean saved = dishService.updateWithFlavor(dishDto);
        if (!saved) {
            return R.error("异常，修改失败");
        }

        return R.success("修改成功");
    }

    /**
     * 新增菜品
     *
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<Object> add(@RequestBody DishDto dishDto) {
        log.info("接收到菜品新增请求，菜品名称：{}", dishDto.getName());
        boolean saved = dishService.saveWithFlavor(dishDto);
        if (!saved) {
            return R.error("新增失败");
        }
        return R.success("新增成功");
    }

    /**
     * 菜品的（批量）删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<Object> delete(@RequestParam("ids") List<Long> ids) {
        log.info("接收到菜品删除请求，删除的菜品有：id:{}", ids);

        boolean removed = dishService.removeWithFlavorByIds(ids);
        if (!removed) {
            return R.error("异常，删除失败");
        }

        return R.success("删除成功");
    }

    /**
     * 菜品停/启售
     *
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<Object> status(@PathVariable int status, @RequestParam("ids") List<Long> ids) {
        log.info("接收到菜品停启售请求，status：{}，id：{}", status, ids);

        boolean updated = dishService.statusUpdate(status, ids);
        if (!updated) {
            return R.error("未知错误，停/启售失败");
        }

        return R.success("停/启售成功");
    }

    /**
     * 菜品列表查询
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<Dish>> list(Dish dish) {
        log.info("接收到菜品列表查询请求...");

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        queryWrapper.like(dish.getName() != null, Dish::getName, dish.getName());

        List<Dish> dishList = dishService.list(queryWrapper);

        return R.success(dishList);
    }


}
