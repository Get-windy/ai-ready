package cn.aiedge.erp.monitor.mapper;

import cn.aiedge.erp.monitor.entity.MetricDefinition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 指标定义Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface MetricDefinitionMapper extends BaseMapper<MetricDefinition> {

    /**
     * 根据类型查询启用的指标定义
     */
    @Select("SELECT * FROM erp_metric_definition WHERE tenant_id = #{tenantId} " +
            "AND metric_type = #{metricType} AND enabled = 1 AND deleted = 0 " +
            "ORDER BY sort_order")
    List<MetricDefinition> selectEnabledByType(@Param("tenantId") Long tenantId,
                                                @Param("metricType") String metricType);

    /**
     * 查询所有启用的指标定义
     */
    @Select("SELECT * FROM erp_metric_definition WHERE tenant_id = #{tenantId} " +
            "AND enabled = 1 AND deleted = 0 ORDER BY metric_type, sort_order")
    List<MetricDefinition> selectAllEnabled(@Param("tenantId") Long tenantId);

    /**
     * 根据编码查询指标定义
     */
    @Select("SELECT * FROM erp_metric_definition WHERE tenant_id = #{tenantId} " +
            "AND metric_code = #{metricCode} AND deleted = 0 LIMIT 1")
    MetricDefinition selectByCode(@Param("tenantId") Long tenantId,
                                   @Param("metricCode") String metricCode);
}
