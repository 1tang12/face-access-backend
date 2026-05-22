package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.dto.AccessRecordPageItem;
import com.example.demo.entity.AccessRecord;
import com.example.demo.entity.SysUser;
import com.example.demo.service.AuthService;
import com.example.demo.service.AccessRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

/**
 * 访问记录控制器
 */
@RestController
@RequestMapping("/access")
@RequiredArgsConstructor
public class AccessRecordController {

    private final AccessRecordService accessRecordService;
    private final AuthService authService;

    /**
     * 分页查询访问记录
     */
    @GetMapping("/record/page")
    public Result<PageResult<AccessRecordPageItem>> getRecordPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        Page<AccessRecord> page = new Page<>(current, size);
        Page<AccessRecordPageItem> resultPage = accessRecordService.getRecordPage(page, keyword, result, deviceId, startDate, endDate);

        PageResult<AccessRecordPageItem> pageResult = new PageResult<>(
            resultPage.getTotal(),
            resultPage.getRecords(),
            resultPage.getCurrent(),
            resultPage.getSize()
        );
        
        return Result.success(pageResult);
    }

    /**
     * 统计今日访问数据
     */
    @GetMapping("/stats/today")
    public Result<Map<String, Object>> getTodayStats() {
        Map<String, Object> stats = accessRecordService.getTodayStats();
        return Result.success(stats);
    }

    /**
     * 统计最近7天访问趋势
     */
    @GetMapping("/stats/week-trend")
    public Result<Map<String, Object>> getWeekTrend() {
        Map<String, Object> trend = accessRecordService.getWeekTrend();
        return Result.success(trend);
    }

    /**
     * 根据ID查询访问记录
     */
    @GetMapping("/record/{id}")
    public Result<AccessRecord> getRecordById(@PathVariable Long id) {
        AccessRecord record = accessRecordService.getById(id);
        if (record != null) {
            return Result.success(record);
        }
        return Result.error("记录不存在");
    }

    @GetMapping("/record/my-page")
    public Result<PageResult<AccessRecordPageItem>> getMyRecordPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }
        Page<AccessRecordPageItem> resultPage = accessRecordService.getMyRecordPage(
                currentUser.getId(),
                new Page<>(current, size),
                result,
                deviceId,
                startDate,
                endDate
        );
        return Result.success(new PageResult<>(
                resultPage.getTotal(),
                resultPage.getRecords(),
                resultPage.getCurrent(),
                resultPage.getSize()
        ));
    }
}
