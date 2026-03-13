package com.example.kafka.controller;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.TopicListing;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class KafkaTopicController {

    private final KafkaAdmin kafkaAdmin;

    @GetMapping("/topics")
    public List<String> getTopics() throws Exception {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Collection<TopicListing> topics = adminClient.listTopics(new ListTopicsOptions()).listings().get();
            return topics.stream()
                    .map(TopicListing::name)
                    .collect(Collectors.toList());
        }
    }
}
