package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public R<User> login(@RequestBody User user, HttpServletRequest request){
        String phone = user.getPhone();
        log.info("接收到用户登录请求，用户手机号：{}", phone);

        //判空，响应失败
        if (phone == null){
            return R.error("手机号不能为空");
        }

        //验证码核对
        //错误，响应验证码错误，提示重试
        //正确，登录（或注册后登录）

        //判断是否有该用户，无则注册
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User getUser = userService.getOne(queryWrapper);
        if (getUser == null){
            user.setStatus(1);
            userService.save(user);
            getUser = userService.getOne(queryWrapper);
        }

        //id存入session
        request.getSession().setAttribute("user", getUser.getId());

        return R.success(getUser);
    }

    @PostMapping("/logout")
    public R<Object> logout(HttpServletRequest request){
        log.info("接收到用户退出请求，当前用户id：{}", BaseContext.getCurrentUser());

        request.getSession().removeAttribute("user");

        return R.success("已退出");
    }


}
