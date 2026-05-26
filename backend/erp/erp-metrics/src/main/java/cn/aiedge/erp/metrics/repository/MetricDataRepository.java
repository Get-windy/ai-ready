package cn.aiedge.erp.metrics.repository;

import cn.aiedge.erp.metrics.entity.MetricData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 指标数据Repository
 */
@Repository
public interface MetricDataRepository extends JpaRepository<MetricData, Long> {
    
    Optional<MetricData> findTopByMetricCodeOrderByMetricTimeDesc(String metricCode);
    
    List<MetricData> findByMetricCodeAndMetricTimeBetweenOrderByMetricTimeAsc(
            String metricCode, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT md FROM MetricData md WHERE md.metricCode = :code " +
           "AND md.metricTime >= :startTime AND md.metricTime <= :endTime " +
           "AND (:dimensionKey IS NULL OR md.dimensionKey = :dimensionKey) " +
           "AND (:dimensionValue IS NULL OR md.dimensionValue = :dimensionValue) " +
           "ORDER BY md.metricTime ASC")
    List<MetricData> findByMetricCodeAndTimeRange(
            @Param("code") String metricCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("dimensionKey") String dimensionKey,
            @Param("dimensionValue") String dimensionValue);
    
    @Query("SELECT md FROM MetricData md WHERE md.metricCode IN :codes " +
           "AND md.metricTime >= :startTime AND md.metricTime <= :endTime " +
           "ORDER BY md.metricTime ASC")
    List<MetricData> findByMetricCodesAndTimeRange(
            @Param("codes") List<String> codes,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT md FROM MetricData md WHERE md.metricCode = :code " +
           "ORDER BY md.metricTime DESC LIMIT 1")
    Optional<MetricData> findLatestByMetricCode(@Param("code") String metricCode);
    
    @Query("SELECT AVG(md.metricValue), MIN(md.metricValue), MAX(md.metricValue), SUM(md.metricValue), COUNT(md) " +
           "FROM MetricData md WHERE md.metricCode = :code " +
           "AND md.metricTime >= :startTime AND md.metricTime <= :endTime")
    Object[] calculateAggregations(
            @Param("code") String metricCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    void deleteByMetricTimeBefore(LocalDateTime before);
}
