package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.AccessDevice;

public interface AccessDeviceService extends IService<AccessDevice> {

    String MOBILE_WEB_DEVICE_CODE = "MOBILE_WEB_DEVICE";

    Page<AccessDevice> getDevicePage(Page<AccessDevice> page, String keyword, Integer status);

    boolean createDevice(AccessDevice accessDevice);

    boolean updateDevice(AccessDevice accessDevice);

    boolean deleteDevice(Long id);

    AccessDevice getOrCreateMobileWebDevice();
}
