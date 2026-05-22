package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.UserPageItem;
import com.example.demo.entity.SysUser;
import com.example.demo.entity.SysUserRole;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.mapper.SysUserRoleMapper;
import com.example.demo.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SysUser getUserByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public Page<UserPageItem> getUserPage(Page<SysUser> page, String keyword, String roleCode, String excludeRoleCode) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        List<SysUser> allUsers = userMapper.selectList(wrapper);
        List<UserPageItem> items = new ArrayList<>();
        for (SysUser user : allUsers) {
            String currentRoleCode = userMapper.selectPrimaryRoleCodeByUserId(user.getId());
            if (StringUtils.isNotBlank(excludeRoleCode) && StringUtils.equals(excludeRoleCode, currentRoleCode)) {
                continue;
            }
            if (StringUtils.isNotBlank(roleCode) && !StringUtils.equals(roleCode, currentRoleCode)) {
                continue;
            }
            UserPageItem item = new UserPageItem();
            item.setId(user.getId());
            item.setUsername(user.getUsername());
            item.setRealName(user.getRealName());
            item.setEmail(user.getEmail());
            item.setPhone(user.getPhone());
            item.setEmployeeNo(user.getEmployeeNo());
            item.setStatus(user.getStatus());
            item.setAvatar(user.getAvatar());
            item.setRemark(user.getRemark());
            item.setCreateTime(user.getCreateTime());
            item.setRoleCode(currentRoleCode);
            item.setRoleName(userMapper.selectPrimaryRoleNameByUserId(user.getId()));
            items.add(item);
        }
        int fromIndex = Math.max(0, (int) ((page.getCurrent() - 1) * page.getSize()));
        int toIndex = Math.min(items.size(), fromIndex + (int) page.getSize());
        List<UserPageItem> pageRecords = fromIndex >= items.size() ? List.of() : items.subList(fromIndex, toIndex);
        Page<UserPageItem> itemPage = new Page<>(page.getCurrent(), page.getSize(), items.size());
        itemPage.setRecords(pageRecords);
        return itemPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createUser(SysUser user, Long[] roleIds) {
        // 检查用户名是否存在
        if (getUserByUsername(user.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 保存用户
        userMapper.insert(user);

        // 保存用户角色关联
        if (roleIds != null && roleIds.length > 0) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUser user, Long[] roleIds) {
        // 更新用户信息
        userMapper.updateById(user);

        // 删除原有角色关联
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, user.getId());
        userRoleMapper.delete(wrapper);

        // 保存新的角色关联
        if (roleIds != null && roleIds.length > 0) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        // 删除用户
        userMapper.deleteById(userId);

        // 删除用户角色关联
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        userRoleMapper.delete(wrapper);

        return true;
    }

    @Override
    public boolean resetPassword(Long userId, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCurrentUserProfile(Long userId, SysUser user) {
        SysUser existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        existing.setRealName(user.getRealName());
        existing.setPhone(user.getPhone());
        existing.setEmail(user.getEmail());
        existing.setRemark(user.getRemark());
        existing.setAvatar(user.getAvatar());
        return userMapper.updateById(existing) > 0;
    }
}
