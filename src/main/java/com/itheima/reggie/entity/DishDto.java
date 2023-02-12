package com.itheima.reggie.entity;

import lombok.Data;

import java.util.List;

/**
 * DTO--用于不同数据表中某些数据的整合传输的对象，这里整合的是dish表中所有数据及category表中name字段的数据
 */
@Data
public class DishDto extends Dish{ //继承Dish，获得其所有属性，用于封装Dish表数据
    public String categoryName;

    public List<DishFlavor> flavors;

}
