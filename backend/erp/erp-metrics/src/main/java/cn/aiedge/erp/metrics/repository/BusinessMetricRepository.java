package cn.aiedge.erp.metrics.repository;

import cn.aiedge.erp.metrics.entity.BusinessMetric;
import cn.aiedge.erp.metrics.enums.MetricStatus;
import cn.aiedge.erp.metrics.enums.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 业务指标定义Repository
 */
@Repository
public interface BusinessMetricRepository extends JpaRepository<BusinessMetric, Long> {
    
    Optional<BusinessMetric> findByMetricCode(String metricCode);
    
    List<BusinessMetric> findByMetricType(MetricType metricType);
    
    List<BusinessMetric> findByStatus(MetricStatus status);
    
    List<BusinessMetric> findByMetricTypeAndStatus(MetricType metricType, MetricStatus status);
    
    @Query("SELECT m FROM BusinessMetric m WHERE m.status = 'ACTIVE' ORDER BY m.metricType, m.metricCode")
    List<BusinessMetric> findAllActiveMetrics();
    
    @Query("SELECT m FROM BusinessMetric m WHERE m.metricType = :type AND m.status = 'ACTIVE'")
    List<BusinessMetric> findActiveMetricsByType(@Param("type") MetricType type);
    
    boolean existsByMetricCode(String metricCode);
}
