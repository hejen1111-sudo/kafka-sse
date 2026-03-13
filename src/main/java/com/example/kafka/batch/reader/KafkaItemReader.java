package com.example.kafka.batch.reader;

import com.example.kafka.config.KafkaProperties;
import com.example.kafka.model.BatchInputData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaItemReader implements ItemReader<BatchInputData> {

    private final ConsumerFactory<String, Object> consumerFactory;
    private final KafkaProperties kafkaProperties;

    // 이벤트를 임시 저장할 큐
    private final BlockingQueue<BatchInputData> messageQueue = new LinkedBlockingQueue<>();
    private KafkaMessageListenerContainer<String, Object> container;

    private volatile boolean isRunning = false;

    @PostConstruct
    public void init() {
        ContainerProperties containerProps = new ContainerProperties(kafkaProperties.getTopics().getInput());
        containerProps.setMessageListener((MessageListener<String, Object>) record -> {
            if (record.value() instanceof BatchInputData data) {
                messageQueue.offer(data);
            }
        });

        // 동적으로 메시지를 가져오도록 컨테이너 설정 (자동 시작 안함)
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProps);
        container.setAutoStartup(false);
    }

    public void startConsuming() {
        if (!isRunning) {
            log.info("Starting KafkaItemReader to consume from topic: {}", kafkaProperties.getTopics().getInput());
            container.start();
            isRunning = true;
        }
    }

    public void stopConsuming() {
        if (isRunning) {
            log.info("Stopping KafkaItemReader");
            container.stop();
            isRunning = false;
        }
    }

    @Override
    public BatchInputData read() throws Exception {
        // 배치 실행 시 일정 시간 대기하며 데이터를 읽음. 데이터가 없으면 배치는 정상 종료됨(null 리턴)
        BatchInputData data = messageQueue.poll(5, TimeUnit.SECONDS);
        if (data == null) {
            log.info("No more records found in kafka topic. Stopping read.");
            stopConsuming(); // 데이터가 없으면 리스너 컨테이너 중지
            return null;
        }
        return data;
    }
}
