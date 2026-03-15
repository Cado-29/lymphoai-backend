package com.example.demo.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.AnalysisRecord;
import com.example.demo.entity.User;
import com.example.demo.repository.AnalysisRecordRepository;
import com.example.demo.service.CustomUserDetailsService;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisRecordRepository analysisRecordRepository;
    private final CustomUserDetailsService userDetailsService;

    public AnalysisController(AnalysisRecordRepository analysisRecordRepository,
            CustomUserDetailsService userDetailsService) {
        this.analysisRecordRepository = analysisRecordRepository;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping
    public ResponseEntity<?> saveAnalysis(@RequestBody Map<String, Object> payload) {
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        String caseId = getStr(payload, "caseId");
        if (caseId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "caseId is required"));
        }

        String prediction = getStr(payload, "prediction");
        if (prediction.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "prediction is required"));
        }

        AnalysisRecord record = new AnalysisRecord();
        record.setCaseId(caseId);
        record.setUserId(currentUser.getId());
        record.setFilename(getStr(payload, "filename"));
        record.setPrediction(prediction);
        record.setConfidence(toDouble(payload.get("confidence")));
        record.setRealProb(toDouble(payload.get("realProb")));
        record.setFakeProb(toDouble(payload.get("fakeProb")));
        record.setCreatedAt(Instant.now());

        AnalysisRecord saved = analysisRecordRepository.save(record);
        return ResponseEntity.ok(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<?> getHistory() {
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        List<Map<String, Object>> response = analysisRecordRepository
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<?> clearHistory() {
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        analysisRecordRepository.deleteByUserId(currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "History cleared"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHistoryRecord(@PathVariable String id) {
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        long deleted = analysisRecordRepository.deleteByIdAndUserId(id, currentUser.getId());
        if (deleted == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "History record not found"));
        }

        return ResponseEntity.ok(Map.of("message", "History record deleted"));
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return null;
        }

        try {
            return userDetailsService.getUserEntityByUsername(username);
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> toResponse(AnalysisRecord record) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", record.getId());
        item.put("caseId", record.getCaseId() == null ? "" : record.getCaseId());
        item.put("filename", record.getFilename() == null ? "" : record.getFilename());
        item.put("prediction", record.getPrediction() == null ? "" : record.getPrediction());
        item.put("confidence", record.getConfidence());
        item.put("realProb", record.getRealProb());
        item.put("fakeProb", record.getFakeProb());
        item.put("createdAt", record.getCreatedAt() == null ? "" : record.getCreatedAt().toString());
        return item;
    }

    private String getStr(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }

        if (value == null) {
            return 0.0;
        }

        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return 0.0;
        }
    }
}
