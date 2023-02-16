package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 发送短信验证码
     *
     * @param user
     * @param request
     * @return
     */
    @PostMapping("/sendMsg")
    public R<Object> sendMsg(@RequestBody User user, HttpServletRequest request) {
        String phone = user.getPhone();
        log.info("接收到 {} 的短信验证码发送请求", phone);
        if (phone == null) {
            return R.error("验证码发送失败");
        }

        String code = String.valueOf(ValidateCodeUtils.generateValidateCode(6));

        request.getSession().setAttribute(phone, code);

        log.info("你的验证码是：{}", code);

        return R.success("验证码发送成功");
    }

    @PostMapping("/login")
    public R<User> login(@RequestBody Map phoneAndCode, HttpServletRequest request) {
        String phone = (String) phoneAndCode.get("phone");
        String code = (String) phoneAndCode.get("code");

        log.info("接收到用户登录请求，手机号：{}，验证码：{}", phone, code);

        //判空，响应失败
        if (phone == null || code == null) {
            return R.error("手机号或验证码不能为空");
        }

        //验证码核对
        //错误，响应验证码错误，提示重试
        if (request.getSession().getAttribute(phone) == null
                || !code.equals(request.getSession().getAttribute(phone))) {

            return R.error("验证码错误");
        }

        //正确，登录（或注册后登录）

        //判断是否有该用户，无则注册
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = userService.getOne(queryWrapper);
        if (user == null) {
            user = new User();
            user.setStatus(1);
            user.setPhone(phone);
            userService.save(user);
            user = userService.getOne(queryWrapper);
        }

        //id存入session
        request.getSession().setAttribute("user", user.getId());

        return R.success(user);
    }

    @PostMapping("/logout")
    public R<Object> logout(HttpServletRequest request) {
        log.info("接收到用户退出请求，当前用户id：{}", BaseContext.getCurrentUser());

        request.getSession().removeAttribute("user");

        return R.success("已退出");
    }


}
