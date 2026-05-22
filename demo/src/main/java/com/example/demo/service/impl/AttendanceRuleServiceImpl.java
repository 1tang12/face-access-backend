package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.AttendanceRuleBatchRequest;
import com.example.demo.entity.AttendanceRule;
import com.example.demo.mapper.AttendanceRuleMapper;
import com.example.demo.service.AttendanceRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceRuleServiceImpl extends ServiceImpl<AttendanceRuleMapper, AttendanceRule> implements AttendanceRuleService {

    private final AttendanceRuleMapper attendanceRuleMapper;

    @Override
    public AttendanceRule getByDate(LocalDate attendanceDate) {
        return attendanceRuleMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRule>()
                        .eq(AttendanceRule::getAttendanceDate, attendanceDate)
        );
    }

    @Override
    public AttendanceRule getDefaultRule(LocalDate attendanceDate) {
        AttendanceRule rule = new AttendanceRule();
        rule.setAttendanceDate(attendanceDate);
        rule.setNeedAttendance(1);
        rule.setCheckInStartTime(LocalTime.of(7, 0));
        rule.setCheckInEndTime(LocalTime.of(10, 0));
        rule.setWorkStartTime(LocalTime.of(9, 0));
        rule.setWorkEndTime(LocalTime.of(18, 0));
        rule.setCheckOutStartTime(LocalTime.of(17, 0));
        rule.setCheckOutEndTime(LocalTime.of(20, 0));
        rule.setStatus(1);
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceRule saveRule(AttendanceRule rule) {
        validateRule(rule);
        AttendanceRule existing = getByDate(rule.getAttendanceDate());
        fillDefaults(rule);
        if (existing == null) {
            attendanceRuleMapper.insert(rule);
            return rule;
        }
        rule.setId(existing.getId());
        attendanceRuleMapper.updateById(rule);
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AttendanceRule> saveBatchRules(AttendanceRuleBatchRequest request) {
        if (request == null || request.getAttendanceDates() == null || request.getAttendanceDates().isEmpty()) {
            throw new IllegalArgumentException("请选择需要配置的考勤日期");
        }
        List<AttendanceRule> rules = new ArrayList<>();
        for (LocalDate date : request.getAttendanceDates()) {
            AttendanceRule rule = new AttendanceRule();
            rule.setAttendanceDate(date);
            rule.setNeedAttendance(request.getNeedAttendance());
            rule.setCheckInStartTime(request.getCheckInStartTime());
            rule.setCheckInEndTime(request.getCheckInEndTime());
            rule.setWorkStartTime(request.getWorkStartTime());
            rule.setWorkEndTime(request.getWorkEndTime());
            rule.setCheckOutStartTime(request.getCheckOutStartTime());
            rule.setCheckOutEndTime(request.getCheckOutEndTime());
            rule.setStatus(request.getStatus());
            rule.setRemark(request.getRemark());
            rules.add(saveRule(rule));
        }
        return rules;
    }

    @Override
    public List<AttendanceRule> listByDateRange(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AttendanceRule> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null) {
            wrapper.ge(AttendanceRule::getAttendanceDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(AttendanceRule::getAttendanceDate, endDate);
        }
        wrapper.orderByDesc(AttendanceRule::getAttendanceDate);
        return attendanceRuleMapper.selectList(wrapper);
    }

    private void fillDefaults(AttendanceRule rule) {
        AttendanceRule defaults = getDefaultRule(rule.getAttendanceDate());
        if (rule.getNeedAttendance() == null) {
            rule.setNeedAttendance(defaults.getNeedAttendance());
        }
        if (rule.getCheckInStartTime() == null) {
            rule.setCheckInStartTime(defaults.getCheckInStartTime());
        }
        if (rule.getCheckInEndTime() == null) {
            rule.setCheckInEndTime(defaults.getCheckInEndTime());
        }
        if (rule.getWorkStartTime() == null) {
            rule.setWorkStartTime(defaults.getWorkStartTime());
        }
        if (rule.getWorkEndTime() == null) {
            rule.setWorkEndTime(defaults.getWorkEndTime());
        }
        if (rule.getCheckOutStartTime() == null) {
            rule.setCheckOutStartTime(defaults.getCheckOutStartTime());
        }
        if (rule.getCheckOutEndTime() == null) {
            rule.setCheckOutEndTime(defaults.getCheckOutEndTime());
        }
        if (rule.getStatus() == null) {
            rule.setStatus(defaults.getStatus());
        }
    }

    private void validateRule(AttendanceRule rule) {
        if (rule == null || rule.getAttendanceDate() == null) {
            throw new IllegalArgumentException("请选择考勤日期");
        }
        fillDefaults(rule);
        if (rule.getCheckInStartTime().isAfter(rule.getCheckInEndTime())) {
            throw new IllegalArgumentException("签到开始时间不能晚于签到结束时间");
        }
        if (rule.getCheckOutStartTime().isAfter(rule.getCheckOutEndTime())) {
            throw new IllegalArgumentException("签退开始时间不能晚于签退结束时间");
        }
        if (rule.getCheckInEndTime().isAfter(rule.getCheckOutStartTime())) {
            throw new IllegalArgumentException("签到结束时间不能晚于签退开始时间");
        }
        if (rule.getWorkStartTime().isAfter(rule.getWorkEndTime())) {
            throw new IllegalArgumentException("上班时间不能晚于下班时间");
        }
    }
}
