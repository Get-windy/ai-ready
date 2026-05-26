package com.qizhilian.monitoring.service.impl;

import com.qizhilian.monitoring.entity.MetricEntity;
import com.qizhilian.monitoring.model.dto.MetricDTO;
import com.qizhilian.monitoring.repository.MetricRepository;
import com.qizhilian.monitoring.service.IMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricServiceImpl implements IMetricService {

    private final MetricRepository metricRepository;

    @Override
    @Transactional
    public MetricEntity saveMetric(MetricDTO metricDTO) {
        MetricEntity entity = convertToEntity(metricDTO);
        return metricRepository.save(entity);
    }

    @Override
    @Transactional
    public List<MetricEntity> saveMetrics(List<MetricDTO> metricDTOs) {
        List<MetricEntity> entities = metricDTOs.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        return metricRepository.saveAll(entities);
    }

    @Override
    public MetricEntity getMetricById(Long id) {
        return metricRepository.findById(id).orElse(null);
    }

    @Override
    public List<MetricEntity> getMetricsByName(String metricName) {
        return metricRepository.findByMetricName(metricName);
    }

    @Override
    public List<MetricEntity> getMetricsByType(String metricType) {
        return metricRepository.findByMetricType(metricType);
    }

    @Override
    public List<MetricEntity> getMetricsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return metricRepository.findByMetricTimeBetween(startTime, endTime);
    }

    @Override
    public List<MetricEntity> getMetricsByNameAndTimeRange(String metricName, LocalDateTime startTime, LocalDateTime endTime) {
        return metricRepository.findByMetricNameAndMetricTimeBetween(metricName, startTime, endTime);
    }

    @Override
    public Page<MetricEntity> searchMetrics(String metricName, String metricType, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return metricRepository.findAll(pageable); // Simplified; can be enhanced with Specification
    }

    @Override
    public List<MetricEntity> getLatestMetrics(String metricName, int limit) {
        return metricRepository.findTopNByMetricNameOrderByMetricTimeDesc(metricName, limit);
    }

    @Override
    public Map<String, Object> calculateMetricStatistics(String metricName, LocalDateTime startTime, LocalDateTime endTime) {
        List<MetricEntity> metrics = getMetricsByNameAndTimeRange(metricName, startTime, endTime);
        Map<String, Object> stats = new HashMap<>();
        if (metrics.isEmpty()) {
            stats.put("count", 0);
            return stats;
        }
        stats.put("count", metrics.size());
        stats.put("avg", calculateAverage(metricName, startTime, endTime));
        stats.put("max", calculateMax(metricName, startTime, endTime));
        stats.put("min", calculateMin(metricName, startTime, endTime));
        stats.put("sum", calculateSum(metricName, startTime, endTime));
        return stats;
    }

    @Override
    public BigDecimal calculateAverage(String metricName, LocalDateTime startTime, LocalDateTime endTime) {
        List<MetricEntity> metrics = getMetricsByNameAndTimeRange(metricName, startTime, endTime);
        return metrics.stream()
                .map(MetricEntity::getMetricValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(metrics.size(), 1)), 6, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateMax(String metricName, LocalDateTime startTime, LocalDateTime endTime) {
        return getMetricsByNameAndTimeRange(metricName, startTime, endTime).stream()
                .map(MetricEntity::getMetricValue)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calculateMin(String metricName, LocalDateTime startTime, LocalDateTime endTime) {
        return getMetricsByNameAndTimeRange(metricName, startTime, endTime).stream()
                .map(MetricEntity::getMetricValue)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calculateSum(String metricName, LocalDateTime startTime, LocalDateTime endTime) {
        return getMetricsByNameAndTimeRange(metricName, startTime, endTime).stream()
                .map(MetricEntity::getMetricValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<String> getAllMetricNames() {
        return metricRepository.findDistinctMetricNames();
    }

    @Override
    public List<String> getAllMetricTypes() {
        return metricRepository.findDistinctMetricTypes();
    }

    @Override
    public List<String> getAllHostnames() {
        return metricRepository.findDistinctHostnames();
    }

    @Override
    public List<String> getAllServiceNames() {
        return metricRepository.findDistinctServiceNames();
    }

    @Override
    @Transactional
    public void deleteMetric(Long id) {
        metricRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteMetrics(List<Long> ids) {
        metricRepository.deleteAllById(ids);
    }

    @Override
    @Transactional
    public int deleteExpiredMetrics(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return metricRepository.deleteByMetricTimeBefore(cutoff);
    }

    @Override
    @Transactional
    public void processMetric(Long metricId, String processingResult) {
        metricRepository.findById(metricId).ifPresent(m -> {
            m.markAsProcessed(processingResult);
            metricRepository.save(m);
        });
    }

    @Override
    @Transactional
    public void processMetrics(List<Long> metricIds, String processingResult) {
        metricIds.forEach(id -> processMetric(id, processingResult));
    }

    @Override
    @Transactional
    public void markMetricAsAlertRelated(Long metricId, Long alertId) {
        metricRepository.findById(metricId).ifPresent(m -> {
            m.markAsAlertRelated(alertId);
            metricRepository.save(m);
        });
    }

    @Override
    @Transactional
    public void markMetricsAsAlertRelated(List<Long> metricIds, Long alertId) {
        metricIds.forEach(id -> markMetricAsAlertRelated(id, alertId));
    }

    @Override
    public List<MetricEntity> getAlertRelatedMetrics(Long alertId) {
        return metricRepository.findByAlertId(alertId);
    }

    @Override
    public boolean existsMetric(String metricName, LocalDateTime metricTime) {
        return metricRepository.existsByMetricNameAndMetricTime(metricName, metricTime);
    }

    @Override
    public Map<String, Long> getMetricCountStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", metricRepository.count());
        stats.put("system", metricRepository.countByMetricTypeStartingWith("system."));
        stats.put("application", metricRepository.countByMetricTypeStartingWith("application."));
        stats.put("business", metricRepository.countByMetricTypeStartingWith("business."));
        return stats;
    }

    @Override
    public List<Map<String, Object>> getMetricTrend(String metricName, LocalDateTime startTime, LocalDateTime endTime, int intervalMinutes) {
        List<MetricEntity> metrics = getMetricsByNameAndTimeRange(metricName, startTime, endTime);
        // Simplified trend aggregation
        return metrics.stream().map(m -> {
            Map<String, Object> point = new HashMap<>();
            point.put("time", m.getMetricTime().toString());
            point.put("value", m.getMetricValue());
            return point;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getSystemMetricsSnapshot(String hostname) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("hostname", hostname);
        snapshot.put("cpu", getLatestMetricValue("system.cpu.usage", hostname));
        snapshot.put("memory", getLatestMetricValue("system.memory.usage", hostname));
        snapshot.put("disk", getLatestMetricValue("system.disk.usage", hostname));
        snapshot.put("load", getLatestMetricValue("system.load.average", hostname));
        return snapshot;
    }

    @Override
    public Map<String, Object> getApplicationMetricsSnapshot(String serviceName) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("service", serviceName);
        snapshot.put("jvmMemory", getLatestMetricValue("application.jvm.memory.used", serviceName));
        snapshot.put("httpRequests", getLatestMetricValue("application.http.requests", serviceName));
        snapshot.put("responseTime", getLatestMetricValue("application.http.response.time", serviceName));
        snapshot.put("errorRate", getLatestMetricValue("application.error.rate", serviceName));
        return snapshot;
    }

    @Override
    public Map<String, Object> getBusinessMetricsSnapshot(String businessCode) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("businessCode", businessCode);
        snapshot.put("activeUsers", getLatestMetricValue("business.user.active", businessCode));
        snapshot.put("orderCount", getLatestMetricValue("business.order.count", businessCode));
        snapshot.put("revenue", getLatestMetricValue("business.payment.amount", businessCode));
        return snapshot;
    }

    @Override
    public boolean validateMetric(MetricDTO metricDTO) {
        return metricDTO != null && metricDTO.getMetricName() != null && metricDTO.getMetricValue() != null;
    }

    @Override
    @Transactional
    public int cleanupInvalidMetrics() {
        // Simplified cleanup logic
        return 0;
    }

    @Override
    public String exportMetrics(String metricName, LocalDateTime startTime, LocalDateTime endTime, String format) {
        return "/tmp/export_" + metricName + "_" + System.currentTimeMillis() + "." + format;
    }

    @Override
    public int importMetrics(String filePath, String format) {
        return 0;
    }

    private MetricEntity convertToEntity(MetricDTO dto) {
        MetricEntity entity = new MetricEntity();
        entity.setMetricName(dto.getMetricName());
        entity.setMetricType(dto.getMetricType());
        entity.setMetricValue(dto.getMetricValue());
        entity.setMetricUnit(dto.getMetricUnit());
        entity.setSourceType(dto.getSourceType());
        entity.setSourceId(dto.getSourceId());
        entity.setHostname(dto.getHostname());
        entity.setServiceName(dto.getServiceName());
        entity.setInstanceId(dto.getInstanceId());
        entity.setEnvironment(dto.getEnvironment());
        entity.setAppName(dto.getAppName());
        entity.setLabels(dto.getLabels());
        entity.setMetricTime(dto.getMetricTime() != null ? dto.getMetricTime() : LocalDateTime.now());
        return entity;
    }

    private BigDecimal getLatestMetricValue(String metricName, String sourceId) {
        List<MetricEntity> metrics = metricRepository.findTopNByMetricNameOrderByMetricTimeDesc(metricName, 1);
        if (!metrics.isEmpty()) {
            MetricEntity m = metrics.get(0);
            if (sourceId == null || sourceId.equals(m.getHostname()) || sourceId.equals(m.getServiceName())) {
                return m.getMetricValue();
            }
        }
        return BigDecimal.ZERO;
    }
}
