package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.SysRole;
import com.example.demo.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 分页查询角色列表
     */
    @GetMapping("/page")
    public Result<IPage<SysRole>> getRolePage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword
    ) {
        Page<SysRole> page = new Page<>(current, size);
        IPage<SysRole> result = roleService.getRolePage(page, keyword);
        return Result.success(result);
    }

    /**
     * 查询所有角色
     */
    @GetMapping("/list")
    public Result<List<SysRole>> getAllRoles() {
        List<SysRole> roles = roleService.list();
        return Result.success(roles);
    }

    /**
     * 根据ID查询角色
     */
    @GetMapping("/{id}")
    public Result<SysRole> getRoleById(@PathVariable Long id) {
        SysRole role = roleService.getById(id);
        if (role != null) {
            return Result.success(role);
        }
        return Result.error("角色不存在");
    }

    /**
     * 创建角色
     */
    @PostMapping
    public Result<Void> createRole(@RequestBody SysRole role) {
        boolean success = roleService.save(role);
        if (success) {
            return Result.success("创建成功", null);
        }
        return Result.error("创建失败");
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        boolean success = roleService.updateById(role);
        if (success) {
            return Result.success("更新成功", null);
        }
        return Result.error("更新失败");
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        boolean success = roleService.removeById(id);
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }

    /**
     * 为角色分配权限
     */
    @PostMapping("/{roleId}/permissions")
    public Result<Void> assignPermissions(
            @PathVariable Long roleId,
            @RequestBody List<Long> permissionIds
    ) {
        roleService.assignPermissions(roleId, permissionIds);
        return Result.success("权限分配成功", null);
    }

    /**
     * 查询角色的权限
     */
    @GetMapping("/{roleId}/permissions")
    public Result<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        List<Long> permissionIds = roleService.getRolePermissions(roleId);
        return Result.success(permissionIds);
    }
}
