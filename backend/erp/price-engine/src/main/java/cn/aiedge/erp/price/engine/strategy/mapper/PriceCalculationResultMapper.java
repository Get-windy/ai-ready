package cn.aiedge.erp.price.engine.strategy.mapper;

import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationResult;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PriceCalculationResultMapper extends BaseMapper<PriceCalculationResult> {

    @Select("SELECT * FROM price_calculation_result WHERE tenant_id = #{tenantId} AND order_id = #{orderId}")
    List<PriceCalculationResult> findByOrderId(@Param("tenantId") Long tenantId, @Param("orderId") Long orderId);

    @Select("SELECT * FROM price_calculation_result WHERE tenant_id = #{tenantId} AND product_id = #{productId}")
    List<PriceCalculationResult> findByProductId(@Param("tenantId") Long tenantId, @Param("productId") Long productId);

    @Select("SELECT * FROM price_calculation_result WHERE tenant_id = #{tenantId} AND calculation_time BETWEEN #{startTime} AND #{endTime}")
    List<PriceCalculationResult> findByTimeRange(@Param("tenantId") Long tenantId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT SUM(original_amount - final_amount) as total_discount FROM price_calculation_result WHERE tenant_id = #{tenantId} AND calculation_time BETWEEN #{startTime} AND #{endTime}")
    Double calculateTotalDiscount(@Param("tenantId") Long tenantId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}