package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Python人脸模型调用客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FaceRecognitionPythonClient {

    private static final String JSON_PREFIX = "JSON_RESULT:";

    @Value("${face.recognition.python-command:python3}")
    private String pythonCommand;

    @Value("${face.recognition.model-dir:../facenet-pytorch-main}")
    private String modelDir;

    @Value("${face.recognition.bridge-script:bridge_api.py}")
    private String bridgeScript;

    @Value("${face.recognition.timeout:120000}")
    private long timeoutMs;

    private final ObjectMapper objectMapper;

    /**
     * 提取人脸特征向量（JSON数组字符串）
     */
    public String extractFeatureVector(Path imagePath) {
        return extractFeatureAnalysis(imagePath).featureVector();
    }

    public ExtractResult extractFeatureAnalysis(Path imagePath) {
        JsonNode response = executeCommand(List.of(
                pythonCommand,
                bridgeScript,
                "extract",
                "--image",
                imagePath.toAbsolutePath().toString()
        ));

        JsonNode featureNode = response.path("feature");
        if (!featureNode.isArray()) {
            throw new RuntimeException("模型返回特征向量格式异常");
        }
        JsonNode qualityNode = response.path("quality");
        boolean qualityPassed = qualityNode.path("passed").asBoolean(true);
        double qualityScore = qualityNode.path("score").asDouble(0.0);
        String qualityMessage = qualityNode.path("message").asText("");
        return new ExtractResult(featureNode.toString(), qualityScore, qualityPassed, qualityMessage);
    }

    public QualityResult checkImageQuality(Path imagePath) {
        JsonNode response = executeCommand(List.of(
                pythonCommand,
                bridgeScript,
                "quality",
                "--image",
                imagePath.toAbsolutePath().toString()
        ));
        JsonNode qualityNode = response.path("quality");
        return new QualityResult(
                qualityNode.path("passed").asBoolean(true),
                qualityNode.path("score").asDouble(0.0),
                qualityNode.path("message").asText(""),
                qualityNode.path("face_count").isNull() ? null : qualityNode.path("face_count").asInt(),
                qualityNode.path("blur_score").isNull() ? null : qualityNode.path("blur_score").asDouble(),
                qualityNode.path("face_ratio").isNull() ? null : qualityNode.path("face_ratio").asDouble(),
                qualityNode.path("brightness").isNull() ? null : qualityNode.path("brightness").asDouble(),
                qualityNode.path("eye_angle").isNull() ? null : qualityNode.path("eye_angle").asDouble()
        );
    }

    /**
     * 识别最相近人脸
     */
    public VerifyResult verify(Path queryImagePath, List<FaceCandidate> candidates, String threshold) {
        if (candidates == null || candidates.isEmpty()) {
            return new VerifyResult(false, null, null, null, 0);
        }

        Path candidatesFile = null;
        try {
            candidatesFile = Files.createTempFile("face-candidates-", ".txt");
            List<String> lines = new ArrayList<>(candidates.size());
            for (FaceCandidate candidate : candidates) {
                lines.add(candidate.userId() + "\t" + candidate.imagePath().toAbsolutePath());
            }
            Files.write(candidatesFile, lines, StandardCharsets.UTF_8);

            JsonNode response = executeCommand(List.of(
                    pythonCommand,
                    bridgeScript,
                    "verify",
                    "--query",
                    queryImagePath.toAbsolutePath().toString(),
                    "--candidates-file",
                    candidatesFile.toAbsolutePath().toString(),
                    "--threshold",
                    threshold
            ));

            boolean recognized = response.path("recognized").asBoolean(false);
            JsonNode matchedUserNode = response.path("matched_user_id");
            Long matchedUserId = matchedUserNode.isNull() ? null : matchedUserNode.asLong();

            JsonNode bestDistanceNode = response.path("best_distance");
            Double bestDistance = bestDistanceNode.isNull() ? null : bestDistanceNode.asDouble();

            String matchedImage = response.path("matched_image").isNull() ? null : response.path("matched_image").asText();
            int checkedCount = response.path("checked_count").asInt(0);

            return new VerifyResult(recognized, matchedUserId, bestDistance, matchedImage, checkedCount);
        } catch (IOException e) {
            throw new RuntimeException("创建候选人脸文件失败: " + e.getMessage(), e);
        } finally {
            if (candidatesFile != null) {
                try {
                    Files.deleteIfExists(candidatesFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private JsonNode executeCommand(List<String> command) {
        Path workDir = resolveModelDir();
        Path scriptPath = workDir.resolve(bridgeScript);
        if (!Files.exists(scriptPath)) {
            throw new RuntimeException("桥接脚本不存在: " + scriptPath);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir.toFile());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("模型调用超时（" + timeoutMs + "ms）");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.exitValue();

            String jsonPayload = extractJsonPayload(output);
            JsonNode rootNode = objectMapper.readTree(jsonPayload);

            if (exitCode != 0 || !rootNode.path("ok").asBoolean(false)) {
                String errorMessage = rootNode.path("error").asText("模型调用失败");
                throw new RuntimeException(errorMessage + "\n模型输出: " + output);
            }

            return rootNode;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("模型调用被中断", e);
        } catch (IOException e) {
            throw new RuntimeException("模型调用失败: " + e.getMessage(), e);
        }
    }

    private String extractJsonPayload(String output) {
        return output.lines()
                .filter(line -> line.startsWith(JSON_PREFIX))
                .reduce((first, second) -> second)
                .map(line -> line.substring(JSON_PREFIX.length()))
                .orElseThrow(() -> new RuntimeException("未解析到模型JSON输出，原始输出: " + output));
    }

    private Path resolveModelDir() {
        Path configuredPath = Path.of(modelDir);
        if (!configuredPath.isAbsolute()) {
            configuredPath = Path.of(System.getProperty("user.dir")).resolve(configuredPath);
        }
        configuredPath = configuredPath.toAbsolutePath().normalize();
        if (!Files.isDirectory(configuredPath)) {
            throw new RuntimeException("模型目录不存在: " + configuredPath);
        }
        return configuredPath;
    }

    public record FaceCandidate(Long userId, Path imagePath) {}

    public record ExtractResult(String featureVector, double qualityScore, boolean qualityPassed,
                                String qualityMessage) {}

    public record QualityResult(boolean passed, double qualityScore, String message, Integer faceCount,
                                Double blurScore, Double faceRatio, Double brightness, Double eyeAngle) {}

    public record VerifyResult(boolean recognized, Long matchedUserId, Double bestDistance,
                               String matchedImage, int checkedCount) {}
}
