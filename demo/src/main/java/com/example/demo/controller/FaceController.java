package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.common.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.FacePageItem;
import com.example.demo.dto.FaceQualityCheckResponse;
import com.example.demo.dto.FaceVerifyResponse;
import com.example.demo.dto.MobileFaceVerifyRequest;
import com.example.demo.dto.ThresholdAnalysisResponse;
import com.example.demo.entity.FaceFeature;
import com.example.demo.entity.SysUser;
import com.example.demo.service.AccessDeviceService;
import com.example.demo.service.AuthService;
import com.example.demo.service.FaceFeatureService;
import com.example.demo.util.InMemoryMultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

/**
 * 人脸管理控制器
 */
@RestController
@RequestMapping("/face")
@RequiredArgsConstructor
public class FaceController {

    private final FaceFeatureService faceFeatureService;
    private final AuthService authService;
    private final AccessDeviceService accessDeviceService;

    /**
     * 注册人脸
     */
    @PostMapping("/register")
    public Result<Void> registerFace(
            @RequestParam Long userId,
            @RequestParam("faceImage") MultipartFile faceImage,
            @RequestParam(required = false) String remark) {
        try {
            if (faceImage.isEmpty()) {
                return Result.error("请上传人脸图片");
            }
            faceFeatureService.registerFace(userId, faceImage, remark);
            return Result.success("人脸注册成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register/self")
    public Result<Void> registerSelfFace(
            @RequestParam("faceImage") MultipartFile faceImage,
            @RequestParam(required = false) String remark) {
        try {
            if (faceImage.isEmpty()) {
                return Result.error("请上传人脸图片");
            }
            SysUser currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return Result.error("未登录");
            }
            faceFeatureService.registerFace(currentUser.getId(), faceImage, remark);
            return Result.success("人脸注册成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/quality-check")
    public Result<FaceQualityCheckResponse> checkFaceQuality(
            @RequestParam("faceImage") MultipartFile faceImage) {
        try {
            if (faceImage.isEmpty()) {
                return Result.error("请上传人脸图片");
            }
            return Result.success(faceFeatureService.checkFaceQuality(faceImage));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 人脸识别验证
     */
    @PostMapping("/verify")
    public Result<FaceVerifyResponse> verifyFace(
            @RequestParam Long deviceId,
            @RequestParam("faceImage") MultipartFile faceImage) {
        try {
            if (faceImage.isEmpty()) {
                return Result.error("请上传人脸图片");
            }
            FaceVerifyResponse response = faceFeatureService.verifyFace(deviceId, faceImage);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/mobile/verify")
    public Result<FaceVerifyResponse> verifyFaceForMobile(
            @RequestParam("faceImage") MultipartFile faceImage) {
        try {
            if (faceImage.isEmpty()) {
                return Result.error("请上传人脸图片");
            }
            Long mobileDeviceId = accessDeviceService.getOrCreateMobileWebDevice().getId();
            FaceVerifyResponse response = faceFeatureService.verifyFace(mobileDeviceId, faceImage);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/mobile/verify-base64")
    public Result<FaceVerifyResponse> verifyFaceForMobileBase64(
            @RequestBody MobileFaceVerifyRequest request) {
        try {
            if (request == null || request.getImageData() == null || request.getImageData().isBlank()) {
                return Result.error("请上传人脸图片");
            }

            String rawImageData = request.getImageData().trim();
            String contentType = "image/jpeg";
            if (rawImageData.startsWith("data:")) {
                int commaIndex = rawImageData.indexOf(',');
                if (commaIndex < 0) {
                    return Result.error("图片数据格式不正确");
                }
                String meta = rawImageData.substring(5, commaIndex);
                int semicolonIndex = meta.indexOf(';');
                if (semicolonIndex > 0) {
                    contentType = meta.substring(0, semicolonIndex);
                }
                rawImageData = rawImageData.substring(commaIndex + 1);
            }

            byte[] imageBytes = Base64.getDecoder().decode(rawImageData);
            MultipartFile faceImage = new InMemoryMultipartFile(
                    "faceImage",
                    "mobile-verify.jpg",
                    contentType,
                    imageBytes
            );
            if (faceImage.isEmpty()) {
                return Result.error("请上传人脸图片");
            }

            Long mobileDeviceId = accessDeviceService.getOrCreateMobileWebDevice().getId();
            FaceVerifyResponse response = faceFeatureService.verifyFace(mobileDeviceId, faceImage);
            return Result.success(response);
        } catch (IllegalArgumentException e) {
            return Result.error("图片数据解码失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/page")
    public Result<PageResult<FacePageItem>> getFacePage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode) {
        Page<FacePageItem> resultPage = faceFeatureService.getFacePage(new Page<>(current, size), keyword, roleCode);
        return Result.success(new PageResult<>(
                resultPage.getTotal(),
                resultPage.getRecords(),
                resultPage.getCurrent(),
                resultPage.getSize()
        ));
    }

    @GetMapping("/threshold-analysis")
    public Result<ThresholdAnalysisResponse> analyzeThreshold() {
        return Result.success(faceFeatureService.analyzeThreshold());
    }

    /**
     * 根据用户ID查询人脸信息
     */
    @GetMapping("/user/{userId}")
    public Result<FaceFeature> getFaceByUserId(@PathVariable Long userId) {
        FaceFeature faceFeature = faceFeatureService.getByUserId(userId);
        if (faceFeature != null) {
            return Result.success(faceFeature);
        }
        return Result.error("该用户未注册人脸");
    }

    /**
     * 删除用户人脸
     */
    @DeleteMapping("/user/{userId}")
    public Result<Void> deleteFaceByUserId(@PathVariable Long userId) {
        try {
            faceFeatureService.deleteFaceByUserId(userId);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
