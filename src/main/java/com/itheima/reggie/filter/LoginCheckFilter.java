package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录检查
 */
@Slf4j
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {

    /**
     * 处理未登录时的请求
     *
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        //向下转型
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        log.info("当前拦截到的请求：{}", request.getRequestURI());

        //1.对无需处理的资源放行
        //1.1构造需要放行的urls
        String[] urls = {
                "/employee/login",
                "/employee/logout",
                "/user/login",
                "/user/logout",
                "/backend/**",
                "/front/**",
                "/common/**"
        };
        //1.2判断接收的请求路径是否匹配以上url，是则放行
        Boolean check = urlMatcher(request.getRequestURI(), urls);
        if (check) {
            log.info("该资源无需处理，已放行...");
            filterChain.doFilter(request, response);
            return;
        }

        //2.1 后台对已登录放行
        if (request.getSession().getAttribute("employee") != null) {//获取session中的员工，判断是否为空，不为空则已登录
            log.info("员工登录状态，已放行...");

            //将登录人的id存入Thread的局部变量中，供后续同一线程的其他程序使用
            BaseContext.setCurrentUser((Long) request.getSession().getAttribute("employee"));

            filterChain.doFilter(request, response);
            return;
        }

        //2.2 移动端对已登录放行
        if (request.getSession().getAttribute("user") != null) {//获取session中的用户，判断是否为空，不为空则已登录
            log.info("用户登录状态，已放行...");

            //将登录人的id存入Thread的局部变量中，供后续同一线程的其他程序使用
            BaseContext.setCurrentUser((Long) request.getSession().getAttribute("user"));

            filterChain.doFilter(request, response);
            return;
        }

        //3.处理未登录时发起的请求，结合前端代码，响应数据
        log.info("已对请求：{} 进行处理...", request.getRequestURI());
        servletResponse.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
    }

    //构造一个路径匹配器
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 路径匹配的方法
     *
     * @param requestURI
     * @param urls
     * @return
     */
    private Boolean urlMatcher(String requestURI, String[] urls) {
        //循环遍历，匹配成功则返回true
        for (String url : urls) {
            if (PATH_MATCHER.match(url, requestURI)) {
                return true;
            }
        }
        //否则返回false
        return false;
    }
}
