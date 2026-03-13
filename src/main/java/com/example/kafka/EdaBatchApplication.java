package com.example.kafka;

import com.example.kafka.util.KafkaProducerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EdaBatchApplication {
    
    private static final Logger log = LoggerFactory.getLogger(EdaBatchApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EdaBatchApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner testKafkaProducer(KafkaProducerUtil producerUtil) {
        return args -> {
            log.info("==========================================");
            log.info("Starting Test Producer to Embedded Kafka");
            String topic = "batch-trigger-topic";
            String key = "test-trigger-key";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("jobName", "sampleJob");
            payload.put("message", "Hello, Embedded Kafka!");
            payload.put("timestamp", System.currentTimeMillis());
            
            try {
                producerUtil.sendSync(topic, key, payload);
                log.info("Successfully sent message to topic: [{}] with key: [{}]", topic, key);
            } catch (Exception e) {
                log.error("Failed to send message to topic: [{}]", topic, e);
            }
            log.info("==========================================");
        };
    }
}
