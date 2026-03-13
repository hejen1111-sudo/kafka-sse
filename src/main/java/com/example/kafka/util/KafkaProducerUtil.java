package com.example.kafka.util;

import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class KafkaProducerUtil {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerUtil.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 동기 방식으로 메시지 송신 (성공 확인 필요 시 사용)
     */
    public <T> SendResult<String, Object> sendSync(String topic, String key, T payload) {
        log.debug("Sending message sync to topic: {}, key: {}", topic, key);
        try {
            return kafkaTemplate.send(topic, key, payload).get();
        } catch (Exception e) {
            log.error("Failed to send sync message to topic: {}, key: {}", topic, key, e);
            throw new RuntimeException("Error sending to Kafka topic: " + topic, e);
        }
    }

    /**
     * 비동기 방식으로 메시지 송신 (일반적인 이벤트 발행 시 사용)
     */
    public <T> CompletableFuture<SendResult<String, Object>> sendAsync(String topic, String key, T payload) {
        log.debug("Sending message async to topic: {}, key: {}", topic, key);
        return kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send async message to topic: {}, key: {}", topic, key, ex);
                    } else {
                        log.debug("Successfully sent async message to topic: {}, partition: {}, offset: {}",
                                topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * Key 없이 비동기 방식으로 메시지 송신
     */
    public <T> CompletableFuture<SendResult<String, Object>> sendAsync(String topic, T payload) {
        return sendAsync(topic, null, payload);
    }
}
