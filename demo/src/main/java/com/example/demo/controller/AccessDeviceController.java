package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.entity.AccessDevice;
import com.example.demo.entity.OperationLog;
import com.example.demo.service.AccessDeviceService;
import com.example.demo.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/access/device")
@RequiredArgsConstructor
public class AccessDeviceController {

    private final AccessDeviceService accessDeviceService;
    private final OperationLogService operationLogService;

    @GetMapping("/page")
    public Result<PageResult<AccessDevice>> getDevicePage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Page<AccessDevice> page = new Page<>(current, size);
        Page<AccessDevice> resultPage = accessDeviceService.getDevicePage(page, keyword, status);
        return Result.success(new PageResult<>(
                resultPage.getTotal(),
                resultPage.getRecords(),
                resultPage.getCurrent(),
                resultPage.getSize()
        ));
    }

    @GetMapping("/list")
    public Result<List<AccessDevice>> getDeviceList() {
        return Result.success(accessDeviceService.list());
    }

    @PostMapping
    public Result<Void> createDevice(@RequestBody AccessDevice accessDevice) {
        try {
            accessDeviceService.createDevice(accessDevice);
            return Result.success("添加成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> updateDevice(@PathVariable Long id, @RequestBody AccessDevice accessDevice) {
        try {
            accessDevice.setId(id);
            accessDeviceService.updateDevice(accessDevice);
            return Result.success("更新成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteDevice(@PathVariable Long id) {
        try {
            accessDeviceService.deleteDevice(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/open")
    public Result<Map<String, Object>> openDoor(@PathVariable Long id) {
        AccessDevice accessDevice = accessDeviceService.getById(id);
        if (accessDevice == null) {
            return Result.error("设备不存在");
        }
        if (!Integer.valueOf(1).equals(accessDevice.getStatus())) {
            return Result.error("设备当前离线，无法开门");
        }
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationType("OPEN_DOOR");
        operationLog.setModuleName("ACCESS_DEVICE");
        operationLog.setOperationContent("虚拟开门：" + accessDevice.getDeviceName());
        operationLog.setRequestMethod("POST");
        operationLog.setRequestUrl("/access/device/" + id + "/open");
        operationLog.setStatus(1);
        operationLog.setOperateTime(LocalDateTime.now());
        operationLogService.createLog(operationLog);
        return Result.success(Map.of(
                "deviceId", accessDevice.getId(),
                "deviceName", accessDevice.getDeviceName(),
                "openedAt", LocalDateTime.now().toString(),
                "message", "虚拟开门成功"
        ));
    }
}
