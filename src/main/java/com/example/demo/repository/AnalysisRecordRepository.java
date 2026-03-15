package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.entity.AnalysisRecord;

public interface AnalysisRecordRepository extends MongoRepository<AnalysisRecord, String> {
    List<AnalysisRecord> findByUserIdOrderByCreatedAtDesc(String userId);

    void deleteByUserId(String userId);

    long deleteByIdAndUserId(String id, String userId);
}
