package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.BudgetAdjustment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BudgetAdjustmentMapper extends BaseMapper<BudgetAdjustment> {
    
    @Select("SELECT * FROM finance_budget_adjustment WHERE tenant_id = #{tenantId} AND deleted = 0 AND budget_id = #{budgetId} ORDER BY adjustment_date DESC")
    List<BudgetAdjustment> listByBudgetId(@Param("tenantId") Long tenantId, @Param("budgetId") Long budgetId);
    
    @Select("SELECT * FROM finance_budget_adjustment WHERE tenant_id = #{tenantId} AND deleted = 0 AND status = #{status} ORDER BY adjustment_date DESC")
    List<BudgetAdjustment> listByStatus(@Param("tenantId") Long tenantId, @Param("status") Integer status);
    
    @Select("SELECT MAX(adjustment_no) FROM finance_budget_adjustment WHERE tenant_id = #{tenantId}")
    String getMaxAdjustmentNo(@Param("tenantId") Long tenantId);
}