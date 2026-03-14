package com.example.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {

    private Topics topics = new Topics();
    private int partitions = 1;
    private short replicas = 1;

    @Getter
    @Setter
    public static class Topics {
        private String trigger;
        private String input;
        private String result;
        private String alarm;
        private String ocr;
    }
}
