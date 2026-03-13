package com.example.kafka.batch.writer;

import com.example.kafka.model.ProcessedData;
import com.example.kafka.repository.ProcessedDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataItemWriter implements ItemWriter<ProcessedData> {

    private final ProcessedDataRepository repository;

    @Override
    public void write(Chunk<? extends ProcessedData> chunk) {
        log.info("Writing chunk of size: {}", chunk.size());
        repository.saveAll(chunk.getItems());
    }
}
