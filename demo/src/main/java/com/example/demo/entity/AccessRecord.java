package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 访问记录实体类
 */
@Data
@TableName("access_record")
public class AccessRecord implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long deviceId;

    private LocalDateTime accessTime;

    private String result;

    private BigDecimal similarityScore;

    private BigDecimal matchDistance;

    private BigDecimal threshold;

    private String snapshotPath;

    private String failReason;

    private BigDecimal temperature;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
