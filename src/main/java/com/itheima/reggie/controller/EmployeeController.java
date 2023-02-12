package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     *
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        log.info("已接收到登录请求...");
        //1.将接收到的密码进行md5加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2.通过用户名查询数据库，并接收
        //创建条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        //构造条件
        queryWrapper.eq(StringUtils.isNotEmpty(employee.getUsername()), Employee::getUsername, employee.getUsername());
        //查库
        Employee emp = employeeService.getOne(queryWrapper);

        //3.判断是否查询到，未查询到则直接返回错误信息，否则进入下一步
        if (emp == null) {
            return R.error("未找到该用户");
        }

        //4.判断密码是否正确，错误直接返回，正确进行下一步
        if (!password.equals(emp.getPassword())) {
            return R.error("密码错误");
        }

        //5.判断用户状态
        if (emp.getStatus() == 0) {
            return R.error("该账户已被禁用");
        }

        //6.将员工id存至Session
        request.getSession().setAttribute("employee", emp.getId());

        //7.返回成功结果
        return R.success(emp);
    }

    /**
     * 退出功能
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<Object> logout(HttpServletRequest request) {
        log.info("已接收到退出请求...");
        request.getSession().removeAttribute("employee");
        return R.success(null);
    }

    /**
     * 新增员工
     *
     * @param employee
     * @return
     */
    @PostMapping
    public R<Object> add(HttpServletRequest request, @RequestBody Employee employee) {
        log.info("新增员工中，员工用户名：{}", employee.getUsername());

        //1.设置初始密码：123456（需md5加密）
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        //2.设置创建时间、更新时间、创建人、更新人信息
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
        //创建人、更新人只需其id，即当前登录的用户的id，也即session域中存储的值
//        employee.setCreateUser((Long) request.getSession().getAttribute("employee"));
//        employee.setUpdateUser((Long) request.getSession().getAttribute("employee"));

        //3.调用MBP提供的save方法将以上员工信息存入数据表
        boolean saved = employeeService.save(employee);
        if (saved) {
            return R.success(null);
        }
        return R.error("员工添加失败");
    }

    /**
     * 分页查询
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page<Employee>> page(int page, int pageSize, String name) {//注：这里接收的只是普通的get请求，不是restful风格的请求，参数在请求体中以?与路径去隔开，而非/的写法

        log.info("收到员工分页查询请求，当前页码：{}，显示条数：{}", page, pageSize);

        //1.创建Page对象封装接收到的页码、每页显示个数
        Page<Employee> pageInfo = new Page<>(page, pageSize);

        //2.创建查询语句构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        //2.1 构造模糊查询语句，按name模糊查询，应对名字查询请求，若name无值则此构造不生效
        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);
        //2.2 构造排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        //3.使用MBP提供的page方法，分页获取数据,同时封装进Page对象
        employeeService.page(pageInfo, queryWrapper);

        //4.响应
        return R.success(pageInfo);
    }

    /**
     * 员工状态及信息更新（根据id）
     *
     * @param request
     * @param employee
     * @return
     */
    @PutMapping
    public R<Object> update(HttpServletRequest request, @RequestBody Employee employee) {

        log.info("已收到更新请求，员工id为：{}", request.getSession().getAttribute("employee"));

        //设置本次更新人及更新时间
//        employee.setUpdateUser((Long) request.getSession().getAttribute("employee"));
//        employee.setUpdateTime(LocalDateTime.now());

        //根据id更新
        boolean updated = employeeService.updateById(employee);

        //响应
        if (updated) {
            return R.success(null);
        }
        return R.error("更新失败");
    }

    /**
     * 根据id查询员工信息
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}") //接收restful风格的请求
    public R<Employee> getOne(@PathVariable String id) {

        log.info("已收到员工信息查询请求，该员工id为：{}", id);

        Employee employee = employeeService.getById(id);
        if (employee != null) {
            return R.success(employee);
        }
        return R.error("未找到该员工");
    }

}
