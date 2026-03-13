package com.example.kafka;

import com.example.kafka.util.KafkaProducerUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" }, topics = {"batch-trigger-topic"})
public class ProducerEmbeddedKafkaTest {

    private static final Logger log = LoggerFactory.getLogger(ProducerEmbeddedKafkaTest.class);

    @Autowired
    private KafkaProducerUtil producerUtil;

    @Test
    public void testProducerToEmbeddedKafka() {
        log.info("==========================================");
        log.info("Starting Test Producer to Embedded Kafka");
        
        String topic = "batch-trigger-topic";
        String key = "test-trigger-key";
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("jobName", "sampleJob");
        payload.put("message", "Hello, Embedded Kafka from Test!");
        payload.put("timestamp", System.currentTimeMillis());
        
        try {
            SendResult<String, Object> result = producerUtil.sendSync(topic, key, payload);
            
            assertThat(result).isNotNull();
            assertThat(result.getRecordMetadata().topic()).isEqualTo(topic);
            
            log.info("Successfully sent message to topic: [{}] with key: [{}], partition: [{}], offset: [{}]", 
                result.getRecordMetadata().topic(), key, 
                result.getRecordMetadata().partition(), 
                result.getRecordMetadata().offset());
                
        } catch (Exception e) {
            log.error("Failed to send message to Embedded Kafka", e);
            throw e;
        }
        
        log.info("==========================================");
    }
}
