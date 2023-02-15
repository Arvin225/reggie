package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addressBook")
@Slf4j
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 地址信息展示
     * @return
     */
    @GetMapping("/list")
    public R<List<AddressBook>> list() {

        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentUser());
        List<AddressBook> addressBookList = addressBookService.list(queryWrapper);
        if (addressBookList.isEmpty()) {
            return R.error("无地址信息");
        }

        return R.success(addressBookList);
    }

    /**
     * 新建地址
     * @param addressBook
     * @return
     */
    @PostMapping
    public R<Object> add(@RequestBody AddressBook addressBook) {
        log.info(addressBook.toString());

        if (addressBook == null){
            return R.error("错误，请检查数据是否有效");
        }

        addressBook.setUserId(BaseContext.getCurrentUser());

        boolean saved = addressBookService.save(addressBook);
        if (!saved) {
            return R.error("异常，保存失败");
        }

        return R.success("保存成功");
    }

    /**
     * 设置默认地址
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    public R<Object> defaultAddress(@RequestBody AddressBook addressBook){
        log.info("接收到地址默认设置请求，id：{}", addressBook.getId());
        if (addressBook.getId() == null){
            return R.error("错误，请检查参数是否有效");
        }

        LambdaUpdateWrapper<AddressBook> updateWrapper1 = new LambdaUpdateWrapper<>();
        updateWrapper1.eq(AddressBook::getUserId, BaseContext.getCurrentUser())
                .set(AddressBook::getIsDefault, 0);
        addressBookService.update(updateWrapper1);

        LambdaUpdateWrapper<AddressBook> updateWrapper2 = new LambdaUpdateWrapper<>();
        updateWrapper2.eq(AddressBook::getId, addressBook.getId())
                .set(AddressBook::getIsDefault, 1);
        addressBookService.update(updateWrapper2);

        return R.success("默认地址设置成功");
    }

    /**
     * 获取默认地址
     * @return
     */
    @GetMapping("/default")
    public R<AddressBook> getDefaultAddress(){
        log.info("接收到默认地址获取请求，当前用户：{}", BaseContext.getCurrentUser());

        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentUser())
                .eq(AddressBook::getIsDefault, 1);

        AddressBook defaultAddressBook = addressBookService.getOne(queryWrapper);

        return R.success(defaultAddressBook);
    }
}
