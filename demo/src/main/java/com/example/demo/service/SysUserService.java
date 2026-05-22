package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.UserPageItem;
import com.example.demo.entity.SysUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser getUserByUsername(String username);

    /**
     * 分页查询用户列表
     */
    Page<UserPageItem> getUserPage(Page<SysUser> page, String keyword, String roleCode, String excludeRoleCode);

    /**
     * 创建用户
     */
    boolean createUser(SysUser user, Long[] roleIds);

    /**
     * 更新用户
     */
    boolean updateUser(SysUser user, Long[] roleIds);

    /**
     * 删除用户
     */
    boolean deleteUser(Long userId);

    /**
     * 重置密码
     */
    boolean resetPassword(Long userId, String newPassword);

    /**
     * 更新当前用户个人信息
     */
    boolean updateCurrentUserProfile(Long userId, SysUser user);
}
