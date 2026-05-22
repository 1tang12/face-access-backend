package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FaceQualityCheckResponse {

    private Boolean passed;

    private BigDecimal qualityScore;

    private Integer faceCount;

    private BigDecimal blurScore;

    private BigDecimal faceRatio;

    private BigDecimal brightness;

    private BigDecimal eyeAngle;

    private String message;
}
