package cn.aiedge.erp.price.engine.strategy.mapper;

import cn.aiedge.erp.price.engine.strategy.entity.DiscountRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DiscountRuleMapper extends BaseMapper<DiscountRule> {

    @Select("SELECT * FROM discount_rule WHERE tenant_id = #{tenantId} AND status = 'ACTIVE' ORDER BY priority DESC")
    List<DiscountRule> findActiveRules(@Param("tenantId") Long tenantId);

    @Select("SELECT * FROM discount_rule WHERE tenant_id = #{tenantId} AND rule_type = #{ruleType} AND status = 'ACTIVE'")
    List<DiscountRule> findByType(@Param("tenantId") Long tenantId, @Param("ruleType") String ruleType);

    @Select("SELECT * FROM discount_rule WHERE tenant_id = #{tenantId} AND min_quantity <= #{quantity} AND max_quantity >= #{quantity} AND status = 'ACTIVE'")
    List<DiscountRule> findByQuantityRange(@Param("tenantId") Long tenantId, @Param("quantity") Integer quantity);

    @Select("SELECT * FROM discount_rule WHERE tenant_id = #{tenantId} AND customer_level = #{customerLevel} AND status = 'ACTIVE'")
    List<DiscountRule> findByCustomerLevel(@Param("tenantId") Long tenantId, @Param("customerLevel") String customerLevel);

    @Select("SELECT * FROM discount_rule WHERE tenant_id = #{tenantId} AND effective_from <= #{now} AND effective_to >= #{now} AND status = 'ACTIVE'")
    List<DiscountRule> findEffectiveRules(@Param("tenantId") Long tenantId, @Param("now") java.time.LocalDateTime now);
}