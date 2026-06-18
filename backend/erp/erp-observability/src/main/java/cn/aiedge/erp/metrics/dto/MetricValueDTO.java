package cn.aiedge.erp.metrics.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标值DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricValueDTO {
    
    private String metricCode;
    private String metricName;
    private BigDecimal value;
    private String unit;
    private LocalDateTime timestamp;
    private String period;
}
