package cn.aiedge.erp.metrics.entity;

import cn.aiedge.erp.metrics.enums.MetricPeriod;
import cn.aiedge.erp.metrics.enums.MetricStatus;
import cn.aiedge.erp.metrics.enums.MetricType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 业务指标定义实体
 */
@Entity
@Table(name = "erp_business_metric")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessMetric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "metric_code", nullable = false, unique = true, length = 64)
    private String metricCode;
    
    @Column(name = "metric_name", nullable = false, length = 128)
    private String metricName;
    
    @Column(name = "description", length = 512)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 32)
    private MetricType metricType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 16)
    private MetricPeriod period;
    
    @Column(name = "calculation_formula", length = 1024)
    private String calculationFormula;
    
    @Column(name = "data_source", length = 256)
    private String dataSource;
    
    @Column(name = "unit", length = 32)
    private String unit;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private MetricStatus status;
    
    @Column(name = "threshold_warning")
    private Double thresholdWarning;
    
    @Column(name = "threshold_critical")
    private Double thresholdCritical;
    
    @Column(name = "refresh_interval_seconds")
    private Integer refreshIntervalSeconds;
    
    @Column(name = "retention_days")
    private Integer retentionDays;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 64)
    private String createdBy;
    
    @Column(name = "updated_by", length = 64)
    private String updatedBy;
}
