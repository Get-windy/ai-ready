package cn.aiedge.erp.metrics.mapper;

import cn.aiedge.erp.metrics.entity.MetricAggregation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MetricAggregationMapper extends BaseMapper<MetricAggregation> {

    @Select("SELECT * FROM erp_metric_aggregation WHERE metric_id = #{metricId} AND aggregation_type = #{aggregationType} ORDER BY aggregation_time DESC")
    List<MetricAggregation> findByMetricIdAndType(@Param("metricId") Long metricId, @Param("aggregationType") String aggregationType);

    @Select("SELECT * FROM erp_metric_aggregation WHERE metric_id = #{metricId} AND aggregation_time BETWEEN #{startTime} AND #{endTime} ORDER BY aggregation_time DESC")
    List<MetricAggregation> findByMetricIdAndTimeRange(@Param("metricId") Long metricId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM erp_metric_aggregation WHERE metric_id = #{metricId} AND aggregation_type = #{aggregationType} AND aggregation_time = #{aggregationTime}")
    MetricAggregation findByMetricIdTypeAndTime(@Param("metricId") Long metricId, @Param("aggregationType") String aggregationType, @Param("aggregationTime") LocalDateTime aggregationTime);

    @Select("SELECT SUM(aggregated_value) FROM erp_metric_aggregation WHERE metric_id = #{metricId} AND aggregation_type = #{aggregationType} AND aggregation_time BETWEEN #{startTime} AND #{endTime}")
    Double sumAggregatedValue(@Param("metricId") Long metricId, @Param("aggregationType") String aggregationType, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}