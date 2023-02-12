package com.itheima.reggie.entity;

import lombok.Data;

import java.util.List;

@Data
public class SetmealDto extends Setmeal{
    public String categoryName;

    public List<Dish> dishList;
}
