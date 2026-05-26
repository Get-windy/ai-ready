package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.CostAllocationRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CostAllocationRuleMapper extends BaseMapper<CostAllocationRule> {
    
    @Select("SELECT * FROM finance_cost_allocation_rule WHERE tenant_id = #{tenantId} AND deleted = 0 AND enabled = 1 ORDER BY priority")
    List<CostAllocationRule> listAllEnabled(@Param("tenantId") Long tenantId);
    
    @Select("SELECT * FROM finance_cost_allocation_rule WHERE tenant_id = #{tenantId} AND deleted = 0 AND from_center_id = #{fromCenterId} AND enabled = 1 ORDER BY priority")
    List<CostAllocationRule> listByFromCenter(@Param("tenantId") Long tenantId, @Param("fromCenterId") Long fromCenterId);
    
    @Select("SELECT * FROM finance_cost_allocation_rule WHERE tenant_id = #{tenantId} AND deleted = 0 AND rule_code = #{ruleCode}")
    CostAllocationRule getByCode(@Param("tenantId") Long tenantId, @Param("ruleCode") String ruleCode);
}