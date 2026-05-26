package com.qizhilian.monitoring.controller;

import com.qizhilian.monitoring.entity.AlertHistoryEntity;
import com.qizhilian.monitoring.entity.AlertRuleEntity;
import com.qizhilian.monitoring.repository.AlertHistoryRepository;
import com.qizhilian.monitoring.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertRuleRepository alertRuleRepository;

    @GetMapping("/history")
    public ResponseEntity<Page<AlertHistoryEntity>> getAlertHistory(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null) {
            return ResponseEntity.ok(alertHistoryRepository.findByAlertStatus(status, pageable));
        }
        return ResponseEntity.ok(alertHistoryRepository.findAll(pageable));
    }

    @GetMapping("/history/active")
    public ResponseEntity<List<AlertHistoryEntity>> getActiveAlerts() {
        return ResponseEntity.ok(alertHistoryRepository.findActiveAlerts());
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<AlertHistoryEntity> getAlertById(@PathVariable Long id) {
        return ResponseEntity.ok(alertHistoryRepository.findById(id).orElse(null));
    }

    @PostMapping("/history/{id}/acknowledge")
    public ResponseEntity<AlertHistoryEntity> acknowledgeAlert(@PathVariable Long id, @RequestParam String user) {
        AlertHistoryEntity alert = alertHistoryRepository.findById(id).orElseThrow();
        alert.acknowledge(user);
        return ResponseEntity.ok(alertHistoryRepository.save(alert));
    }

    @PostMapping("/history/{id}/resolve")
    public ResponseEntity<AlertHistoryEntity> resolveAlert(@PathVariable Long id) {
        AlertHistoryEntity alert = alertHistoryRepository.findById(id).orElseThrow();
        alert.resolve();
        return ResponseEntity.ok(alertHistoryRepository.save(alert));
    }

    @GetMapping("/history/stats")
    public ResponseEntity<Map<String, Object>> getAlertStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", alertHistoryRepository.count());
        stats.put("firing", alertHistoryRepository.countByStatus("firing"));
        stats.put("resolved", alertHistoryRepository.countByStatus("resolved"));
        stats.put("acknowledged", alertHistoryRepository.countByStatus("acknowledged"));
        stats.put("severityDistribution", alertHistoryRepository.countBySeveritySince(LocalDateTime.now().minusDays(7)));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/rules")
    public ResponseEntity<List<AlertRuleEntity>> getAllRules() {
        return ResponseEntity.ok(alertRuleRepository.findAll());
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<AlertRuleEntity> getRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(alertRuleRepository.findById(id).orElse(null));
    }

    @PostMapping("/rules")
    public ResponseEntity<AlertRuleEntity> createRule(@RequestBody AlertRuleEntity rule) {
        return ResponseEntity.ok(alertRuleRepository.save(rule));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<AlertRuleEntity> updateRule(@PathVariable Long id, @RequestBody AlertRuleEntity rule) {
        rule.setId(id);
        return ResponseEntity.ok(alertRuleRepository.save(rule));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        alertRuleRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rules/{id}/enable")
    public ResponseEntity<AlertRuleEntity> enableRule(@PathVariable Long id) {
        AlertRuleEntity rule = alertRuleRepository.findById(id).orElseThrow();
        rule.enableRule();
        return ResponseEntity.ok(alertRuleRepository.save(rule));
    }

    @PostMapping("/rules/{id}/disable")
    public ResponseEntity<AlertRuleEntity> disableRule(@PathVariable Long id) {
        AlertRuleEntity rule = alertRuleRepository.findById(id).orElseThrow();
        rule.disableRule();
        return ResponseEntity.ok(alertRuleRepository.save(rule));
    }
}
