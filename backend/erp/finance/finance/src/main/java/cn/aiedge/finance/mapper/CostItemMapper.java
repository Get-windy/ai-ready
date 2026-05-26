package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.CostItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CostItemMapper extends BaseMapper<CostItem> {
    
    @Select("SELECT * FROM finance_cost_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period} ORDER BY item_code")
    List<CostItem> listByPeriod(@Param("tenantId") Long tenantId, @Param("period") String period);
    
    @Select("SELECT * FROM finance_cost_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND cost_center_id = #{costCenterId} ORDER BY item_code")
    List<CostItem> listByCostCenterId(@Param("tenantId") Long tenantId, @Param("costCenterId") Long costCenterId);
    
    @Select("SELECT * FROM finance_cost_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND cost_type = #{costType} ORDER BY item_code")
    List<CostItem> listByCostType(@Param("tenantId") Long tenantId, @Param("costType") Integer costType);
    
    @Select("SELECT SUM(amount) FROM finance_cost_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period}")
    BigDecimal sumAmountByPeriod(@Param("tenantId") Long tenantId, @Param("period") String period);
    
    @Select("SELECT SUM(amount) FROM finance_cost_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND cost_center_id = #{costCenterId} AND period = #{period}")
    BigDecimal sumAmountByCenterAndPeriod(@Param("tenantId") Long tenantId, @Param("costCenterId") Long costCenterId, @Param("period") String period);
    
    @Select("SELECT SUM(unallocated_amount) FROM finance_cost_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND cost_center_id = #{costCenterId}")
    BigDecimal sumUnallocatedByCenter(@Param("tenantId") Long tenantId, @Param("costCenterId") Long costCenterId);
}