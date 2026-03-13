package com.example.kafka.repository;

import com.example.kafka.model.ProcessedData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedDataRepository extends JpaRepository<ProcessedData, String> {
}
