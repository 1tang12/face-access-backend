package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.dto.FacePageItem;
import com.example.demo.dto.FaceQualityCheckResponse;
import com.example.demo.dto.FaceVerifyResponse;
import com.example.demo.dto.ThresholdAnalysisResponse;
import com.example.demo.entity.AccessRecord;
import com.example.demo.entity.FaceFeature;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.FaceFeatureMapper;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.AccessRecordService;
import com.example.demo.service.AttendanceRecordService;
import com.example.demo.service.FaceFeatureService;
import com.example.demo.service.FaceRecognitionPythonClient;
import com.example.demo.service.VisitorAppointmentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 人脸特征服务实现类
 */
@Service
@RequiredArgsConstructor
public class FaceFeatureServiceImpl extends ServiceImpl<FaceFeatureMapper, FaceFeature> implements FaceFeatureService {

    private final FaceFeatureMapper faceFeatureMapper;
    private final SysUserMapper userMapper;
    private final AccessRecordService accessRecordService;
    private final AttendanceRecordService attendanceRecordService;
    private final FaceRecognitionPythonClient faceRecognitionPythonClient;
    private final VisitorAppointmentService visitorAppointmentService;
    private final ObjectMapper objectMapper;

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.face-image-path}")
    private String faceImagePath;

    @Value("${file.snapshot-path}")
    private String snapshotPath;

    @Value("${face.recognition.threshold}")
    private BigDecimal threshold;

    @Value("${face.recognition.max-templates-per-user:5}")
    private int maxTemplatesPerUser;

    @Value("${face.recognition.feature-version:v1.6-tta-flip-auto}")
    private String currentFeatureVersion;

    @Override
    public FaceQualityCheckResponse checkFaceQuality(MultipartFile faceImage) {
        String fileName = "quality_" + System.currentTimeMillis() + ".jpg";
        File dir = ensureDirectory(snapshotPath);
        File targetFile = new File(dir, fileName);
        try {
            faceImage.transferTo(targetFile);
            FaceRecognitionPythonClient.QualityResult result = faceRecognitionPythonClient.checkImageQuality(targetFile.toPath());
            FaceQualityCheckResponse response = new FaceQualityCheckResponse();
            response.setPassed(result.passed());
            response.setQualityScore(BigDecimal.valueOf(result.qualityScore()).setScale(2, RoundingMode.HALF_UP));
            response.setFaceCount(result.faceCount());
            response.setBlurScore(result.blurScore() == null ? null : BigDecimal.valueOf(result.blurScore()).setScale(2, RoundingMode.HALF_UP));
            response.setFaceRatio(result.faceRatio() == null ? null : BigDecimal.valueOf(result.faceRatio()).setScale(4, RoundingMode.HALF_UP));
            response.setBrightness(result.brightness() == null ? null : BigDecimal.valueOf(result.brightness()).setScale(2, RoundingMode.HALF_UP));
            response.setEyeAngle(result.eyeAngle() == null ? null : BigDecimal.valueOf(result.eyeAngle()).setScale(2, RoundingMode.HALF_UP));
            response.setMessage(result.message());
            return response;
        } catch (Exception e) {
            throw new RuntimeException("质量检测失败：" + e.getMessage(), e);
        } finally {
            try {
                Files.deleteIfExists(targetFile.toPath());
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerFace(Long userId, MultipartFile faceImage, String remark) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String fileName = "face_" + userId + "_" + System.currentTimeMillis() + ".jpg";
        File dir = ensureDirectory(faceImagePath);

        File targetFile = new File(dir, fileName);
        String relativeUrl = "/uploads/face/" + fileName;

        try {
            faceImage.transferTo(targetFile);
        } catch (Exception e) {
            throw new RuntimeException("图片保存失败：" + e.getMessage());
        }

        FaceRecognitionPythonClient.ExtractResult extractResult = faceRecognitionPythonClient.extractFeatureAnalysis(targetFile.toPath());
        if (!extractResult.qualityPassed()) {
            try {
                Files.deleteIfExists(targetFile.toPath());
            } catch (Exception ignored) {
            }
            throw new RuntimeException(extractResult.qualityMessage());
        }

        FaceFeature faceFeature = new FaceFeature();
        faceFeature.setUserId(userId);
        faceFeature.setFaceImagePath(relativeUrl);
        faceFeature.setFeatureVector(extractResult.featureVector());
        faceFeature.setFeatureVersion(currentFeatureVersion);
        faceFeature.setQualityScore(BigDecimal.valueOf(extractResult.qualityScore()).setScale(2, RoundingMode.HALF_UP));
        faceFeature.setStatus(1);
        faceFeature.setRemark(remark);
        boolean inserted = faceFeatureMapper.insert(faceFeature) > 0;
        if (inserted) {
            trimUserTemplates(userId);
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FaceVerifyResponse verifyFace(Long deviceId, MultipartFile faceImage) {
        FaceVerifyResponse response = new FaceVerifyResponse();

        String fileName = "snapshot_" + System.currentTimeMillis() + ".jpg";
        File dir = ensureDirectory(snapshotPath);

        File targetFile = new File(dir, fileName);
        String relativeUrl = "/uploads/snapshot/" + fileName;

        try {
            faceImage.transferTo(targetFile);
        } catch (Exception e) {
            throw new RuntimeException("图片保存失败：" + e.getMessage());
        }

        response.setSnapshotPath(relativeUrl);

        List<FaceFeature> allFeatures = faceFeatureMapper.selectList(
                new LambdaQueryWrapper<FaceFeature>()
                        .eq(FaceFeature::getStatus, 1)
                        .orderByDesc(FaceFeature::getRegisterTime)
        );

        if (allFeatures.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("未找到匹配的人脸");
            createAccessRecord(null, deviceId, relativeUrl, "fail", null, null, "未找到匹配的人脸");
            return response;
        }

        double[] queryFeature = parseFeatureVector(faceRecognitionPythonClient.extractFeatureVector(targetFile.toPath()));
        MatchCandidate bestCandidate = findBestCandidate(queryFeature, allFeatures);
        if (bestCandidate == null) {
            response.setSuccess(false);
            response.setMessage("人脸库为空或特征不可用");
            createAccessRecord(null, deviceId, relativeUrl, "fail", null, null, "人脸库为空或特征不可用");
            return response;
        }

        BigDecimal similarityScore = toCosineSimilarity(bestCandidate.distance());
        response.setSimilarityScore(similarityScore);
        response.setMatchDistance(toDistanceValue(bestCandidate.distance()));
        response.setThreshold(threshold);

        if (isRecognized(bestCandidate.distance()) && bestCandidate.userId() != null) {
            SysUser user = userMapper.selectById(bestCandidate.userId());
            if (user == null) {
                response.setSuccess(false);
                response.setMessage("匹配到人脸，但用户信息不存在");
                createAccessRecord(null, deviceId, relativeUrl, "fail", similarityScore, response.getMatchDistance(), "匹配用户不存在");
                return response;
            }

            List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
            if (roles.contains("ROLE_GUEST")) {
                String visitorAccessMessage = visitorAppointmentService.validateVisitorAccess(user.getId());
                if (visitorAccessMessage != null) {
                    response.setSuccess(false);
                    response.setMessage(visitorAccessMessage);
                    createAccessRecord(null, deviceId, relativeUrl, "fail", similarityScore, response.getMatchDistance(), visitorAccessMessage);
                    return response;
                }
            }

            response.setSuccess(true);
            response.setUserId(user.getId());
            response.setRealName(user.getRealName());
            response.setMessage("识别成功，欢迎 " + user.getRealName());
            createAccessRecord(user.getId(), deviceId, relativeUrl, "success", similarityScore, response.getMatchDistance(), null);
            if (roles.contains("ROLE_USER") || roles.contains("ROLE_ADMIN")) {
                attendanceRecordService.recordAttendance(user.getId(), LocalDateTime.now());
            }
            return response;
        }

        response.setSuccess(false);
        response.setMessage("相似度过低，识别失败");
        createAccessRecord(null, deviceId, relativeUrl, "fail", similarityScore, response.getMatchDistance(), "相似度过低");
        return response;
    }

    @Override
    public Page<FacePageItem> getFacePage(Page<FaceFeature> page, String keyword, String roleCode) {
        List<SysUser> allMatchedUsers = userMapper.selectList(buildUserKeywordWrapper(keyword));
        if (allMatchedUsers.isEmpty()) {
            Page<FacePageItem> emptyPage = new Page<>(page.getCurrent(), page.getSize(), 0);
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        Set<Long> userIds = allMatchedUsers.stream().map(SysUser::getId).collect(Collectors.toSet());
        List<FaceFeature> faceFeatures = faceFeatureMapper.selectList(
                new LambdaQueryWrapper<FaceFeature>()
                        .in(FaceFeature::getUserId, userIds)
                        .orderByDesc(FaceFeature::getRegisterTime)
        );

        Map<Long, List<FaceFeature>> faceFeatureMap = faceFeatures.stream()
                .collect(Collectors.groupingBy(FaceFeature::getUserId, LinkedHashMap::new, Collectors.toList()));

        List<FacePageItem> allItems = new ArrayList<>();
        for (SysUser user : allMatchedUsers) {
            String currentRoleCode = userMapper.selectPrimaryRoleCodeByUserId(user.getId());
            if (roleCode != null && !roleCode.isBlank()) {
                boolean isGuestRole = "ROLE_GUEST".equals(currentRoleCode);
                if ("ROLE_GUEST".equals(roleCode) && !isGuestRole) {
                    continue;
                }
                if ("ROLE_USER".equals(roleCode) && isGuestRole) {
                    continue;
                }
            }
            List<FaceFeature> userTemplates = faceFeatureMap.get(user.getId());
            if (userTemplates == null || userTemplates.isEmpty()) {
                continue;
            }
            FaceFeature faceFeature = userTemplates.stream()
                    .max(
                            Comparator.comparing(
                                            FaceFeature::getQualityScore,
                                            Comparator.nullsLast(BigDecimal::compareTo)
                                    )
                                    .thenComparing(FaceFeature::getRegisterTime, Comparator.nullsLast(LocalDateTime::compareTo))
                    )
                    .orElse(userTemplates.get(0));
            Optional<FaceFeature> readyTemplate = userTemplates.stream()
                    .filter(item -> item.getFeatureVector() != null && !item.getFeatureVector().isBlank())
                    .findFirst();
            FacePageItem item = new FacePageItem();
            item.setId(faceFeature.getId());
            item.setUserId(user.getId());
            item.setUsername(user.getUsername());
            item.setRealName(user.getRealName());
            item.setRoleCode(currentRoleCode);
            item.setEmployeeNo(user.getEmployeeNo());
            item.setFaceImagePath(faceFeature.getFaceImagePath());
            item.setFeatureVersion(faceFeature.getFeatureVersion());
            item.setQualityScore(faceFeature.getQualityScore());
            item.setFeatureReady(readyTemplate.isPresent());
            item.setFeatureDimension(readyTemplate.map(value -> parseFeatureDimension(value.getFeatureVector())).orElse(null));
            item.setTemplateCount(userTemplates.size());
            item.setRegisterTime(faceFeature.getRegisterTime());
            item.setStatus(faceFeature.getStatus());
            item.setRemark(faceFeature.getRemark());
            allItems.add(item);
        }

        int fromIndex = Math.max(0, (int) ((page.getCurrent() - 1) * page.getSize()));
        int toIndex = Math.min(allItems.size(), fromIndex + (int) page.getSize());
        List<FacePageItem> records = fromIndex >= allItems.size() ? List.of() : allItems.subList(fromIndex, toIndex);

        Page<FacePageItem> resultPage = new Page<>(page.getCurrent(), page.getSize(), allItems.size());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public FaceFeature getByUserId(Long userId) {
        List<FaceFeature> templates = faceFeatureMapper.selectList(
                new LambdaQueryWrapper<FaceFeature>()
                        .eq(FaceFeature::getUserId, userId)
                        .orderByDesc(FaceFeature::getRegisterTime)
                        .last("LIMIT 1")
        );
        return templates.isEmpty() ? null : templates.get(0);
    }

    @Override
    public ThresholdAnalysisResponse analyzeThreshold() {
        List<FaceFeature> allFeatures = faceFeatureMapper.selectList(
                new LambdaQueryWrapper<FaceFeature>().eq(FaceFeature::getStatus, 1)
        );
        List<TemplateVector> vectors = new ArrayList<>();
        for (FaceFeature feature : allFeatures) {
            double[] vector = loadOrBuildTemplateVector(feature);
            if (vector != null) {
                vectors.add(new TemplateVector(feature.getUserId(), vector));
            }
        }

        ThresholdAnalysisResponse response = new ThresholdAnalysisResponse();
        response.setCurrentThreshold(threshold);
        response.setTemplateCount(vectors.size());
        response.setUserCount((int) vectors.stream().map(TemplateVector::userId).distinct().count());

        List<Double> positiveDistances = new ArrayList<>();
        List<Double> negativeDistances = new ArrayList<>();
        for (int i = 0; i < vectors.size(); i++) {
            for (int j = i + 1; j < vectors.size(); j++) {
                double distance = computeDistance(vectors.get(i).vector(), vectors.get(j).vector());
                if (vectors.get(i).userId().equals(vectors.get(j).userId())) {
                    positiveDistances.add(distance);
                } else {
                    negativeDistances.add(distance);
                }
            }
        }

        response.setPositiveSampleCount(positiveDistances.size());
        response.setNegativeSampleCount(negativeDistances.size());

        if (positiveDistances.isEmpty() || negativeDistances.isEmpty()) {
            response.setSuggestedThreshold(threshold);
            response.setMessage("当前多模板样本不足，建议每个用户至少保留 2~5 张高质量模板后再进行阈值校准。系统默认阈值为 0.80，工程推荐区间为 0.80~0.85。");
            return response;
        }

        positiveDistances.sort(Double::compareTo);
        negativeDistances.sort(Double::compareTo);

        double positive95 = percentile(positiveDistances, 0.95D);
        double negative05 = percentile(negativeDistances, 0.05D);
        double suggested = threshold.doubleValue();
        if (positive95 < negative05) {
            suggested = (positive95 + negative05) / 2D;
        }
        suggested = Math.max(0.40D, Math.min(1.20D, suggested));
        response.setSuggestedThreshold(BigDecimal.valueOf(suggested).setScale(2, RoundingMode.HALF_UP));
        response.setMessage(String.format(
                "已基于 %d 组同人模板距离和 %d 组异人模板距离完成估计。当前默认阈值为 %.2f，工程推荐区间为 0.80~0.85，本地样本估计值约为 %.2f。",
                positiveDistances.size(),
                negativeDistances.size(),
                threshold.doubleValue(),
                suggested
        ));
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFaceByUserId(Long userId) {
        LambdaQueryWrapper<FaceFeature> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaceFeature::getUserId, userId);
        return faceFeatureMapper.delete(wrapper) > 0;
    }

    private File ensureDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("无法创建目录：" + dirPath);
        }
        return dir;
    }

    private Path resolveFaceImagePath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        String normalizedPath = storedPath.replace("\\", "/").trim();
        String relativePath = normalizedPath;
        // "/uploads/..." is a URL path returned to the frontend, not a real absolute file path on macOS/Linux.
        if (relativePath.startsWith("/uploads/")) {
            relativePath = relativePath.substring("/uploads/".length());
        } else if (relativePath.startsWith("uploads/")) {
            relativePath = relativePath.substring("uploads/".length());
        } else if (relativePath.startsWith("/")) {
            Path rawPath = Path.of(relativePath);
            if (rawPath.isAbsolute()) {
                return rawPath.normalize();
            }
            relativePath = relativePath.substring(1);
        }

        Path uploadBase = Path.of(uploadPath).toAbsolutePath().normalize();
        Path resolved = uploadBase.resolve(relativePath).normalize();
        if (!resolved.startsWith(uploadBase)) {
            return null;
        }

        return resolved;
    }

    private BigDecimal toCosineSimilarity(Double distance) {
        if (distance == null || distance.isNaN() || distance.isInfinite()) {
            return BigDecimal.ZERO;
        }
        // For L2-normalized embeddings, cosine similarity and Euclidean distance follow:
        // d^2 = 2 - 2cos(theta), so cos(theta) = 1 - d^2 / 2.
        double similarity = 1D - (distance * distance / 2D);
        similarity = Math.max(0D, Math.min(1D, similarity));
        return BigDecimal.valueOf(similarity).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toDistanceValue(Double distance) {
        if (distance == null || distance.isNaN() || distance.isInfinite()) {
            return null;
        }
        return BigDecimal.valueOf(distance).setScale(3, RoundingMode.HALF_UP);
    }

    private Integer parseFeatureDimension(String featureVector) {
        if (featureVector == null || featureVector.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(featureVector);
            return node.isArray() ? node.size() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isRecognized(Double distance) {
        return distance != null && threshold != null && distance < threshold.doubleValue();
    }

    private MatchCandidate findBestCandidate(double[] queryFeature, List<FaceFeature> allFeatures) {
        MatchCandidate bestCandidate = null;
        for (FaceFeature feature : allFeatures) {
            double[] candidateVector = loadOrBuildTemplateVector(feature);
            if (candidateVector == null) {
                continue;
            }
            double distance = computeDistance(queryFeature, candidateVector);
            if (bestCandidate == null || distance < bestCandidate.distance()) {
                bestCandidate = new MatchCandidate(feature.getUserId(), feature.getFaceImagePath(), distance);
            }
        }
        return bestCandidate;
    }

    private double[] loadOrBuildTemplateVector(FaceFeature feature) {
        double[] vector = parseFeatureVector(feature.getFeatureVector());
        if (vector != null && !shouldRefreshFeature(feature)) {
            return vector;
        }
        Path imagePath = resolveFaceImagePath(feature.getFaceImagePath());
        if (imagePath == null || !Files.exists(imagePath)) {
            return null;
        }
        try {
            String extracted = faceRecognitionPythonClient.extractFeatureVector(imagePath);
            feature.setFeatureVector(extracted);
            feature.setFeatureVersion(currentFeatureVersion);
            if (feature.getId() != null) {
                faceFeatureMapper.updateById(feature);
            }
            return parseFeatureVector(extracted);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean shouldRefreshFeature(FaceFeature feature) {
        if (feature == null) {
            return false;
        }
        if (feature.getFeatureVector() == null || feature.getFeatureVector().isBlank()) {
            return true;
        }
        if (currentFeatureVersion == null || currentFeatureVersion.isBlank()) {
            return false;
        }
        return !currentFeatureVersion.equals(feature.getFeatureVersion());
    }

    private double[] parseFeatureVector(String featureVector) {
        if (featureVector == null || featureVector.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(featureVector);
            if (!node.isArray() || node.isEmpty()) {
                return null;
            }
            double[] result = new double[node.size()];
            for (int i = 0; i < node.size(); i++) {
                result[i] = node.get(i).asDouble();
            }
            return result;
        } catch (Exception ignored) {
            return null;
        }
    }

    private double computeDistance(double[] queryFeature, double[] candidateVector) {
        int length = Math.min(queryFeature.length, candidateVector.length);
        double sum = 0D;
        for (int i = 0; i < length; i++) {
            double diff = queryFeature[i] - candidateVector[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private double percentile(List<Double> values, double ratio) {
        if (values.isEmpty()) {
            return 0D;
        }
        int index = (int) Math.ceil(ratio * values.size()) - 1;
        index = Math.max(0, Math.min(values.size() - 1, index));
        return values.get(index);
    }

    private void trimUserTemplates(Long userId) {
        if (maxTemplatesPerUser <= 0) {
            return;
        }
        List<FaceFeature> templates = faceFeatureMapper.selectList(
                new LambdaQueryWrapper<FaceFeature>()
                        .eq(FaceFeature::getUserId, userId)
        );
        templates.sort((left, right) -> {
            BigDecimal leftScore = left.getQualityScore() == null ? BigDecimal.ZERO : left.getQualityScore();
            BigDecimal rightScore = right.getQualityScore() == null ? BigDecimal.ZERO : right.getQualityScore();
            int scoreCompare = rightScore.compareTo(leftScore);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            LocalDateTime leftTime = left.getRegisterTime();
            LocalDateTime rightTime = right.getRegisterTime();
            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return 1;
            }
            if (rightTime == null) {
                return -1;
            }
            return rightTime.compareTo(leftTime);
        });
        if (templates.size() <= maxTemplatesPerUser) {
            return;
        }
        for (int i = maxTemplatesPerUser; i < templates.size(); i++) {
            FaceFeature stale = templates.get(i);
            faceFeatureMapper.deleteById(stale.getId());
            Path stalePath = resolveFaceImagePath(stale.getFaceImagePath());
            if (stalePath != null) {
                try {
                    Files.deleteIfExists(stalePath);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private record MatchCandidate(Long userId, String imagePath, double distance) {}

    private record TemplateVector(Long userId, double[] vector) {}

    private LambdaQueryWrapper<SysUser> buildUserKeywordWrapper(String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getEmployeeNo, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        return wrapper;
    }

    /**
     * 创建访问记录
     */
    private void createAccessRecord(Long userId, Long deviceId, String snapshotPath,
                                    String result, BigDecimal similarityScore, BigDecimal matchDistance, String failReason) {
        AccessRecord record = new AccessRecord();
        record.setUserId(userId);
        record.setDeviceId(deviceId);
        record.setAccessTime(LocalDateTime.now());
        record.setResult(result);
        record.setSimilarityScore(similarityScore);
        record.setMatchDistance(matchDistance);
        record.setThreshold(threshold);
        record.setSnapshotPath(snapshotPath);
        record.setFailReason(failReason);
        accessRecordService.createRecord(record);
    }
}
