package com.example.kafka.batch.listener;

import com.example.kafka.util.KafkaConsumerUtil;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {
    private final JobLauncher jobLauncher;
    private final Job edaBatchJob;
    private final KafkaConsumerUtil consumerUtil;

    @KafkaListener(topics = "${app.kafka.topics.trigger}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleBatchTrigger(ConsumerRecord<String, java.util.Map<String, Object>> record,
            Acknowledgment acknowledgment) {
        java.util.Map<String, Object> event = record.value();
        log.info("Received BatchTriggerEvent: {}", event);

        try {
            // 배치 파라미터 구성
            JobParametersBuilder paramsBuilder = new JobParametersBuilder()
                    .addString("requestId", String.valueOf(event.get("requestId")), true) // 식별 가능한 ID
                    .addLong("timestamp", System.currentTimeMillis(), true);

            // 추가 파라미터가 있다면 세팅
            Object parametersObj = event.get("parameters");
            if (parametersObj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> parameters = (java.util.Map<String, Object>) parametersObj;
                parameters.forEach((k, v) -> paramsBuilder.addString(k, String.valueOf(v)));
            }

            JobParameters jobParameters = paramsBuilder.toJobParameters();

            // Job명에 따라 유동적으로 분기 처리 가능 (현재는 단일 Job이라고 가정)
            if ("edaBatchJob".equals(event.get("jobName")) || event.get("jobName") == null) {
                jobLauncher.run(edaBatchJob, jobParameters);
            } else {
                log.warn("Unknown job name: {}", event.get("jobName"));
            }

            // 배치 실행 시작이 정상적으로 이뤄졌다면 Kafka Offset 커밋
            consumerUtil.commit(acknowledgment, "trigger");

        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException e) {
            log.error("Failed to launch batch job", e);
            // 에러 상황에 따라 커밋을 안하거나 Dead Letter Queue 로 보내는 정책 결정
        } catch (Exception e) {
            log.error("Unexpected error during batch trigger handling", e);
        }
    }
}
