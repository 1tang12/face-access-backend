package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.AttendanceRuleBatchRequest;
import com.example.demo.entity.AttendanceRule;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRuleService extends IService<AttendanceRule> {

    AttendanceRule getByDate(LocalDate attendanceDate);

    AttendanceRule getDefaultRule(LocalDate attendanceDate);

    AttendanceRule saveRule(AttendanceRule rule);

    List<AttendanceRule> saveBatchRules(AttendanceRuleBatchRequest request);

    List<AttendanceRule> listByDateRange(LocalDate startDate, LocalDate endDate);
}
