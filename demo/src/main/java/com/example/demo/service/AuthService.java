package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.GuestEntryRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.entity.SysUser;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 访客免账号入口
     */
    LoginResponse guestEntry(GuestEntryRequest request);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 获取当前登录用户信息
     */
    SysUser getCurrentUser();
}
