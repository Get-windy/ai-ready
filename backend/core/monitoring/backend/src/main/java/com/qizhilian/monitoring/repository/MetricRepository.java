package com.qizhilian.monitoring.repository;

import com.qizhilian.monitoring.entity.MetricEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 监控指标数据访问接口
 * 
 * @author AI-Ready Team
 */
@Repository
public interface MetricRepository extends JpaRepository<MetricEntity, Long>, 
                                         JpaSpecificationExecutor<MetricEntity> {
    
    /**
     * 根据指标名称查询
     * 
     * @param metricName 指标名称
     * @return 指标实体列表
     */
    List<MetricEntity> findByMetricName(String metricName);
    
    /**
     * 根据指标名称和租户ID查询
     * 
     * @param metricName 指标名称
     * @param tenantId 租户ID
     * @return 指标实体列表
     */
    List<MetricEntity> findByMetricNameAndTenantId(String metricName, String tenantId);
    
    /**
     * 根据指标类型查询
     * 
     * @param metricType 指标类型
     * @return 指标实体列表
     */
    List<MetricEntity> findByMetricType(String metricType);
    
    /**
     * 根据指标类型和租户ID查询
     * 
     * @param metricType 指标类型
     * @param tenantId 租户ID
     * @return 指标实体列表
     */
    List<MetricEntity> findByMetricTypeAndTenantId(String metricType, String tenantId);
    
    /**
     * 根据数据来源查询
     * 
     * @param sourceType 数据来源类型
     * @return 指标实体列表
     */
    List<MetricEntity> findBySourceType(String sourceType);
    
    /**
     * 根据主机名查询
     * 
     * @param hostname 主机名
     * @return 指标实体列表
     */
    List<MetricEntity> findByHostname(String hostname);
    
    /**
     * 根据服务名查询
     * 
     * @param serviceName 服务名
     * @return 指标实体列表
     */
    List<MetricEntity> findByServiceName(String serviceName);
    
    /**
     * 根据环境标识查询
     * 
     * @param environment 环境标识
     * @return 指标实体列表
     */
    List<MetricEntity> findByEnvironment(String environment);
    
    /**
     * 根据时间范围查询
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指标实体列表
     */
    List<MetricEntity> findByMetricTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据指标名称和时间范围查询
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指标实体列表
     */
    List<MetricEntity> findByMetricNameAndMetricTimeBetween(String metricName, 
                                                           LocalDateTime startTime, 
                                                           LocalDateTime endTime);
    
    /**
     * 根据指标类型和时间范围查询
     * 
     * @param metricType 指标类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指标实体列表
     */
    List<MetricEntity> findByMetricTypeAndMetricTimeBetween(String metricType, 
                                                           LocalDateTime startTime, 
                                                           LocalDateTime endTime);
    
    /**
     * 查询最近N条记录
     * 
     * @param metricName 指标名称
     * @param limit 限制条数
     * @return 指标实体列表
     */
    @Query("SELECT m FROM MetricEntity m WHERE m.metricName = :metricName " +
           "ORDER BY m.metricTime DESC")
    List<MetricEntity> findLatestByMetricName(@Param("metricName") String metricName, 
                                              Pageable pageable);
    
    /**
     * 查询指定时间点之前的最近一条记录
     * 
     * @param metricName 指标名称
     * @param timePoint 时间点
     * @return 指标实体
     */
    @Query("SELECT m FROM MetricEntity m WHERE m.metricName = :metricName " +
           "AND m.metricTime <= :timePoint ORDER BY m.metricTime DESC")
    Optional<MetricEntity> findLatestBeforeTime(@Param("metricName") String metricName, 
                                               @Param("timePoint") LocalDateTime timePoint, 
                                               Pageable pageable);
    
    /**
     * 查询指定时间点之后的最近一条记录
     * 
     * @param metricName 指标名称
     * @param timePoint 时间点
     * @return 指标实体
     */
    @Query("SELECT m FROM MetricEntity m WHERE m.metricName = :metricName " +
           "AND m.metricTime >= :timePoint ORDER BY m.metricTime ASC")
    Optional<MetricEntity> findEarliestAfterTime(@Param("metricName") String metricName, 
                                                @Param("timePoint") LocalDateTime timePoint, 
                                                Pageable pageable);
    
    /**
     * 统计指定指标的记录数
     * 
     * @param metricName 指标名称
     * @return 记录数
     */
    Long countByMetricName(String metricName);
    
    /**
     * 统计指定时间范围内的记录数
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 记录数
     */
    Long countByMetricNameAndMetricTimeBetween(String metricName, 
                                              LocalDateTime startTime, 
                                              LocalDateTime endTime);
    
    /**
     * 计算指定指标的平均值
     * 
     * @param metricName 指标名称
     * @return 平均值
     */
    @Query("SELECT AVG(m.metricValue) FROM MetricEntity m WHERE m.metricName = :metricName")
    Optional<BigDecimal> calculateAverageByMetricName(@Param("metricName") String metricName);
    
    /**
     * 计算指定时间范围内的平均值
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 平均值
     */
    @Query("SELECT AVG(m.metricValue) FROM MetricEntity m " +
           "WHERE m.metricName = :metricName AND m.metricTime BETWEEN :startTime AND :endTime")
    Optional<BigDecimal> calculateAverageByMetricNameAndTimeRange(
            @Param("metricName") String metricName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 计算指定指标的最大值
     * 
     * @param metricName 指标名称
     * @return 最大值
     */
    @Query("SELECT MAX(m.metricValue) FROM MetricEntity m WHERE m.metricName = :metricName")
    Optional<BigDecimal> findMaxByMetricName(@Param("metricName") String metricName);
    
    /**
     * 计算指定指标的最小值
     * 
     * @param metricName 指标名称
     * @return 最小值
     */
    @Query("SELECT MIN(m.metricValue) FROM MetricEntity m WHERE m.metricName = :metricName")
    Optional<BigDecimal> findMinByMetricName(@Param("metricName") String metricName);
    
    /**
     * 计算指定指标的总和
     * 
     * @param metricName 指标名称
     * @return 总和
     */
    @Query("SELECT SUM(m.metricValue) FROM MetricEntity m WHERE m.metricName = :metricName")
    Optional<BigDecimal> calculateSumByMetricName(@Param("metricName") String metricName);
    
    /**
     * 查询所有不同的指标名称
     * 
     * @return 指标名称列表
     */
    @Query("SELECT DISTINCT m.metricName FROM MetricEntity m")
    List<String> findAllDistinctMetricNames();
    
    /**
     * 查询所有不同的指标类型
     * 
     * @return 指标类型列表
     */
    @Query("SELECT DISTINCT m.metricType FROM MetricEntity m")
    List<String> findAllDistinctMetricTypes();
    
    /**
     * 查询所有不同的主机名
     * 
     * @return 主机名列表
     */
    @Query("SELECT DISTINCT m.hostname FROM MetricEntity m WHERE m.hostname IS NOT NULL")
    List<String> findAllDistinctHostnames();
    
    /**
     * 查询所有不同的服务名
     * 
     * @return 服务名列表
     */
    @Query("SELECT DISTINCT m.serviceName FROM MetricEntity m WHERE m.serviceName IS NOT NULL")
    List<String> findAllDistinctServiceNames();
    
    /**
     * 根据告警ID查询指标
     * 
     * @param alertId 告警ID
     * @return 指标实体列表
     */
    List<MetricEntity> findByAlertId(Long alertId);
    
    /**
     * 查询告警相关的指标
     * 
     * @param isAlertRelated 是否告警相关
     * @return 指标实体列表
     */
    List<MetricEntity> findByIsAlertRelated(Boolean isAlertRelated);
    
    /**
     * 查询指定指标的聚合数据
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param aggregationType 聚合类型
     * @return 聚合结果
     */
    @Query("SELECT m FROM MetricEntity m WHERE m.metricName = :metricName " +
           "AND m.metricTime BETWEEN :startTime AND :endTime " +
           "AND m.aggregationType = :aggregationType " +
           "ORDER BY m.metricTime ASC")
    List<MetricEntity> findAggregatedMetrics(@Param("metricName") String metricName,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            @Param("aggregationType") String aggregationType);
    
    /**
     * 分页查询指标数据
     * 
     * @param metricName 指标名称（可选）
     * @param metricType 指标类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT m FROM MetricEntity m WHERE " +
           "(:metricName IS NULL OR m.metricName = :metricName) AND " +
           "(:metricType IS NULL OR m.metricType = :metricType) AND " +
           "(:startTime IS NULL OR m.metricTime >= :startTime) AND " +
           "(:endTime IS NULL OR m.metricTime <= :endTime) " +
           "ORDER BY m.metricTime DESC")
    Page<MetricEntity> searchMetrics(@Param("metricName") String metricName,
                                    @Param("metricType") String metricType,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime,
                                    Pageable pageable);
    
    /**
     * 删除过期的指标数据
     * 
     * @param retentionDays 保留天数
     * @return 删除的记录数
     */
    @Query("DELETE FROM MetricEntity m WHERE m.metricTime < :cutoffTime")
    int deleteExpiredMetrics(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 批量插入指标数据
     * 
     * @param metrics 指标实体列表
     * @return 插入的实体列表
     */
    @Override
    <S extends MetricEntity> List<S> saveAll(Iterable<S> metrics);
    
    /**
     * 批量删除指标数据
     * 
     * @param ids 指标ID列表
     */
    void deleteAllByIdIn(List<Long> ids);
    
    /**
     * 根据业务标识查询指标
     * 
     * @param businessCode 业务标识
     * @return 指标实体列表
     */
    List<MetricEntity> findByBusinessCode(String businessCode);
    
    /**
     * 根据租户ID查询指标
     * 
     * @param tenantId 租户ID
     * @return 指标实体列表
     */
    List<MetricEntity> findByTenantId(String tenantId);
    
    /**
     * 检查指标是否存在
     * 
     * @param metricName 指标名称
     * @param metricTime 指标时间
     * @return 是否存在
     */
    boolean existsByMetricNameAndMetricTime(String metricName, LocalDateTime metricTime);
}