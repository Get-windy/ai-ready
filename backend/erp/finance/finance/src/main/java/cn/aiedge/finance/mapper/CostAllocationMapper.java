package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.CostAllocation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CostAllocationMapper extends BaseMapper<CostAllocation> {
    
    @Select("SELECT * FROM finance_cost_allocation WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period} ORDER BY allocation_no")
    List<CostAllocation> listByPeriod(@Param("tenantId") Long tenantId, @Param("period") String period);
    
    @Select("SELECT * FROM finance_cost_allocation WHERE tenant_id = #{tenantId} AND deleted = 0 AND from_center_id = #{fromCenterId} ORDER BY allocation_date DESC")
    List<CostAllocation> listByFromCenter(@Param("tenantId") Long tenantId, @Param("fromCenterId") Long fromCenterId);
    
    @Select("SELECT * FROM finance_cost_allocation WHERE tenant_id = #{tenantId} AND deleted = 0 AND to_center_id = #{toCenterId} ORDER BY allocation_date DESC")
    List<CostAllocation> listByToCenter(@Param("tenantId") Long tenantId, @Param("toCenterId") Long toCenterId);
    
    @Select("SELECT SUM(allocated_amount) FROM finance_cost_allocation WHERE tenant_id = #{tenantId} AND deleted = 0 AND to_center_id = #{toCenterId} AND period = #{period}")
    BigDecimal sumAllocatedByToCenterAndPeriod(@Param("tenantId") Long tenantId, @Param("toCenterId") Long toCenterId, @Param("period") String period);
}