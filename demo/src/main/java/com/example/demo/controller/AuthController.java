package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.GuestEntryRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.entity.SysUser;
import com.example.demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return Result.success("登录成功", response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 访客入口：提交基本信息后进入访客端
     */
    @PostMapping("/guest-entry")
    public Result<LoginResponse> guestEntry(@Validated @RequestBody GuestEntryRequest request) {
        try {
            LoginResponse response = authService.guestEntry(request);
            return Result.success("访客登记成功", response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("登出成功", null);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Result<SysUser> getCurrentUser() {
        SysUser user = authService.getCurrentUser();
        if (user != null) {
            user.setPassword(null); // 不返回密码
            return Result.success(user);
        }
        return Result.error("未登录");
    }
}
