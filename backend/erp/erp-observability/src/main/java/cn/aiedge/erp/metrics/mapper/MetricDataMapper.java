package cn.aiedge.erp.metrics.mapper;

import cn.aiedge.erp.metrics.entity.MetricData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MetricDataMapper extends BaseMapper<MetricData> {

    @Select("SELECT * FROM erp_metric_data WHERE metric_id = #{metricId} ORDER BY recorded_at DESC LIMIT #{limit}")
    List<MetricData> findByMetricId(@Param("metricId") Long metricId, @Param("limit") Integer limit);

    @Select("SELECT * FROM erp_metric_data WHERE metric_id = #{metricId} AND recorded_at BETWEEN #{startTime} AND #{endTime} ORDER BY recorded_at DESC")
    List<MetricData> findByMetricIdAndTimeRange(@Param("metricId") Long metricId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM erp_metric_data WHERE metric_id = #{metricId} ORDER BY recorded_at DESC LIMIT 1")
    MetricData findLatestByMetricId(@Param("metricId") Long metricId);

    @Select("SELECT AVG(value) FROM erp_metric_data WHERE metric_id = #{metricId} AND recorded_at BETWEEN #{startTime} AND #{endTime}")
    Double calculateAverage(@Param("metricId") Long metricId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT MAX(value) FROM erp_metric_data WHERE metric_id = #{metricId} AND recorded_at BETWEEN #{startTime} AND #{endTime}")
    Double findMaxValue(@Param("metricId") Long metricId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT MIN(value) FROM erp_metric_data WHERE metric_id = #{metricId} AND recorded_at BETWEEN #{startTime} AND #{endTime}")
    Double findMinValue(@Param("metricId") Long metricId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("DELETE FROM erp_metric_data WHERE metric_id = #{metricId} AND recorded_at < #{beforeTime}")
    int deleteOldData(@Param("metricId") Long metricId, @Param("beforeTime") LocalDateTime beforeTime);
}