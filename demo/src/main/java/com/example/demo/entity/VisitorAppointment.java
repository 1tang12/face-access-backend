package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 访客预约实体
 */
@Data
@TableName("visitor_appointment")
public class VisitorAppointment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
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

    private Long reviewerId;

    private String reviewerName;

    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
