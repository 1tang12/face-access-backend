package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录分页项
 */
@Data
public class AttendanceRecordPageItem {

    private Long id;

    private Long userId;

    private String realName;

    private LocalDate attendanceDate;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private Integer workDuration;

    private String status;

    private Integer lateMinutes;

    private Integer earlyMinutes;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
