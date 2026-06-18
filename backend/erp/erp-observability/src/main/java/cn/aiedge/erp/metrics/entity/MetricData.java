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
 * 指标数据实体
 */
@Entity
@Table(name = "erp_metric_data", indexes = {
    @Index(name = "idx_metric_code_time", columnList = "metric_code,metric_time"),
    @Index(name = "idx_metric_time", columnList = "metric_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "metric_code", nullable = false, length = 64)
    private String metricCode;
    
    @Column(name = "metric_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal metricValue;
    
    @Column(name = "metric_time", nullable = false)
    private LocalDateTime metricTime;
    
    @Column(name = "dimension_key", length = 128)
    private String dimensionKey;
    
    @Column(name = "dimension_value", length = 256)
    private String dimensionValue;
    
    @Column(name = "period_code", length = 16)
    private String periodCode;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
