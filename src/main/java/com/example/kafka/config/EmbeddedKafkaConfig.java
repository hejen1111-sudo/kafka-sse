package com.example.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;

@Configuration
@Profile("embedded") // 내장 Kafka를 사용하고 싶을 때만 이 프로파일을 활성화합니다.
public class EmbeddedKafkaConfig {

    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker(KafkaProperties kafkaProperties) {
        // application.yml에 정의된 9092 포트로 로컬에 내장형 Kafka 브로커 구동
        return new EmbeddedKafkaZKBroker(1, true, 
                kafkaProperties.getTopics().getTrigger(),
                kafkaProperties.getTopics().getInput(),
                kafkaProperties.getTopics().getResult(),
                kafkaProperties.getTopics().getAlarm(),
                kafkaProperties.getTopics().getOcr())
                .kafkaPorts(9092);
    }
}
