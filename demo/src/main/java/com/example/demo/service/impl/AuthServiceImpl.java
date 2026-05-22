package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.GuestEntryRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.entity.SysRole;
import com.example.demo.entity.SysUser;
import com.example.demo.entity.SysUserRole;
import com.example.demo.mapper.SysRoleMapper;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.mapper.SysUserRoleMapper;
import com.example.demo.service.AuthService;
import com.example.demo.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 认证服务实现类
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        boolean passwordMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatch) {
            throw new RuntimeException("用户名或密码错误");
        }

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse guestEntry(GuestEntryRequest request) {
        SysRole guestRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "ROLE_GUEST")
                .last("limit 1"));
        if (guestRole == null) {
            throw new RuntimeException("访客角色未配置");
        }

        String phone = request.getPhone().trim();
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone)
                .last("limit 1"));

        if (user != null) {
            List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
            if (!roles.contains("ROLE_GUEST")) {
                throw new RuntimeException("该手机号已绑定正式用户，请使用账号密码登录");
            }
            user.setRealName(request.getRealName().trim());
            user.setEmail(StringUtils.trimToNull(request.getEmail()));
            user.setRemark(StringUtils.defaultIfBlank(request.getRemark(), "访客入口登记"));
            user.setStatus(1);
            userMapper.updateById(user);
        } else {
            user = new SysUser();
            user.setUsername(generateGuestUsername(phone));
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRealName(request.getRealName().trim());
            user.setPhone(phone);
            user.setEmail(StringUtils.trimToNull(request.getEmail()));
            user.setStatus(1);
            user.setRemark(StringUtils.defaultIfBlank(request.getRemark(), "访客入口登记"));
            userMapper.insert(user);

            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(guestRole.getId());
            userRoleMapper.insert(userRole);
        }

        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(SysUser user) {
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionCodesByUserId(user.getId());

        LoginResponse response = new LoginResponse();
        response.setToken(token);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);
        response.setUserInfo(userInfo);
        return response;
    }

    private String generateGuestUsername(String phone) {
        String normalizedPhone = phone.replaceAll("[^0-9A-Za-z]", "");
        String baseUsername = "guest_" + (StringUtils.isBlank(normalizedPhone) ? UUID.randomUUID().toString().substring(0, 8) : normalizedPhone);
        String username = baseUsername;
        int suffix = 1;
        while (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            username = baseUsername + "_" + suffix;
            suffix++;
        }
        return username;
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    @Override
    public SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            String username = (String) authentication.getPrincipal();
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getUsername, username);
            return userMapper.selectOne(wrapper);
        }
        return null;
    }
}
