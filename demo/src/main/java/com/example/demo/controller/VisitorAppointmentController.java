package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.dto.VisitorAppointmentPageItem;
import com.example.demo.dto.VisitorAppointmentRequest;
import com.example.demo.entity.SysUser;
import com.example.demo.entity.VisitorAppointment;
import com.example.demo.service.AuthService;
import com.example.demo.service.VisitorAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/visitor/appointment")
@RequiredArgsConstructor
public class VisitorAppointmentController {

    private final VisitorAppointmentService visitorAppointmentService;
    private final AuthService authService;

    @PostMapping
    public Result<VisitorAppointment> createAppointment(@RequestBody VisitorAppointmentRequest request) {
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }
            return Result.success("提交成功", visitorAppointmentService.createAppointment(currentUser, request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/page")
    public Result<PageResult<VisitorAppointmentPageItem>> getAppointmentPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean reviewed) {
        Page<VisitorAppointmentPageItem> resultPage = visitorAppointmentService.getAppointmentPage(
                new Page<>(current, size),
                keyword,
                status,
                reviewed
        );
        return Result.success(new PageResult<>(
                resultPage.getTotal(),
                resultPage.getRecords(),
                resultPage.getCurrent(),
                resultPage.getSize()
        ));
    }

    @GetMapping("/my-page")
    public Result<PageResult<VisitorAppointmentPageItem>> getMyAppointmentPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        SysUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }
        Page<VisitorAppointmentPageItem> resultPage = visitorAppointmentService.getMyAppointmentPage(
                currentUser.getId(),
                new Page<>(current, size)
        );
        return Result.success(new PageResult<>(
                resultPage.getTotal(),
                resultPage.getRecords(),
                resultPage.getCurrent(),
                resultPage.getSize()
        ));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approveAppointment(@PathVariable Long id, @RequestBody VisitorAppointmentRequest request) {
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }
            visitorAppointmentService.approveAppointment(id, currentUser, request.getReviewRemark());
            return Result.success("审核通过", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public Result<Void> rejectAppointment(@PathVariable Long id, @RequestBody VisitorAppointmentRequest request) {
        try {
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }
            visitorAppointmentService.rejectAppointment(id, currentUser, request.getReviewRemark());
            return Result.success("已拒绝", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
