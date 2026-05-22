package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@TableName("operation_log")
public class OperationLog implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String operationType;

    private String moduleName;

    private String operationContent;

    private String requestMethod;

    private String requestUrl;

    private String requestParams;

    private String ipAddress;

    private String location;

    private String browser;

    private String os;

    private Integer status;

    private String errorMsg;

    private LocalDateTime operateTime;
}
