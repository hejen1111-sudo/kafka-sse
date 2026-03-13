package com.example.kafka.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumerUtil {

    /**
     * 수동 커밋 수행 헬퍼 메서드
     *
     * @param acknowledgment Kafka Acknowledgment 객체
     * @param topic 토픽명 (로깅용)
     */
    public void commit(Acknowledgment acknowledgment, String topic) {
        if (acknowledgment != null) {
            try {
                acknowledgment.acknowledge();
                log.debug("Successfully acknowledged message for topic: {}", topic);
            } catch (Exception e) {
                log.error("Failed to acknowledge message for topic: {}", topic, e);
                throw e; // 재처리 등을 위해 던짐
            }
        } else {
            log.warn("Acknowledgment is null, unable to manual commit for topic: {}", topic);
        }
    }
}
