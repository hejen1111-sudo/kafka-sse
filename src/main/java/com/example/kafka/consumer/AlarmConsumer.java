package com.example.kafka.consumer;

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
public class AlarmConsumer {

    private final SseService sseService;

    @KafkaListener(
            topics = "${app.kafka.topics.alarm}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAlarmEvent(ConsumerRecord<String, Map<String, Object>> record, Acknowledgment acknowledgment) {
        log.info("==========================================");
        log.info("Alarm Consumer Event Received!");
        log.info("topic-alarm 에 이벤트가 수신되었습니다.");
        log.info("Received Message payload: {}", record.value());
        log.info("==========================================");
        
        // UI 로 메시지를 푸시 (SSE 브로드캐스트)
        if (record.value() != null && record.value().get("message") != null) {
            sseService.broadcastMessage(String.valueOf(record.value().get("message")));
        }
        
        // 메시지 처리 완료 후 수동 커밋 (ack-mode: manual_immediate)
        acknowledgment.acknowledge();
    }
}
