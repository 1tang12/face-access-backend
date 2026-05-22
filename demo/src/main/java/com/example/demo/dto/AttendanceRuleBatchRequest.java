package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class AttendanceRuleBatchRequest {

    private List<LocalDate> attendanceDates;

    private Integer needAttendance;

    private LocalTime checkInStartTime;

    private LocalTime checkInEndTime;

    private LocalTime workStartTime;

    private LocalTime workEndTime;

    private LocalTime checkOutStartTime;

    private LocalTime checkOutEndTime;

    private Integer status;

    private String remark;
}
