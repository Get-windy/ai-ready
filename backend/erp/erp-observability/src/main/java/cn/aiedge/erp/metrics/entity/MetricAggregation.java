package cn.aiedge.erp.metrics.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标聚合数据实体
 */
@Entity
@Table(name = "erp_metric_aggregation", indexes = {
    @Index(name = "idx_agg_metric_period", columnList = "metric_code,period_code,aggregation_time"),
    @Index(name = "idx_agg_time", columnList = "aggregation_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricAggregation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "metric_code", nullable = false, length = 64)
    private String metricCode;
    
    @Column(name = "period_code", nullable = false, length = 16)
    private String periodCode;
    
    @Column(name = "aggregation_time", nullable = false)
    private LocalDateTime aggregationTime;
    
    @Column(name = "avg_value", precision = 19, scale = 4)
    private BigDecimal avgValue;
    
    @Column(name = "min_value", precision = 19, scale = 4)
    private BigDecimal minValue;
    
    @Column(name = "max_value", precision = 19, scale = 4)
    private BigDecimal maxValue;
    
    @Column(name = "sum_value", precision = 19, scale = 4)
    private BigDecimal sumValue;
    
    @Column(name = "count_value")
    private Long countValue;
    
    @Column(name = "dimension_key", length = 128)
    private String dimensionKey;
    
    @Column(name = "dimension_value", length = 256)
    private String dimensionValue;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
