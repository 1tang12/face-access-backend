package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitorAppointmentPageItem {

    private Long id;

    private Long visitorUserId;

    private String visitorName;

    private String visitorPhone;

    private String visitPurpose;

    private String hostName;

    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;

    private String status;

    private String reviewRemark;

    private String reviewerName;

    private LocalDateTime reviewedAt;

    private LocalDateTime createTime;
}
