package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.VisitorAppointmentPageItem;
import com.example.demo.dto.VisitorAppointmentRequest;
import com.example.demo.entity.SysUser;
import com.example.demo.entity.VisitorAppointment;

public interface VisitorAppointmentService extends IService<VisitorAppointment> {

    VisitorAppointment createAppointment(SysUser currentUser, VisitorAppointmentRequest request);

    Page<VisitorAppointmentPageItem> getAppointmentPage(Page<VisitorAppointment> page, String keyword, String status, Boolean reviewed);

    Page<VisitorAppointmentPageItem> getMyAppointmentPage(Long visitorUserId, Page<VisitorAppointment> page);

    /**
     * 校验访客当前是否具备通行资格，返回null表示允许通行
     */
    String validateVisitorAccess(Long visitorUserId);

    boolean approveAppointment(Long id, SysUser reviewer, String reviewRemark);

    boolean rejectAppointment(Long id, SysUser reviewer, String reviewRemark);
}
