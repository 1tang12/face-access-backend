package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.SysRole;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService extends IService<SysRole> {

    /**
     * 分页查询角色
     */
    IPage<SysRole> getRolePage(Page<SysRole> page, String keyword);

    /**
     * 为角色分配权限
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 查询角色的权限ID列表
     */
    List<Long> getRolePermissions(Long roleId);
}
