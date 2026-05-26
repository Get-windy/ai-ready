package cn.aiedge.erp.supply.dashboard.repository;

import cn.aiedge.erp.supply.dashboard.entity.MetricData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 指标数据数据仓库接口
 * 
 * @author AI-Edge
 * @since 2026-05-04
 */
@Repository
public interface MetricDataRepository extends JpaRepository<MetricData, Long>, JpaSpecificationExecutor<MetricData> {

    /**
     * 根据指标编码和日期查找
     */
    Optional<MetricData> findByMetricCodeAndMetricDate(String metricCode, LocalDate metricDate);

    /**
     * 根据指标编码、日期和组织ID查找
     */
    Optional<MetricData> findByMetricCodeAndMetricDateAndOrgId(String metricCode, LocalDate metricDate, Long orgId);

    /**
     * 根据指标编码和时间范围查找
     */
    List<MetricData> findByMetricCodeAndMetricDateBetween(String metricCode, LocalDate startDate, LocalDate endDate);

    /**
     * 根据指标编码、组织ID和时间范围查找
     */
    List<MetricData> findByMetricCodeAndOrgIdAndMetricDateBetween(String metricCode, Long orgId, LocalDate startDate, LocalDate endDate);

    /**
     * 根据类别和时间范围查找
     */
    List<MetricData> findByCategoryAndMetricDateBetween(String category, LocalDate startDate, LocalDate endDate);

    /**
     * 根据类别、组织ID和时间范围查找
     */
    List<MetricData> findByCategoryAndOrgIdAndMetricDateBetween(String category, Long orgId, LocalDate startDate, LocalDate endDate);

    /**
     * 根据指标编码查找最新数据
     */
    @Query("SELECT md FROM MetricData md WHERE md.metricCode = :metricCode ORDER BY md.metricDate DESC, md.calculationTime DESC LIMIT 1")
    Optional<MetricData> findLatestByMetricCode(@Param("metricCode") String metricCode);

    /**
     * 根据指标编码和组织ID查找最新数据
     */
    @Query("SELECT md FROM MetricData md WHERE md.metricCode = :metricCode AND md.orgId = :orgId ORDER BY md.metricDate DESC, md.calculationTime DESC LIMIT 1")
    Optional<MetricData> findLatestByMetricCodeAndOrgId(@Param("metricCode") String metricCode, @Param("orgId") Long orgId);

    /**
     * 根据状态查找数据
     */
    List<MetricData> findByStatus(String status);

    /**
     * 查找预警数据
     */
    List<MetricData> findByStatusIn(List<String> statuses);

    /**
     * 根据计算时间查找
     */
    List<MetricData> findByCalculationTimeAfter(LocalDateTime calculationTime);

    /**
     * 根据指标编码和状态查找
     */
    List<MetricData> findByMetricCodeAndStatus(String metricCode, String status);

    /**
     * 删除指定日期之前的数据
     */
    void deleteByMetricDateBefore(LocalDate date);

    /**
     * 统计指定指标的数据量
     */
    @Query("SELECT COUNT(md) FROM MetricData md WHERE md.metricCode = :metricCode")
    Long countByMetricCode(@Param("metricCode") String metricCode);

    /**
     * 获取指标的最大日期
     */
    @Query("SELECT MAX(md.metricDate) FROM MetricData md WHERE md.metricCode = :metricCode")
    Optional<LocalDate> findMaxDateByMetricCode(@Param("metricCode") String metricCode);

    /**
     * 获取指标的最小日期
     */
    @Query("SELECT MIN(md.metricDate) FROM MetricData md WHERE md.metricCode = :metricCode")
    Optional<LocalDate> findMinDateByMetricCode(@Param("metricCode") String metricCode);

    /**
     * 根据多个指标编码和时间范围查找数据
     */
    @Query("SELECT md FROM MetricData md WHERE md.metricCode IN :metricCodes AND md.metricDate BETWEEN :startDate AND :endDate")
    List<MetricData> findByMetricCodesAndDateRange(@Param("metricCodes") List<String> metricCodes, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);

    /**
     * 根据多个指标编码、组织ID和时间范围查找数据
     */
    @Query("SELECT md FROM MetricData md WHERE md.metricCode IN :metricCodes AND md.orgId = :orgId AND md.metricDate BETWEEN :startDate AND :endDate")
    List<MetricData> findByMetricCodesAndOrgIdAndDateRange(@Param("metricCodes") List<String> metricCodes,
                                                           @Param("orgId") Long orgId,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);

    /**
     * 查找需要重新计算的数据
     */
    @Query("SELECT md FROM MetricData md WHERE md.calculationVersion < :currentVersion OR md.dataQualityScore < :minQualityScore")
    List<MetricData> findDataForRecalculation(@Param("currentVersion") Integer currentVersion,
                                               @Param("minQualityScore") BigDecimal minQualityScore);

    /**
     * 统计各类别数据量
     */
    @Query("SELECT md.category, COUNT(md) FROM MetricData md WHERE md.metricDate BETWEEN :startDate AND :endDate GROUP BY md.category")
    List<Object[]> countDataByCategoryAndDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取数据质量统计
     */
    @Query("SELECT AVG(md.dataQualityScore), MIN(md.dataQualityScore), MAX(md.dataQualityScore) FROM MetricData md WHERE md.metricDate = :date")
    Object[] getDataQualityStats(@Param("date") LocalDate date);
}