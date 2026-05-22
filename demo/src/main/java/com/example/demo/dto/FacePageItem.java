package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 人脸列表分页项
 */
@Data
public class FacePageItem {

    private Long id;

    private Long userId;

    private String username;

    private String realName;

    private String roleCode;

    private String employeeNo;

    private String faceImagePath;

    private String featureVersion;

    private BigDecimal qualityScore;

    private Integer featureDimension;

    private Boolean featureReady;

    private Integer templateCount;

    private LocalDateTime registerTime;

    private Integer status;

    private String remark;
}
