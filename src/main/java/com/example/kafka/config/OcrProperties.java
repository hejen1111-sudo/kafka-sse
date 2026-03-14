package com.example.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.ocr")
public class OcrProperties {

    private String apiUrl;            // 외부 OCR 마스킹 API 주소
    private String fileStoragePath;   // 파일 저장 경로
    private int timeoutSeconds = 60;  // 외부 API 호출 타임아웃 (초)
}
