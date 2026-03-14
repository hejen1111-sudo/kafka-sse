package com.example.kafka.service;

import com.example.kafka.config.OcrProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class OcrMaskingService {

    private final OcrProperties ocrProperties;
    private final RestTemplate restTemplate;

    public OcrMaskingService(OcrProperties ocrProperties, RestTemplateBuilder restTemplateBuilder) {
        this.ocrProperties = ocrProperties;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(ocrProperties.getTimeoutSeconds()))
                .build();
    }

    /**
     * 외부 OCR 마스킹 API를 호출하여 파일을 처리합니다.
     *
     * @param filePath  마스킹할 파일의 경로
     * @param requestId 요청 식별자
     * @return OCR 마스킹 결과 (API 응답 본문)
     */
    public String requestMasking(String filePath, String requestId) {
        log.info("[OCR] 마스킹 요청 시작 - requestId: {}, filePath: {}", requestId, filePath);

        File file = new File(filePath);
        if (!file.exists()) {
            log.error("[OCR] 파일을 찾을 수 없습니다: {}", filePath);
            throw new RuntimeException("OCR 대상 파일이 존재하지 않습니다: " + filePath);
        }

        try {
            // Multipart 요청 구성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(file));
            body.add("requestId", requestId);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("[OCR] 외부 API 호출 중... URL: {}", ocrProperties.getApiUrl());

            ResponseEntity<String> response = restTemplate.exchange(
                    ocrProperties.getApiUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            log.info("[OCR] 마스킹 완료 - requestId: {}, 응답 코드: {}", requestId, response.getStatusCode());
            return response.getBody();

        } catch (Exception e) {
            log.error("[OCR] 외부 API 호출 실패 - requestId: {}, 에러: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("OCR 마스킹 API 호출 실패: " + e.getMessage(), e);
        }
    }
}
