package com.example.kafka.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    private final KafkaProperties properties;

    @Bean
    public NewTopic batchTriggerTopic() {
        return TopicBuilder.name(properties.getTopics().getTrigger())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .build();
    }

    @Bean
    public NewTopic batchInputTopic() {
        return TopicBuilder.name(properties.getTopics().getInput())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .build();
    }

    @Bean
    public NewTopic batchResultTopic() {
        return TopicBuilder.name(properties.getTopics().getResult())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .build();
    }

    @Bean
    public NewTopic bizgTopic() {
        return TopicBuilder.name(properties.getTopics().getBizg())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .build();
    }
}
