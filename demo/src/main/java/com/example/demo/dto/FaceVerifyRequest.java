package com.example.demo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 人脸识别请求DTO
 */
@Data
public class FaceVerifyRequest {
    
    private Long deviceId;
    private MultipartFile faceImage;
}
