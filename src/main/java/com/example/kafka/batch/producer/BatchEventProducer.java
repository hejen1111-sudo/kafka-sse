package com.example.kafka.batch.producer;

import com.example.kafka.config.KafkaProperties;
import com.example.kafka.model.BatchResultEvent;
import com.example.kafka.util.KafkaProducerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchEventProducer {

    private final KafkaProducerUtil kafkaProducerUtil;
    private final KafkaProperties kafkaProperties;

    public void publishResultEvent(BatchResultEvent event) {
        String topic = kafkaProperties.getTopics().getResult();
        log.info("Publishing batch result to topic: {}", topic);
        
        kafkaProducerUtil.sendAsync(topic, event.getRequestId(), event);
    }
}
