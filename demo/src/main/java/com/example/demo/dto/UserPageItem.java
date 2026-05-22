package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户分页项
 */
@Data
public class UserPageItem {

    private Long id;

    private String username;

    private String realName;

    private String email;

    private String phone;

    private String employeeNo;

    private Integer status;

    private String avatar;

    private String remark;

    private LocalDateTime createTime;

    private String roleCode;

    private String roleName;
}
