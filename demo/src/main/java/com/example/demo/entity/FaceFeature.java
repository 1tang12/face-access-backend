package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 人脸特征实体类
 */
@Data
@TableName("face_feature")
public class FaceFeature implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String faceImagePath;

    private String featureVector;

    private String featureVersion;

    private BigDecimal qualityScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime registerTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Integer status;

    private String remark;
}
