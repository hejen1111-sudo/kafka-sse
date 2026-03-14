package com.example.kafka.controller;

import com.example.kafka.util.KafkaProducerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmProducerController {

    private final KafkaProducerUtil kafkaProducerUtil;
    
    @Value("${app.kafka.topics.alarm}")
    private String alarmTopic;

    @PostMapping("/produce")
    public ResponseEntity<String> produceMessage(@RequestParam String message) {
        log.info("Request received to produce message to alarm topic: {}", message);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("timestamp", System.currentTimeMillis());
        
        // 동기 방식으로 메시지 송신 (요청 응답 시 확정성 보장)
        kafkaProducerUtil.sendSync(alarmTopic, "alarm-key", payload);
        
        return ResponseEntity.ok("Successfully sent message to topic: " + alarmTopic);
    }

    @PostMapping("/produce/file")
    public ResponseEntity<String> produceFileMessage(@RequestPart("file") MultipartFile file) {
        String filename = (file != null) ? file.getOriginalFilename() : "unknown";
        log.info("Request received to produce file message to alarm topic. Filename: {}", filename);
        
        Map<String, Object> payload = new HashMap<>();
        // Consumer에서 기존처럼 "message" 속성에서 값을 읽어 UI로 브로드캐스트할 수 있게 "message" 필드 사용
        payload.put("message", "Uploaded File: " + filename);
        payload.put("filename", filename);
        payload.put("timestamp", System.currentTimeMillis());
        
        kafkaProducerUtil.sendSync(alarmTopic, "alarm-file-key", payload);
        log.info("Successfully produced file message to Kafka. Returning response.");
        
        return ResponseEntity.ok("Successfully sent file info to topic: " + alarmTopic);
    }
}
