package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 访问记录分页项
 */
@Data
public class AccessRecordPageItem {

    private Long id;

    private Long userId;

    private String realName;

    private Long deviceId;

    private String deviceName;

    private Boolean success;

    private BigDecimal similarityScore;

    private BigDecimal matchDistance;

    private BigDecimal threshold;

    private String snapshotPath;

    private String failReason;

    private LocalDateTime accessTime;
}
