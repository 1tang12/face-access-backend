package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.dto.FacePageItem;
import com.example.demo.dto.FaceQualityCheckResponse;
import com.example.demo.dto.FaceRegisterRequest;
import com.example.demo.dto.FaceVerifyResponse;
import com.example.demo.dto.ThresholdAnalysisResponse;
import com.example.demo.entity.FaceFeature;
import org.springframework.web.multipart.MultipartFile;

/**
 * 人脸特征服务接口
 */
public interface FaceFeatureService extends IService<FaceFeature> {

    /**
     * 注册人脸
     */
    boolean registerFace(Long userId, MultipartFile faceImage, String remark);

    /**
     * 人脸识别验证
     */
    FaceVerifyResponse verifyFace(Long deviceId, MultipartFile faceImage);

    /**
     * 注册前做人脸质量检查
     */
    FaceQualityCheckResponse checkFaceQuality(MultipartFile faceImage);

    /**
     * 分页查询人脸信息
     */
    Page<FacePageItem> getFacePage(Page<FaceFeature> page, String keyword, String roleCode);

    /**
     * 根据用户ID查询人脸特征
     */
    FaceFeature getByUserId(Long userId);

    /**
     * 阈值分析
     */
    ThresholdAnalysisResponse analyzeThreshold();

    /**
     * 删除用户人脸
     */
    boolean deleteFaceByUserId(Long userId);
}
