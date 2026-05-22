package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.VisitorAppointmentPageItem;
import com.example.demo.dto.VisitorAppointmentRequest;
import com.example.demo.entity.SysUser;
import com.example.demo.entity.VisitorAppointment;
import com.example.demo.mapper.VisitorAppointmentMapper;
import com.example.demo.service.VisitorAppointmentService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisitorAppointmentServiceImpl extends ServiceImpl<VisitorAppointmentMapper, VisitorAppointment> implements VisitorAppointmentService {

    private final VisitorAppointmentMapper visitorAppointmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VisitorAppointment createAppointment(SysUser currentUser, VisitorAppointmentRequest request) {
        if (request.getValidStartTime() == null || request.getValidEndTime() == null) {
            throw new RuntimeException("请选择预约有效时间");
        }
        if (!request.getValidEndTime().isAfter(request.getValidStartTime())) {
            throw new RuntimeException("结束时间必须晚于开始时间");
        }
        VisitorAppointment appointment = new VisitorAppointment();
        appointment.setVisitorUserId(currentUser.getId());
        appointment.setVisitorName(currentUser.getRealName());
        appointment.setVisitorPhone(currentUser.getPhone());
        appointment.setVisitPurpose(request.getVisitPurpose());
        appointment.setHostName(request.getHostName());
        appointment.setValidStartTime(request.getValidStartTime());
        appointment.setValidEndTime(request.getValidEndTime());
        appointment.setStatus("pending");
        appointment.setCreateTime(LocalDateTime.now());
        appointment.setUpdateTime(LocalDateTime.now());
        visitorAppointmentMapper.insert(appointment);
        return appointment;
    }

    @Override
    public Page<VisitorAppointmentPageItem> getAppointmentPage(Page<VisitorAppointment> page, String keyword, String status, Boolean reviewed) {
        LambdaQueryWrapper<VisitorAppointment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(VisitorAppointment::getStatus, status);
        } else if (reviewed != null) {
            if (reviewed) {
                wrapper.ne(VisitorAppointment::getStatus, "pending");
            } else {
                wrapper.eq(VisitorAppointment::getStatus, "pending");
            }
        }
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(VisitorAppointment::getVisitorName, keyword)
                    .or().like(VisitorAppointment::getVisitorPhone, keyword)
                    .or().like(VisitorAppointment::getVisitPurpose, keyword)
                    .or().like(VisitorAppointment::getHostName, keyword));
        }
        wrapper.orderByDesc(VisitorAppointment::getCreateTime);
        Page<VisitorAppointment> resultPage = visitorAppointmentMapper.selectPage(page, wrapper);
        return toPageItems(resultPage);
    }

    @Override
    public Page<VisitorAppointmentPageItem> getMyAppointmentPage(Long visitorUserId, Page<VisitorAppointment> page) {
        LambdaQueryWrapper<VisitorAppointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitorAppointment::getVisitorUserId, visitorUserId)
                .orderByDesc(VisitorAppointment::getCreateTime);
        Page<VisitorAppointment> resultPage = visitorAppointmentMapper.selectPage(page, wrapper);
        return toPageItems(resultPage);
    }

    @Override
    public String validateVisitorAccess(Long visitorUserId) {
        List<VisitorAppointment> appointments = visitorAppointmentMapper.selectList(
                new LambdaQueryWrapper<VisitorAppointment>()
                        .eq(VisitorAppointment::getVisitorUserId, visitorUserId)
                        .orderByDesc(VisitorAppointment::getCreateTime)
        );
        if (appointments.isEmpty()) {
            return "访客尚未提交预约，请先完成访客登记";
        }

        LocalDateTime now = LocalDateTime.now();
        for (VisitorAppointment appointment : appointments) {
            if ("approved".equalsIgnoreCase(appointment.getStatus())
                    && !now.isBefore(appointment.getValidStartTime())
                    && !now.isAfter(appointment.getValidEndTime())) {
                return null;
            }
        }

        VisitorAppointment latest = appointments.get(0);
        if ("pending".equalsIgnoreCase(latest.getStatus())) {
            return "访客预约待管理员审核，暂不可通行";
        }
        if ("rejected".equalsIgnoreCase(latest.getStatus())) {
            return "访客预约未审核通过，请联系管理员";
        }
        if ("approved".equalsIgnoreCase(latest.getStatus())) {
            return "当前不在访客有效期内，无法通行";
        }
        return "访客当前无有效通行资格";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveAppointment(Long id, SysUser reviewer, String reviewRemark) {
        VisitorAppointment appointment = visitorAppointmentMapper.selectById(id);
        if (appointment == null) {
            throw new RuntimeException("预约记录不存在");
        }
        appointment.setStatus("approved");
        appointment.setReviewRemark(reviewRemark);
        appointment.setReviewerId(reviewer.getId());
        appointment.setReviewerName(reviewer.getRealName());
        appointment.setReviewedAt(LocalDateTime.now());
        appointment.setUpdateTime(LocalDateTime.now());
        return visitorAppointmentMapper.updateById(appointment) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectAppointment(Long id, SysUser reviewer, String reviewRemark) {
        VisitorAppointment appointment = visitorAppointmentMapper.selectById(id);
        if (appointment == null) {
            throw new RuntimeException("预约记录不存在");
        }
        appointment.setStatus("rejected");
        appointment.setReviewRemark(reviewRemark);
        appointment.setReviewerId(reviewer.getId());
        appointment.setReviewerName(reviewer.getRealName());
        appointment.setReviewedAt(LocalDateTime.now());
        appointment.setUpdateTime(LocalDateTime.now());
        return visitorAppointmentMapper.updateById(appointment) > 0;
    }

    private Page<VisitorAppointmentPageItem> toPageItems(Page<VisitorAppointment> page) {
        List<VisitorAppointmentPageItem> records = page.getRecords().stream().map(item -> {
            VisitorAppointmentPageItem dto = new VisitorAppointmentPageItem();
            dto.setId(item.getId());
            dto.setVisitorUserId(item.getVisitorUserId());
            dto.setVisitorName(item.getVisitorName());
            dto.setVisitorPhone(item.getVisitorPhone());
            dto.setVisitPurpose(item.getVisitPurpose());
            dto.setHostName(item.getHostName());
            dto.setValidStartTime(item.getValidStartTime());
            dto.setValidEndTime(item.getValidEndTime());
            dto.setStatus(item.getStatus());
            dto.setReviewRemark(item.getReviewRemark());
            dto.setReviewerName(item.getReviewerName());
            dto.setReviewedAt(item.getReviewedAt());
            dto.setCreateTime(item.getCreateTime());
            return dto;
        }).collect(Collectors.toList());

        Page<VisitorAppointmentPageItem> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(records);
        return dtoPage;
    }
}
