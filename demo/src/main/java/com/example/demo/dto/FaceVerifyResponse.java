package com.example.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 人脸识别响应DTO
 */
@Data
public class FaceVerifyResponse {
    
    private Boolean success;
    private Long userId;
    private String realName;
    private BigDecimal similarityScore;
    private BigDecimal matchDistance;
    private BigDecimal threshold;
    private String message;
    private String snapshotPath;
}
