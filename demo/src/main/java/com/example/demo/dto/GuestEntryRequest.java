package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 访客入口请求DTO
 */
@Data
public class GuestEntryRequest {

    @NotBlank(message = "访客姓名不能为空")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String email;

    private String remark;
}
