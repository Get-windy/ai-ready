package cn.aiedge.erp.metrics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标告警DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricAlertDTO {
    
    private Long alertId;
    private String metricCode;
    private String metricName;
    private String alertLevel; // WARNING, CRITICAL, INFO
    private String alertStatus; // ACTIVE, ACKNOWLEDGED, RESOLVED
    private BigDecimal currentValue;
    private BigDecimal thresholdValue;
    private String thresholdType; // ABOVE, BELOW
    private String description;
    private LocalDateTime triggeredAt;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String severity;
    private String direction;
    private boolean acknowledged;
    private boolean resolved;
    private String resolutionNote;
}
