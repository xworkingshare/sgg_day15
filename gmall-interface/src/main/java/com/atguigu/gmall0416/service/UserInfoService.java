package com.atguigu.gmall0416.service;

import com.atguigu.gmall0416.bean.UserAddress;
import com.atguigu.gmall0416.bean.UserInfo;

import java.util.List;

public interface UserInfoService {
    // alt+enter
    List<UserInfo> findAll();
    // 根据userId 查询用户地址列表
    List<UserAddress> getUserAddressList(String userId);
    // 登录
    UserInfo login(UserInfo userInfo);
    // 认证方法
    UserInfo verify(String userId);
}
