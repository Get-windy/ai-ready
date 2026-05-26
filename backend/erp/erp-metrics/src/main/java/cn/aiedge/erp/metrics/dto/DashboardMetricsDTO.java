package cn.aiedge.erp.metrics.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘指标数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDTO {
    
    private LocalDateTime updateTime;
    private List<MetricCardDTO> cards;
    private List<MetricChartDTO> charts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricCardDTO {
        private String metricCode;
        private String metricName;
        private BigDecimal currentValue;
        private BigDecimal previousValue;
        private BigDecimal changePercent;
        private String unit;
        private String trend; // UP, DOWN, FLAT
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricChartDTO {
        private String chartType; // line, bar, pie
        private String title;
        private List<String> labels;
        private List<ChartSeriesDTO> series;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartSeriesDTO {
        private String name;
        private List<BigDecimal> data;
    }
}
