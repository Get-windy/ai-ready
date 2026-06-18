package cn.aiedge.erp.monitor.mapper;

import cn.aiedge.erp.monitor.dto.MetricQueryDTO;
import cn.aiedge.erp.monitor.entity.BusinessMetric;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务指标Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface MonitorBusinessMetricMapper extends BaseMapper<BusinessMetric> {

    /**
     * 分页查询指标
     */
    IPage<BusinessMetric> selectMetricPage(Page<BusinessMetric> page, @Param("query") MetricQueryDTO query);

    /**
     * 查询最新指标
     */
    List<BusinessMetric> selectLatestMetrics(@Param("tenantId") Long tenantId,
                                              @Param("metricTypes") List<String> metricTypes);

    /**
     * 查询历史指标
     */
    List<BusinessMetric> selectHistoryMetrics(@Param("tenantId") Long tenantId,
                                               @Param("metricCode") String metricCode,
                                               @Param("period") String period,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指标趋势
     */
    List<BusinessMetric> selectMetricTrend(@Param("tenantId") Long tenantId,
                                            @Param("metricCode") String metricCode,
                                            @Param("period") String period,
                                            @Param("hours") int hours);

    /**
     * 批量插入或更新
     */
    int batchInsertOrUpdate(@Param("list") List<BusinessMetric> list);

    /**
     * 查询实时指标
     */
    @Select("SELECT * FROM erp_business_metric WHERE tenant_id = #{tenantId} " +
            "AND metric_type = #{metricType} AND period = 'realtime' " +
            "AND deleted = 0 ORDER BY stat_time DESC LIMIT #{limit}")
    List<BusinessMetric> selectRealTimeMetrics(@Param("tenantId") Long tenantId,
                                                @Param("metricType") String metricType,
                                                @Param("limit") int limit);
}
