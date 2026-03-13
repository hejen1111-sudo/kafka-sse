package com.example.kafka.batch.processor;

import com.example.kafka.model.BatchInputData;
import com.example.kafka.model.ProcessedData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataItemProcessor implements ItemProcessor<BatchInputData, ProcessedData> {

    @Override
    public ProcessedData process(BatchInputData item) {
        log.debug("Processing item: {}", item.getId());
        
        // 간단한 비즈니스 로직 적용 (대문자 변환 및 타임스탬프 등)
        String processedValue = String.valueOf(item.getPayload()).toUpperCase() + "_PROCESSED";
        
        return ProcessedData.builder()
                .id(item.getId())
                .originalPayload(String.valueOf(item.getPayload()))
                .processedResult(processedValue)
                .processedTime(System.currentTimeMillis())
                .build();
    }
}
