package cn.aiedge.erp.price.engine.strategy.mapper;

import cn.aiedge.erp.price.engine.strategy.entity.PricingStrategy;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PricingStrategyMapper extends BaseMapper<PricingStrategy> {

    @Select("SELECT * FROM pricing_strategy WHERE tenant_id = #{tenantId} AND status = 'ACTIVE' ORDER BY priority DESC")
    List<PricingStrategy> findActiveStrategies(@Param("tenantId") Long tenantId);

    @Select("SELECT * FROM pricing_strategy WHERE tenant_id = #{tenantId} AND product_id = #{productId} AND status = 'ACTIVE'")
    List<PricingStrategy> findByProductId(@Param("tenantId") Long tenantId, @Param("productId") Long productId);

    @Select("SELECT * FROM pricing_strategy WHERE tenant_id = #{tenantId} AND customer_level = #{customerLevel} AND status = 'ACTIVE'")
    List<PricingStrategy> findByCustomerLevel(@Param("tenantId") Long tenantId, @Param("customerLevel") String customerLevel);

    @Select("SELECT * FROM pricing_strategy WHERE tenant_id = #{tenantId} AND strategy_type = #{strategyType} AND status = 'ACTIVE'")
    List<PricingStrategy> findByType(@Param("tenantId") Long tenantId, @Param("strategyType") String strategyType);

    @Select("SELECT * FROM pricing_strategy WHERE tenant_id = #{tenantId} AND effective_from <= #{now} AND effective_to >= #{now} AND status = 'ACTIVE'")
    List<PricingStrategy> findEffectiveStrategies(@Param("tenantId") Long tenantId, @Param("now") java.time.LocalDateTime now);
}