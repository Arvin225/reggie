package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * 公共controller，用于处理一些不同业务所复用的请求，例如上传、下载
 */
@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    //注入yml文件中自定义的存储路径的值
    @Value("${reggie.path}")
    private String basePath;

    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file) {

        //1.给文件名加上uuid或直接uuid命名，避免文件名相同而发生覆盖
        //1.1 获取传入文件的名称（带后缀）
        String originalFilename = file.getOriginalFilename();
        //1.2 加上生成的uuid为新的文件名
        String fileName = UUID.randomUUID().toString() + originalFilename;

        //2,判断存储目录是否存在，不存在则创建
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        //3.转存到指定文件
        try {
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //响应存下的文件名，以供前端使用
        return R.success(fileName);
    }

    /**
     * 文件下载
     *
     * @param name
     * @param response
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response) {
        //创建输入输出流(管道)
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(basePath + name));
            ServletOutputStream servletOutputStream = response.getOutputStream();

            //设置写回去的文件类型
            response.setContentType("image/jpeg");

            //根据文件名读取文件,同时写回
            byte[] buff = new byte[1024];
            int len;
            while ((len = fileInputStream.read(buff)) != -1) { //一次读取1024个字节，因为buff大小为1024，若是read()方法，则一次读取一个字节
                //写回浏览器页面
                servletOutputStream.write(buff, 0, len);
                servletOutputStream.flush();
            }

            //关闭流
            fileInputStream.close();
            servletOutputStream.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
