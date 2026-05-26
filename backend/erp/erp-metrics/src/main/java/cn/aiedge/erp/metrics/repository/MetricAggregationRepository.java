package cn.aiedge.erp.metrics.repository;

import cn.aiedge.erp.metrics.entity.MetricAggregation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 指标聚合数据Repository
 */
@Repository
public interface MetricAggregationRepository extends JpaRepository<MetricAggregation, Long> {
    
    Optional<MetricAggregation> findTopByMetricCodeAndPeriodCodeOrderByAggregationTimeDesc(
            String metricCode, String periodCode);
    
    List<MetricAggregation> findByMetricCodeAndPeriodCodeAndAggregationTimeBetweenOrderByAggregationTimeAsc(
            String metricCode, String periodCode, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT ma FROM MetricAggregation ma WHERE ma.metricCode = :code " +
           "AND ma.periodCode = :period " +
           "AND ma.aggregationTime >= :startTime AND ma.aggregationTime <= :endTime " +
           "ORDER BY ma.aggregationTime ASC")
    List<MetricAggregation> findByMetricCodePeriodAndTimeRange(
            @Param("code") String metricCode,
            @Param("period") String periodCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT ma FROM MetricAggregation ma WHERE ma.metricCode IN :codes " +
           "AND ma.periodCode = :period " +
           "AND ma.aggregationTime >= :startTime AND ma.aggregationTime <= :endTime " +
           "ORDER BY ma.aggregationTime ASC")
    List<MetricAggregation> findByMetricCodesPeriodAndTimeRange(
            @Param("codes") List<String> codes,
            @Param("period") String periodCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    boolean existsByMetricCodeAndPeriodCodeAndAggregationTime(
            String metricCode, String periodCode, LocalDateTime aggregationTime);
    
    void deleteByAggregationTimeBefore(LocalDateTime before);
}
