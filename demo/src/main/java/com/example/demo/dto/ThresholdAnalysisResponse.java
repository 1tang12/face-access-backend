package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 阈值分析结果
 */
@Data
public class ThresholdAnalysisResponse {

    private BigDecimal currentThreshold;

    private BigDecimal suggestedThreshold;

    private Integer userCount;

    private Integer templateCount;

    private Integer positiveSampleCount;

    private Integer negativeSampleCount;

    private String message;
}
