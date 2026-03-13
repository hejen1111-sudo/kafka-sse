package com.example.kafka.batch.listener;

import com.example.kafka.EdaBatchApplication;
import com.example.kafka.model.BatchTriggerEvent;
import com.example.kafka.util.KafkaProducerUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(classes = EdaBatchApplication.class) // 메인 클래스 명시
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
public class KafkaEventListenerTest {

    @Autowired
    private KafkaProducerUtil kafkaProducerUtil;

    @Test
    void testTriggerBatchEvent() throws ExecutionException, InterruptedException {
        BatchTriggerEvent event = BatchTriggerEvent.builder()
                .jobName("edaBatchJob")
                .requestId(UUID.randomUUID().toString())
                .parameters(new HashMap<>())
                .build();

        assertDoesNotThrow(() -> {
            kafkaProducerUtil.sendSync("batch-trigger-topic", event.getRequestId(), event);
        });

        // 비동기 처리되는 Job Launch를 잠시 대기
        Thread.sleep(2000);
    }
}
