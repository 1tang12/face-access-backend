package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitorAppointmentRequest {

    private String visitPurpose;

    private String hostName;

    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;

    private String reviewRemark;
}
