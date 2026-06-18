package cn.aiedge.erp.metrics.mapper;

import cn.aiedge.erp.metrics.entity.BusinessMetric;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BusinessMetricMapper extends BaseMapper<BusinessMetric> {

    @Select("SELECT * FROM erp_business_metric WHERE metric_code = #{metricCode}")
    BusinessMetric findByMetricCode(@Param("metricCode") String metricCode);

    @Select("SELECT * FROM erp_business_metric WHERE metric_type = #{metricType} AND status = 'ACTIVE'")
    List<BusinessMetric> findByType(@Param("metricType") String metricType);

    @Select("SELECT * FROM erp_business_metric WHERE status = #{status}")
    List<BusinessMetric> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM erp_business_metric WHERE status = 'ACTIVE' ORDER BY metric_code")
    List<BusinessMetric> findActiveMetrics();

    @Select("SELECT * FROM erp_business_metric WHERE period = #{period} AND status = 'ACTIVE'")
    List<BusinessMetric> findByPeriod(@Param("period") String period);

    @Select("SELECT COUNT(*) FROM erp_business_metric WHERE status = 'ACTIVE'")
    Integer countActiveMetrics();
}