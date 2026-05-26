package cn.aiedge.erp.metrics.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 指标历史数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricHistoryDTO {
    
    private String metricCode;
    private String metricName;
    private String unit;
    private String period;
    private List<MetricDataPoint> dataPoints;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricDataPoint {
        private LocalDateTime timestamp;
        private BigDecimal value;
    }
}
