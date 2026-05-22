package com.example.demo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 人脸注册请求DTO
 */
@Data
public class FaceRegisterRequest {
    
    private Long userId;
    private MultipartFile faceImage;
    private String remark;
}
