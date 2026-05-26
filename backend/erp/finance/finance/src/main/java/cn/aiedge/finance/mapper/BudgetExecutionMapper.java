package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.BudgetExecution;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface BudgetExecutionMapper extends BaseMapper<BudgetExecution> {
    
    @Select("SELECT * FROM finance_budget_execution WHERE tenant_id = #{tenantId} AND deleted = 0 AND budget_id = #{budgetId} ORDER BY execution_date DESC")
    List<BudgetExecution> listByBudgetId(@Param("tenantId") Long tenantId, @Param("budgetId") Long budgetId);
    
    @Select("SELECT * FROM finance_budget_execution WHERE tenant_id = #{tenantId} AND deleted = 0 AND budget_item_id = #{budgetItemId} ORDER BY execution_date DESC")
    List<BudgetExecution> listByBudgetItemId(@Param("tenantId") Long tenantId, @Param("budgetItemId") Long budgetItemId);
    
    @Select("SELECT SUM(amount) FROM finance_budget_execution WHERE tenant_id = #{tenantId} AND deleted = 0 AND budget_item_id = #{budgetItemId}")
    BigDecimal sumAmountByBudgetItemId(@Param("tenantId") Long tenantId, @Param("budgetItemId") Long budgetItemId);
    
    @Select("SELECT SUM(amount) FROM finance_budget_execution WHERE tenant_id = #{tenantId} AND deleted = 0 AND budget_item_id = #{budgetItemId} AND period = #{period}")
    BigDecimal sumAmountByBudgetItemIdAndPeriod(@Param("tenantId") Long tenantId, @Param("budgetItemId") Long budgetItemId, @Param("period") String period);
}