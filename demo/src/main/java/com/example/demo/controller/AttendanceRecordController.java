package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.dto.AttendanceRuleBatchRequest;
import com.example.demo.dto.AttendanceRecordPageItem;
import com.example.demo.entity.AttendanceRecord;
import com.example.demo.entity.AttendanceRule;
import com.example.demo.entity.SysUser;
import com.example.demo.service.AttendanceRecordService;
import com.example.demo.service.AttendanceRuleService;
import com.example.demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceRecordController {

    private final AttendanceRecordService attendanceRecordService;
    private final AttendanceRuleService attendanceRuleService;
    private final AuthService authService;

    @GetMapping("/my/today")
    public Result<AttendanceRecord> getMyTodayAttendance() {
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }
        AttendanceRecord record = attendanceRecordService.getByUserIdAndDate(currentUser.getId(), LocalDate.now());
        return Result.success(record);
    }

    @GetMapping("/my/page")
    public Result<PageResult<AttendanceRecord>> getMyAttendancePage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }
        Page<AttendanceRecord> resultPage = attendanceRecordService.getMyAttendancePage(currentUser.getId(), new Page<>(current, size), startDate, endDate);
        return Result.success(new PageResult<>(
                resultPage.getTotal(),
                resultPage.getRecords(),
                resultPage.getCurrent(),
                resultPage.getSize()
        ));
    }

    @GetMapping("/page")
    public Result<PageResult<AttendanceRecordPageItem>> getAttendancePage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Page<AttendanceRecordPageItem> resultPage = attendanceRecordService.getAttendancePage(new Page<>(current, size), keyword, startDate, endDate);
        return Result.success(new PageResult<>(
                resultPage.getTotal(),
                resultPage.getRecords(),
                resultPage.getCurrent(),
                resultPage.getSize()
        ));
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> getAttendanceSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(attendanceRecordService.getAttendanceSummary(startDate, endDate));
    }

    @GetMapping("/rule")
    public Result<AttendanceRule> getAttendanceRule(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate attendanceDate) {
        AttendanceRule rule = attendanceRuleService.getByDate(attendanceDate);
        return Result.success(rule != null ? rule : attendanceRuleService.getDefaultRule(attendanceDate));
    }

    @GetMapping("/rule/list")
    public Result<List<AttendanceRule>> getAttendanceRules(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(attendanceRuleService.listByDateRange(startDate, endDate));
    }

    @PostMapping("/rule")
    public Result<AttendanceRule> saveAttendanceRule(@RequestBody AttendanceRule rule) {
        try {
            return Result.success("保存成功", attendanceRuleService.saveRule(rule));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/rule/batch")
    public Result<List<AttendanceRule>> saveAttendanceRuleBatch(@RequestBody AttendanceRuleBatchRequest request) {
        try {
            return Result.success("批量保存成功", attendanceRuleService.saveBatchRules(request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
