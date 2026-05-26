package com.qizhilian.monitoring.service;

import com.qizhilian.monitoring.entity.MetricEntity;
import com.qizhilian.monitoring.model.dto.MetricDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 监控指标服务接口
 * 
 * @author AI-Ready Team
 */
public interface IMetricService {
    
    /**
     * 保存单个指标
     * 
     * @param metricDTO 指标DTO
     * @return 保存后的指标实体
     */
    MetricEntity saveMetric(MetricDTO metricDTO);
    
    /**
     * 批量保存指标
     * 
     * @param metricDTOs 指标DTO列表
     * @return 保存后的指标实体列表
     */
    List<MetricEntity> saveMetrics(List<MetricDTO> metricDTOs);
    
    /**
     * 根据ID查询指标
     * 
     * @param id 指标ID
     * @return 指标实体
     */
    MetricEntity getMetricById(Long id);
    
    /**
     * 根据指标名称查询
     * 
     * @param metricName 指标名称
     * @return 指标实体列表
     */
    List<MetricEntity> getMetricsByName(String metricName);
    
    /**
     * 根据指标类型查询
     * 
     * @param metricType 指标类型
     * @return 指标实体列表
     */
    List<MetricEntity> getMetricsByType(String metricType);
    
    /**
     * 根据时间范围查询
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指标实体列表
     */
    List<MetricEntity> getMetricsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据指标名称和时间范围查询
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指标实体列表
     */
    List<MetricEntity> getMetricsByNameAndTimeRange(String metricName, 
                                                   LocalDateTime startTime, 
                                                   LocalDateTime endTime);
    
    /**
     * 分页查询指标
     * 
     * @param metricName 指标名称（可选）
     * @param metricType 指标类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<MetricEntity> searchMetrics(String metricName, String metricType, 
                                    LocalDateTime startTime, LocalDateTime endTime, 
                                    Pageable pageable);
    
    /**
     * 获取最近N条指标记录
     * 
     * @param metricName 指标名称
     * @param limit 限制条数
     * @return 指标实体列表
     */
    List<MetricEntity> getLatestMetrics(String metricName, int limit);
    
    /**
     * 计算指标统计信息
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息
     */
    Map<String, Object> calculateMetricStatistics(String metricName, 
                                                 LocalDateTime startTime, 
                                                 LocalDateTime endTime);
    
    /**
     * 计算指标平均值
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 平均值
     */
    BigDecimal calculateAverage(String metricName, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 计算指标最大值
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 最大值
     */
    BigDecimal calculateMax(String metricName, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 计算指标最小值
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 最小值
     */
    BigDecimal calculateMin(String metricName, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 计算指标总和
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 总和
     */
    BigDecimal calculateSum(String metricName, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取所有不同的指标名称
     * 
     * @return 指标名称列表
     */
    List<String> getAllMetricNames();
    
    /**
     * 获取所有不同的指标类型
     * 
     * @return 指标类型列表
     */
    List<String> getAllMetricTypes();
    
    /**
     * 获取所有不同的主机名
     * 
     * @return 主机名列表
     */
    List<String> getAllHostnames();
    
    /**
     * 获取所有不同的服务名
     * 
     * @return 服务名列表
     */
    List<String> getAllServiceNames();
    
    /**
     * 删除指标
     * 
     * @param id 指标ID
     */
    void deleteMetric(Long id);
    
    /**
     * 批量删除指标
     * 
     * @param ids 指标ID列表
     */
    void deleteMetrics(List<Long> ids);
    
    /**
     * 删除过期的指标数据
     * 
     * @param retentionDays 保留天数
     * @return 删除的记录数
     */
    int deleteExpiredMetrics(int retentionDays);
    
    /**
     * 处理指标数据
     * 
     * @param metricId 指标ID
     * @param processingResult 处理结果
     */
    void processMetric(Long metricId, String processingResult);
    
    /**
     * 批量处理指标数据
     * 
     * @param metricIds 指标ID列表
     * @param processingResult 处理结果
     */
    void processMetrics(List<Long> metricIds, String processingResult);
    
    /**
     * 标记指标为告警相关
     * 
     * @param metricId 指标ID
     * @param alertId 告警ID
     */
    void markMetricAsAlertRelated(Long metricId, Long alertId);
    
    /**
     * 批量标记指标为告警相关
     * 
     * @param metricIds 指标ID列表
     * @param alertId 告警ID
     */
    void markMetricsAsAlertRelated(List<Long> metricIds, Long alertId);
    
    /**
     * 获取告警相关的指标
     * 
     * @param alertId 告警ID
     * @return 指标实体列表
     */
    List<MetricEntity> getAlertRelatedMetrics(Long alertId);
    
    /**
     * 检查指标是否存在
     * 
     * @param metricName 指标名称
     * @param metricTime 指标时间
     * @return 是否存在
     */
    boolean existsMetric(String metricName, LocalDateTime metricTime);
    
    /**
     * 获取指标数量统计
     * 
     * @return 统计信息
     */
    Map<String, Long> getMetricCountStatistics();
    
    /**
     * 获取指标趋势数据
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param intervalMinutes 间隔分钟数
     * @return 趋势数据
     */
    List<Map<String, Object>> getMetricTrend(String metricName, 
                                            LocalDateTime startTime, 
                                            LocalDateTime endTime, 
                                            int intervalMinutes);
    
    /**
     * 获取系统指标快照
     * 
     * @param hostname 主机名
     * @return 系统指标快照
     */
    Map<String, Object> getSystemMetricsSnapshot(String hostname);
    
    /**
     * 获取应用指标快照
     * 
     * @param serviceName 服务名
     * @return 应用指标快照
     */
    Map<String, Object> getApplicationMetricsSnapshot(String serviceName);
    
    /**
     * 获取业务指标快照
     * 
     * @param businessCode 业务标识
     * @return 业务指标快照
     */
    Map<String, Object> getBusinessMetricsSnapshot(String businessCode);
    
    /**
     * 验证指标数据
     * 
     * @param metricDTO 指标DTO
     * @return 验证结果
     */
    boolean validateMetric(MetricDTO metricDTO);
    
    /**
     * 清理无效指标数据
     * 
     * @return 清理的记录数
     */
    int cleanupInvalidMetrics();
    
    /**
     * 导出指标数据
     * 
     * @param metricName 指标名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param format 导出格式（csv, json, excel）
     * @return 导出文件路径
     */
    String exportMetrics(String metricName, LocalDateTime startTime, 
                        LocalDateTime endTime, String format);
    
    /**
     * 导入指标数据
     * 
     * @param filePath 文件路径
     * @param format 导入格式
     * @return 导入的记录数
     */
    int importMetrics(String filePath, String format);
}