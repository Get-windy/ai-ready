package cn.aiedge.erp.metrics.service.impl;

import cn.aiedge.erp.metrics.dto.MetricAlertDTO;
import cn.aiedge.erp.metrics.dto.MetricValueDTO;
import cn.aiedge.erp.metrics.entity.BusinessMetric;
import cn.aiedge.erp.metrics.repository.BusinessMetricRepository;
import cn.aiedge.erp.metrics.service.MetricsAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsAlertServiceImpl implements MetricsAlertService {
    
    private final BusinessMetricRepository metricRepository;
    private final Map<Long, MetricAlertDTO> activeAlerts = new ConcurrentHashMap<>();
    private final Map<Long, MetricAlertDTO> alertHistory = new ConcurrentHashMap<>();
    private final AtomicLong alertIdGenerator = new AtomicLong(1);
    private volatile boolean monitoringEnabled = true;
    
    @Override
    public List<MetricAlertDTO> checkMetricAlerts(MetricValueDTO metricValue) {
        List<MetricAlertDTO> alerts = new ArrayList<>();
        
        Optional<BusinessMetric> metricOpt = metricRepository.findByMetricCode(metricValue.getMetricCode());
        if (metricOpt.isEmpty()) {
            return alerts;
        }
        
        BusinessMetric metric = metricOpt.get();
        BigDecimal currentValue = metricValue.getValue();
        
        if (metric.getThresholdCritical() != null) {
            BigDecimal thresholdCritical = BigDecimal.valueOf(metric.getThresholdCritical());
            if (currentValue.compareTo(thresholdCritical) > 0) {
                MetricAlertDTO alert = createAlert(metric, currentValue, thresholdCritical, "CRITICAL", "ABOVE");
                alerts.add(alert);
                activeAlerts.put(alert.getAlertId(), alert);
                log.warn("CRITICAL ALERT: {} = {} exceeds threshold {}", 
                    metric.getMetricCode(), currentValue, thresholdCritical);
            }
        }
        
        if (metric.getThresholdWarning() != null) {
            BigDecimal thresholdWarning = BigDecimal.valueOf(metric.getThresholdWarning());
            BigDecimal thresholdCritical = metric.getThresholdCritical() != null 
                ? BigDecimal.valueOf(metric.getThresholdCritical()) : null;
            if (currentValue.compareTo(thresholdWarning) > 0 && 
                (thresholdCritical == null || currentValue.compareTo(thresholdCritical) <= 0)) {
                MetricAlertDTO alert = createAlert(metric, currentValue, thresholdWarning, "WARNING", "ABOVE");
                alerts.add(alert);
                activeAlerts.put(alert.getAlertId(), alert);
                log.warn("WARNING ALERT: {} = {} exceeds threshold {}", 
                    metric.getMetricCode(), currentValue, thresholdWarning);
            }
        }
        
        return alerts;
    }
    
    @Override
    public List<MetricAlertDTO> checkBatchMetricAlerts(List<MetricValueDTO> metricValues) {
        return metricValues.stream()
            .flatMap(mv -> checkMetricAlerts(mv).stream())
            .collect(Collectors.toList());
    }
    
    @Override
    public List<MetricAlertDTO> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }
    
    @Override
    public List<MetricAlertDTO> getAlertHistory(String metricCode, int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return alertHistory.values().stream()
            .filter(alert -> alert.getMetricCode().equals(metricCode))
            .filter(alert -> alert.getTriggeredAt().isAfter(cutoff))
            .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
            .collect(Collectors.toList());
    }
    
    @Override
    public void acknowledgeAlert(Long alertId, String operator) {
        MetricAlertDTO alert = activeAlerts.remove(alertId);
        if (alert != null) {
            alert.setAcknowledged(true);
            alert.setAcknowledgedAt(LocalDateTime.now());
            alert.setAcknowledgedBy(operator);
            alertHistory.put(alertId, alert);
            log.info("Alert acknowledged: {} by {}", alertId, operator);
        }
    }
    
    @Override
    public void clearAlert(Long alertId, String operator) {
        MetricAlertDTO alert = activeAlerts.remove(alertId);
        if (alert != null) {
            alert.setResolved(true);
            alert.setResolvedAt(LocalDateTime.now());
            alert.setResolvedBy(operator);
            alertHistory.put(alertId, alert);
            log.info("Alert cleared: {} by {}", alertId, operator);
        }
    }
    
    @Override
    public void startAlertMonitoring() {
        this.monitoringEnabled = true;
        log.info("Alert monitoring started");
    }
    
    @Override
    public void stopAlertMonitoring() {
        this.monitoringEnabled = false;
        log.info("Alert monitoring stopped");
    }
    
    @Scheduled(fixedRate = 60000)
    public void periodicAlertCheck() {
        if (!monitoringEnabled) {
            return;
        }
        log.debug("Running periodic alert check...");
    }
    
    private MetricAlertDTO createAlert(BusinessMetric metric, BigDecimal currentValue, 
                                       BigDecimal threshold, String severity, String direction) {
        MetricAlertDTO alert = new MetricAlertDTO();
        alert.setAlertId(alertIdGenerator.getAndIncrement());
        alert.setMetricCode(metric.getMetricCode());
        alert.setMetricName(metric.getMetricName());
        alert.setCurrentValue(currentValue);
        alert.setThresholdValue(threshold);
        alert.setSeverity(severity);
        alert.setDirection(direction);
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setAcknowledged(false);
        alert.setResolved(false);
        alertHistory.put(alert.getAlertId(), alert);
        return alert;
    }
}