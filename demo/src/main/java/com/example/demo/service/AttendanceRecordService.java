package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.AttendanceRecordPageItem;
import com.example.demo.entity.AttendanceRecord;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public interface AttendanceRecordService extends IService<AttendanceRecord> {

    AttendanceRecord recordAttendance(Long userId, LocalDateTime accessTime);

    AttendanceRecord getByUserIdAndDate(Long userId, LocalDate attendanceDate);

    Page<AttendanceRecord> getMyAttendancePage(Long userId, Page<AttendanceRecord> page, LocalDate startDate, LocalDate endDate);

    Page<AttendanceRecordPageItem> getAttendancePage(Page<AttendanceRecord> page, String keyword, LocalDate startDate, LocalDate endDate);

    Map<String, Object> getAttendanceSummary(LocalDate startDate, LocalDate endDate);
}
