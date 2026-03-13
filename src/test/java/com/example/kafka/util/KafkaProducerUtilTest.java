package com.example.kafka.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
public class KafkaProducerUtilTest {

    @Autowired
    private KafkaProducerUtil kafkaProducerUtil;

    @Test
    void testSendSync() throws ExecutionException, InterruptedException {
        String topic = "batch-input-topic";
        String payload = "{\"id\":\"test1\", \"payload\":\"test data\", \"timestamp\":1690000000}";

        SendResult<String, Object> result = kafkaProducerUtil.sendSync(topic, "key1", payload);

        assertNotNull(result);
        assertNotNull(result.getRecordMetadata());
        assertTrue(result.getRecordMetadata().hasOffset());
    }
}
