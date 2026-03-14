package com.example.kafka.controller;

import com.example.kafka.config.OcrProperties;
import com.example.kafka.util.KafkaProducerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrProducerController {

    private final KafkaProducerUtil kafkaProducerUtil;
    private final OcrProperties ocrProperties;

    @Value("${app.kafka.topics.ocr}")
    private String ocrTopic;

    /**
     * 기존 WAS(Java 8)에서 호출하는 OCR 마스킹 접수 API.
     * 파일을 받아 로컬 스토리지에 저장한 후, Kafka 토픽에 메시지를 발행합니다.
     * 기존 WAS에는 즉시 "접수 완료" 응답을 돌려줍니다.
     */
    @PostMapping("/produce/file")
    public ResponseEntity<Map<String, String>> produceOcrRequest(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "requestId", required = false) String requestId) {

        // 요청 ID가 없으면 자동 생성
        if (requestId == null || requestId.isBlank()) {
            requestId = "OCR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        String originalFilename = (file != null) ? file.getOriginalFilename() : "unknown";
        log.info("[OCR] 파일 접수 - requestId: {}, filename: {}", requestId, originalFilename);

        try {
            // 1. 파일 저장 디렉토리 생성 및 파일 저장
            Path storagePath = Paths.get(ocrProperties.getFileStoragePath());
            Files.createDirectories(storagePath);

            String savedFileName = requestId + "_" + originalFilename;
            Path filePath = storagePath.resolve(savedFileName);
            file.transferTo(filePath.toFile());

            log.info("[OCR] 파일 저장 완료 - path: {}", filePath.toAbsolutePath());

            // 2. Kafka 메시지 발행 (파일 경로 + 메타데이터)
            Map<String, Object> payload = new HashMap<>();
            payload.put("requestId", requestId);
            payload.put("originalFilename", originalFilename);
            payload.put("filePath", filePath.toAbsolutePath().toString());
            payload.put("timestamp", System.currentTimeMillis());

            kafkaProducerUtil.sendSync(ocrTopic, requestId, payload);
            log.info("[OCR] Kafka 메시지 발행 완료 - topic: {}, requestId: {}", ocrTopic, requestId);

            // 3. 기존 WAS에 즉시 응답
            Map<String, String> response = new HashMap<>();
            response.put("status", "ACCEPTED");
            response.put("requestId", requestId);
            response.put("message", "OCR 마스킹 요청이 접수되었습니다. requestId로 결과를 조회하세요.");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("[OCR] 파일 저장 실패 - requestId: {}", requestId, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "파일 저장에 실패했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
