package cn.aiedge.erp.supply.dashboard.repository;

import cn.aiedge.erp.supply.dashboard.entity.MetricConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 指标配置数据仓库接口
 * 
 * @author AI-Edge
 * @since 2026-05-04
 */
@Repository
public interface MetricConfigRepository extends JpaRepository<MetricConfig, Long>, JpaSpecificationExecutor<MetricConfig> {

    /**
     * 根据指标编码查找
     */
    Optional<MetricConfig> findByMetricCode(String metricCode);

    /**
     * 根据指标编码和组织ID查找
     */
    Optional<MetricConfig> findByMetricCodeAndOrgId(String metricCode, Long orgId);

    /**
     * 根据类别查找激活的指标
     */
    List<MetricConfig> findByCategoryAndStatus(String category, String status);

    /**
     * 根据类别和组织ID查找激活的指标
     */
    List<MetricConfig> findByCategoryAndOrgIdAndStatus(String category, Long orgId, String status);

    /**
     * 查找所有激活的指标
     */
    List<MetricConfig> findByStatus(String status);

    /**
     * 根据组织ID查找所有激活的指标
     */
    List<MetricConfig> findByOrgIdAndStatus(Long orgId, String status);

    /**
     * 根据是否可见查找指标
     */
    List<MetricConfig> findByVisibleAndStatus(Boolean visible, String status);

    /**
     * 根据类别和是否可见查找指标
     */
    List<MetricConfig> findByCategoryAndVisibleAndStatus(String category, Boolean visible, String status);

    /**
     * 查找需要计算的指标
     */
    @Query("SELECT mc FROM MetricConfig mc WHERE mc.status = 'ACTIVE' AND mc.refreshFrequency IS NOT NULL")
    List<MetricConfig> findActiveMetricsForCalculation();

    /**
     * 根据类别和刷新频率查找指标
     */
    List<MetricConfig> findByCategoryAndRefreshFrequencyAndStatus(String category, String refreshFrequency, String status);

    /**
     * 统计各类别指标数量
     */
    @Query("SELECT mc.category, COUNT(mc) FROM MetricConfig mc WHERE mc.status = 'ACTIVE' GROUP BY mc.category")
    List<Object[]> countMetricsByCategory();

    /**
     * 查找指定组织的指标数量
     */
    @Query("SELECT COUNT(mc) FROM MetricConfig mc WHERE mc.orgId = :orgId AND mc.status = 'ACTIVE'")
    Long countByOrgId(@Param("orgId") Long orgId);

    /**
     * 检查指标编码是否存在
     */
    boolean existsByMetricCode(String metricCode);

    /**
     * 检查指标编码是否存在（排除指定ID）
     */
    boolean existsByMetricCodeAndIdNot(String metricCode, Long id);
}