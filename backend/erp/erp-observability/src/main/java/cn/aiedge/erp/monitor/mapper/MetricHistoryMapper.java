package cn.aiedge.erp.monitor.mapper;

import cn.aiedge.erp.monitor.entity.MetricHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指标历史数据Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface MetricHistoryMapper extends BaseMapper<MetricHistory> {

    /**
     * 批量插入历史数据
     */
    int batchInsert(@Param("list") List<MetricHistory> list);

    /**
     * 查询历史数据
     */
    @Select("SELECT * FROM erp_metric_history WHERE tenant_id = #{tenantId} " +
            "AND metric_code = #{metricCode} AND period = #{period} " +
            "AND stat_time >= #{startTime} AND stat_time <= #{endTime} " +
            "AND deleted = 0 ORDER BY stat_time")
    List<MetricHistory> selectHistory(@Param("tenantId") Long tenantId,
                                       @Param("metricCode") String metricCode,
                                       @Param("period") String period,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 清理过期历史数据
     */
    int deleteExpiredData(@Param("beforeTime") LocalDateTime beforeTime);
}
