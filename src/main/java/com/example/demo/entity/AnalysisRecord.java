package com.example.demo.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "analysis_records")
public class AnalysisRecord {

    @Id
    private String id;

    private String caseId;
    private String userId;
    private String filename;
    private String prediction;
    private double confidence;
    private double realProb;
    private double fakeProb;
    private Instant createdAt;

    public AnalysisRecord() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPrediction() {
        return prediction;
    }

    public void setPrediction(String prediction) {
        this.prediction = prediction;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public double getRealProb() {
        return realProb;
    }

    public void setRealProb(double realProb) {
        this.realProb = realProb;
    }

    public double getFakeProb() {
        return fakeProb;
    }

    public void setFakeProb(double fakeProb) {
        this.fakeProb = fakeProb;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
