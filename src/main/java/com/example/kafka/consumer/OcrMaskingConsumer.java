package com.example.kafka.consumer;

import com.example.kafka.service.OcrMaskingService;
import com.example.kafka.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class OcrMaskingConsumer {

    private final OcrMaskingService ocrMaskingService;
    private final SseService sseService;

    @KafkaListener(
            topics = "${app.kafka.topics.ocr}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOcrEvent(ConsumerRecord<String, Map<String, Object>> record, Acknowledgment acknowledgment) {
        Map<String, Object> payload = record.value();
        String requestId = String.valueOf(payload.get("requestId"));
        String filePath = String.valueOf(payload.get("filePath"));
        String originalFilename = String.valueOf(payload.get("originalFilename"));

        log.info("==========================================");
        log.info("[OCR Consumer] 마스킹 요청 수신!");
        log.info("[OCR Consumer] requestId: {}, file: {}", requestId, originalFilename);
        log.info("==========================================");

        try {
            // 1. 외부 OCR 마스킹 API 호출
            String result = ocrMaskingService.requestMasking(filePath, requestId);
            log.info("[OCR Consumer] 마스킹 처리 완료 - requestId: {}, result: {}", requestId, result);

            // 2. SSE 알람으로 결과 브로드캐스트
            sseService.broadcastMessage("OCR 마스킹 완료: " + originalFilename + " (ID: " + requestId + ")");

            // 3. 성공 시 오프셋 커밋
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("[OCR Consumer] 마스킹 처리 실패 - requestId: {}, error: {}", requestId, e.getMessage(), e);

            // 실패 시에도 SSE로 실패 알람 전송
            sseService.broadcastMessage("OCR 마스킹 실패: " + originalFilename + " (" + e.getMessage() + ")");

            // 실패해도 무한 재시도 방지를 위해 커밋 (실패 기록은 별도 DB/로그에서 관리)
            acknowledgment.acknowledge();
        }
    }
}
