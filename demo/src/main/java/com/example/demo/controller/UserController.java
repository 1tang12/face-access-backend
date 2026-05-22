package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.dto.UserPageItem;
import com.example.demo.entity.SysUser;
import com.example.demo.service.AuthService;
import com.example.demo.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;
    private final AuthService authService;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/page")
    public Result<PageResult<UserPageItem>> getUserPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String excludeRoleCode) {

        Page<SysUser> page = new Page<>(current, size);
        Page<UserPageItem> result = userService.getUserPage(page, keyword, roleCode, excludeRoleCode);

        PageResult<UserPageItem> pageResult = new PageResult<>(
            result.getTotal(),
            result.getRecords(),
            result.getCurrent(),
            result.getSize()
        );
        
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<Void> createUser(@RequestBody SysUser user, @RequestParam(required = false) Long[] roleIds) {
        try {
            userService.createUser(user, roleIds);
            return Result.success("创建成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody SysUser user, 
                                   @RequestParam(required = false) Long[] roleIds) {
        try {
            user.setId(id);
            userService.updateUser(user, roleIds);
            return Result.success("更新成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重置密码
     */
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        try {
            userService.resetPassword(id, newPassword);
            return Result.success("密码重置成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            SysUser user = userService.getById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            user.setStatus(status);
            userService.updateById(user);
            return Result.success("状态更新成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/current/profile")
    public Result<Void> updateCurrentProfile(@RequestBody SysUser user) {
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }
            userService.updateCurrentUserProfile(currentUser.getId(), user);
            return Result.success("个人信息更新成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
