package cn.aiedge.erp.price.engine.strategy.mapper;

import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PriceCalculationRequestMapper extends BaseMapper<PriceCalculationRequest> {

    @Select("SELECT * FROM price_calculation_request WHERE tenant_id = #{tenantId} AND request_id = #{requestId}")
    PriceCalculationRequest findByRequestId(@Param("tenantId") Long tenantId, @Param("requestId") String requestId);

    @Select("SELECT * FROM price_calculation_request WHERE tenant_id = #{tenantId} AND customer_id = #{customerId}")
    List<PriceCalculationRequest> findByCustomerId(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    @Select("SELECT * FROM price_calculation_request WHERE tenant_id = #{tenantId} AND request_time BETWEEN #{startTime} AND #{endTime}")
    List<PriceCalculationRequest> findByTimeRange(@Param("tenantId") Long tenantId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM price_calculation_request WHERE tenant_id = #{tenantId} AND request_time BETWEEN #{startTime} AND #{endTime}")
    Long countByTimeRange(@Param("tenantId") Long tenantId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}