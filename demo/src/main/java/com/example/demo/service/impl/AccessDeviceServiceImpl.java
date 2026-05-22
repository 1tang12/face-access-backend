package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.AccessDevice;
import com.example.demo.mapper.AccessDeviceMapper;
import com.example.demo.service.AccessDeviceService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessDeviceServiceImpl extends ServiceImpl<AccessDeviceMapper, AccessDevice> implements AccessDeviceService {

    private final AccessDeviceMapper accessDeviceMapper;

    @Override
    public Page<AccessDevice> getDevicePage(Page<AccessDevice> page, String keyword, Integer status) {
        LambdaQueryWrapper<AccessDevice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(AccessDevice::getDeviceName, keyword)
                    .or()
                    .like(AccessDevice::getDeviceCode, keyword)
                    .or()
                    .like(AccessDevice::getLocation, keyword);
        }
        if (status != null) {
            wrapper.eq(AccessDevice::getStatus, status);
        }
        wrapper.orderByDesc(AccessDevice::getId);
        return accessDeviceMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createDevice(AccessDevice accessDevice) {
        LambdaQueryWrapper<AccessDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccessDevice::getDeviceCode, accessDevice.getDeviceCode());
        if (accessDeviceMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("设备编码已存在");
        }
        return accessDeviceMapper.insert(accessDevice) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDevice(AccessDevice accessDevice) {
        AccessDevice existing = accessDeviceMapper.selectById(accessDevice.getId());
        if (existing == null) {
            throw new RuntimeException("设备不存在");
        }
        LambdaQueryWrapper<AccessDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccessDevice::getDeviceCode, accessDevice.getDeviceCode())
                .ne(AccessDevice::getId, accessDevice.getId());
        if (accessDeviceMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("设备编码已存在");
        }
        return accessDeviceMapper.updateById(accessDevice) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDevice(Long id) {
        return accessDeviceMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessDevice getOrCreateMobileWebDevice() {
        LambdaQueryWrapper<AccessDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccessDevice::getDeviceCode, MOBILE_WEB_DEVICE_CODE).last("LIMIT 1");
        AccessDevice existing = accessDeviceMapper.selectOne(wrapper);
        if (existing != null) {
            boolean needsUpdate = !Integer.valueOf(1).equals(existing.getStatus())
                    || !"virtual".equalsIgnoreCase(existing.getDeviceType())
                    || !StringUtils.equals(existing.getDeviceName(), "手机识别终端");
            if (needsUpdate) {
                existing.setStatus(1);
                existing.setDeviceType("virtual");
                existing.setDeviceName("手机识别终端");
                if (StringUtils.isBlank(existing.getLocation())) {
                    existing.setLocation("移动端浏览器");
                }
                if (StringUtils.isBlank(existing.getDescription())) {
                    existing.setDescription("用于手机浏览器人脸识别的虚拟设备");
                }
                accessDeviceMapper.updateById(existing);
            }
            return existing;
        }

        AccessDevice device = new AccessDevice();
        device.setDeviceName("手机识别终端");
        device.setDeviceCode(MOBILE_WEB_DEVICE_CODE);
        device.setLocation("移动端浏览器");
        device.setDeviceType("virtual");
        device.setIpAddress("mobile-web");
        device.setStatus(1);
        device.setDescription("用于手机浏览器人脸识别的虚拟设备");
        accessDeviceMapper.insert(device);
        return device;
    }
}
