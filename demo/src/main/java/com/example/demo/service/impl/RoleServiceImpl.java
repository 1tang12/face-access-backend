package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.SysRole;
import com.example.demo.mapper.SysRoleMapper;
import com.example.demo.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色服务实现类
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements RoleService {

    private final SysRoleMapper roleMapper;

    @Override
    public IPage<SysRole> getRolePage(Page<SysRole> page, String keyword) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(SysRole::getRoleName, keyword)
                .or()
                .like(SysRole::getRoleCode, keyword)
                .or()
                .like(SysRole::getDescription, keyword)
            );
        }
        
        wrapper.orderByDesc(SysRole::getCreateTime);
        
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 先删除该角色的所有权限
        roleMapper.deleteRolePermissions(roleId);
        
        // 再添加新的权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            roleMapper.insertRolePermissions(roleId, permissionIds);
        }
    }

    @Override
    public List<Long> getRolePermissions(Long roleId) {
        return roleMapper.selectPermissionIdsByRoleId(roleId);
    }
}
