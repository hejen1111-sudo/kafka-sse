package com.example.kafka.batch.config;

import com.example.kafka.batch.processor.DataItemProcessor;
import com.example.kafka.batch.producer.BatchEventProducer;
import com.example.kafka.batch.reader.KafkaItemReader;
import com.example.kafka.batch.writer.DataItemWriter;
import com.example.kafka.model.BatchInputData;
import com.example.kafka.model.BatchResultEvent;
import com.example.kafka.model.ProcessedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final KafkaItemReader kafkaItemReader;
    private final DataItemProcessor dataItemProcessor;
    private final DataItemWriter dataItemWriter;
    private final BatchEventProducer batchEventProducer;

    public static final String JOB_NAME = "edaBatchJob";

    @Bean
    public Job edaBatchJob(Step edaBatchStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(edaBatchStep)
                .listener(jobExecutionListener())
                .build();
    }

    @Bean
    public Step edaBatchStep() {
        return new StepBuilder("edaBatchStep", jobRepository)
                .<BatchInputData, ProcessedData>chunk(100, transactionManager)
                .reader(kafkaItemReader)
                .processor(dataItemProcessor)
                .writer(dataItemWriter)
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        log.info("Starting consuming from Kafka...");
                        kafkaItemReader.startConsuming();
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        log.info("Stopping consuming from Kafka... Step ExitStatus: {}", stepExecution.getExitStatus());
                        kafkaItemReader.stopConsuming();
                        return stepExecution.getExitStatus();
                    }
                })
                .build();
    }

    @Bean
    @NonNull
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            private long startTime;

            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("Starting Job: {}, Request ID: {}", jobExecution.getJobInstance().getJobName(),
                        jobExecution.getJobParameters().getString("requestId"));
                startTime = System.currentTimeMillis();
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                long duration = System.currentTimeMillis() - startTime;

                int readCount = 0;
                int writeCount = 0;

                for (StepExecution step : jobExecution.getStepExecutions()) {
                    readCount += step.getReadCount();
                    writeCount += step.getWriteCount();
                }

                log.info("Finished Job: {}. Status: {}. Duration: {}ms. Read: {}, Written: {}",
                        jobExecution.getJobInstance().getJobName(), jobExecution.getStatus(), duration, readCount,
                        writeCount);

                BatchResultEvent resultEvent = BatchResultEvent.builder()
                        .jobName(jobExecution.getJobInstance().getJobName())
                        .requestId(jobExecution.getJobParameters().getString("requestId", "unknown"))
                        .status(jobExecution.getStatus().name())
                        .processedCount(writeCount)
                        .durationMs(duration)
                        .build();

                batchEventProducer.publishResultEvent(resultEvent);
            }
        };
    }
}
