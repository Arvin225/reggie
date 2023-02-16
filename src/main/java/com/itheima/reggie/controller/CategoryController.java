package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    public CategoryService categoryService;

    /**
     * 分页查询
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize) {

        log.info("接收到分页查询请求，页码：{},显示个数：{}", page, pageSize);

        Page<Category> pageInfo = new Page<>(page, pageSize);

        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Category::getSort);

        categoryService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 新增菜品/套餐分类
     *
     * @param category
     * @return
     */
    @PostMapping
    public R<Object> add(@RequestBody Category category) {

        log.info("接收到新增分类请求，分类名称；{}", category.getName());

        boolean saved = categoryService.save(category);
        if (saved) {
            return R.success(null);
        }
        return R.error("添加失败");
    }

    /**
     * 分类修改
     *
     * @param category
     * @return
     */
    @PutMapping
    public R<Object> update(@RequestBody Category category) {

        log.info("接收到分类修改请求，分类名称拟更改为：{}", category.getName());

        boolean updated = categoryService.updateById(category);
        if (updated) {
            return R.success(null);
        }
        return R.error("分类修改失败");
    }

    @DeleteMapping
    public R<Object> delete(long id) {

        log.info("接收到分类删除请求，id：{}", id);

        boolean removed = categoryService.removeById(id);
        if (removed) {
            return R.success(null);
        }
        return R.error("未知错误，删除失败");

    }

    /**
     * 查询菜品或套餐分类下的所有数据（根据type区分）
     *
     * @param category
     * @return
     */
    @GetMapping("/list")
    public R<List<Category>> list(Category category) {
        log.info("接收到分类查询list请求，type={}", category.getType());

        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(category.getType() != null, Category::getType, category.getType());
        queryWrapper.orderByAsc(Category::getSort);

        List<Category> categories = categoryService.list(queryWrapper);

        if (categories.isEmpty()) {
            R.error(category.getType() != null ? "未找到该类别的数据" : "未找到任何分类信息");
        }

        return R.success(categories);
    }
}
